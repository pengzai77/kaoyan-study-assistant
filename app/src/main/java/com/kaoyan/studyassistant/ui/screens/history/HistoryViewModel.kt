package com.kaoyan.studyassistant.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.backup.AutoBackupCoordinator
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.local.entity.StudySession
import com.kaoyan.studyassistant.data.repository.StudySessionRepository
import com.kaoyan.studyassistant.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistoryQuickRange(val label: String, val days: Int) {
    LAST_7("最近7天", 7),
    LAST_30("最近30天", 30),
    LAST_90("最近90天", 90),
    ALL("全部", 0)
}

data class HistoryUiState(
    val sessions: List<StudySession> = emptyList(),
    val filterStartDate: String = DateUtils.daysAgo(30),
    val filterEndDate: String = DateUtils.today(),
    val quickRange: HistoryQuickRange = HistoryQuickRange.LAST_30,
    val isLoading: Boolean = true,
    val dateRangeError: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: StudySessionRepository,
    private val appPreferences: AppPreferences,
    private val autoBackupCoordinator: AutoBackupCoordinator
) : ViewModel() {

    private val _dateRange = MutableStateFlow(Pair(DateUtils.daysAgo(30), DateUtils.today()))
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var defaultQuickRange: HistoryQuickRange = HistoryQuickRange.LAST_30

    init {
        viewModelScope.launch {
            val initialRange = toQuickRange(appPreferences.userSettings.first().historyDefaultRange)
            defaultQuickRange = initialRange
            applyQuickRange(initialRange)
        }

        viewModelScope.launch {
            appPreferences.userSettings
                .map { toQuickRange(it.historyDefaultRange) }
                .distinctUntilChanged()
                .collect { defaultQuickRange = it }
        }

        viewModelScope.launch {
            _dateRange.flatMapLatest { (start, end) ->
                sessionRepository.getSessionsBetweenDates(start, end)
            }.collect { sessions ->
                _uiState.update {
                    it.copy(
                        sessions = sessions.sortedByDescending { s -> s.startTime },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun applyQuickRange(range: HistoryQuickRange) {
        val end = DateUtils.today()
        val start = if (range.days == 0) "2000-01-01" else DateUtils.daysAgo(range.days)
        _uiState.update {
            it.copy(
                filterStartDate = start,
                filterEndDate = end,
                quickRange = range,
                isLoading = true,
                dateRangeError = null
            )
        }
        _dateRange.value = Pair(start, end)
    }

    fun updateDateFilter(startDate: String, endDate: String) {
        if (startDate > endDate) {
            _uiState.update { it.copy(dateRangeError = "开始日期不能晚于结束日期") }
            return
        }
        _uiState.update {
            it.copy(
                filterStartDate = startDate,
                filterEndDate = endDate,
                quickRange = HistoryQuickRange.ALL,
                isLoading = true,
                dateRangeError = null
            )
        }
        _dateRange.value = Pair(startDate, endDate)
    }

    fun resetFilter() = applyQuickRange(defaultQuickRange)

    fun clearDateRangeError() { _uiState.update { it.copy(dateRangeError = null) } }

    fun deleteSession(session: StudySession) {
        viewModelScope.launch {
            sessionRepository.deleteSession(session)
            autoBackupCoordinator.triggerIfEnabled()
        }
    }

    fun updateSessionNote(session: StudySession, newNote: String) {
        viewModelScope.launch {
            sessionRepository.updateSession(session.copy(note = newNote.trim()))
            autoBackupCoordinator.triggerIfEnabled()
        }
    }

    private fun toQuickRange(days: Int): HistoryQuickRange = when (days) {
        7 -> HistoryQuickRange.LAST_7
        30 -> HistoryQuickRange.LAST_30
        90 -> HistoryQuickRange.LAST_90
        else -> HistoryQuickRange.ALL
    }
}
