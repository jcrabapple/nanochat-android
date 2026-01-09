package com.nanogpt.chat.ui.assistants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.entity.AssistantEntity
import com.nanogpt.chat.data.repository.AssistantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantsViewModel @Inject constructor(
    private val assistantRepository: AssistantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantsUiState())
    val uiState: StateFlow<AssistantsUiState> = _uiState.asStateFlow()

    init {
        observeAssistants()
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

    fun createAssistant(
        name: String,
        description: String,
        instructions: String,
        modelId: String,
        webSearchEnabled: Boolean,
        webSearchProvider: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = assistantRepository.createAssistant(
                name = name,
                description = description.takeIf { it.isNotBlank() },
                instructions = instructions,
                modelId = modelId,
                webSearchEnabled = webSearchEnabled,
                webSearchProvider = webSearchProvider
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
        description: String,
        instructions: String,
        modelId: String,
        webSearchEnabled: Boolean,
        webSearchProvider: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = assistantRepository.updateAssistant(
                id = id,
                name = name.takeIf { it.isNotBlank() },
                description = description.takeIf { it.isNotBlank() },
                instructions = instructions.takeIf { it.isNotBlank() },
                modelId = modelId.takeIf { it.isNotBlank() },
                webSearchEnabled = webSearchEnabled,
                webSearchProvider = webSearchProvider
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
    val error: String? = null
)
