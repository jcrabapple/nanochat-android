package com.nanogpt.chat.data.repository

import com.nanogpt.chat.data.local.dao.ConversationDao
import com.nanogpt.chat.data.local.dao.MessageDao
import com.nanogpt.chat.data.local.entity.ConversationEntity
import com.nanogpt.chat.data.local.entity.SyncStatus
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.ConversationDto
import com.nanogpt.chat.data.remote.dto.CreateConversationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

                // Intelligent merge: insert or update based on updatedAt timestamp
                // This preserves local state and avoids the "delete all" approach
                entities.forEach { entity ->
                    val existing = conversationDao.getConversationById(entity.id)
                    if (existing == null || existing.updatedAt < entity.updatedAt) {
                        // Either conversation doesn't exist locally, or server version is newer
                        conversationDao.insertConversation(entity)
                    }
                    // If local version is newer, keep it (it might have local-only changes)
                }

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
            // Try to get conversation from API first
            val conversationResponse = api.getConversation(id)
            if (conversationResponse.isSuccessful && conversationResponse.body() != null) {
                val conversationDto = conversationResponse.body()!!
                val conversationEntity = conversationDto.toEntity()

                // Insert conversation
                conversationDao.insertConversation(conversationEntity)

                // Fetch messages separately using the dedicated messages endpoint
                val messagesResponse = api.getMessages(id)
                if (messagesResponse.isSuccessful && messagesResponse.body() != null) {
                    val messageDtos = messagesResponse.body()!!

                    android.util.Log.d("ConversationRepository", "Fetched ${messageDtos.size} messages from API for conversation $id")

                    // Delete old messages for this conversation first
                    messageDao.deleteMessagesForConversation(id)

                    // Insert fresh messages
                    val messageEntities = messageDtos.map { messageDto ->
                        messageDto.toEntity(conversationEntity.id)
                    }
                    if (messageEntities.isNotEmpty()) {
                        messageDao.insertMessages(messageEntities)
                        android.util.Log.d("ConversationRepository", "Inserted ${messageEntities.size} messages into database for conversation $id")
                    } else {
                        android.util.Log.w("ConversationRepository", "No messages to insert for conversation $id")
                    }
                } else {
                    android.util.Log.e("ConversationRepository", "Failed to fetch messages for conversation $id: ${messagesResponse.code()}")
                }

                Result.success(conversationEntity)
            } else {
                android.util.Log.e("ConversationRepository", "Failed to fetch conversation $id: ${conversationResponse.code()}")
                // Fallback to local if API fails
                Result.success(conversationDao.getConversationById(id))
            }
        } catch (e: Exception) {
            android.util.Log.e("ConversationRepository", "Exception fetching conversation $id: ${e.message}", e)
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
            // First, unstar all messages in this conversation to clean up starred messages
            unstarAllMessagesInConversation(id)

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

    private suspend fun unstarAllMessagesInConversation(conversationId: String) {
        try {
            // Get all messages for this conversation
            val messages = messageDao.getMessagesForConversation(conversationId)

            // Collect the flow to get the actual list
            val messageList = messages.first()

            // Unstar all starred messages
            messageList.forEach { message ->
                if (message.starred == true) {
                    // Call API to unstar
                    api.updateMessage(
                        message.id,
                        com.nanogpt.chat.data.remote.dto.UpdateMessageRequest(starred = false)
                    )
                }
            }
        } catch (e: Exception) {
            // Log but don't fail the deletion if unstar fails
            android.util.Log.e("ConversationRepository", "Failed to unstar messages: ${e.message}")
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

    suspend fun insertConversation(conversation: ConversationEntity) {
        conversationDao.insertConversation(conversation)
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
