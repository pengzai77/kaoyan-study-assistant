package com.kaoyan.studyassistant.ui.screens.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.backup.AutoBackupCoordinator
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.local.entity.Subject
import com.kaoyan.studyassistant.data.local.entity.StudySession
import com.kaoyan.studyassistant.data.repository.StudySessionRepository
import com.kaoyan.studyassistant.data.repository.SubjectRepository
import com.kaoyan.studyassistant.service.PomodoroArchiveSnapshot
import com.kaoyan.studyassistant.service.PomodoroPhase
import com.kaoyan.studyassistant.service.StudyTimerService
import com.kaoyan.studyassistant.service.TimerRunState
import com.kaoyan.studyassistant.service.TimerServiceState
import com.kaoyan.studyassistant.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 保留旧枚举别名，避免 TimerScreen.kt 大幅改动

/** 番茄钟预设 */
data class PomodoroPreset(
    val label: String,
    val focusMin: Int,
    val breakMin: Int
)

val POMODORO_PRESETS = listOf(
    PomodoroPreset("25 / 5", 25, 5),
    PomodoroPreset("50 / 10", 50, 10),
    PomodoroPreset("90 / 20", 90, 20)
)

data class TimerUiState(
    // ── 普通计时 ──
    val timerState: TimerRunState = TimerRunState.IDLE,
    val elapsedMillis: Long = 0L,
    val startTime: Long = 0L,
    val originalStartTime: Long = 0L,

    // ── 科目 ──
    val subjects: List<Subject> = emptyList(),
    val selectedSubject: Subject? = null,

    // ── 保存弹窗 ──
    val showSaveDialog: Boolean = false,
    val saveDialogTitle: String = "保存学习记录",
    val saveDialogDurationMillis: Long = 0L,
    val note: String = "",
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val errorMessage: String? = null,

    // ── 今日统计 ──
    val todayTotalMillis: Long = 0L,

    // ── 番茄钟 ──
    val pomodoroRunState: TimerRunState = TimerRunState.IDLE,
    val pomodoroPhase: PomodoroPhase = PomodoroPhase.FOCUS,
    val pomodoroRemainingMillis: Long = 25L * 60_000L,
    val pomodoroTargetMillis: Long = 25L * 60_000L,
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val pomodoroCompletedCount: Int = 0,
    val selectedPresetIndex: Int = 0,       // 0=25/5, 1=50/10, 2=90/20, -1=自定义
    val customFocusMinutes: Int = 25,
    val customBreakMinutes: Int = 5
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val sessionRepository: StudySessionRepository,
    private val subjectRepository: SubjectRepository,
    val appPreferences: AppPreferences,
    private val autoBackupCoordinator: AutoBackupCoordinator,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /* ── 仅用于保存弹窗 / 本地 UI 覆盖字段的 overlay ── */
    private data class LocalOverlay(
        val selectedSubject: Subject? = null,
        val showSaveDialog: Boolean = false,
        val saveDialogTitle: String = "保存学习记录",
        val saveDialogDurationMillis: Long = 0L,
        val note: String = "",
        val isSaving: Boolean = false,
        val savedSuccess: Boolean = false,
        val errorMessage: String? = null,
        val selectedPresetIndex: Int = 0,
        val customFocusMinutes: Int = 25,
        val customBreakMinutes: Int = 5
    )

    private val _overlay = MutableStateFlow(LocalOverlay())

    /**
     * 性能优化核心：使用 combine 将所有 TimerServiceState 的 StateFlow
     * 和本地数据源合并为一个 uiState，替代原来 12 个独立 collector。
     *
     * 好处：
     * 1. 同一帧内多个 StateFlow 变化只触发一次 uiState 更新
     * 2. 减少碎片化 recomposition
     * 3. 使用 stateIn + WhileSubscribed 自动管理生命周期
     */
    val uiState: StateFlow<TimerUiState> = combine(
        subjectRepository.getAllSubjects(),
        sessionRepository.getTodayTotalDuration(DateUtils.today()),
        TimerServiceState.runState,
        TimerServiceState.startTimeMs,
        TimerServiceState.originalStartTimeMs,
        // combine 最多 5 个参数，用嵌套 combine 扩展
    ) { subjects, todayTotal, runState, startMs, originalStartMs ->
        CombinedPart1(subjects, todayTotal, runState, startMs, originalStartMs)
    }.combine(
        combine(
            TimerServiceState.pomodoroRunState,
            TimerServiceState.pomodoroPhase,
            TimerServiceState.pomodoroFocusMinutes,
            TimerServiceState.pomodoroBreakMinutes,
            TimerServiceState.pomodoroCompletedCount,
        ) { pomoRunState, pomoPhase, pomoFocusMin, pomoBreakMin, pomoCompleted ->
            CombinedPart2(pomoRunState, pomoPhase, pomoFocusMin, pomoBreakMin, pomoCompleted)
        }
    ) { p1, p2 -> Pair(p1, p2) }
    .combine(_overlay) { (p1, p2), overlay ->
        val selected = overlay.selectedSubject?.let { sel ->
            p1.subjects.find { it.id == sel.id }
        } ?: p1.subjects.firstOrNull()

        TimerUiState(
            timerState = p1.runState,
            startTime = if (overlay.showSaveDialog) 0L else p1.startMs,
            originalStartTime = if (overlay.showSaveDialog) 0L else p1.originalStartMs,
            subjects = p1.subjects,
            selectedSubject = selected,
            todayTotalMillis = p1.todayTotal,

            pomodoroRunState = p2.pomoRunState,
            pomodoroPhase = p2.pomoPhase,
            pomodoroFocusMinutes = p2.pomoFocusMin,
            pomodoroBreakMinutes = p2.pomoBreakMin,
            pomodoroCompletedCount = p2.pomoCompleted,

            showSaveDialog = overlay.showSaveDialog,
            saveDialogTitle = overlay.saveDialogTitle,
            saveDialogDurationMillis = overlay.saveDialogDurationMillis,
            note = overlay.note,
            isSaving = overlay.isSaving,
            savedSuccess = overlay.savedSuccess,
            errorMessage = overlay.errorMessage,
            selectedPresetIndex = overlay.selectedPresetIndex,
            customFocusMinutes = overlay.customFocusMinutes,
            customBreakMinutes = overlay.customBreakMinutes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimerUiState()
    )

    val stopwatchElapsedMillis: StateFlow<Long> = TimerServiceState.elapsedMillis
    val pomodoroRemainingMillis: StateFlow<Long> = TimerServiceState.pomodoroRemainingMillis
    val pomodoroTargetMillis: StateFlow<Long> = TimerServiceState.pomodoroTargetMillis

    private var notePersistJob: Job? = null

    /** 停止计时后，冻结一份待归档快照 */
    private data class PendingSessionSnapshot(
        val elapsedMillis: Long,
        val archiveStartTime: Long,
        val archiveEndTime: Long,
        val subject: Subject?
    )
    private var pendingSnapshot: PendingSessionSnapshot? = null

    // 中间数据类，用于 combine
    private data class CombinedPart1(
        val subjects: List<Subject>,
        val todayTotal: Long,
        val runState: TimerRunState,
        val startMs: Long,
        val originalStartMs: Long
    )
    private data class CombinedPart2(
        val pomoRunState: TimerRunState,
        val pomoPhase: PomodoroPhase,
        val pomoFocusMin: Int,
        val pomoBreakMin: Int,
        val pomoCompleted: Int
    )

    init {
        // 监听番茄钟待归档快照 → 弹出保存弹窗
        viewModelScope.launch {
            TimerServiceState.pomodoroPendingArchive.collect { snapshot ->
                if (snapshot != null && !_overlay.value.showSaveDialog) {
                    onPomodoroPendingArchive(snapshot)
                }
            }
        }

        // 从 DataStore 恢复上次计时状态（进程重启场景）
        viewModelScope.launch {
            val saved = appPreferences.timerState.first()
            if (saved.isRunning && TimerServiceState.runState.value == TimerRunState.IDLE) {
                val elapsed = if (saved.isPaused) {
                    saved.pausedElapsed
                } else {
                    (System.currentTimeMillis() - saved.startTime).coerceAtLeast(0L) + saved.pausedElapsed
                }
                _overlay.update {
                    it.copy(note = saved.noteDraft)
                }
                TimerServiceState.updateElapsedMillis(elapsed)
            }
        }
    }

    // ════════════════════════════════════════════
    // 普通计时操作
    // ════════════════════════════════════════════

    fun startTimer() {
        val subject = uiState.value.selectedSubject ?: return
        context.startForegroundService(
            StudyTimerService.buildStartIntent(context, subject.id, subject.name)
        )
    }

    fun pauseTimer() {
        context.startService(StudyTimerService.buildPauseIntent(context))
    }

    fun resumeTimer() {
        context.startService(StudyTimerService.buildResumeIntent(context))
    }

    fun stopTimer() {
        val current = uiState.value
        val now = System.currentTimeMillis()
        val elapsedMillis = TimerServiceState.elapsedMillis.value.coerceAtLeast(0L)
        val archiveStart = when {
            current.originalStartTime > 0L -> current.originalStartTime
            current.startTime > 0L -> current.startTime
            elapsedMillis > 0L -> now - elapsedMillis
            else -> now
        }
        pendingSnapshot = PendingSessionSnapshot(
            elapsedMillis = elapsedMillis,
            archiveStartTime = archiveStart,
            archiveEndTime = now,
            subject = current.selectedSubject
        )
        _overlay.update {
            it.copy(
                showSaveDialog = true,
                saveDialogTitle = "保存学习记录",
                saveDialogDurationMillis = elapsedMillis
            )
        }
        context.startService(StudyTimerService.buildStopIntent(context))
    }

    // ════════════════════════════════════════════
    // 番茄钟操作
    // ════════════════════════════════════════════

    fun startPomodoro() {
        val state = uiState.value
        val focusMin = state.pomodoroFocusMinutes
        val breakMin = state.pomodoroBreakMinutes
        context.startForegroundService(
            StudyTimerService.buildPomoStartIntent(context, focusMin, breakMin)
        )
    }

    fun pausePomodoro() {
        context.startService(StudyTimerService.buildPomoPauseIntent(context))
    }

    fun resumePomodoro() {
        context.startService(StudyTimerService.buildPomoResumeIntent(context))
    }

    fun resetPomodoro() {
        context.startService(StudyTimerService.buildPomoResetIntent(context))
        TimerServiceState.resetPomodoro()
    }

    /** 选择预设 */
    fun selectPreset(index: Int) {
        if (index !in POMODORO_PRESETS.indices) return
        val preset = POMODORO_PRESETS[index]
        TimerServiceState.updatePomodoroFocusMinutes(preset.focusMin)
        TimerServiceState.updatePomodoroBreakMinutes(preset.breakMin)
        TimerServiceState.updatePomodoroRemainingMillis(preset.focusMin * 60_000L)
        TimerServiceState.updatePomodoroTargetMillis(preset.focusMin * 60_000L)
        _overlay.update {
            it.copy(
                selectedPresetIndex = index,
                customFocusMinutes = preset.focusMin,
                customBreakMinutes = preset.breakMin
            )
        }
    }

    fun applyCustomPomodoro(focusMin: Int, breakMin: Int): String? {
        if (focusMin < 1 || focusMin > 180) return "专注时长须在 1~180 分钟之间"
        if (breakMin < 1 || breakMin > 60) return "休息时长须在 1~60 分钟之间"
        TimerServiceState.updatePomodoroFocusMinutes(focusMin)
        TimerServiceState.updatePomodoroBreakMinutes(breakMin)
        TimerServiceState.updatePomodoroRemainingMillis(focusMin * 60_000L)
        TimerServiceState.updatePomodoroTargetMillis(focusMin * 60_000L)
        _overlay.update {
            it.copy(
                selectedPresetIndex = -1,
                customFocusMinutes = focusMin,
                customBreakMinutes = breakMin
            )
        }
        return null // null 表示成功
    }

    /** 番茄钟专注完成 → 弹出保存弹窗 */
    private fun onPomodoroPendingArchive(snapshot: PomodoroArchiveSnapshot) {
        if (_overlay.value.showSaveDialog) return
        pendingSnapshot = PendingSessionSnapshot(
            elapsedMillis = snapshot.elapsedMillis,
            archiveStartTime = snapshot.archiveStartTime,
            archiveEndTime = snapshot.archiveEndTime,
            subject = uiState.value.selectedSubject
        )
        _overlay.update {
            it.copy(
                showSaveDialog = true,
                saveDialogTitle = "保存本轮专注记录",
                saveDialogDurationMillis = snapshot.elapsedMillis,
                note = ""
            )
        }
        // 清除待归档快照，避免重复弹窗
        TimerServiceState.updatePomodoroPendingArchive(null)
    }

    // ════════════════════════════════════════════
    // 通用操作
    // ════════════════════════════════════════════

    fun selectSubject(subject: Subject) {
        _overlay.update { it.copy(selectedSubject = subject) }
    }

    fun updateNote(note: String) {
        _overlay.update { it.copy(note = note) }
        notePersistJob?.cancel()
        notePersistJob = viewModelScope.launch {
            delay(450)
            appPreferences.updateTimerNoteDraft(note)
        }
    }

    fun saveSession() {
        val state = uiState.value
        if (state.isSaving) return
        val snapshot = pendingSnapshot
        val subject = state.selectedSubject ?: snapshot?.subject
        val now = System.currentTimeMillis()
        val elapsedMillis = (snapshot?.elapsedMillis ?: TimerServiceState.elapsedMillis.value).coerceAtLeast(0L)
        val archiveStart = snapshot?.archiveStartTime ?: when {
            state.originalStartTime > 0L -> state.originalStartTime
            state.startTime > 0L -> state.startTime
            elapsedMillis > 0L -> now - elapsedMillis
            else -> now
        }
        val archiveEnd = snapshot?.archiveEndTime ?: now
        val sessionDate = DateUtils.timestampToDate(archiveStart)
        val session = StudySession(
            subjectId = subject?.id,
            subjectName = subject?.name ?: "未分类",
            startTime = archiveStart,
            endTime = archiveEnd,
            durationMillis = elapsedMillis,
            date = sessionDate,
            note = state.note.trim()
        )
        viewModelScope.launch {
            _overlay.update { it.copy(isSaving = true) }
            try {
                sessionRepository.saveSession(session)
                appPreferences.clearTimerState()
                autoBackupCoordinator.triggerIfEnabled()
                clearPendingSnapshot()
                _overlay.update {
                    LocalOverlay(
                        selectedSubject = it.selectedSubject,
                        savedSuccess = true
                    )
                }
            } catch (e: Exception) {
                _overlay.update { it.copy(isSaving = false, errorMessage = "保存失败：${e.message}") }
            }
        }
    }

    fun cancelSave() = discardSession()

    fun discardSession() {
        notePersistJob?.cancel()
        viewModelScope.launch { appPreferences.clearTimerState() }
        clearPendingSnapshot()
        _overlay.update {
            LocalOverlay(selectedSubject = it.selectedSubject)
        }
    }

    private fun clearPendingSnapshot() {
        pendingSnapshot = null
        notePersistJob?.cancel()
    }

    fun clearError() { _overlay.update { it.copy(errorMessage = null) } }
    fun clearSuccess() { _overlay.update { it.copy(savedSuccess = false) } }

    // 兼容旧 TimerScreen 调用的 preparePomodoroSession
    fun preparePomodoroSession(
        elapsedMillis: Long,
        archiveStartTime: Long,
        archiveEndTime: Long = System.currentTimeMillis()
    ) {
        if (elapsedMillis <= 0L || _overlay.value.showSaveDialog) return
        pendingSnapshot = PendingSessionSnapshot(
            elapsedMillis = elapsedMillis,
            archiveStartTime = archiveStartTime,
            archiveEndTime = archiveEndTime,
            subject = uiState.value.selectedSubject
        )
        _overlay.update {
            it.copy(
                showSaveDialog = true,
                saveDialogTitle = "保存本轮专注记录",
                saveDialogDurationMillis = elapsedMillis,
                note = ""
            )
        }
    }

}
