package com.kaoyan.studyassistant.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.local.dao.DateDurationSummary
import com.kaoyan.studyassistant.data.local.dao.SubjectDurationSummary
import com.kaoyan.studyassistant.data.repository.StudySessionRepository
import com.kaoyan.studyassistant.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatisticsUiState(
    val subjectDurations: List<SubjectDurationSummary> = emptyList(),
    val dateDurations: List<DateDurationSummary> = emptyList(),
    val totalMillis: Long = 0L,
    val isLoading: Boolean = true
)

/**
 * 学习统计 ViewModel
 *
 * 性能优化（v2）：
 * - 将 3 个独立 collector 合并为 combine + stateIn
 * - 同一帧内多个数据源变化只触发一次 uiState 更新
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val sessionRepository: StudySessionRepository
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = combine(
        sessionRepository.getDurationBySubject(),
        sessionRepository.getDurationByDateRange(DateUtils.daysAgo(30), DateUtils.today()),
        sessionRepository.getTotalDuration()
    ) { subjectDurations, dateDurations, totalMillis ->
        StatisticsUiState(
            subjectDurations = subjectDurations,
            dateDurations = dateDurations,
            totalMillis = totalMillis,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState()
    )
}
