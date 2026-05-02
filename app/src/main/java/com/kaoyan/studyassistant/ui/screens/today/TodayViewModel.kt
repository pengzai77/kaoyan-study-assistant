package com.kaoyan.studyassistant.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.local.entity.StudySession
import com.kaoyan.studyassistant.data.repository.StudySessionRepository
import com.kaoyan.studyassistant.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/** 按科目分组的今日学习数据 */
data class SubjectStudySummary(
    val subjectName: String,
    val totalMillis: Long,
    val sessionCount: Int,
    val latestNote: String,
    val sessions: List<StudySession>
)

data class TodayUiState(
    val selectedDate: String = DateUtils.today(),
    val subjectSummaries: List<SubjectStudySummary> = emptyList(),
    val totalMillis: Long = 0L,
    val isLoading: Boolean = true
)

/**
 * 今日学习 ViewModel（重构版）
 *
 * 改造点：
 * 1. 原版每次调用 selectDate() 都会新建一个 collect 协程，导致重复订阅内存泄漏
 * 2. 改为用 flatMapLatest 响应日期变化，保证同一时刻只有一个活跃订阅
 * 3. 使用 stateIn 将 Flow 转为 StateFlow，避免 UI 层重复触发数据库查询
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val sessionRepository: StudySessionRepository
) : ViewModel() {

    /** 当前选中日期（驱动数据流） */
    private val _selectedDate = MutableStateFlow(DateUtils.today())

    /**
     * UI 状态 StateFlow
     *
     * 数据流：_selectedDate → flatMapLatest → getSessionsByDate → map → TodayUiState
     * 当 _selectedDate 变化时，flatMapLatest 自动取消旧的数据库订阅并启动新的
     */
    val uiState: StateFlow<TodayUiState> = _selectedDate
        .flatMapLatest { date ->
            sessionRepository.getSessionsByDate(date)
                .map { sessions -> buildUiState(date, sessions) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(isLoading = true)
        )

    /** 切换查看日期 */
    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    /** 回到今天 */
    fun goToToday() {
        _selectedDate.value = DateUtils.today()
    }

    /** 构建 UI 状态（纯函数，便于测试） */
    private fun buildUiState(date: String, sessions: List<StudySession>): TodayUiState {
        val grouped = sessions.groupBy { it.subjectName.ifEmpty { "未分类" } }
        val summaries = grouped.map { (subject, list) ->
            SubjectStudySummary(
                subjectName = subject,
                totalMillis = list.sumOf { it.durationMillis },
                sessionCount = list.size,
                latestNote = list.maxByOrNull { it.startTime }?.note ?: "",
                sessions = list.sortedByDescending { it.startTime }
            )
        }.sortedByDescending { it.totalMillis }

        val total = sessions.sumOf { it.durationMillis }
        return TodayUiState(
            selectedDate = date,
            subjectSummaries = summaries,
            totalMillis = total,
            isLoading = false
        )
    }
}
