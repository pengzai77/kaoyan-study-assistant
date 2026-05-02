package com.kaoyan.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 科目实体类
 * 存储用户自定义的学习科目信息
 *
 * v2 变更：name 字段加唯一约束（indices unique=true），防止重复科目
 */
@Entity(
    tableName = "subjects",
    indices = [Index(value = ["name"], unique = true)]
)
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 科目名称，唯一，如"政治"、"英语"等 */
    val name: String,
    /** 科目颜色，十六进制字符串，如 "#4CAF50" */
    val color: String = "#4A90D9",
    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis(),
    /** 是否为默认科目（默认科目不可删除） */
    val isDefault: Boolean = false
)
