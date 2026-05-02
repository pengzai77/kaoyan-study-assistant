package com.kaoyan.studyassistant.data.local.dao

import androidx.room.*
import com.kaoyan.studyassistant.data.local.entity.GoalSettings
import kotlinx.coroutines.flow.Flow

/**
 * 考研目标设置 DAO
 * 全局只有一条记录（id=1），使用 REPLACE 策略实现 upsert
 */
@Dao
interface GoalSettingsDao {

    /** 查询目标设置（Flow，实时响应变化） */
    @Query("SELECT * FROM goal_settings WHERE id = 1 LIMIT 1")
    fun getGoalSettings(): Flow<GoalSettings?>

    /** 查询目标设置（同步，用于备份） */
    @Query("SELECT * FROM goal_settings WHERE id = 1 LIMIT 1")
    suspend fun getGoalSettingsSync(): GoalSettings?

    /** 保存目标设置（upsert） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGoalSettings(settings: GoalSettings)
}
