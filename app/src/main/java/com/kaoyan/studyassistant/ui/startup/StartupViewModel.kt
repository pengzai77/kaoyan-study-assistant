package com.kaoyan.studyassistant.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.backup.BackupManager
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.repository.DailyReviewRepository
import com.kaoyan.studyassistant.data.repository.GoalSettingsRepository
import com.kaoyan.studyassistant.data.repository.StudySessionRepository
import com.kaoyan.studyassistant.data.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class StartupState(
    val isChecking: Boolean = true,
    val showRestorePrompt: Boolean = false,
    val backupSummary: String = "",
    val isRestoring: Boolean = false,
    val restoreResultMessage: String? = null
)

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val sessionRepository: StudySessionRepository,
    private val subjectRepository: SubjectRepository,
    private val reviewRepository: DailyReviewRepository,
    private val goalSettingsRepository: GoalSettingsRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(StartupState())
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        checkForRestoreOpportunity()
    }

    private fun checkForRestoreOpportunity() {
        viewModelScope.launch {
            try {
                // 将所有 IO 密集操作移到 IO 线程，避免阻塞主线程影响首屏渲染
                val result = withContext(Dispatchers.IO) {
                    // 快速短路：先检查最轻量的条件
                    if (appPreferences.getStartupRestorePrompted()) {
                        return@withContext StartupState(isChecking = false)
                    }

                    if (!backupManager.hasAutoBackup()) {
                        return@withContext StartupState(isChecking = false)
                    }

                    if (!isEffectivelyEmptyDatabase()) {
                        return@withContext StartupState(isChecking = false)
                    }

                    val payload = backupManager.peekLatestAutoBackup()
                    if (payload != null) {
                        appPreferences.setStartupRestorePrompted(true)
                        StartupState(
                            isChecking = false,
                            showRestorePrompt = true,
                            backupSummary = payload.summary()
                        )
                    } else {
                        StartupState(isChecking = false)
                    }
                }
                _state.value = result
            } catch (_: Exception) {
                _state.value = StartupState(isChecking = false)
            }
        }
    }

    /**
     * 短路检查：按数据量从小到大依次查询，一旦发现非空即返回 false，
     * 避免无谓地加载全部 sessions（可能很多）。
     */
    private suspend fun isEffectivelyEmptyDatabase(): Boolean {
        // 1. 先查 goal（最轻量的单条查询）
        val goal = goalSettingsRepository.getGoalSettingsSync()
        val hasGoalData = goal != null && (
            goal.targetSchool.isNotBlank() ||
            goal.targetMajor.isNotBlank() ||
            goal.examDate.isNotBlank() ||
            goal.weeklyGoalMinutes > 0
        )
        if (hasGoalData) return false

        // 2. 查 reviews（通常比 sessions 少）
        val reviews = reviewRepository.getAllReviewsSync()
        if (reviews.isNotEmpty()) return false

        // 3. 查 subjects（默认4个以内视为空）
        val subjects = subjectRepository.getAllSubjectsSync()
        if (subjects.size > 4) return false

        // 4. 最后查 sessions（可能最多，放最后）
        val sessions = sessionRepository.getAllSessionsSync()
        return sessions.isEmpty()
    }

    fun confirmRestore() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRestoring = true, showRestorePrompt = false)
            try {
                val result = withContext(Dispatchers.IO) {
                    backupManager.restoreFromLatestAutoBackup()
                }
                _state.value = _state.value.copy(
                    isRestoring = false,
                    restoreResultMessage = result
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRestoring = false,
                    restoreResultMessage = "恢复失败：${e.message}"
                )
            }
        }
    }

    fun dismissRestore() {
        _state.value = _state.value.copy(showRestorePrompt = false)
    }

    fun clearResultMessage() {
        _state.value = _state.value.copy(restoreResultMessage = null)
    }
}
