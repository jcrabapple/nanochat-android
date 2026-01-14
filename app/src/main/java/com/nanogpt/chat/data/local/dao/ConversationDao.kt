package com.nanogpt.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nanogpt.chat.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE pinned = 1 ORDER BY updatedAt DESC")
    fun getPinnedConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getConversationsByProject(projectId: String): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)

    @Query("DELETE FROM conversations WHERE syncStatus = 'PENDING'")
    suspend fun deletePendingConversations()

    @Query("UPDATE conversations SET pinned = :pinned WHERE id = :id")
    suspend fun updatePinnedStatus(id: String, pinned: Boolean)

    @Query("UPDATE conversations SET projectId = :projectId WHERE id = :id")
    suspend fun updateProjectId(id: String, projectId: String?)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    @Query("SELECT COUNT(*) FROM conversations WHERE projectId = :projectId")
    suspend fun getConversationCountForProject(projectId: String): Int

    @Query("DELETE FROM conversations WHERE projectId = :projectId")
    suspend fun deleteConversationsForProject(projectId: String)
}
