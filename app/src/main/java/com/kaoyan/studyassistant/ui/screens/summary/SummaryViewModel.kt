package com.kaoyan.studyassistant.ui.screens.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.local.entity.DailyReview
import com.kaoyan.studyassistant.data.local.entity.GoalSettings
import com.kaoyan.studyassistant.data.local.entity.StudySession
import com.kaoyan.studyassistant.data.repository.DailyReviewRepository
import com.kaoyan.studyassistant.data.repository.GoalSettingsRepository
import com.kaoyan.studyassistant.data.repository.StudySessionRepository
import com.kaoyan.studyassistant.data.repository.SummaryChatRepository
import com.kaoyan.studyassistant.domain.summarizer.OpenAiLikeSummarizer
import com.kaoyan.studyassistant.domain.summarizer.RuleBasedSummarizer
import com.kaoyan.studyassistant.domain.summarizer.SummaryChatAssistant
import com.kaoyan.studyassistant.domain.summarizer.SummaryChatMessage
import com.kaoyan.studyassistant.domain.summarizer.SummaryChatRole
import com.kaoyan.studyassistant.domain.summarizer.SummaryConversationContext
import com.kaoyan.studyassistant.domain.summarizer.SummaryResult
import com.kaoyan.studyassistant.domain.summarizer.SummarySource
import com.kaoyan.studyassistant.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class SummaryRange { LAST_7_DAYS, LAST_30_DAYS, ALL }
enum class SummaryTab { SUMMARY, CHAT }

data class SummaryDataFootprint(
    val sessionCount: Int = 0,
    val reviewCount: Int = 0,
    val noteCount: Int = 0
) {
    fun confidenceHint(): String {
        return when {
            sessionCount + reviewCount >= 12 && noteCount >= 3 -> "本次总结参考数据比较充足，判断会更稳一些。"
            sessionCount + reviewCount >= 6 -> "本次总结有一定数据基础，适合结合你自己的体感一起判断。"
            else -> "当前样本偏少，这次总结更适合当作提醒，不建议过度解读。"
        }
    }
}

data class SummaryUiState(
    val range: SummaryRange = SummaryRange.LAST_7_DAYS,
    val lastGeneratedRange: SummaryRange? = null,
    val selectedTab: SummaryTab = SummaryTab.SUMMARY,
    val result: SummaryResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val chatMessages: List<SummaryChatMessage> = emptyList(),
    val isChatLoading: Boolean = false,
    val chatErrorMessage: String? = null,
    val generatedAtMillis: Long? = null,
    val dataFootprint: SummaryDataFootprint = SummaryDataFootprint(),
    val problemEvidence: Map<String, List<String>> = emptyMap(),
    val quickQuestions: List<String> = defaultQuickQuestions()
)

private data class SummaryRangeMeta(
    val label: String,
    val startDate: String,
    val endDate: String
)

private data class ContextInsights(
    val footprint: SummaryDataFootprint,
    val problemEvidence: Map<String, List<String>>
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val reviewRepository: DailyReviewRepository,
    private val sessionRepository: StudySessionRepository,
    private val goalSettingsRepository: GoalSettingsRepository,
    private val summarizer: RuleBasedSummarizer,
    private val appPreferences: AppPreferences,
    private val aiSummarizer: OpenAiLikeSummarizer,
    private val chatAssistant: SummaryChatAssistant,
    private val summaryChatRepository: SummaryChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    private var latestConversationContext: SummaryConversationContext? = null

    init {
        viewModelScope.launch {
            val cache = withContext(Dispatchers.IO) {
                appPreferences.summaryCache.first()
            }
            cache?.let { c ->
                val cachedRange = SummaryRange.entries.firstOrNull { it.name == c.rangeName }
                    ?: SummaryRange.LAST_7_DAYS
                _uiState.update { current ->
                    current.copy(
                        range = cachedRange,
                        lastGeneratedRange = cachedRange,
                        result = c.result,
                        generatedAtMillis = c.generatedAtMillis,
                        quickQuestions = buildQuickQuestions(c.result)
                    )
                }
                hydrateInsightsForRange(cachedRange, c.result)
            }
        }

        viewModelScope.launch {
            summaryChatRepository.observeConversation().collect { messages ->
                _uiState.update { current -> current.copy(chatMessages = messages) }
            }
        }
    }

    fun setRange(range: SummaryRange) {
        _uiState.update {
            it.copy(
                range = range,
                errorMessage = null,
                chatErrorMessage = null,
                dataFootprint = if (it.lastGeneratedRange == range) it.dataFootprint else SummaryDataFootprint(),
                problemEvidence = if (it.lastGeneratedRange == range) it.problemEvidence else emptyMap()
            )
        }
        if (_uiState.value.lastGeneratedRange == range && _uiState.value.result != null) {
            hydrateInsightsForRange(range, _uiState.value.result)
        }
    }

    fun setTab(tab: SummaryTab) {
        _uiState.update { it.copy(selectedTab = tab, chatErrorMessage = null) }
    }

    fun generateSummary() {
        val targetRange = _uiState.value.range
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    chatErrorMessage = null
                )
            }

            try {
                val generatedAt = System.currentTimeMillis()
                val result = withContext(Dispatchers.IO) {
                    val rangeMeta = buildRangeMeta(targetRange)
                    val reviews = reviewRepository.getReviewsBetweenDatesSync(rangeMeta.startDate, rangeMeta.endDate)
                    val sessions = sessionRepository.getSessionsBetweenDatesSync(rangeMeta.startDate, rangeMeta.endDate)
                    val goalSettings = goalSettingsRepository.getGoalSettingsSync()
                    val insights = buildContextInsights(reviews, sessions, null)

                    if (reviews.isEmpty() && sessions.isEmpty()) {
                        latestConversationContext = null
                        appPreferences.clearSummaryCache()
                        return@withContext GenerateSummaryResult.Empty(
                            range = targetRange,
                            insights = insights
                        )
                    }

                    val ruleResult = summarizer.summarize(reviews, sessions)
                    val aiSettings = appPreferences.aiSettings.first()
                    val finalResult = enhanceWithAiIfNeeded(
                        ruleResult = ruleResult,
                        aiSettings = aiSettings,
                        rangeMeta = rangeMeta,
                        reviews = reviews,
                        sessions = sessions,
                        goalSettings = goalSettings
                    )
                    val resolvedInsights = buildContextInsights(reviews, sessions, finalResult)

                    latestConversationContext = SummaryConversationContext(
                        rangeLabel = rangeMeta.label,
                        startDate = rangeMeta.startDate,
                        endDate = rangeMeta.endDate,
                        result = finalResult,
                        reviews = reviews,
                        sessions = sessions,
                        goalSettings = goalSettings
                    )
                    appPreferences.saveSummaryCache(targetRange.name, finalResult, generatedAt)

                    GenerateSummaryResult.Success(
                        range = targetRange,
                        summaryResult = finalResult,
                        generatedAtMillis = generatedAt,
                        insights = resolvedInsights
                    )
                }

                when (result) {
                    is GenerateSummaryResult.Empty -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                result = null,
                                lastGeneratedRange = result.range,
                                generatedAtMillis = null,
                                errorMessage = "这个范围内还没有足够的数据，记录后再来生成总结。",
                                dataFootprint = result.insights.footprint,
                                problemEvidence = emptyMap(),
                                quickQuestions = defaultQuickQuestions()
                            )
                        }
                    }
                    is GenerateSummaryResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                result = result.summaryResult,
                                lastGeneratedRange = result.range,
                                generatedAtMillis = result.generatedAtMillis,
                                errorMessage = null,
                                dataFootprint = result.insights.footprint,
                                problemEvidence = result.insights.problemEvidence,
                                quickQuestions = buildQuickQuestions(result.summaryResult)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                latestConversationContext = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "生成总结失败：${e.message ?: "未知错误"}"
                    )
                }
            }
        }
    }

    fun askQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val aiSettings = withContext(Dispatchers.IO) {
                appPreferences.aiSettings.first()
            }
            if (!aiSettings.enabled || aiSettings.apiKey.isBlank()) {
                _uiState.update {
                    it.copy(chatErrorMessage = "对话功能需要先在设置中启用 AI 增强并填写 API Key。")
                }
                return@launch
            }

            val context = withContext(Dispatchers.IO) {
                resolveConversationContext()
            }

            _uiState.update {
                it.copy(
                    isChatLoading = true,
                    chatErrorMessage = null,
                    selectedTab = SummaryTab.CHAT
                )
            }

            try {
                val historyBeforeQuestion = _uiState.value.chatMessages
                summaryChatRepository.addMessage(
                    role = SummaryChatRole.USER,
                    content = trimmed,
                    summaryRange = _uiState.value.lastGeneratedRange?.name
                )

                val answer = withContext(Dispatchers.IO) {
                    chatAssistant.ask(
                        settings = aiSettings,
                        context = context,
                        history = historyBeforeQuestion,
                        question = trimmed
                    )
                }

                summaryChatRepository.addMessage(
                    role = SummaryChatRole.ASSISTANT,
                    content = answer,
                    summaryRange = _uiState.value.lastGeneratedRange?.name
                )

                _uiState.update { it.copy(isChatLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isChatLoading = false,
                        chatErrorMessage = e.message ?: "提问失败"
                    )
                }
            }
        }
    }

    fun deleteChatMessage(messageId: Long) {
        viewModelScope.launch {
            summaryChatRepository.deleteMessage(messageId)
        }
    }

    fun clearChatConversation() {
        viewModelScope.launch {
            summaryChatRepository.clearConversation()
        }
    }

    private suspend fun resolveConversationContext(): SummaryConversationContext {
        latestConversationContext?.let { return it }

        val currentState = _uiState.value
        val rangeMeta = buildRangeMeta(currentState.range)
        val reviews = reviewRepository.getReviewsBetweenDatesSync(rangeMeta.startDate, rangeMeta.endDate)
        val sessions = sessionRepository.getSessionsBetweenDatesSync(rangeMeta.startDate, rangeMeta.endDate)
        val goalSettings = goalSettingsRepository.getGoalSettingsSync()
        val result = currentState.result ?: buildFallbackSummaryResult(reviews, sessions)

        return SummaryConversationContext(
            rangeLabel = rangeMeta.label,
            startDate = rangeMeta.startDate,
            endDate = rangeMeta.endDate,
            result = result,
            reviews = reviews,
            sessions = sessions,
            goalSettings = goalSettings
        ).also {
            latestConversationContext = it
        }
    }

    private suspend fun buildFallbackSummaryResult(
        reviews: List<DailyReview>,
        sessions: List<StudySession>
    ): SummaryResult {
        return if (reviews.isNotEmpty() || sessions.isNotEmpty()) {
            summarizer.summarize(reviews, sessions)
        } else {
            SummaryResult(
                topKeywords = emptyList(),
                commonProblems = emptyList(),
                topSubjects = emptyList(),
                dailyDurations = emptyMap(),
                topTomorrowGoals = emptyList(),
                naturalLanguageSummary = "当前还没有生成智能总结，请直接围绕你的问题进行对话。",
                totalStudyMillis = 0L,
                coverDays = 0,
                source = SummarySource.RULE
            )
        }
    }

    private suspend fun enhanceWithAiIfNeeded(
        ruleResult: SummaryResult,
        aiSettings: AppPreferences.AiSettings,
        rangeMeta: SummaryRangeMeta,
        reviews: List<DailyReview>,
        sessions: List<StudySession>,
        goalSettings: GoalSettings?
    ): SummaryResult {
        if (!aiSettings.enabled || aiSettings.apiKey.isBlank()) {
            return ruleResult
        }

        return try {
            val aiOutput = aiSummarizer.summarize(
                settings = aiSettings,
                rangeLabel = rangeMeta.label,
                startDate = rangeMeta.startDate,
                endDate = rangeMeta.endDate,
                ruleResult = ruleResult,
                reviews = reviews,
                sessions = sessions,
                goalSettings = goalSettings
            )

            val fallbackFields = mutableListOf<String>()
            val mergedProblems = aiOutput.problems.ifEmpty {
                fallbackFields += "problems"
                ruleResult.commonProblems
            }
            val mergedTomorrowFocus = aiOutput.tomorrowFocus.ifEmpty {
                fallbackFields += "tomorrowFocus"
                ruleResult.topTomorrowGoals
            }
            val mergedStrengths = aiOutput.strengths.ifEmpty {
                if (ruleResult.strengths.isNotEmpty()) fallbackFields += "strengths"
                ruleResult.strengths
            }
            val mergedSuggestions = aiOutput.suggestions.ifEmpty {
                if (ruleResult.suggestions.isNotEmpty()) fallbackFields += "suggestions"
                ruleResult.suggestions
            }

            ruleResult.copy(
                naturalLanguageSummary = aiOutput.summary,
                commonProblems = mergedProblems,
                topTomorrowGoals = mergedTomorrowFocus,
                strengths = mergedStrengths,
                suggestions = mergedSuggestions,
                source = SummarySource.AI,
                warningMessage = buildPartialFallbackWarning(fallbackFields)
            )
        } catch (e: Exception) {
            if (aiSettings.fallbackToRule) {
                ruleResult.copy(
                    source = SummarySource.HYBRID_FALLBACK,
                    warningMessage = buildAiFailureWarning(e)
                )
            } else {
                throw e
            }
        }
    }

    private fun hydrateInsightsForRange(range: SummaryRange, result: SummaryResult?) {
        viewModelScope.launch {
            val insights = withContext(Dispatchers.IO) {
                val rangeMeta = buildRangeMeta(range)
                val reviews = reviewRepository.getReviewsBetweenDatesSync(rangeMeta.startDate, rangeMeta.endDate)
                val sessions = sessionRepository.getSessionsBetweenDatesSync(rangeMeta.startDate, rangeMeta.endDate)
                buildContextInsights(reviews, sessions, result)
            }
            _uiState.update {
                if (it.lastGeneratedRange == range || it.range == range) {
                    it.copy(
                        dataFootprint = insights.footprint,
                        problemEvidence = if (it.lastGeneratedRange == range) insights.problemEvidence else emptyMap()
                    )
                } else {
                    it
                }
            }
        }
    }

    private fun buildContextInsights(
        reviews: List<DailyReview>,
        sessions: List<StudySession>,
        result: SummaryResult?
    ): ContextInsights {
        val footprint = SummaryDataFootprint(
            sessionCount = sessions.size,
            reviewCount = reviews.size,
            noteCount = sessions.count { it.note.isNotBlank() }
        )
        val evidence = result?.commonProblems
            ?.associateWith { problem -> collectProblemEvidence(problem, reviews, sessions) }
            ?.filterValues { it.isNotEmpty() }
            .orEmpty()
        return ContextInsights(
            footprint = footprint,
            problemEvidence = evidence
        )
    }

    private fun collectProblemEvidence(
        problem: String,
        reviews: List<DailyReview>,
        sessions: List<StudySession>
    ): List<String> {
        val keywords = extractProblemKeywords(problem)
        if (keywords.isEmpty()) return emptyList()

        val reviewEvidence = reviews.asReversed().mapNotNull { review ->
            listOf(
                review.problems to "复盘问题",
                review.extraNotes to "复盘备注",
                review.completedContent to "完成内容"
            ).firstNotNullOfOrNull { (content, label) ->
                content.trim().takeIf { it.isNotBlank() && matchesProblem(it, problem, keywords) }
                    ?.let { "${DateUtils.formatDisplayDate(review.date)} · $label：${it.take(48)}${if (it.length > 48) "…" else ""}" }
            }
        }

        val sessionEvidence = sessions.asReversed().mapNotNull { session ->
            val note = session.note.trim()
            note.takeIf { it.isNotBlank() && matchesProblem(it, problem, keywords) }
                ?.let { "${DateUtils.formatDisplayDate(session.date)} · 学习备注：${it.take(48)}${if (it.length > 48) "…" else ""}" }
        }

        return (reviewEvidence + sessionEvidence)
            .distinct()
            .take(3)
    }

    private fun matchesProblem(text: String, problem: String, keywords: List<String>): Boolean {
        val normalizedText = text.lowercase()
        val normalizedProblem = problem.lowercase()
        return normalizedText.contains(normalizedProblem) || keywords.any { normalizedText.contains(it.lowercase()) }
    }

    private fun extractProblemKeywords(problem: String): List<String> {
        return problem
            .replace(Regex("[，。；：、,.!?！？()（）【】\\[\\]\\s]+"), "|")
            .split('|')
            .map { it.trim() }
            .filter { it.length >= 2 }
            .ifEmpty { listOf(problem.trim()).filter { it.length >= 2 } }
    }

    private fun buildPartialFallbackWarning(fallbackFields: List<String>): String? {
        if (fallbackFields.isEmpty()) return null
        return "AI 返回部分字段为空，已使用规则层兜底：${fallbackFields.distinct().joinToString("、")}" 
    }

    private fun buildAiFailureWarning(error: Throwable): String {
        val detail = error.message
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(140)
            ?.ifBlank { null }
            ?: (error::class.simpleName ?: "未知错误")
        return "AI 增强失败：$detail；已回退到本地总结"
    }

    private fun buildRangeMeta(range: SummaryRange): SummaryRangeMeta {
        val endDate = DateUtils.today()
        return when (range) {
            SummaryRange.LAST_7_DAYS -> SummaryRangeMeta("近7天", DateUtils.daysAgo(7), endDate)
            SummaryRange.LAST_30_DAYS -> SummaryRangeMeta("近30天", DateUtils.daysAgo(30), endDate)
            SummaryRange.ALL -> SummaryRangeMeta("全部", "2000-01-01", endDate)
        }
    }

    private sealed class GenerateSummaryResult {
        data class Empty(
            val range: SummaryRange,
            val insights: ContextInsights
        ) : GenerateSummaryResult()

        data class Success(
            val range: SummaryRange,
            val summaryResult: SummaryResult,
            val generatedAtMillis: Long,
            val insights: ContextInsights
        ) : GenerateSummaryResult()
    }
}

private fun defaultQuickQuestions(): List<String> = listOf(
    "我现在最大的问题是什么？",
    "明天最该先学什么？",
    "怎样把明日重点落到行动上？",
    "最近哪门科目最需要补？"
)

private fun buildQuickQuestions(result: SummaryResult?): List<String> {
    if (result == null) return defaultQuickQuestions()
    val subjectPrompt = result.topSubjects.firstOrNull()?.first?.let { "为什么我最近在${it}上投入最多？" }
    val problemPrompt = result.commonProblems.firstOrNull()?.let { "怎么改善“${it}”？" }
    return listOfNotNull(
        problemPrompt ?: "我现在最大的问题是什么？",
        "明天最该先学什么？",
        "怎样把明日重点落到行动上？",
        subjectPrompt ?: "最近哪门科目最需要补？"
    ).distinct()
}
