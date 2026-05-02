package com.kaoyan.studyassistant.domain.summarizer

import com.kaoyan.studyassistant.data.local.entity.DailyReview
import com.kaoyan.studyassistant.data.local.entity.GoalSettings
import com.kaoyan.studyassistant.data.local.entity.StudySession
import com.kaoyan.studyassistant.util.DateUtils
import javax.inject.Inject
import javax.inject.Singleton

data class SummaryPrompt(
    val systemPrompt: String,
    val userPrompt: String
)

@Singleton
class SummaryPromptBuilder @Inject constructor() {

    fun build(
        rangeLabel: String,
        startDate: String,
        endDate: String,
        ruleResult: SummaryResult,
        reviews: List<DailyReview>,
        sessions: List<StudySession>,
        goalSettings: GoalSettings?
    ): SummaryPrompt {
        val sessionNoteExcerpts = sessions
            .sortedByDescending { it.startTime }
            .mapNotNull { session ->
                session.note.takeIf { it.isNotBlank() }?.let { note ->
                    "${session.date} ${session.subjectName.ifBlank { "未分类" }}：${note.take(56)}"
                }
            }
            .take(8)
            .ifEmpty { listOf("暂无学习备注") }

        val reviewExcerpts = reviews
            .sortedByDescending { it.date }
            .take(6)
            .map { review ->
                buildString {
                    append("${review.date}：")
                    append("完成 ${review.completedContent.take(42).ifBlank { "未写" }}；")
                    append("问题 ${review.problems.take(42).ifBlank { "未写" }}；")
                    append("明日 ${review.tomorrowPlan.take(32).ifBlank { "未写" }}")
                    if (review.extraNotes.isNotBlank()) {
                        append("；补充 ${review.extraNotes.take(30)}")
                    }
                }
            }
            .ifEmpty { listOf("暂无每日复盘") }

        val sessionTextSize = sessions.count { it.note.isNotBlank() }
        val subjectSummary = sessions
            .groupBy { it.subjectName.ifBlank { "未分类" } }
            .mapValues { (_, value) -> value.sumOf { it.durationMillis } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .joinToString(separator = "\n") { (subject, duration) ->
                "- $subject：${DateUtils.formatDuration(duration)}"
            }
            .ifBlank { "- 暂无" }

        val systemPrompt = """
            你要根据用户留下的文字记录，生成一份阶段性学习总结。
            请优先依据这些文字信息来判断：
            1. 学习完成后的备注、说明、吐槽、反思。
            2. 每日复盘里的完成情况、问题、明日计划、补充说明。
            学习时长、科目时长、覆盖天数这些统计，只能作为次级参考，不能盖过文字记录本身。

            你必须只输出一个合法 JSON 对象，不要输出 Markdown、代码块或额外解释。
            只根据输入判断，不要编造没有出现过的事实。

            JSON 结构固定为：
            {
              "summary": "字符串",
              "strengths": ["字符串"],
              "problems": ["字符串"],
              "suggestions": ["字符串"],
              "tomorrowFocus": ["字符串"],
              "confidence": "高|中|低",
              "tone": "自然|直接|谨慎"
            }

            其中：
            - summary 必须非空。
            - problems 和 tomorrowFocus 尽量给出，但如果证据不够可以少写。
            - strengths 和 suggestions 允许为空，不要为了凑字段而硬写。
        """.trimIndent()

        val userPrompt = """
            总结范围：
            - 范围：$rangeLabel
            - 统计区间：$startDate 到 $endDate

            学习备注与记录说明：
            ${sessionNoteExcerpts.joinToString(separator = "\n") { "- $it" }}

            每日复盘内容：
            ${reviewExcerpts.joinToString(separator = "\n") { "- $it" }}

            规则层基于文本的初步判断：
            - 总结：${ruleResult.naturalLanguageSummary}
            - 常见问题：${ruleResult.commonProblems.joinToString("、").ifBlank { "暂无" }}
            - 明日重点：${ruleResult.topTomorrowGoals.joinToString("、").ifBlank { "暂无" }}

            次级统计参考：
            - 总学习时长：${DateUtils.formatDuration(ruleResult.totalStudyMillis)}
            - 覆盖天数：${ruleResult.coverDays}
            - 有备注的学习记录数：$sessionTextSize
            - 主要投入科目：
            $subjectSummary
            - 目标背景：
            ${buildGoalContext(goalSettings)}

            请优先根据文字记录里的真实困难、执行情况、反思和计划来写总结。
            时长和科目投入只能作为辅助证据。
        """.trimIndent()

        return SummaryPrompt(systemPrompt = systemPrompt, userPrompt = userPrompt)
    }

    private fun buildGoalContext(goalSettings: GoalSettings?): String {
        if (goalSettings == null) {
            return "- 目标院校：未设置\n- 目标专业：未设置\n- 考试日期：未设置"
        }

        val examLine = goalSettings.examDate.takeIf { it.isNotBlank() }?.let { examDate ->
            val daysLeft = DateUtils.daysBetween(DateUtils.today(), examDate)
            val countdown = when {
                daysLeft > 0 -> "距考试约 $daysLeft 天"
                daysLeft == 0 -> "今天就是考试日"
                else -> "考试日期已过"
            }
            "- 考试日期：$examDate（$countdown）"
        } ?: "- 考试日期：未设置"

        return buildString {
            append("- 目标院校：${goalSettings.targetSchool.ifBlank { "未设置" }}\n")
            append("- 目标专业：${goalSettings.targetMajor.ifBlank { "未设置" }}\n")
            append(examLine)
            if (goalSettings.weeklyGoalMinutes > 0) {
                append("\n- 每周目标：${goalSettings.weeklyGoalMinutes} 分钟")
            }
        }
    }
}
