package com.kaoyan.studyassistant.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "summary_chat_messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["createdAt"])
    ]
)
data class SummaryChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val conversationId: String = "summary_chat",
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val summaryRange: String? = null
)
