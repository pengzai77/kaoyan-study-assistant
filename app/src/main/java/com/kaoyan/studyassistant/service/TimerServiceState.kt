package com.kaoyan.studyassistant.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 计时器服务状态枚举
 */
enum class TimerRunState {
    IDLE,    // 空闲（未开始）
    RUNNING, // 计时中
    PAUSED   // 已暂停
}

/**
 * 番茄钟阶段枚举
 */
enum class PomodoroPhase {
    FOCUS,  // 专注阶段
    BREAK   // 休息阶段
}

/**
 * 计时器全局状态（单例，Service 与 ViewModel 共享）
 *
 * 分为两个模式：
 * - 普通正计时：使用 runState / elapsedMillis / subjectId / subjectName / startTimeMs / pausedElapsed / originalStartTimeMs
 * - 番茄钟模式：使用 pomodoroXxx 系列字段，与普通计时完全隔离
 */
object TimerServiceState {

    // ─────────────────────────────────────────────
    // 普通正计时状态
    // ─────────────────────────────────────────────

    private val _runState = MutableStateFlow(TimerRunState.IDLE)
    val runState: StateFlow<TimerRunState> = _runState.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _subjectId = MutableStateFlow(0L)
    val subjectId: StateFlow<Long> = _subjectId.asStateFlow()

    private val _subjectName = MutableStateFlow("")
    val subjectName: StateFlow<String> = _subjectName.asStateFlow()

    private val _startTimeMs = MutableStateFlow(0L)
    val startTimeMs: StateFlow<Long> = _startTimeMs.asStateFlow()

    private val _pausedElapsed = MutableStateFlow(0L)
    val pausedElapsed: StateFlow<Long> = _pausedElapsed.asStateFlow()

    private val _originalStartTimeMs = MutableStateFlow(0L)
    val originalStartTimeMs: StateFlow<Long> = _originalStartTimeMs.asStateFlow()

    // ─────────────────────────────────────────────
    // 番茄钟状态（与普通计时完全隔离）
    // ─────────────────────────────────────────────

    private val _pomodoroRunState = MutableStateFlow(TimerRunState.IDLE)
    val pomodoroRunState: StateFlow<TimerRunState> = _pomodoroRunState.asStateFlow()

    private val _pomodoroPhase = MutableStateFlow(PomodoroPhase.FOCUS)
    val pomodoroPhase: StateFlow<PomodoroPhase> = _pomodoroPhase.asStateFlow()

    private val _pomodoroRemainingMillis = MutableStateFlow(25L * 60_000L)
    val pomodoroRemainingMillis: StateFlow<Long> = _pomodoroRemainingMillis.asStateFlow()

    private val _pomodoroTargetMillis = MutableStateFlow(25L * 60_000L)
    val pomodoroTargetMillis: StateFlow<Long> = _pomodoroTargetMillis.asStateFlow()

    private val _pomodoroFocusMinutes = MutableStateFlow(25)
    val pomodoroFocusMinutes: StateFlow<Int> = _pomodoroFocusMinutes.asStateFlow()

    private val _pomodoroBreakMinutes = MutableStateFlow(5)
    val pomodoroBreakMinutes: StateFlow<Int> = _pomodoroBreakMinutes.asStateFlow()

    private val _pomodoroCompletedCount = MutableStateFlow(0)
    val pomodoroCompletedCount: StateFlow<Int> = _pomodoroCompletedCount.asStateFlow()

    private val _pomodoroFocusStartedAt = MutableStateFlow(0L)
    val pomodoroFocusStartedAt: StateFlow<Long> = _pomodoroFocusStartedAt.asStateFlow()

    /** 待归档快照：专注完成后由 Service 写入，ViewModel 监听后弹出保存弹窗 */
    private val _pomodoroPendingArchive = MutableStateFlow<PomodoroArchiveSnapshot?>(null)
    val pomodoroPendingArchive: StateFlow<PomodoroArchiveSnapshot?> = _pomodoroPendingArchive.asStateFlow()

    // ─────────────────────────────────────────────
    // 普通计时写入方法
    // ─────────────────────────────────────────────
    fun updateRunState(state: TimerRunState) { _runState.value = state }
    fun updateElapsedMillis(millis: Long) { _elapsedMillis.value = millis }
    fun updateSubject(id: Long, name: String) { _subjectId.value = id; _subjectName.value = name }
    fun updateStartTime(ms: Long) { _startTimeMs.value = ms }
    fun updatePausedElapsed(ms: Long) { _pausedElapsed.value = ms }
    fun updateOriginalStartTime(ms: Long) { _originalStartTimeMs.value = ms }

    // ─────────────────────────────────────────────
    // 番茄钟写入方法
    // ─────────────────────────────────────────────
    fun updatePomodoroRunState(state: TimerRunState) { _pomodoroRunState.value = state }
    fun updatePomodoroPhase(phase: PomodoroPhase) { _pomodoroPhase.value = phase }
    fun updatePomodoroRemainingMillis(millis: Long) { _pomodoroRemainingMillis.value = millis }
    fun updatePomodoroTargetMillis(millis: Long) { _pomodoroTargetMillis.value = millis }
    fun updatePomodoroFocusMinutes(minutes: Int) { _pomodoroFocusMinutes.value = minutes }
    fun updatePomodoroBreakMinutes(minutes: Int) { _pomodoroBreakMinutes.value = minutes }
    fun updatePomodoroCompletedCount(count: Int) { _pomodoroCompletedCount.value = count }
    fun updatePomodoroFocusStartedAt(ms: Long) { _pomodoroFocusStartedAt.value = ms }
    fun updatePomodoroPendingArchive(snapshot: PomodoroArchiveSnapshot?) {
        _pomodoroPendingArchive.value = snapshot
    }

    /** 重置普通计时 */
    fun reset() {
        _runState.value = TimerRunState.IDLE
        _elapsedMillis.value = 0L
        _subjectId.value = 0L
        _subjectName.value = ""
        _startTimeMs.value = 0L
        _pausedElapsed.value = 0L
        _originalStartTimeMs.value = 0L
    }

    /** 重置番茄钟（保留配置，清除运行状态） */
    fun resetPomodoro() {
        _pomodoroRunState.value = TimerRunState.IDLE
        _pomodoroPhase.value = PomodoroPhase.FOCUS
        _pomodoroRemainingMillis.value = _pomodoroFocusMinutes.value * 60_000L
        _pomodoroTargetMillis.value = _pomodoroFocusMinutes.value * 60_000L
        _pomodoroFocusStartedAt.value = 0L
        _pomodoroPendingArchive.value = null
    }
}

/**
 * 番茄钟专注完成后的待归档快照
 */
data class PomodoroArchiveSnapshot(
    val elapsedMillis: Long,
    val archiveStartTime: Long,
    val archiveEndTime: Long
)
