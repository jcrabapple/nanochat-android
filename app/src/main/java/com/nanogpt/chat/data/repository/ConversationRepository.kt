package com.nanogpt.chat.data.repository

import com.nanogpt.chat.data.local.dao.ConversationDao
import com.nanogpt.chat.data.local.dao.MessageDao
import com.nanogpt.chat.data.local.entity.ConversationEntity
import com.nanogpt.chat.data.local.entity.SyncStatus
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.ConversationDto
import com.nanogpt.chat.data.remote.dto.CreateConversationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val api: NanoChatApi
) {

    fun getConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }

    suspend fun fetchConversationsFromApi(): Result<List<ConversationEntity>> {
        return try {
            val response = api.getConversations()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val entities = dtos.map { it.toEntity() }
                // Clear local conversations and insert fresh ones from API
                conversationDao.deleteAllConversations()
                entities.forEach { conversationDao.insertConversation(it) }
                Result.success(entities)
            } else {
                Result.failure(Exception("Failed to fetch conversations: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPinnedConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getPinnedConversations()
    }

    suspend fun getConversationById(id: String): ConversationEntity? {
        return conversationDao.getConversationById(id)
    }

  suspend fun getConversationWithMessages(id: String): Result<ConversationEntity?> {
        return try {
            // Try to get from API first to get fresh data with messages
            val response = api.getConversation(id)
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                if (dtos.isNotEmpty()) {
                    val conversationDto = dtos.first()
                    val conversationEntity = conversationDto.toEntity()

                    // Insert conversation and messages
                    conversationDao.insertConversation(conversationEntity)

                    // Insert messages if present
                    conversationDto.messages?.let { messageDtos ->
                        val messageEntities = messageDtos.map { messageDto ->
                            messageDto.toEntity(conversationEntity.id)
                        }
                        messageDao.insertMessages(messageEntities)
                    }

                    Result.success(conversationEntity)
                } else {
                    Result.success(conversationDao.getConversationById(id))
                }
            } else {
                // Fallback to local if API fails
                Result.success(conversationDao.getConversationById(id))
            }
        } catch (e: Exception) {
            // Fallback to local if network fails
            Result.success(conversationDao.getConversationById(id))
        }
    }

  
    suspend fun updateConversationTitle(id: String, title: String): Result<Unit> {
        return try {
            conversationDao.getConversationById(id)?.let { conversation ->
                val updated = conversation.copy(title = title, syncStatus = SyncStatus.PENDING)
                conversationDao.updateConversation(updated)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePin(id: String): Result<Unit> {
        return try {
            val conversation = conversationDao.getConversationById(id)
            conversation?.let {
                conversationDao.updatePinnedStatus(id, !it.pinned)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(id: String): Result<Unit> {
        return try {
            // Delete from API
            val response = api.deleteConversation(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete conversation: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun refreshConversations(): Result<Unit> {
        return try {
            val response = api.getConversations()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val entities = dtos.map { it.toEntity() }
                entities.forEach { conversationDao.insertConversation(it) }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to refresh: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Extension function to convert DTO to Entity
fun ConversationDto.toEntity(): ConversationEntity {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    return ConversationEntity(
        id = id,
        title = title,
        userId = userId,
        assistantId = assistantId,
        projectId = projectId,
        modelId = modelId,
        createdAt = sdf.parse(createdAt)?.time ?: System.currentTimeMillis(),
        updatedAt = sdf.parse(updatedAt)?.time ?: System.currentTimeMillis(),
        messageCount = messageCount,
        pinned = pinned,
        costUsd = costUsd,
        syncStatus = SyncStatus.SYNCED
    )
}
