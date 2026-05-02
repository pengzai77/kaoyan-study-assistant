package com.kaoyan.studyassistant.ui.screens.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.backup.AutoBackupCoordinator
import com.kaoyan.studyassistant.data.local.entity.GoalSettings
import com.kaoyan.studyassistant.data.repository.GoalSettingsRepository
import com.kaoyan.studyassistant.data.repository.StudySessionRepository
import com.kaoyan.studyassistant.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalUiState(
    val targetSchool: String = "",
    val targetMajor: String = "",
    val examDate: String = "",
    val weeklyGoalMinutes: Int = 0,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val errorMessage: String? = null,
    val daysToExam: Int = -1,
    val weeklyStudiedMinutes: Long = 0L,
    val weeklyProgress: Int = 0
)

/**
 * 考研目标 ViewModel
 *
 * 性能优化（v2）：
 * - 将 2 个独立 collector 合并为 combine + stateIn
 * - 使用 localOverlay 管理编辑状态，与数据流分离
 */
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalSettingsRepository: GoalSettingsRepository,
    private val sessionRepository: StudySessionRepository,
    private val autoBackupCoordinator: AutoBackupCoordinator
) : ViewModel() {

    /** 本地 UI 覆盖层（编辑状态、保存状态等） */
    private data class LocalOverlay(
        val isEditing: Boolean = false,
        val isSaving: Boolean = false,
        val savedSuccess: Boolean = false,
        val errorMessage: String? = null,
        // 编辑中的临时值（null 表示使用数据库值）
        val editTargetSchool: String? = null,
        val editTargetMajor: String? = null,
        val editExamDate: String? = null,
        val editWeeklyGoalMinutes: Int? = null
    )

    private val _overlay = MutableStateFlow(LocalOverlay())

    private val weekStart = DateUtils.daysAgo(6)
    private val today = DateUtils.today()

    val uiState: StateFlow<GoalUiState> = combine(
        goalSettingsRepository.getGoalSettings(),
        sessionRepository.getDurationByDateRange(weekStart, today),
        _overlay
    ) { settings, weeklyDurations, overlay ->
        val s = settings ?: GoalSettings()
        val school = overlay.editTargetSchool ?: s.targetSchool
        val major = overlay.editTargetMajor ?: s.targetMajor
        val examDate = overlay.editExamDate ?: s.examDate
        val weeklyGoal = overlay.editWeeklyGoalMinutes ?: s.weeklyGoalMinutes
        val daysToExam = calculateDaysToExam(examDate)

        val totalMillis = weeklyDurations.sumOf { it.totalMillis }
        val totalMinutes = (totalMillis / 60_000).toInt()
        val progress = if (weeklyGoal > 0) ((totalMinutes * 100) / weeklyGoal).coerceAtMost(100) else 0

        GoalUiState(
            targetSchool = school,
            targetMajor = major,
            examDate = examDate,
            weeklyGoalMinutes = weeklyGoal,
            isEditing = overlay.isEditing,
            isSaving = overlay.isSaving,
            savedSuccess = overlay.savedSuccess,
            errorMessage = overlay.errorMessage,
            daysToExam = daysToExam,
            weeklyStudiedMinutes = totalMinutes.toLong(),
            weeklyProgress = progress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalUiState()
    )

    fun startEditing() { _overlay.update { it.copy(isEditing = true) } }
    fun cancelEditing() {
        _overlay.update {
            it.copy(
                isEditing = false,
                editTargetSchool = null,
                editTargetMajor = null,
                editExamDate = null,
                editWeeklyGoalMinutes = null
            )
        }
    }

    fun updateTargetSchool(v: String) { _overlay.update { it.copy(editTargetSchool = v) } }
    fun updateTargetMajor(v: String) { _overlay.update { it.copy(editTargetMajor = v) } }
    fun updateExamDate(v: String) { _overlay.update { it.copy(editExamDate = v) } }
    fun updateWeeklyGoal(minutes: Int) { _overlay.update { it.copy(editWeeklyGoalMinutes = minutes) } }

    fun save() {
        val state = uiState.value
        viewModelScope.launch {
            _overlay.update { it.copy(isSaving = true) }
            try {
                goalSettingsRepository.saveGoalSettings(
                    GoalSettings(
                        targetSchool = state.targetSchool.trim(),
                        targetMajor = state.targetMajor.trim(),
                        examDate = state.examDate.trim(),
                        weeklyGoalMinutes = state.weeklyGoalMinutes
                    )
                )
                autoBackupCoordinator.triggerIfEnabled()
                _overlay.update {
                    LocalOverlay(savedSuccess = true) // 重置所有编辑状态
                }
            } catch (e: Exception) {
                _overlay.update { it.copy(isSaving = false, errorMessage = "保存失败：${e.message}") }
            }
        }
    }

    fun clearSuccess() { _overlay.update { it.copy(savedSuccess = false) } }
    fun clearError() { _overlay.update { it.copy(errorMessage = null) } }

    private fun calculateDaysToExam(examDate: String): Int {
        if (examDate.isBlank()) return -1
        return try {
            DateUtils.daysBetween(DateUtils.today(), examDate)
        } catch (_: Exception) {
            -1
        }
    }
}
