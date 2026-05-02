package com.kaoyan.studyassistant.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.local.dao.DateDurationSummary
import com.kaoyan.studyassistant.data.local.entity.DailyReview
import com.kaoyan.studyassistant.data.repository.DailyReviewRepository
import com.kaoyan.studyassistant.data.repository.GoalSettingsRepository
import com.kaoyan.studyassistant.data.repository.StudySessionRepository
import com.kaoyan.studyassistant.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 考研倒计时状态
 */
data class CountdownState(
    val examDate: String = "",
    val daysLeft: Int = Int.MIN_VALUE
)

data class DashboardUiState(
    val todayTotalMillis: Long = 0L,
    val todaySubjectCount: Int = 0,
    val last7DaysDurations: List<DateDurationSummary> = emptyList(),
    val latestReview: DailyReview? = null,
    val showLatestReview: Boolean = true,
    val countdown: CountdownState = CountdownState(),
    val isLoading: Boolean = true
)

/**
 * 首页 Dashboard ViewModel
 *
 * 性能优化（v4）：
 * - 将原来 6 个独立 collector 合并为一个 combine 流
 * - 使用 stateIn + WhileSubscribed 自动管理生命周期
 * - 同一帧内多个数据源变化只触发一次 uiState 更新
 * - 消除碎片化 recomposition
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionRepository: StudySessionRepository,
    private val reviewRepository: DailyReviewRepository,
    private val goalSettingsRepository: GoalSettingsRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val today = DateUtils.today()
    private val weekAgo = DateUtils.daysAgo(6)

    val uiState: StateFlow<DashboardUiState> = combine(
        sessionRepository.getTodayTotalDuration(today),
        sessionRepository.getSessionsByDate(today).map { sessions ->
            sessions.map { it.subjectName }.distinct().size
        },
        sessionRepository.getDurationByDateRange(weekAgo, today),
        reviewRepository.getAllReviews().map { it.firstOrNull() },
        goalSettingsRepository.getGoalSettings().map { goal ->
            if (goal == null || goal.examDate.isBlank()) {
                CountdownState(examDate = "", daysLeft = Int.MIN_VALUE)
            } else {
                val days = DateUtils.daysBetween(today, goal.examDate)
                CountdownState(examDate = goal.examDate, daysLeft = days)
            }
        },
        appPreferences.userSettings.map { it.showLatestReview }
    ) { values ->
        // combine with 6 flows uses Array<Any?>
        @Suppress("UNCHECKED_CAST")
        DashboardUiState(
            todayTotalMillis = values[0] as Long,
            todaySubjectCount = values[1] as Int,
            last7DaysDurations = values[2] as List<DateDurationSummary>,
            latestReview = values[3] as DailyReview?,
            countdown = values[4] as CountdownState,
            showLatestReview = values[5] as Boolean,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )
}
