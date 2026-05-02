package com.kaoyan.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 考研目标设置 Entity
 *
 * 采用单例行模式（id 固定为 1），全局只有一条记录。
 * 存储在 Room 而非 DataStore 的原因：
 * - 目标数据与学习记录强关联（需要计算完成率），放 Room 便于 JOIN 查询
 * - 科目目标时长需要与 Subject 关联，放 Room 更自然
 */
@Entity(tableName = "goal_settings")
data class GoalSettings(
    @PrimaryKey
    val id: Int = 1,

    /** 目标院校 */
    val targetSchool: String = "",

    /** 目标专业 */
    val targetMajor: String = "",

    /**
     * 考试日期，格式 "yyyy-MM-dd"
     * 空字符串表示未设置
     */
    val examDate: String = "",

    /** 每周学习目标（分钟） */
    val weeklyGoalMinutes: Int = 0
)
