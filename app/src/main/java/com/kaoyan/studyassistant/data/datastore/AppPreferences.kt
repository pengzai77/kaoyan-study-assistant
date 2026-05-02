package com.kaoyan.studyassistant.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kaoyan.studyassistant.domain.summarizer.AiProvider
import com.kaoyan.studyassistant.domain.summarizer.SummaryResult
import com.kaoyan.studyassistant.domain.summarizer.SummarySource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kaoyan_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_TIMER_IS_RUNNING = booleanPreferencesKey("timer_is_running")
        val KEY_TIMER_IS_PAUSED = booleanPreferencesKey("timer_is_paused")
        val KEY_TIMER_START_TIME = longPreferencesKey("timer_start_time")
        val KEY_TIMER_PAUSED_ELAPSED = longPreferencesKey("timer_paused_elapsed")
        val KEY_TIMER_SUBJECT_ID = longPreferencesKey("timer_subject_id")
        val KEY_TIMER_SUBJECT_NAME = stringPreferencesKey("timer_subject_name")
        val KEY_TIMER_NOTE_DRAFT = stringPreferencesKey("timer_note_draft")
        val KEY_TIMER_ORIGINAL_START_TIME = longPreferencesKey("timer_original_start_time")

        val KEY_SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val KEY_SHOW_LATEST_REVIEW = booleanPreferencesKey("show_latest_review")
        val KEY_HISTORY_DEFAULT_RANGE = intPreferencesKey("history_default_range")
        val KEY_ENABLE_VIBRATION = booleanPreferencesKey("enable_vibration")
        val KEY_DARK_MODE = intPreferencesKey("dark_mode")

        val KEY_AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val KEY_LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val KEY_BACKUP_URI = stringPreferencesKey("backup_uri")

        val KEY_STARTUP_RESTORE_PROMPTED = booleanPreferencesKey("startup_restore_prompted")

        val KEY_POMO_IS_RUNNING = booleanPreferencesKey("pomo_is_running")
        val KEY_POMO_IS_PAUSED = booleanPreferencesKey("pomo_is_paused")
        val KEY_POMO_PHASE = stringPreferencesKey("pomo_phase")
        val KEY_POMO_FOCUS_MIN = intPreferencesKey("pomo_focus_min")
        val KEY_POMO_BREAK_MIN = intPreferencesKey("pomo_break_min")
        val KEY_POMO_COMPLETED = intPreferencesKey("pomo_completed")
        val KEY_POMO_END_AT = longPreferencesKey("pomo_end_at")
        val KEY_POMO_REMAINING = longPreferencesKey("pomo_remaining")
        val KEY_POMO_FOCUS_STARTED_AT = longPreferencesKey("pomo_focus_started_at")

        val KEY_AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_AI_API_KEY = stringPreferencesKey("ai_api_key")
        val KEY_AI_API_KEY_MIMO = stringPreferencesKey("ai_api_key_mimo")
        val KEY_AI_API_KEY_MINIMAX = stringPreferencesKey("ai_api_key_minimax")
        val KEY_AI_API_KEY_DEEPSEEK = stringPreferencesKey("ai_api_key_deepseek")
        val KEY_AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val KEY_AI_MODEL = stringPreferencesKey("ai_model")
        val KEY_AI_MODEL_HISTORY = stringPreferencesKey("ai_model_history")
        val KEY_AI_TIMEOUT = intPreferencesKey("ai_timeout")
        val KEY_AI_JSON_MODE = booleanPreferencesKey("ai_json_mode")
        val KEY_AI_FALLBACK_TO_RULE = booleanPreferencesKey("ai_fallback_to_rule")
        val KEY_AI_ONLY_WIFI = booleanPreferencesKey("ai_only_wifi")
        val KEY_SUMMARY_CACHE = stringPreferencesKey("summary_cache")
    }

    data class TimerState(
        val isRunning: Boolean = false,
        val isPaused: Boolean = false,
        val startTime: Long = 0L,
        val pausedElapsed: Long = 0L,
        val subjectId: Long = 0L,
        val subjectName: String = "",
        val noteDraft: String = "",
        val originalStartTime: Long = 0L
    )

    val timerState: Flow<TimerState> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            TimerState(
                isRunning = prefs[KEY_TIMER_IS_RUNNING] ?: false,
                isPaused = prefs[KEY_TIMER_IS_PAUSED] ?: false,
                startTime = prefs[KEY_TIMER_START_TIME] ?: 0L,
                pausedElapsed = prefs[KEY_TIMER_PAUSED_ELAPSED] ?: 0L,
                subjectId = prefs[KEY_TIMER_SUBJECT_ID] ?: 0L,
                subjectName = prefs[KEY_TIMER_SUBJECT_NAME] ?: "",
                noteDraft = prefs[KEY_TIMER_NOTE_DRAFT] ?: "",
                originalStartTime = prefs[KEY_TIMER_ORIGINAL_START_TIME] ?: 0L
            )
        }

    suspend fun saveTimerState(state: TimerState) {
        dataStore.edit { prefs ->
            prefs[KEY_TIMER_IS_RUNNING] = state.isRunning
            prefs[KEY_TIMER_IS_PAUSED] = state.isPaused
            prefs[KEY_TIMER_START_TIME] = state.startTime
            prefs[KEY_TIMER_PAUSED_ELAPSED] = state.pausedElapsed
            prefs[KEY_TIMER_SUBJECT_ID] = state.subjectId
            prefs[KEY_TIMER_SUBJECT_NAME] = state.subjectName
            prefs[KEY_TIMER_NOTE_DRAFT] = state.noteDraft
            prefs[KEY_TIMER_ORIGINAL_START_TIME] = state.originalStartTime
        }
    }

    suspend fun clearTimerState() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_TIMER_IS_RUNNING)
            prefs.remove(KEY_TIMER_IS_PAUSED)
            prefs.remove(KEY_TIMER_START_TIME)
            prefs.remove(KEY_TIMER_PAUSED_ELAPSED)
            prefs.remove(KEY_TIMER_SUBJECT_ID)
            prefs.remove(KEY_TIMER_SUBJECT_NAME)
            prefs.remove(KEY_TIMER_NOTE_DRAFT)
            prefs.remove(KEY_TIMER_ORIGINAL_START_TIME)
        }
    }


    suspend fun updateTimerNoteDraft(note: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TIMER_NOTE_DRAFT] = note
        }
    }

    data class UserSettings(
        val showSeconds: Boolean = true,
        val showLatestReview: Boolean = true,
        val historyDefaultRange: Int = 30,
        val enableVibration: Boolean = false,
        val darkMode: Int = 0,
        val autoBackupEnabled: Boolean = true
    )

    val userSettings: Flow<UserSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            UserSettings(
                showSeconds = prefs[KEY_SHOW_SECONDS] ?: true,
                showLatestReview = prefs[KEY_SHOW_LATEST_REVIEW] ?: true,
                historyDefaultRange = prefs[KEY_HISTORY_DEFAULT_RANGE] ?: 30,
                enableVibration = prefs[KEY_ENABLE_VIBRATION] ?: false,
                darkMode = prefs[KEY_DARK_MODE] ?: 0,
                autoBackupEnabled = prefs[KEY_AUTO_BACKUP_ENABLED] ?: true
            )
        }

    suspend fun updateShowSeconds(value: Boolean) {
        dataStore.edit { it[KEY_SHOW_SECONDS] = value }
    }

    suspend fun updateShowLatestReview(value: Boolean) {
        dataStore.edit { it[KEY_SHOW_LATEST_REVIEW] = value }
    }

    suspend fun updateHistoryDefaultRange(days: Int) {
        dataStore.edit { it[KEY_HISTORY_DEFAULT_RANGE] = days }
    }

    suspend fun updateEnableVibration(value: Boolean) {
        dataStore.edit { it[KEY_ENABLE_VIBRATION] = value }
    }

    suspend fun updateDarkMode(mode: Int) {
        dataStore.edit { it[KEY_DARK_MODE] = mode }
    }

    /**
     * 批量更新用户设置 — 单次 DataStore 事务，避免 5 次独立写入
     * 主要用于备份恢复场景
     */
    suspend fun updateUserSettingsBatch(
        showSeconds: Boolean,
        showLatestReview: Boolean,
        historyDefaultRange: Int,
        enableVibration: Boolean,
        darkMode: Int
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_SECONDS] = showSeconds
            prefs[KEY_SHOW_LATEST_REVIEW] = showLatestReview
            prefs[KEY_HISTORY_DEFAULT_RANGE] = historyDefaultRange
            prefs[KEY_ENABLE_VIBRATION] = enableVibration
            prefs[KEY_DARK_MODE] = darkMode
        }
    }

    data class BackupSettings(
        val autoBackupEnabled: Boolean = true,
        val lastBackupTime: Long = 0L,
        val backupUri: String = ""
    )

    val backupSettings: Flow<BackupSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            BackupSettings(
                autoBackupEnabled = prefs[KEY_AUTO_BACKUP_ENABLED] ?: true,
                lastBackupTime = prefs[KEY_LAST_BACKUP_TIME] ?: 0L,
                backupUri = prefs[KEY_BACKUP_URI] ?: ""
            )
        }

    suspend fun updateAutoBackupEnabled(value: Boolean) {
        dataStore.edit { it[KEY_AUTO_BACKUP_ENABLED] = value }
    }

    suspend fun updateLastBackupTime(time: Long) {
        dataStore.edit { it[KEY_LAST_BACKUP_TIME] = time }
    }

    suspend fun updateBackupUri(uri: String) {
        dataStore.edit { it[KEY_BACKUP_URI] = uri }
    }

    data class AiSettings(
        val enabled: Boolean = false,
        val provider: AiProvider = AiProvider.MIMO,
        val apiKey: String = "",
        val baseUrl: String = AiProvider.MIMO.defaultBaseUrl,
        val model: String = AiProvider.MIMO.defaultModel,
        val modelHistory: List<String> = emptyList(),
        val timeoutSeconds: Int = 45,
        val jsonMode: Boolean = true,
        val fallbackToRule: Boolean = true,
        val onlyWifi: Boolean = false
    )

    data class SummaryCache(
        val rangeName: String,
        val result: SummaryResult,
        val generatedAtMillis: Long? = null
    )

    val aiSettings: Flow<AiSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val rawProvider = prefs[KEY_AI_PROVIDER]
            val provider = AiProvider.fromStorageValue(rawProvider)
            val storedBaseUrl = prefs[KEY_AI_BASE_URL]
            val storedModel = prefs[KEY_AI_MODEL]
            AiSettings(
                enabled = prefs[KEY_AI_ENABLED] ?: false,
                provider = provider,
                apiKey = resolvedApiKey(rawProvider, prefs, provider),
                baseUrl = resolvedBaseUrl(rawProvider, storedBaseUrl, provider),
                model = resolvedModel(rawProvider, storedModel, provider),
                modelHistory = decodeModelHistory(prefs[KEY_AI_MODEL_HISTORY]),
                timeoutSeconds = prefs[KEY_AI_TIMEOUT] ?: 45,
                jsonMode = prefs[KEY_AI_JSON_MODE] ?: true,
                fallbackToRule = prefs[KEY_AI_FALLBACK_TO_RULE] ?: true,
                onlyWifi = prefs[KEY_AI_ONLY_WIFI] ?: false
            )
        }

    val summaryCache: Flow<SummaryCache?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[KEY_SUMMARY_CACHE]?.let(::decodeSummaryCache)
        }

    suspend fun updateAiEnabled(value: Boolean) {
        dataStore.edit { it[KEY_AI_ENABLED] = value }
    }

    suspend fun updateAiProvider(provider: AiProvider) {
        dataStore.edit { prefs ->
            migrateLegacyApiKey(prefs, AiProvider.fromStorageValue(prefs[KEY_AI_PROVIDER]))
            prefs[KEY_AI_PROVIDER] = provider.name
            prefs[KEY_AI_BASE_URL] = provider.defaultBaseUrl
            prefs[KEY_AI_MODEL] = provider.defaultModel
        }
    }

    suspend fun updateAiApiKey(value: String) {
        dataStore.edit { prefs ->
            val normalized = value.trim()
            val providerKey = apiKeyPreference(AiProvider.fromStorageValue(prefs[KEY_AI_PROVIDER]))
            if (normalized.isBlank()) {
                prefs.remove(providerKey)
            } else {
                prefs[providerKey] = normalized
            }
            prefs.remove(KEY_AI_API_KEY)
        }
    }

    suspend fun updateAiBaseUrl(value: String) {
        dataStore.edit { prefs ->
            val normalized = value.trim()
            if (normalized.isBlank()) {
                prefs.remove(KEY_AI_BASE_URL)
            } else {
                prefs[KEY_AI_BASE_URL] = normalized
            }
        }
    }

    suspend fun updateAiModel(value: String) {
        dataStore.edit { prefs ->
            val normalized = value.trim()
            if (normalized.isBlank()) {
                prefs.remove(KEY_AI_MODEL)
            } else {
                prefs[KEY_AI_MODEL] = normalized
            }
        }
    }

    suspend fun saveAiModelToHistory(value: String) {
        dataStore.edit { prefs ->
            val updated = addModelToHistory(
                current = decodeModelHistory(prefs[KEY_AI_MODEL_HISTORY]),
                model = value
            )
            if (updated.isEmpty()) {
                prefs.remove(KEY_AI_MODEL_HISTORY)
            } else {
                prefs[KEY_AI_MODEL_HISTORY] = encodeModelHistory(updated)
            }
        }
    }

    suspend fun updateAiTimeout(value: Int) {
        dataStore.edit { it[KEY_AI_TIMEOUT] = value.coerceIn(5, 180) }
    }

    suspend fun updateAiJsonMode(value: Boolean) {
        dataStore.edit { it[KEY_AI_JSON_MODE] = value }
    }

    suspend fun updateAiFallbackToRule(value: Boolean) {
        dataStore.edit { it[KEY_AI_FALLBACK_TO_RULE] = value }
    }

    suspend fun updateAiOnlyWifi(value: Boolean) {
        dataStore.edit { it[KEY_AI_ONLY_WIFI] = value }
    }

    suspend fun saveSummaryCache(rangeName: String, result: SummaryResult, generatedAtMillis: Long? = null) {
        dataStore.edit { prefs ->
            prefs[KEY_SUMMARY_CACHE] = encodeSummaryCache(
                SummaryCache(
                    rangeName = rangeName,
                    result = result,
                    generatedAtMillis = generatedAtMillis
                )
            )
        }
    }

    suspend fun clearSummaryCache() {
        dataStore.edit { prefs -> prefs.remove(KEY_SUMMARY_CACHE) }
    }

    suspend fun getStartupRestorePrompted(): Boolean {
        return dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()[KEY_STARTUP_RESTORE_PROMPTED] ?: false
    }

    suspend fun setStartupRestorePrompted(value: Boolean) {
        dataStore.edit { it[KEY_STARTUP_RESTORE_PROMPTED] = value }
    }

    suspend fun resetStartupRestorePrompted() {
        dataStore.edit { it.remove(KEY_STARTUP_RESTORE_PROMPTED) }
    }

    data class PomodoroState(
        val isRunning: Boolean = false,
        val isPaused: Boolean = false,
        val phase: String = "FOCUS",
        val focusMinutes: Int = 25,
        val breakMinutes: Int = 5,
        val completedCount: Int = 0,
        val endAtMillis: Long = 0L,
        val remainingMillis: Long = 25L * 60_000L,
        val focusStartedAt: Long = 0L
    )

    val pomodoroState: Flow<PomodoroState> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            PomodoroState(
                isRunning = prefs[KEY_POMO_IS_RUNNING] ?: false,
                isPaused = prefs[KEY_POMO_IS_PAUSED] ?: false,
                phase = prefs[KEY_POMO_PHASE] ?: "FOCUS",
                focusMinutes = prefs[KEY_POMO_FOCUS_MIN] ?: 25,
                breakMinutes = prefs[KEY_POMO_BREAK_MIN] ?: 5,
                completedCount = prefs[KEY_POMO_COMPLETED] ?: 0,
                endAtMillis = prefs[KEY_POMO_END_AT] ?: 0L,
                remainingMillis = prefs[KEY_POMO_REMAINING] ?: 25L * 60_000L,
                focusStartedAt = prefs[KEY_POMO_FOCUS_STARTED_AT] ?: 0L
            )
        }

    suspend fun savePomodoroState(state: PomodoroState) {
        dataStore.edit { prefs ->
            prefs[KEY_POMO_IS_RUNNING] = state.isRunning
            prefs[KEY_POMO_IS_PAUSED] = state.isPaused
            prefs[KEY_POMO_PHASE] = state.phase
            prefs[KEY_POMO_FOCUS_MIN] = state.focusMinutes
            prefs[KEY_POMO_BREAK_MIN] = state.breakMinutes
            prefs[KEY_POMO_COMPLETED] = state.completedCount
            prefs[KEY_POMO_END_AT] = state.endAtMillis
            prefs[KEY_POMO_REMAINING] = state.remainingMillis
            prefs[KEY_POMO_FOCUS_STARTED_AT] = state.focusStartedAt
        }
    }

    suspend fun clearPomodoroState() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_POMO_IS_RUNNING)
            prefs.remove(KEY_POMO_IS_PAUSED)
            prefs.remove(KEY_POMO_PHASE)
            prefs.remove(KEY_POMO_FOCUS_MIN)
            prefs.remove(KEY_POMO_BREAK_MIN)
            prefs.remove(KEY_POMO_COMPLETED)
            prefs.remove(KEY_POMO_END_AT)
            prefs.remove(KEY_POMO_REMAINING)
            prefs.remove(KEY_POMO_FOCUS_STARTED_AT)
        }
    }

    private fun resolvedBaseUrl(
        rawProvider: String?,
        storedBaseUrl: String?,
        provider: AiProvider
    ): String {
        return when (rawProvider) {
            "QWEN" -> if (storedBaseUrl.isNullOrBlank() ||
                storedBaseUrl.contains("dashscope.aliyuncs.com", ignoreCase = true)
            ) {
                AiProvider.MINIMAX.defaultBaseUrl
            } else {
                storedBaseUrl
            }

            "GEMINI" -> if (storedBaseUrl.isNullOrBlank() ||
                storedBaseUrl.contains("generativelanguage.googleapis.com", ignoreCase = true)
            ) {
                AiProvider.MIMO.defaultBaseUrl
            } else {
                storedBaseUrl
            }

            else -> storedBaseUrl ?: provider.defaultBaseUrl
        }
    }

    private fun resolvedApiKey(
        rawProvider: String?,
        prefs: Preferences,
        provider: AiProvider
    ): String {
        prefs[apiKeyPreference(provider)]?.let { return it }

        val legacyKey = prefs[KEY_AI_API_KEY].orEmpty()
        if (legacyKey.isBlank()) return ""

        return when (rawProvider) {
            null -> if (provider == AiProvider.MIMO) legacyKey else ""
            "MIMO", "GEMINI" -> if (provider == AiProvider.MIMO) legacyKey else ""
            "MINIMAX", "QWEN" -> if (provider == AiProvider.MINIMAX) legacyKey else ""
            "DEEPSEEK", "CUSTOM" -> if (provider == AiProvider.DEEPSEEK) legacyKey else ""
            else -> ""
        }
    }

    private fun migrateLegacyApiKey(prefs: androidx.datastore.preferences.core.MutablePreferences, provider: AiProvider) {
        val legacyKey = prefs[KEY_AI_API_KEY]?.trim().orEmpty()
        val providerKey = apiKeyPreference(provider)
        if (legacyKey.isNotBlank() && prefs[providerKey].isNullOrBlank()) {
            prefs[providerKey] = legacyKey
        }
        prefs.remove(KEY_AI_API_KEY)
    }

    private fun apiKeyPreference(provider: AiProvider): Preferences.Key<String> {
        return when (provider) {
            AiProvider.MIMO -> KEY_AI_API_KEY_MIMO
            AiProvider.MINIMAX -> KEY_AI_API_KEY_MINIMAX
            AiProvider.DEEPSEEK -> KEY_AI_API_KEY_DEEPSEEK
        }
    }

    private fun resolvedModel(
        rawProvider: String?,
        storedModel: String?,
        provider: AiProvider
    ): String {
        return when (rawProvider) {
            "QWEN" -> if (storedModel.isNullOrBlank() || storedModel.startsWith("qwen", ignoreCase = true)) {
                AiProvider.MINIMAX.defaultModel
            } else {
                storedModel
            }

            "GEMINI" -> if (storedModel.isNullOrBlank() || storedModel.startsWith("gemini", ignoreCase = true)) {
                AiProvider.MIMO.defaultModel
            } else {
                storedModel
            }

            else -> storedModel ?: provider.defaultModel
        }
    }

    private fun encodeSummaryCache(cache: SummaryCache): String {
        val result = cache.result
        return JSONObject().apply {
            put("rangeName", cache.rangeName)
            put("generatedAtMillis", cache.generatedAtMillis ?: JSONObject.NULL)
            put("topKeywords", JSONArray(result.topKeywords))
            put("commonProblems", JSONArray(result.commonProblems))
            put(
                "topSubjects",
                JSONArray(
                    result.topSubjects.map { (name, duration) ->
                        JSONObject().apply {
                            put("name", name)
                            put("duration", duration)
                        }
                    }
                )
            )
            put(
                "dailyDurations",
                JSONObject().apply {
                    result.dailyDurations.forEach { (date, duration) ->
                        put(date, duration)
                    }
                }
            )
            put("topTomorrowGoals", JSONArray(result.topTomorrowGoals))
            put("naturalLanguageSummary", result.naturalLanguageSummary)
            put("totalStudyMillis", result.totalStudyMillis)
            put("coverDays", result.coverDays)
            put("strengths", JSONArray(result.strengths))
            put("suggestions", JSONArray(result.suggestions))
            put("source", result.source.name)
            put("warningMessage", result.warningMessage ?: JSONObject.NULL)
        }.toString()
    }

    private fun decodeSummaryCache(raw: String): SummaryCache? {
        return runCatching {
            val json = JSONObject(raw)
            val result = SummaryResult(
                topKeywords = json.optJSONArray("topKeywords").toStringList(),
                commonProblems = json.optJSONArray("commonProblems").toStringList(),
                topSubjects = json.optJSONArray("topSubjects").toTopSubjects(),
                dailyDurations = json.optJSONObject("dailyDurations").toDurationMap(),
                topTomorrowGoals = json.optJSONArray("topTomorrowGoals").toStringList(),
                naturalLanguageSummary = json.optString("naturalLanguageSummary"),
                totalStudyMillis = json.optLong("totalStudyMillis"),
                coverDays = json.optInt("coverDays"),
                strengths = json.optJSONArray("strengths").toStringList(),
                suggestions = json.optJSONArray("suggestions").toStringList(),
                source = json.optString("source")
                    .takeIf { it.isNotBlank() }
                    ?.let { SummarySource.entries.firstOrNull { source -> source.name == it } }
                    ?: SummarySource.RULE,
                warningMessage = json.optString("warningMessage")
                    .takeIf { it.isNotBlank() && it != "null" }
            )

            if (result.naturalLanguageSummary.isBlank()) {
                null
            } else {
                SummaryCache(
                    rangeName = json.optString("rangeName").takeIf { it.isNotBlank() } ?: "LAST_7_DAYS",
                    result = result,
                    generatedAtMillis = json.optLong("generatedAtMillis").takeIf { json.has("generatedAtMillis") && !json.isNull("generatedAtMillis") }
                )
            }
        }.getOrNull()
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) {
                optString(index)
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let(::add)
            }
        }
    }

    private fun JSONArray?.toTopSubjects(): List<Pair<String, Long>> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                if (name.isEmpty()) continue
                add(name to item.optLong("duration"))
            }
        }
    }

    private fun JSONObject?.toDurationMap(): Map<String, Long> {
        if (this == null) return emptyMap()
        return buildMap {
            keys().forEach { key ->
                put(key, optLong(key))
            }
        }
    }

    private fun decodeModelHistory(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val model = array.optString(index).trim()
                    if (model.isNotBlank() && model !in this) {
                        add(model)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeModelHistory(models: List<String>): String {
        return JSONArray(models.take(8)).toString()
    }

    private fun addModelToHistory(current: List<String>, model: String): List<String> {
        val normalized = model.trim()
        if (normalized.isBlank()) return current
        return listOf(normalized)
            .plus(current.filterNot { it.equals(normalized, ignoreCase = true) })
            .take(8)
    }
}
