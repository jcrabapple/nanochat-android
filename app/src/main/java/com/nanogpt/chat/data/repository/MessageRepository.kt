package com.nanogpt.chat.data.repository

import com.nanogpt.chat.data.local.dao.MessageDao
import com.nanogpt.chat.data.local.entity.MessageEntity
import com.nanogpt.chat.data.local.entity.SyncStatus
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.MessageDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val api: NanoChatApi
) {

    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    suspend fun getMessageById(id: String): MessageEntity? {
        return messageDao.getMessageById(id)
    }

    suspend fun createUserMessage(
        conversationId: String,
        content: String
    ): Result<MessageEntity> {
        return try {
            val message = MessageEntity(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "user",
                content = content,
                createdAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )

            messageDao.insertMessage(message)
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAssistantMessage(
        conversationId: String,
        content: String,
        reasoning: String? = null,
        modelId: String? = null
    ): Result<MessageEntity> {
        return try {
            val message = MessageEntity(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "assistant",
                content = content,
                reasoning = reasoning,
                modelId = modelId,
                createdAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED
            )

            messageDao.insertMessage(message)
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMessageContent(
        messageId: String,
        content: String,
        reasoning: String? = null
    ): Result<Unit> {
        return try {
            messageDao.updateMessageContent(messageId, content)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(id: String): Result<Unit> {
        return try {
            messageDao.deleteMessageById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

  
    suspend fun getMessageCount(conversationId: String): Int {
        return messageDao.getMessageCount(conversationId)
    }

    suspend fun getStarredMessages(): Result<List<MessageDto>> {
        return try {
            val response = api.getStarredMessages()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch starred messages: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleMessageStar(messageId: String, starred: Boolean): Result<Unit> {
        return try {
            val response = api.updateMessage(
                com.nanogpt.chat.data.remote.dto.UpdateMessageRequest(
                    action = "setStarred",
                    messageId = messageId,
                    starred = starred
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val successResponse = response.body()!!
                if (successResponse.ok) {
                    // Update the starred status in local database
                    messageDao.updateMessageStarred(messageId, starred)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to update message: Backend returned ok=false"))
                }
            } else {
                Result.failure(Exception("Failed to update message: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Extension function to convert DTO to Entity
fun MessageDto.toEntity(conversationId: String): MessageEntity {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    val json = Json { ignoreUnknownKeys = true }

    // Combine annotations and images into a single JSON structure
    val allAnnotations = annotations?.toMutableList() ?: mutableListOf()
    // Add images as annotations if they exist
    images?.forEach { imageDto ->
        allAnnotations.add(
            com.nanogpt.chat.data.remote.dto.AnnotationDto(
                type = "image",
                data = kotlinx.serialization.json.buildJsonObject {
                    put("url", kotlinx.serialization.json.JsonPrimitive(imageDto.url))
                    imageDto.storage_id?.let { put("storage_id", kotlinx.serialization.json.JsonPrimitive(it)) }
                    imageDto.fileName?.let { put("fileName", kotlinx.serialization.json.JsonPrimitive(it)) }
                }
            )
        )
    }

    return MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        reasoning = reasoning,
        modelId = modelId,
        annotationsJson = if (allAnnotations.isNotEmpty()) json.encodeToString(allAnnotations) else null,
        followUpSuggestions = followUpSuggestions?.let { json.encodeToString(it) },
        createdAt = sdf.parse(createdAt)?.time ?: System.currentTimeMillis(),
        tokenCount = tokenCount,
        costUsd = costUsd,
        starred = starred,
        syncStatus = SyncStatus.SYNCED
    )
}
