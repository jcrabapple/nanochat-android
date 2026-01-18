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
        instructions: String,
        modelId: String,
        description: String? = null,
        webSearchEnabled: Boolean,
        webSearchProvider: String?,
        temperature: Double? = null,
        topP: Double? = null,
        reasoningEffort: String? = null,
        webSearchMode: String? = null,
        webSearchExaDepth: String? = null,
        webSearchContextSize: String? = null,
        webSearchKagiSource: String? = null,
        webSearchValyuSearchType: String? = null
    ): Result<AssistantEntity> {
        return try {
            // Create locally first
            val localAssistant = AssistantEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,  // Store description locally
                instructions = instructions,
                modelId = modelId,
                webSearchEnabled = webSearchEnabled,
                webSearchProvider = webSearchProvider,
                icon = null,
                temperature = temperature,
                topP = topP,
                maxTokens = null,
                contextSize = null,
                reasoningEffort = reasoningEffort,
                webSearchMode = webSearchMode,
                webSearchExaDepth = webSearchExaDepth,
                webSearchContextSize = webSearchContextSize,
                webSearchKagiSource = webSearchKagiSource,
                webSearchValyuSearchType = webSearchValyuSearchType,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )

            assistantDao.insertAssistant(localAssistant)

            // Try to sync with server
            try {
                // Map entity fields to DTO field names
                val defaultWebSearchMode = if (webSearchEnabled) {
                    webSearchMode ?: "standard"
                } else {
                    null
                }

                val response = api.createAssistant(
                    CreateAssistantRequest(
                        name = name,
                        description = null,  // Description not sent to backend
                        systemPrompt = instructions,  // Map: instructions -> systemPrompt
                        defaultModelId = modelId,  // Map: modelId -> defaultModelId
                        defaultWebSearchMode = defaultWebSearchMode,  // Map: webSearchEnabled + webSearchMode -> defaultWebSearchMode
                        defaultWebSearchProvider = webSearchProvider,  // Map: webSearchProvider -> defaultWebSearchProvider
                        defaultWebSearchExaDepth = webSearchExaDepth,
                        defaultWebSearchContextSize = webSearchContextSize,
                        defaultWebSearchKagiSource = webSearchKagiSource,
                        defaultWebSearchValyuSearchType = webSearchValyuSearchType,
                        icon = null,
                        temperature = temperature,
                        topP = topP,
                        maxTokens = null,
                        contextSize = null,
                        reasoningEffort = reasoningEffort
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!

                    // Delete the local temporary assistant and insert/update with server data
                    // But preserve the description we set locally
                    assistantDao.deleteAssistantById(localAssistant.id)
                    val serverAssistant = dto.toEntity().copy(description = description)
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
        instructions: String? = null,
        modelId: String? = null,
        description: String? = null,
        webSearchEnabled: Boolean? = null,
        webSearchProvider: String? = null,
        temperature: Double? = null,
        topP: Double? = null,
        reasoningEffort: String? = null,
        webSearchMode: String? = null,
        webSearchExaDepth: String? = null,
        webSearchContextSize: String? = null,
        webSearchKagiSource: String? = null,
        webSearchValyuSearchType: String? = null
    ): Result<Unit> {
        return try {
            val existing = assistantDao.getAssistantById(id)
            if (existing != null) {
                val updated = existing.copy(
                    name = name ?: existing.name,
                    instructions = instructions ?: existing.instructions,
                    description = description ?: existing.description,  // Update description locally
                    modelId = modelId ?: existing.modelId,
                    webSearchEnabled = webSearchEnabled ?: existing.webSearchEnabled,
                    webSearchProvider = webSearchProvider ?: existing.webSearchProvider,
                    temperature = temperature,
                    topP = topP,
                    reasoningEffort = reasoningEffort,
                    webSearchMode = webSearchMode,
                    webSearchExaDepth = webSearchExaDepth,
                    webSearchContextSize = webSearchContextSize,
                    webSearchKagiSource = webSearchKagiSource,
                    webSearchValyuSearchType = webSearchValyuSearchType,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING
                )
                assistantDao.updateAssistant(updated)

                // Sync with backend (excluding description)
                try {
                    val defaultWebSearchMode = if (updated.webSearchEnabled) {
                        updated.webSearchMode ?: "standard"
                    } else {
                        "off"
                    }

                    api.updateAssistant(
                        id = id,
                        updates = com.nanogpt.chat.data.remote.dto.AssistantUpdates(
                            name = name,
                            systemPrompt = instructions,
                            defaultModelId = modelId,
                            defaultWebSearchMode = defaultWebSearchMode,
                            defaultWebSearchProvider = updated.webSearchProvider,
                            defaultWebSearchExaDepth = updated.webSearchExaDepth,
                            defaultWebSearchContextSize = updated.webSearchContextSize,
                            defaultWebSearchKagiSource = updated.webSearchKagiSource,
                            defaultWebSearchValyuSearchType = updated.webSearchValyuSearchType,
                            temperature = temperature,
                            topP = topP,
                            reasoningEffort = reasoningEffort
                        )
                    )
                    // Mark as synced if successful
                    assistantDao.updateAssistant(updated.copy(syncStatus = SyncStatus.SYNCED))
                } catch (e: Exception) {
                    // Keep local changes even if sync fails
                    android.util.Log.e("AssistantRepository", "Failed to sync assistant update", e)
                }
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
                entities.forEach { serverAssistant ->
                    // Preserve local-only fields (description) when refreshing from server
                    val existing = assistantDao.getAssistantById(serverAssistant.id)
                    val assistantToSave = if (existing != null && existing.description != null) {
                        serverAssistant.copy(description = existing.description)
                    } else {
                        serverAssistant
                    }
                    assistantDao.insertAssistant(assistantToSave)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to refresh: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync all pending assistants to the server.
     * Called on app startup and when user refreshes the assistant list.
     *
     * For new assistants (that were never synced), attempts to create them on the server.
     * For updated assistants (that failed to sync updates), attempts to update them on the server.
     */
    suspend fun syncPendingAssistants(): Result<Int> {
        return try {
            val pendingAssistants = assistantDao.getPendingAssistants()
            var syncedCount = 0

            for (assistant in pendingAssistants) {
                try {
                    // Map entity fields to DTO field names
                    val defaultWebSearchMode = if (assistant.webSearchEnabled) {
                        assistant.webSearchMode ?: "standard"
                    } else {
                        "off"
                    }

                    // Try to update the assistant (works for both new and existing)
                    // If it's a new assistant, the server will return 404, and we'll create it instead
                    val updateResponse = api.updateAssistant(
                        id = assistant.id,
                        updates = com.nanogpt.chat.data.remote.dto.AssistantUpdates(
                            name = assistant.name,
                            systemPrompt = assistant.instructions,
                            defaultModelId = assistant.modelId,
                            defaultWebSearchMode = defaultWebSearchMode,
                            defaultWebSearchProvider = assistant.webSearchProvider,
                            defaultWebSearchExaDepth = assistant.webSearchExaDepth,
                            defaultWebSearchContextSize = assistant.webSearchContextSize,
                            defaultWebSearchKagiSource = assistant.webSearchKagiSource,
                            defaultWebSearchValyuSearchType = assistant.webSearchValyuSearchType,
                            temperature = assistant.temperature,
                            topP = assistant.topP,
                            reasoningEffort = assistant.reasoningEffort
                        )
                    )

                    if (updateResponse.isSuccessful) {
                        // Update successful, mark as synced
                        assistantDao.updateAssistant(assistant.copy(syncStatus = SyncStatus.SYNCED))
                        syncedCount++
                    } else if (updateResponse.code() == 404) {
                        // Assistant doesn't exist on server (new assistant), create it
                        val createResponse = api.createAssistant(
                            CreateAssistantRequest(
                                name = assistant.name,
                                description = null,
                                systemPrompt = assistant.instructions,
                                defaultModelId = assistant.modelId,
                                defaultWebSearchMode = if (assistant.webSearchEnabled) assistant.webSearchMode else null,
                                defaultWebSearchProvider = assistant.webSearchProvider,
                                defaultWebSearchExaDepth = assistant.webSearchExaDepth,
                                defaultWebSearchContextSize = assistant.webSearchContextSize,
                                defaultWebSearchKagiSource = assistant.webSearchKagiSource,
                                defaultWebSearchValyuSearchType = assistant.webSearchValyuSearchType,
                                icon = assistant.icon,
                                temperature = assistant.temperature,
                                topP = assistant.topP,
                                maxTokens = assistant.maxTokens,
                                contextSize = assistant.contextSize,
                                reasoningEffort = assistant.reasoningEffort
                            )
                        )

                        if (createResponse.isSuccessful && createResponse.body() != null) {
                            // Delete the local temporary assistant and insert with server data
                            assistantDao.deleteAssistantById(assistant.id)
                            val serverAssistant = createResponse.body()!!.toEntity().copy(
                                description = assistant.description
                            )
                            assistantDao.insertAssistant(serverAssistant)
                            syncedCount++
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AssistantRepository", "Failed to sync assistant ${assistant.id}", e)
                    // Continue with next assistant
                }
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Extension function to convert DTO to Entity
fun AssistantDto.toEntity(): AssistantEntity {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    // Map: defaultWebSearchMode -> webSearchEnabled + webSearchMode
    val webSearchEnabled = defaultWebSearchMode != null && defaultWebSearchMode != "off"
    val webSearchMode = if (defaultWebSearchMode == "off") null else defaultWebSearchMode

    return AssistantEntity(
        id = id,
        name = name,
        description = description,
        instructions = systemPrompt,  // Map: systemPrompt -> instructions
        modelId = defaultModelId ?: "gpt-4o-mini",  // Map: defaultModelId -> modelId
        webSearchEnabled = webSearchEnabled,  // Map: defaultWebSearchMode -> webSearchEnabled
        webSearchProvider = defaultWebSearchProvider,  // Map: defaultWebSearchProvider -> webSearchProvider
        icon = icon,
        temperature = temperature,
        topP = topP,
        maxTokens = maxTokens,
        contextSize = contextSize,
        reasoningEffort = reasoningEffort,
        webSearchMode = webSearchMode,  // Map: defaultWebSearchMode -> webSearchMode
        webSearchExaDepth = defaultWebSearchExaDepth,
        webSearchContextSize = defaultWebSearchContextSize,
        webSearchKagiSource = defaultWebSearchKagiSource,
        webSearchValyuSearchType = defaultWebSearchValyuSearchType,
        createdAt = sdf.parse(createdAt)?.time ?: System.currentTimeMillis(),
        updatedAt = sdf.parse(updatedAt)?.time ?: System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}
