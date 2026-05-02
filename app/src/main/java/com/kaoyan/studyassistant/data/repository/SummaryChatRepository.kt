package com.kaoyan.studyassistant.data.repository

import com.kaoyan.studyassistant.data.local.dao.SummaryChatMessageDao
import com.kaoyan.studyassistant.data.local.entity.SummaryChatMessageEntity
import com.kaoyan.studyassistant.domain.summarizer.SummaryChatMessage
import com.kaoyan.studyassistant.domain.summarizer.SummaryChatRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryChatRepository @Inject constructor(
    private val dao: SummaryChatMessageDao
) {
    fun observeConversation(
        conversationId: String = DEFAULT_CONVERSATION_ID
    ): Flow<List<SummaryChatMessage>> {
        return dao.observeConversation(conversationId).map { messages ->
            messages.map { entity ->
                SummaryChatMessage(
                    id = entity.id,
                    role = SummaryChatRole.fromStorageValue(entity.role),
                    content = entity.content,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    suspend fun addMessage(
        role: SummaryChatRole,
        content: String,
        summaryRange: String?,
        conversationId: String = DEFAULT_CONVERSATION_ID
    ): Long {
        return dao.insert(
            SummaryChatMessageEntity(
                conversationId = conversationId,
                role = role.name,
                content = content.trim(),
                summaryRange = summaryRange
            )
        )
    }

    suspend fun deleteMessage(messageId: Long) {
        dao.deleteById(messageId)
    }

    suspend fun clearConversation(conversationId: String = DEFAULT_CONVERSATION_ID) {
        dao.clearConversation(conversationId)
    }

    companion object {
        const val DEFAULT_CONVERSATION_ID = "summary_chat"
    }
}
