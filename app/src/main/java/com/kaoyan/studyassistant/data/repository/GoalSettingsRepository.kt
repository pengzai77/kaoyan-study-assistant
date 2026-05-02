package com.kaoyan.studyassistant.data.repository

import com.kaoyan.studyassistant.data.local.dao.GoalSettingsDao
import com.kaoyan.studyassistant.data.local.entity.GoalSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 考研目标设置 Repository
 */
@Singleton
class GoalSettingsRepository @Inject constructor(
    private val dao: GoalSettingsDao
) {
    fun getGoalSettings(): Flow<GoalSettings?> = dao.getGoalSettings()

    suspend fun getGoalSettingsSync(): GoalSettings? = dao.getGoalSettingsSync()

    suspend fun saveGoalSettings(settings: GoalSettings) = dao.saveGoalSettings(settings)
}
