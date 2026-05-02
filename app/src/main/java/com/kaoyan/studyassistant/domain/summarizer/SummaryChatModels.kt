package com.kaoyan.studyassistant.domain.summarizer

enum class SummaryChatRole {
    USER,
    ASSISTANT;

    companion object {
        fun fromStorageValue(value: String?): SummaryChatRole {
            return entries.firstOrNull { it.name == value } ?: ASSISTANT
        }
    }
}

data class SummaryChatMessage(
    val id: Long,
    val role: SummaryChatRole,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
