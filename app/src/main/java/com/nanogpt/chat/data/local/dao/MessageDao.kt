package com.nanogpt.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nanogpt.chat.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT id FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageIdsForConversation(conversationId: String): List<String>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET content = :content WHERE id = :id")
    suspend fun updateMessageContent(id: String, content: String)

    @Query("UPDATE messages SET starred = :starred WHERE id = :id")
    suspend fun updateMessageStarred(id: String, starred: Boolean)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND id NOT IN (:ids)")
    suspend fun deleteMessagesNotIn(conversationId: String, ids: List<String>)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND id NOT IN (:messageIds)")
    suspend fun deleteMessagesNotInList(conversationId: String, messageIds: List<String>)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND id NOT IN (:messageIds)")
    suspend fun deleteMessagesNotInConversation(
        conversationId: String,
        messageIds: List<String>
    )

    @Query("SELECT * FROM messages WHERE syncStatus = 'PENDING'")
    suspend fun getPendingMessages(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: String): Int
}
