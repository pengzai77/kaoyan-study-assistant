package com.kaoyan.studyassistant.domain.summarizer

import com.kaoyan.studyassistant.data.local.entity.DailyReview
import com.kaoyan.studyassistant.data.local.entity.StudySession

data class SummaryResult(
    val topKeywords: List<String>,
    val commonProblems: List<String>,
    val topSubjects: List<Pair<String, Long>>,
    val dailyDurations: Map<String, Long>,
    val topTomorrowGoals: List<String>,
    val naturalLanguageSummary: String,
    val totalStudyMillis: Long,
    val coverDays: Int,
    val strengths: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val source: SummarySource = SummarySource.RULE,
    val warningMessage: String? = null
)

enum class SummarySource {
    RULE,
    AI,
    HYBRID_FALLBACK
}

interface LocalSummarizer {
    suspend fun summarize(
        reviews: List<DailyReview>,
        sessions: List<StudySession>
    ): SummaryResult
}
