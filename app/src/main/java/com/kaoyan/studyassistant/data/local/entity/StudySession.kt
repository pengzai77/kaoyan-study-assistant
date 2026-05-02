package com.kaoyan.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 学习记录实体类
 * 每次计时结束后保存为一条学习记录
 */
@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL  // 科目删除后，记录保留但 subjectId 置为 null
        )
    ],
    indices = [Index("subjectId"), Index("date")]
)
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 关联的科目 ID，可为 null（科目被删除时） */
    val subjectId: Long? = null,
    /** 科目名称快照（即使科目被删除也能显示历史记录） */
    val subjectName: String = "",
    /** 开始时间戳（毫秒） */
    val startTime: Long,
    /** 结束时间戳（毫秒） */
    val endTime: Long,
    /** 学习时长（毫秒） */
    val durationMillis: Long,
    /** 备注 */
    val note: String = "",
    /** 日期字符串，格式 "yyyy-MM-dd"，用于按天查询 */
    val date: String
)
