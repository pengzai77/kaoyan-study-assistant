package com.kaoyan.studyassistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaoyan.studyassistant.data.local.entity.SummaryChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryChatMessageDao {
    @Query(
        """
        SELECT * FROM summary_chat_messages
        WHERE conversationId = :conversationId
        ORDER BY createdAt ASC, id ASC
        """
    )
    fun observeConversation(conversationId: String): Flow<List<SummaryChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: SummaryChatMessageEntity): Long

    @Query("DELETE FROM summary_chat_messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)

    @Query("DELETE FROM summary_chat_messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: String)
}
