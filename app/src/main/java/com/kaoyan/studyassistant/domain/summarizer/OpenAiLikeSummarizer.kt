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
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiLikeSummarizer @Inject constructor(
    private val apiFactory: OpenAiLikeApiFactory,
    private val promptBuilder: SummaryPromptBuilder,
    private val jsonParser: AiSummaryJsonParser,
    private val gson: Gson
) {
    suspend fun summarize(
        settings: AppPreferences.AiSettings,
        rangeLabel: String,
        startDate: String,
        endDate: String,
        ruleResult: SummaryResult,
        reviews: List<DailyReview>,
        sessions: List<StudySession>,
        goalSettings: GoalSettings?
    ): AiSummaryOutput {
        require(settings.apiKey.isNotBlank()) { "请先填写 API Key" }

        val prompt = promptBuilder.build(
            rangeLabel = rangeLabel,
            startDate = startDate,
            endDate = endDate,
            ruleResult = ruleResult,
            reviews = reviews,
            sessions = sessions,
            goalSettings = goalSettings
        )

        return executeSummaryRequest(
            settings = settings,
            systemPrompt = prompt.systemPrompt,
            userPrompt = prompt.userPrompt,
            responseFormatEnabled = AiRequestPolicy.responseFormatOrNull(settings) != null,
            allowJsonModeRetry = true
        )
    }

    private suspend fun executeSummaryRequest(
        settings: AppPreferences.AiSettings,
        systemPrompt: String,
        userPrompt: String,
        responseFormatEnabled: Boolean,
        allowJsonModeRetry: Boolean
    ): AiSummaryOutput {
        val request = OpenAiLikeChatRequest(
            model = settings.model,
            messages = listOf(
                OpenAiLikeMessage(role = "system", content = systemPrompt),
                OpenAiLikeMessage(role = "user", content = userPrompt)
            ),
            temperature = 0.2,
            responseFormat = if (responseFormatEnabled) AiRequestPolicy.responseFormatOrNull(settings) else null,
            stream = false
        )

        val response = apiFactory
            .createService(settings.baseUrl, settings.apiKey, settings.timeoutSeconds)
            .createChatCompletion(request)

        if (!response.isSuccessful) {
            val errorMessage = extractErrorMessage(response)
            if (allowJsonModeRetry && AiRequestPolicy.shouldRetryWithoutJsonMode(errorMessage, responseFormatEnabled)) {
                return executeSummaryRequest(
                    settings = settings,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    responseFormatEnabled = false,
                    allowJsonModeRetry = false
                )
            }
            throw IllegalStateException(errorMessage)
        }

        // 获取 content 字段，并清洗掉 <think>...</think> 等思考过程标签
        // thinking_content 字段（MiniMax 专有）已在 DTO 层独立映射，不会混入 content
        val rawContent = response.body()
            ?.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?.trim()
            .orEmpty()

        val content = AiResponseCleaner.clean(rawContent)

        if (content.isBlank()) {
            throw IllegalStateException("AI 没有返回有效内容")
        }

        return try {
            jsonParser.parse(content)
        } catch (e: Exception) {
            if (allowJsonModeRetry && AiRequestPolicy.shouldRetryWithoutJsonMode(e.message.orEmpty(), responseFormatEnabled)) {
                executeSummaryRequest(
                    settings = settings,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    responseFormatEnabled = false,
                    allowJsonModeRetry = false
                )
            } else {
                throw e
            }
        }
    }

    private fun extractErrorMessage(response: Response<*>): String {
        val rawBody = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
        val parsed = runCatching {
            gson.fromJson(rawBody, OpenAiLikeErrorEnvelope::class.java)?.error?.message
        }.getOrNull()

        if (!parsed.isNullOrBlank()) {
            return "HTTP ${response.code()} - $parsed"
        }

        val bodySnippet = rawBody
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(180)

        return if (bodySnippet.isNotBlank()) {
            "HTTP ${response.code()} ${response.message()} - $bodySnippet"
        } else {
            "HTTP ${response.code()} ${response.message()}"
        }
    }
}
