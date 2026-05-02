package com.kaoyan.studyassistant.data.backup

import com.kaoyan.studyassistant.data.datastore.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自动备份协调器
 *
 * 统一后台触发入口，内部自带开关判断与 debounce，
 * 避免多个 ViewModel 在短时间内频繁写出多份备份。
 */
@Singleton
class AutoBackupCoordinator @Inject constructor(
    private val backupManager: BackupManager,
    private val appPreferences: AppPreferences
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pendingJob: Job? = null

    companion object {
        private const val DEBOUNCE_MS = 15_000L
    }

    fun triggerIfEnabled() {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(DEBOUNCE_MS)
            try {
                val settings = appPreferences.userSettings.first()
                if (!settings.autoBackupEnabled) return@launch
                backupManager.backup()
            } catch (e: Exception) {
                android.util.Log.w("AutoBackup", "自动备份失败（静默处理）: ${e.message}")
            }
        }
    }

    suspend fun triggerForced(): String = backupManager.backup()
}
