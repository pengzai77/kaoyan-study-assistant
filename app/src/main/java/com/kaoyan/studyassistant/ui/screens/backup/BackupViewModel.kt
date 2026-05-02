package com.kaoyan.studyassistant.ui.screens.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.backup.AutoBackupCoordinator
import com.kaoyan.studyassistant.data.backup.BackupManager
import com.kaoyan.studyassistant.data.backup.BackupPayload
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BackupUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    /** 待确认恢复的 Payload（用于弹出确认对话框） */
    val pendingRestorePayload: BackupPayload? = null,
    /** 待确认恢复的 URI */
    val pendingRestoreUri: Uri? = null,
    /** 自动备份文件列表 */
    val autoBackupFiles: List<File> = emptyList()
)

/**
 * 备份与恢复页面 ViewModel
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val autoBackupCoordinator: AutoBackupCoordinator,
    private val appPreferences: AppPreferences
) : ViewModel() {

    /** 自动备份开关状态（绑定到备份页 UI） */
    val autoBackupEnabled: StateFlow<Boolean> = appPreferences.userSettings
        .map { it.autoBackupEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    /** 订阅自动备份开关，当开关开启时尝试自动备份（一次性，每次进入备份页触发） */
    fun triggerAutoBackupIfEnabled() {
        viewModelScope.launch {
            val enabled = appPreferences.userSettings
                .map { it.autoBackupEnabled }
                .first()
            if (enabled) {
                try {
                    backupManager.backup()
                    refreshAutoBackupList()
                } catch (e: Exception) {
                    // 自动备份失败不弹窗，静默失败
                }
            }
        }
    }

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        refreshAutoBackupList()
    }

    /** 刷新自动备份文件列表 */
    fun refreshAutoBackupList() {
        _uiState.update { it.copy(autoBackupFiles = backupManager.getAutoBackupFiles()) }
    }

    /** 执行备份（写入共享存储 Documents/考研学习助手/） */
    fun backup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val fileName = autoBackupCoordinator.triggerForced()
                refreshAutoBackupList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "备份成功：$fileName"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "备份失败：${e.message}")
                }
            }
        }
    }

    /** 切换自动备份开关 */
    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.updateAutoBackupEnabled(enabled)
        }
    }

    /** 备份到用户选择的 URI（SAF） */
    fun backupToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                backupManager.backupToUri(uri)
                _uiState.update {
                    it.copy(isLoading = false, successMessage = "已导出备份文件")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "导出失败：${e.message}")
                }
            }
        }
    }

    /** 预读备份文件（弹出确认对话框前调用） */
    fun previewRestore(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val payload = backupManager.readBackupFromUri(uri)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingRestorePayload = payload,
                        pendingRestoreUri = uri
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "读取备份失败：${e.message}")
                }
            }
        }
    }

    /** 确认恢复（用户在对话框中点击确认后调用） */
    fun confirmRestore() {
        val uri = _uiState.value.pendingRestoreUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, pendingRestorePayload = null, pendingRestoreUri = null) }
            try {
                val summary = backupManager.restoreFromUri(uri)
                refreshAutoBackupList()
                _uiState.update {
                    it.copy(isLoading = false, successMessage = summary)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "恢复失败：${e.message}")
                }
            }
        }
    }

    /** 取消恢复 */
    fun cancelRestore() {
        _uiState.update { it.copy(pendingRestorePayload = null, pendingRestoreUri = null) }
    }

    /** 从最新自动备份恢复 */
    fun restoreFromLatestAutoBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val summary = backupManager.restoreFromLatestAutoBackup()
                refreshAutoBackupList()
                _uiState.update {
                    it.copy(isLoading = false, successMessage = summary)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "恢复失败：${e.message}")
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
