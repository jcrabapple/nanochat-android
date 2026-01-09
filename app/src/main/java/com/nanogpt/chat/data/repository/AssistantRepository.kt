package com.nanogpt.chat.data.repository

import com.nanogpt.chat.data.local.dao.AssistantDao
import com.nanogpt.chat.data.local.entity.AssistantEntity
import com.nanogpt.chat.data.local.entity.SyncStatus
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.AssistantDto
import com.nanogpt.chat.data.remote.dto.CreateAssistantRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantRepository @Inject constructor(
    private val assistantDao: AssistantDao,
    private val api: NanoChatApi
) {

    fun getAssistants(): Flow<List<AssistantEntity>> {
        return assistantDao.getAllAssistants()
    }

    suspend fun getAssistantById(id: String): AssistantEntity? {
        return assistantDao.getAssistantById(id)
    }

    suspend fun createAssistant(
        name: String,
        description: String?,
        instructions: String,
        modelId: String,
        webSearchEnabled: Boolean,
        webSearchProvider: String?
    ): Result<AssistantEntity> {
        return try {
            // Create locally first
            val localAssistant = AssistantEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                instructions = instructions,
                modelId = modelId,
                webSearchEnabled = webSearchEnabled,
                webSearchProvider = webSearchProvider,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )

            assistantDao.insertAssistant(localAssistant)

            // Try to sync with server
            try {
                val response = api.createAssistant(
                    CreateAssistantRequest(
                        name = name,
                        description = description,
                        instructions = instructions,
                        modelId = modelId,
                        webSearchEnabled = webSearchEnabled,
                        webSearchProvider = webSearchProvider
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    val serverAssistant = dto.toEntity()
                    assistantDao.insertAssistant(serverAssistant)
                    Result.success(serverAssistant)
                } else {
                    Result.success(localAssistant)
                }
            } catch (e: Exception) {
                Result.success(localAssistant)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAssistant(
        id: String,
        name: String? = null,
        description: String? = null,
        instructions: String? = null,
        modelId: String? = null,
        webSearchEnabled: Boolean? = null,
        webSearchProvider: String? = null
    ): Result<Unit> {
        return try {
            val existing = assistantDao.getAssistantById(id)
            if (existing != null) {
                val updated = existing.copy(
                    name = name ?: existing.name,
                    description = description ?: existing.description,
                    instructions = instructions ?: existing.instructions,
                    modelId = modelId ?: existing.modelId,
                    webSearchEnabled = webSearchEnabled ?: existing.webSearchEnabled,
                    webSearchProvider = webSearchProvider ?: existing.webSearchProvider,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING
                )
                assistantDao.updateAssistant(updated)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAssistant(id: String): Result<Unit> {
        return try {
            assistantDao.deleteAssistantById(id)

            // Also delete from server
            try {
                api.deleteAssistant(id)
            } catch (e: Exception) {
                // Ignore network errors
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshAssistants(): Result<Unit> {
        return try {
            val response = api.getAssistants()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val entities = dtos.map { it.toEntity() }
                entities.forEach { assistantDao.insertAssistant(it) }
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
fun AssistantDto.toEntity(): AssistantEntity {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    return AssistantEntity(
        id = id,
        name = name,
        description = description,
        instructions = instructions,
        modelId = modelId,
        webSearchEnabled = webSearchEnabled,
        webSearchProvider = webSearchProvider,
        createdAt = sdf.parse(createdAt)?.time ?: System.currentTimeMillis(),
        updatedAt = sdf.parse(updatedAt)?.time ?: System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}
