package com.nanogpt.chat.ui.assistants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.entity.AssistantEntity
import com.nanogpt.chat.data.repository.AssistantRepository
import com.nanogpt.chat.data.remote.api.NanoChatApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

@HiltViewModel
class AssistantsViewModel @Inject constructor(
    private val assistantRepository: AssistantRepository,
    private val api: NanoChatApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantsUiState())
    val uiState: StateFlow<AssistantsUiState> = _uiState.asStateFlow()

    init {
        observeAssistants()
        fetchUserModels()
    }

    private fun observeAssistants() {
        viewModelScope.launch {
            assistantRepository.getAssistants().collect { assistants ->
                _uiState.value = _uiState.value.copy(
                    assistants = assistants,
                    isLoading = false
                )
            }
        }
    }

    private fun fetchUserModels() {
        viewModelScope.launch {
            try {
                val response = api.getUserModels()
                if (response.isSuccessful) {
                    val body = response.body()?.string() ?: "{}"
                    val jsonElement = Json.parseToJsonElement(body)
                    val userModels = parseUserModelsResponse(jsonElement)
                    _uiState.value = _uiState.value.copy(
                        availableModels = userModels
                            .filter { it.enabled }
                            .sortedByDescending { it.pinned }
                            .map { it.modelId to it.name }
                    )
                }
            } catch (e: Exception) {
                // Keep empty list on error, will fall back to hardcoded models
                android.util.Log.e("AssistantsViewModel", "Failed to fetch user models", e)
            }
        }
    }

    private fun parseUserModelsResponse(jsonElement: kotlinx.serialization.json.JsonElement): List<UserModelInfo> {
        val models = mutableListOf<UserModelInfo>()
        if (jsonElement is kotlinx.serialization.json.JsonObject) {
            jsonElement.forEach { (key, value) ->
                if (value is kotlinx.serialization.json.JsonObject) {
                    val modelId = value["modelId"]?.jsonPrimitive?.content ?: key
                    val name = value["name"]?.jsonPrimitive?.content ?: modelId
                    val enabled = value["enabled"]?.jsonPrimitive?.content?.toBoolean() ?: true
                    val pinned = value["pinned"]?.jsonPrimitive?.content?.toBoolean() ?: false
                    models.add(UserModelInfo(modelId, name, enabled, pinned))
                }
            }
        }
        return models
    }

    fun createAssistant(
        name: String,
        instructions: String,
        modelId: String,
        description: String?,
        webSearchEnabled: Boolean,
        webSearchProvider: String?,
        webSearchMode: String?,
        temperature: Double?,
        topP: Double?,
        reasoningEffort: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = assistantRepository.createAssistant(
                name = name,
                instructions = instructions,
                modelId = modelId,
                description = description,
                webSearchEnabled = webSearchEnabled,
                webSearchProvider = webSearchProvider,
                temperature = temperature,
                topP = topP,
                reasoningEffort = reasoningEffort,
                webSearchMode = webSearchMode
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateAssistant(
        id: String,
        name: String,
        instructions: String,
        modelId: String,
        description: String?,
        webSearchEnabled: Boolean,
        webSearchProvider: String?,
        webSearchMode: String?,
        temperature: Double?,
        topP: Double?,
        reasoningEffort: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = assistantRepository.updateAssistant(
                id = id,
                name = name.takeIf { it.isNotBlank() },
                instructions = instructions.takeIf { it.isNotBlank() },
                modelId = modelId.takeIf { it.isNotBlank() },
                description = description,
                webSearchEnabled = webSearchEnabled,
                webSearchProvider = webSearchProvider,
                temperature = temperature,
                topP = topP,
                reasoningEffort = reasoningEffort,
                webSearchMode = webSearchMode
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun deleteAssistant(assistant: AssistantEntity) {
        viewModelScope.launch {
            assistantRepository.deleteAssistant(assistant.id)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                assistantRepository.refreshAssistants()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}

data class AssistantsUiState(
    val assistants: List<AssistantEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val availableModels: List<Pair<String, String>> = emptyList() // (modelId, name)
)

private data class UserModelInfo(
    val modelId: String,
    val name: String,
    val enabled: Boolean,
    val pinned: Boolean
)
