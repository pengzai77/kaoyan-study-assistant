package com.kaoyan.studyassistant.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaoyan.studyassistant.data.backup.AutoBackupCoordinator
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.domain.summarizer.AiConnectionTester
import com.kaoyan.studyassistant.domain.summarizer.AiModelOption
import com.kaoyan.studyassistant.domain.summarizer.AiProvider
import com.kaoyan.studyassistant.domain.summarizer.ModelCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiConnectionUiState(
    val isTesting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

data class AiModelCatalogUiState(
    val isLoading: Boolean = false,
    val options: List<AiModelOption> = emptyList(),
    val message: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val autoBackupCoordinator: AutoBackupCoordinator,
    private val aiConnectionTester: AiConnectionTester,
    private val modelCatalogRepository: ModelCatalogRepository
) : ViewModel() {

    val userSettings: StateFlow<AppPreferences.UserSettings> =
        appPreferences.userSettings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPreferences.UserSettings()
        )

    val backupSettings: StateFlow<AppPreferences.BackupSettings> =
        appPreferences.backupSettings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPreferences.BackupSettings()
        )

    val aiSettings: StateFlow<AppPreferences.AiSettings> =
        appPreferences.aiSettings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPreferences.AiSettings()
        )

    private val _aiConnectionState = MutableStateFlow(AiConnectionUiState())
    val aiConnectionState: StateFlow<AiConnectionUiState> = _aiConnectionState.asStateFlow()

    private val _modelCatalogState = MutableStateFlow(AiModelCatalogUiState())
    val modelCatalogState: StateFlow<AiModelCatalogUiState> = _modelCatalogState.asStateFlow()

    private var pendingModelRefreshJob: Job? = null

    init {
        viewModelScope.launch {
            loadModelCatalog(appPreferences.aiSettings.first(), forceRefresh = false)
        }
    }

    fun setShowSeconds(value: Boolean) {
        viewModelScope.launch {
            appPreferences.updateShowSeconds(value)
            autoBackupCoordinator.triggerIfEnabled()
        }
    }

    fun setShowLatestReview(value: Boolean) {
        viewModelScope.launch {
            appPreferences.updateShowLatestReview(value)
            autoBackupCoordinator.triggerIfEnabled()
        }
    }

    fun setHistoryDefaultRange(days: Int) {
        viewModelScope.launch {
            appPreferences.updateHistoryDefaultRange(days)
            autoBackupCoordinator.triggerIfEnabled()
        }
    }

    fun setEnableVibration(value: Boolean) {
        viewModelScope.launch {
            appPreferences.updateEnableVibration(value)
            autoBackupCoordinator.triggerIfEnabled()
        }
    }

    fun setDarkMode(mode: Int) {
        viewModelScope.launch {
            appPreferences.updateDarkMode(mode)
            autoBackupCoordinator.triggerIfEnabled()
        }
    }

    fun setAutoBackupEnabled(value: Boolean) {
        viewModelScope.launch {
            appPreferences.updateAutoBackupEnabled(value)
            if (value) {
                autoBackupCoordinator.triggerIfEnabled()
            }
        }
    }

    fun setAiEnabled(value: Boolean) {
        clearAiConnectionState()
        viewModelScope.launch {
            appPreferences.updateAiEnabled(value)
        }
    }

    fun setAiProvider(provider: AiProvider) {
        clearAiConnectionState()
        modelCatalogRepository.invalidate(provider)
        viewModelScope.launch {
            appPreferences.updateAiProvider(provider)
            val settings = appPreferences.aiSettings.first()
            loadModelCatalog(settings, forceRefresh = false)
        }
    }

    fun setAiApiKey(value: String) {
        clearAiConnectionState()
        viewModelScope.launch {
            appPreferences.updateAiApiKey(value)
            scheduleModelRefresh()
        }
    }

    fun setAiBaseUrl(value: String) {
        clearAiConnectionState()
        viewModelScope.launch {
            appPreferences.updateAiBaseUrl(value)
            scheduleModelRefresh()
        }
    }

    fun setAiModel(value: String) {
        clearAiConnectionState()
        viewModelScope.launch {
            appPreferences.updateAiModel(value)
        }
    }

    fun selectAiModel(option: AiModelOption) {
        clearAiConnectionState()
        viewModelScope.launch {
            appPreferences.updateAiModel(option.id)
            appPreferences.saveAiModelToHistory(option.id)
        }
    }

    fun setAiJsonMode(value: Boolean) {
        clearAiConnectionState()
        viewModelScope.launch {
            appPreferences.updateAiJsonMode(value)
        }
    }

    fun refreshModelCatalog() {
        viewModelScope.launch {
            loadModelCatalog(appPreferences.aiSettings.first(), forceRefresh = true)
        }
    }

    fun testAiConnection() {
        viewModelScope.launch {
            _aiConnectionState.value = AiConnectionUiState(isTesting = true)
            try {
                appPreferences.saveAiModelToHistory(aiSettings.value.model)
                val result = aiConnectionTester.test(aiSettings.value)
                _aiConnectionState.value = if (result.summaryCapabilityReady) {
                    AiConnectionUiState(successMessage = "✅ ${result.message}")
                } else {
                    AiConnectionUiState(
                        errorMessage = buildString {
                            append("❌ 连接失败：${result.message}")
                            append("\n\n💡 请检查：")
                            append("\n• API Key 是否正确")
                            append("\n• Base URL 是否可访问")
                            append("\n• 网络连接是否正常")
                        }
                    )
                }
            } catch (e: Exception) {
                _aiConnectionState.value = AiConnectionUiState(
                    errorMessage = buildString {
                        val message = e.message ?: "未知错误"
                        append("❌ 连接异常：$message")
                        append("\n\n💡 可能原因：")
                        when {
                            message.contains("timeout", ignoreCase = true) -> {
                                append("\n• 网络超时，请检查网络连接")
                            }
                            message.contains("401", ignoreCase = true) -> {
                                append("\n• API Key 无效或已过期")
                            }
                            message.contains("404", ignoreCase = true) -> {
                                append("\n• Base URL 地址错误")
                            }
                            else -> {
                                append("\n• 请检查配置是否正确")
                                append("\n• 确认网络可以访问该服务")
                            }
                        }
                    }
                )
            }
        }
    }

    private suspend fun loadModelCatalog(settings: AppPreferences.AiSettings, forceRefresh: Boolean) {
        _modelCatalogState.value = _modelCatalogState.value.copy(
            isLoading = true,
            message = null,
            errorMessage = null
        )

        val result = modelCatalogRepository.load(settings, forceRefresh = forceRefresh)
        _modelCatalogState.value = AiModelCatalogUiState(
            isLoading = false,
            options = result.options,
            message = result.message,
            errorMessage = result.errorMessage
        )

        reconcileModelSelection(settings, result.options)
    }

    private suspend fun reconcileModelSelection(
        settings: AppPreferences.AiSettings,
        options: List<AiModelOption>
    ) {
        val belongsToProvider = options.any { it.id == settings.model && it.provider == settings.provider }
        if (settings.provider == AiProvider.DEEPSEEK) {
            if (settings.model.isBlank()) {
                val fallback = recommendedOption(options, settings.provider)
                appPreferences.updateAiModel(fallback?.id ?: settings.provider.defaultModel)
            }
            return
        }

        if (!belongsToProvider) {
            val fallback = recommendedOption(options, settings.provider)
            appPreferences.updateAiModel(fallback?.id ?: settings.provider.defaultModel)
        }
    }

    private fun recommendedOption(
        options: List<AiModelOption>,
        provider: AiProvider
    ): AiModelOption? {
        return options.firstOrNull { it.provider == provider && it.isRecommended }
            ?: options.firstOrNull { it.provider == provider && it.isLatest }
            ?: options.firstOrNull { it.provider == provider }
    }

    private fun scheduleModelRefresh() {
        pendingModelRefreshJob?.cancel()
        pendingModelRefreshJob = viewModelScope.launch {
            delay(650)
            loadModelCatalog(appPreferences.aiSettings.first(), forceRefresh = true)
        }
    }

    private fun clearAiConnectionState() {
        _aiConnectionState.value = AiConnectionUiState()
    }
}
