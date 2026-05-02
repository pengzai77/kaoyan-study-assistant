package com.kaoyan.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 每日复盘实体类
 * 用于记录每天的学习复盘内容
 *
 * v2 变更：date 字段加唯一约束，保证每天只有一条复盘记录
 */
@Entity(
    tableName = "daily_reviews",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyReview(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 复盘日期，格式 "yyyy-MM-dd"，唯一 */
    val date: String,
    /** 今日完成内容 */
    val completedContent: String = "",
    /** 今日遇到的问题 */
    val problems: String = "",
    /** 明日计划 */
    val tomorrowPlan: String = "",
    /** 自由补充内容 */
    val extraNotes: String = "",
    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis(),
    /** 最后更新时间戳（毫秒） */
    val updatedAt: Long = System.currentTimeMillis()
)
