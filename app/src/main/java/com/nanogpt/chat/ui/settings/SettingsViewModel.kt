package com.nanogpt.chat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.SettingsUpdates
import com.nanogpt.chat.data.remote.dto.UserSettingsDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: NanoChatApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        fetchSettings()
    }

    private fun fetchSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = api.getUserSettings()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        settings = response.body()!!,
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

    fun updatePrivacyMode(enabled: Boolean) {
        updateSetting(SettingsUpdates(privacyMode = enabled)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(privacyMode = enabled)
            )
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
        updateSetting(SettingsUpdates(chatTitleModel = model)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(chatTitleModel = model)
            )
        }
    }

    fun updateFollowUpQuestionsModel(model: String?) {
        updateSetting(SettingsUpdates(followUpQuestionsModel = model)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(followUpQuestionsModel = model)
            )
        }
    }

    fun updateTtsModel(model: String?) {
        updateSetting(SettingsUpdates(ttsModel = model)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(ttsModel = model)
            )
        }
    }

    fun updateTtsVoice(voice: String?) {
        updateSetting(SettingsUpdates(ttsVoice = voice)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(ttsVoice = voice)
            )
        }
    }

    fun updateTtsSpeed(speed: Float) {
        updateSetting(SettingsUpdates(ttsSpeed = speed)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(ttsSpeed = speed)
            )
        }
    }

    fun updateSttModel(model: String?) {
        updateSetting(SettingsUpdates(sttModel = model)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(sttModel = model)
            )
        }
    }

    fun updateNanoGptMcp(enabled: Boolean) {
        updateSetting(SettingsUpdates(nanoGptMcpEnabled = enabled)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings?.copy(nanoGptMcpEnabled = enabled)
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
}

data class SettingsUiState(
    val settings: UserSettingsDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
