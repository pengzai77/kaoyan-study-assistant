package com.kaoyan.studyassistant.domain.summarizer

import com.google.gson.Gson
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.local.entity.DailyReview
import com.kaoyan.studyassistant.data.local.entity.GoalSettings
import com.kaoyan.studyassistant.data.local.entity.StudySession
import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeApiFactory
import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeChatRequest
import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeErrorEnvelope
import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeMessage
import com.kaoyan.studyassistant.util.DateUtils
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

data class SummaryConversationContext(
    val rangeLabel: String,
    val startDate: String,
    val endDate: String,
    val result: SummaryResult,
    val reviews: List<DailyReview>,
    val sessions: List<StudySession>,
    val goalSettings: GoalSettings?
)

@Singleton
class SummaryChatAssistant @Inject constructor(
    private val apiFactory: OpenAiLikeApiFactory,
    private val gson: Gson
) {
    suspend fun ask(
        settings: AppPreferences.AiSettings,
        context: SummaryConversationContext,
        history: List<SummaryChatMessage>,
        question: String
    ): String {
        require(settings.apiKey.isNotBlank()) { "请先在设置里填写 API Key" }

        val messages = buildList {
            add(OpenAiLikeMessage(role = "system", content = buildSystemPrompt(context)))
            history.takeLast(8).forEach { message ->
                add(
                    OpenAiLikeMessage(
                        role = if (message.role == SummaryChatRole.USER) "user" else "assistant",
                        content = message.content
                    )
                )
            }
            add(OpenAiLikeMessage(role = "user", content = buildUserPrompt(question)))
        }

        val response = apiFactory
            .createService(settings.baseUrl, settings.apiKey, settings.timeoutSeconds)
            .createChatCompletion(
                OpenAiLikeChatRequest(
                    model = settings.model,
                    messages = messages,
                    temperature = 0.55,
                    responseFormat = null,
                    stream = false
                )
            )

        if (!response.isSuccessful) {
            throw IllegalStateException(extractErrorMessage(response))
        }

        val messageResponse = response.body()
            ?.choices
            ?.firstOrNull()
            ?.message

        // 优先使用 content 字段（thinking_content 是独立的思考字段，不展示给用户）
        // 同时对 content 内容进行清洗，移除 <think>...</think> 等内联思考标签
        val rawContent = messageResponse?.content?.trim().orEmpty()
        val cleanedContent = AiResponseCleaner.clean(rawContent)

        return cleanedContent.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("AI 没有返回有效回复")
    }

    private fun buildSystemPrompt(context: SummaryConversationContext): String {
        return """
            你可以自由回答用户的问题。
            你的回答要尽量基于当前总结、学习记录、学习备注、每日复盘和已有上下文，不要胡编没有出现过的事实。
            如果上下文不够，可以明确说"根据目前这些记录，我只能大致判断……"。
            不需要固定语气，不需要固定结构，也不需要强行给建议。
            重点是围绕用户当前的问题，给出自然、清楚、真实的回答。

            当前上下文：
            - 时间范围：${context.rangeLabel}
            - 统计区间：${context.startDate} 到 ${context.endDate}
            - 当前总结：${context.result.naturalLanguageSummary}
            - 常见问题：${context.result.commonProblems.joinToString("、").ifBlank { "暂无" }}
            - 明日重点：${context.result.topTomorrowGoals.joinToString("、").ifBlank { "暂无" }}
            - 最近学习备注：
            ${buildSessionSummary(context.sessions)}
            - 最近复盘摘要：
            ${buildReviewSummary(context.reviews)}
            - 目标背景：${buildGoalSummary(context.goalSettings)}
        """.trimIndent()
    }

    private fun buildUserPrompt(question: String): String {
        return """
            用户的问题：
            $question

            请直接围绕这个问题回答。
        """.trimIndent()
    }

    private fun buildReviewSummary(reviews: List<DailyReview>): String {
        return reviews
            .sortedByDescending { it.date }
            .take(5)
            .joinToString(separator = "\n") {
                buildString {
                    append("- ${it.date}：完成 ${it.completedContent.take(30).ifBlank { "未写" }}")
                    append("；问题 ${it.problems.take(30).ifBlank { "未写" }}")
                    append("；明日 ${it.tomorrowPlan.take(24).ifBlank { "未写" }}")
                    if (it.extraNotes.isNotBlank()) {
                        append("；补充 ${it.extraNotes.take(24)}")
                    }
                }
            }
            .ifBlank { "- 暂无复盘记录" }
    }

    private fun buildSessionSummary(sessions: List<StudySession>): String {
        return sessions
            .sortedByDescending { it.startTime }
            .mapNotNull { session ->
                session.note.takeIf { it.isNotBlank() }?.let { note ->
                    "- ${session.date} ${session.subjectName.ifBlank { "未分类" }}：${note.take(36)}"
                }
            }
            .take(6)
            .ifEmpty { listOf("- 暂无学习备注") }
            .joinToString(separator = "\n")
    }

    private fun buildGoalSummary(goalSettings: GoalSettings?): String {
        if (goalSettings == null) return "未设置"
        val school = goalSettings.targetSchool.ifBlank { "未设置院校" }
        val major = goalSettings.targetMajor.ifBlank { "未设置专业" }
        val examDate = goalSettings.examDate.ifBlank { "未设置考试日期" }
        return "$school / $major / $examDate"
    }

    private fun extractErrorMessage(response: Response<*>): String {
        val rawBody = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
        val parsed = runCatching {
            gson.fromJson(rawBody, OpenAiLikeErrorEnvelope::class.java)?.error?.message
        }.getOrNull()

        if (!parsed.isNullOrBlank()) {
            return "提问失败：HTTP ${response.code()} - $parsed"
        }

        val bodySnippet = rawBody
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(160)

        return if (bodySnippet.isNotBlank()) {
            "提问失败：HTTP ${response.code()} ${response.message()} - $bodySnippet"
        } else {
            "提问失败：HTTP ${response.code()} ${response.message()}"
        }
    }
}
