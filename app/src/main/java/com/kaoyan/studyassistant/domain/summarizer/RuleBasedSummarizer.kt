package com.kaoyan.studyassistant.domain.summarizer

import com.kaoyan.studyassistant.data.local.entity.DailyReview
import com.kaoyan.studyassistant.data.local.entity.StudySession
import com.kaoyan.studyassistant.util.DateUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleBasedSummarizer @Inject constructor() : LocalSummarizer {

    private val stopWords = setOf(
        "今天", "明天", "昨天", "学习", "计划", "内容", "问题", "开始", "继续", "完成", "感觉", "觉得",
        "需要", "应该", "这个", "那个", "已经", "还是", "因为", "所以", "然后", "比较", "一个"
    )

    private val problemKeywords = listOf(
        "拖延", "分心", "走神", "卡住", "不会", "理解不了", "记不住", "背了又忘",
        "做题慢", "正确率低", "状态差", "没做完", "没完成", "焦虑", "困", "没时间"
    )

    override suspend fun summarize(
        reviews: List<DailyReview>,
        sessions: List<StudySession>
    ): SummaryResult {
        val sessionNotes = sessions.mapNotNull { it.note.takeIf(String::isNotBlank) }
        val reviewTexts = reviews.flatMap {
            listOf(it.completedContent, it.problems, it.tomorrowPlan, it.extraNotes)
        }.filter { it.isNotBlank() }
        val allText = (sessionNotes + reviewTexts).joinToString(" ")

        val topKeywords = extractKeywords(allText, topN = 10)
        val commonProblems = extractProblems(sessionNotes, reviews)
        val topTomorrowGoals = extractKeywords(reviews.joinToString(" ") { it.tomorrowPlan }, topN = 5)
        val topSubjects = sessions
            .groupBy { it.subjectName.ifBlank { "未分类" } }
            .mapValues { (_, list) -> list.sumOf { it.durationMillis } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }
        val dailyDurations = sessions
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.durationMillis } }
        val totalStudyMillis = sessions.sumOf { it.durationMillis }
        val coverDays = sessions.map { it.date }.distinct().size

        val naturalLanguageSummary = generateNaturalSummary(
            sessionNotes = sessionNotes,
            reviews = reviews,
            commonProblems = commonProblems,
            totalStudyMillis = totalStudyMillis,
            coverDays = coverDays
        )

        return SummaryResult(
            topKeywords = topKeywords,
            commonProblems = commonProblems,
            topSubjects = topSubjects,
            dailyDurations = dailyDurations,
            topTomorrowGoals = topTomorrowGoals,
            naturalLanguageSummary = naturalLanguageSummary,
            totalStudyMillis = totalStudyMillis,
            coverDays = coverDays,
            strengths = emptyList(),
            suggestions = emptyList(),
            source = SummarySource.RULE
        )
    }

    private fun extractKeywords(text: String, topN: Int): List<String> {
        if (text.isBlank()) return emptyList()
        return text.split(Regex("[，。！？、；：\\s,.!?;:\\n]+"))
            .map { it.trim() }
            .filter { it.length in 2..8 }
            .filter { it !in stopWords }
            .filter { token -> token.any { ch -> ch.code in 0x4E00..0x9FFF } }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(topN)
            .map { it.key }
    }

    private fun extractProblems(
        sessionNotes: List<String>,
        reviews: List<DailyReview>
    ): List<String> {
        val text = buildString {
            append(sessionNotes.joinToString(" "))
            append(' ')
            append(reviews.joinToString(" ") { "${it.problems} ${it.extraNotes}" })
        }

        val matched = problemKeywords
            .mapNotNull { keyword ->
                val count = text.split(keyword).size - 1
                if (count > 0) keyword to count else null
            }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }

        return if (matched.isNotEmpty()) {
            matched
        } else {
            extractKeywords(text, topN = 4)
        }
    }

    private fun generateNaturalSummary(
        sessionNotes: List<String>,
        reviews: List<DailyReview>,
        commonProblems: List<String>,
        totalStudyMillis: Long,
        coverDays: Int
    ): String {
        val latestSessionNote = sessionNotes.firstOrNull()
        val latestReview = reviews.maxByOrNull { it.date }
        val coreProblem = commonProblems.firstOrNull()
        val tomorrowPlan = latestReview?.tomorrowPlan?.takeIf { it.isNotBlank() }
        val totalDuration = DateUtils.formatDuration(totalStudyMillis)

        if (latestSessionNote == null && latestReview == null) {
            return "这段时间的文字记录还比较少，目前只能从时长上看到你累计学习了 $totalDuration、覆盖 $coverDays 天；如果想让总结更贴近实际状态，建议多写学习备注和复盘内容。"
        }

        return buildString {
            append("从这段时间留下的文字记录看，")
            if (coreProblem != null) {
                append("你反复卡住的点更像是“$coreProblem”。")
            } else {
                append("你当前更值得关注的是执行过程里的真实阻碍，而不是单纯的时长多少。")
            }

            latestSessionNote?.let {
                append("最近的学习备注里提到：${it.take(40)}。")
            }

            latestReview?.let {
                if (it.problems.isNotBlank()) {
                    append("最近复盘里也写到：${it.problems.take(40)}。")
                }
                if (!tomorrowPlan.isNullOrBlank()) {
                    append("你自己给出的下一步重点是：${tomorrowPlan.take(36)}。")
                }
            }

            append("时长上这段时间累计约 $totalDuration、覆盖 $coverDays 天，但这些只适合作为辅助参考。")
        }
    }
}
