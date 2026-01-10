package com.nanogpt.chat.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.SettingsUpdates
import com.nanogpt.chat.data.remote.dto.UserSettingsDto
import com.nanogpt.chat.data.remote.dto.parseUserModelsResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: NanoChatApi,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        fetchSettings()
        fetchUserModels()
        loadLocalTtsSttSettings()
    }

    private fun loadLocalTtsSttSettings() {
        _uiState.value = _uiState.value.copy(
            ttsModel = secureStorage.getTtsModel(),
            ttsVoice = secureStorage.getTtsVoice(),
            ttsSpeed = secureStorage.getTtsSpeed(),
            sttModel = secureStorage.getSttModel()
        )
    }

    private fun fetchUserModels() {
        viewModelScope.launch {
            try {
                val response = api.getUserModels()
                if (response.isSuccessful && response.body() != null) {
                    val json = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                    val jsonElement = json.parseToJsonElement(response.body()!!.string())
                    val models = parseUserModelsResponse(jsonElement)
                    _uiState.value = _uiState.value.copy(userModels = models)
                }
            } catch (e: Exception) {
                // Log error but don't show to user - models are optional for settings
                _uiState.value = _uiState.value.copy(userModels = emptyList())
            }
        }
    }

    private fun fetchSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = api.getUserSettings()
                if (response.isSuccessful && response.body() != null) {
                    val settings = response.body()!!
                    // Normalize empty strings to null for model IDs
                    val normalizedSettings = settings.copy(
                        titleModelId = normalizeModelId(settings.titleModelId),
                        followUpModelId = normalizeModelId(settings.followUpModelId)
                    )
                    _uiState.value = _uiState.value.copy(
                        settings = normalizedSettings,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load settings: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading settings: ${e.message}"
                )
            }
        }
    }

    fun updateContextMemory(enabled: Boolean) {
        updateSetting(SettingsUpdates(contextMemoryEnabled = enabled)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(contextMemoryEnabled = enabled)
            )
        }
    }

    fun updatePersistentMemory(enabled: Boolean) {
        updateSetting(SettingsUpdates(persistentMemoryEnabled = enabled)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(persistentMemoryEnabled = enabled)
            )
        }
    }

    fun updateYoutubeTranscripts(enabled: Boolean) {
        updateSetting(SettingsUpdates(youtubeTranscriptsEnabled = enabled)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(youtubeTranscriptsEnabled = enabled)
            )
        }
    }

    fun updateWebScraping(enabled: Boolean) {
        updateSetting(SettingsUpdates(webScrapingEnabled = enabled)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(webScrapingEnabled = enabled)
            )
        }
    }

    fun updateFollowUpQuestions(enabled: Boolean) {
        updateSetting(SettingsUpdates(followUpQuestionsEnabled = enabled)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(followUpQuestionsEnabled = enabled)
            )
        }
    }

    fun updateChatTitleModel(model: String?) {
        // Convert null to empty string for the default "GLM-4.5-Air" option
        val modelValue = model ?: ""
        updateSetting(SettingsUpdates(titleModelId = modelValue)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(titleModelId = modelValue.ifEmpty { null })
            )
        }
    }

    fun updateFollowUpQuestionsModel(model: String?) {
        // Convert null to empty string for the default "GLM-4.5-Air" option
        val modelValue = model ?: ""
        updateSetting(SettingsUpdates(followUpModelId = modelValue)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(followUpModelId = modelValue.ifEmpty { null })
            )
        }
    }

    // Helper to normalize model IDs (convert empty string to null)
    private fun normalizeModelId(modelId: String?): String? {
        return modelId?.ifEmpty { null }
    }

    fun updateTtsModel(model: String?) {
        secureStorage.saveTtsModel(model)
        _uiState.value = _uiState.value.copy(ttsModel = model)

        // Auto-switch voice to default for new model
        val newVoice = when (model) {
            "Kokoro-82m" -> "af_alloy"
            "Elevenlabs-Turbo-V2.5" -> "Rachel"
            "tts-1", "tts-1-hd", "gpt-4o-mini-tts" -> "alloy"
            else -> "alloy"
        }
        updateTtsVoice(newVoice)
    }

    fun updateTtsVoice(voice: String?) {
        secureStorage.saveTtsVoice(voice)
        _uiState.value = _uiState.value.copy(ttsVoice = voice)
    }

    fun updateTtsSpeed(speed: Float) {
        secureStorage.saveTtsSpeed(speed)
        _uiState.value = _uiState.value.copy(ttsSpeed = speed)
    }

    fun updateSttModel(model: String?) {
        secureStorage.saveSttModel(model)
        _uiState.value = _uiState.value.copy(sttModel = model)
    }

    fun updateMcpEnabled(enabled: Boolean) {
        updateSetting(SettingsUpdates(mcpEnabled = enabled)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(mcpEnabled = enabled)
            )
        }
    }

    private fun updateSetting(updates: SettingsUpdates, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.updateSettings(updates)
                if (response.isSuccessful && response.body() != null) {
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update setting: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error updating setting: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun deleteAllChats(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.deleteConversation(all = "true")
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Failed to delete all chats: ${response.code()}")
                }
            } catch (e: Exception) {
                onError("Error deleting all chats: ${e.message}")
            }
        }
    }
}

data class SettingsUiState(
    val settings: UserSettingsDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val userModels: List<com.nanogpt.chat.data.remote.dto.UserModelDto> = emptyList(),
    // Local TTS/STT settings (not synced to backend)
    val ttsModel: String? = null,
    val ttsVoice: String? = null,
    val ttsSpeed: Float = 1.0f,
    val sttModel: String? = null
)
