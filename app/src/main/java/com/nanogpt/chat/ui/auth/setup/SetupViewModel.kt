package com.nanogpt.chat.ui.auth.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onBackendUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(backendUrl = url, error = null)
    }

    fun onApiKeyChange(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key, error = null)
    }

    fun testConnection(onSuccess: () -> Unit) {
        val url = _uiState.value.backendUrl.trim()
        val apiKey = _uiState.value.apiKey.trim()

        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a backend URL")
            return
        }

        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your API key")
            return
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _uiState.value = _uiState.value.copy(error = "URL must start with http:// or https://")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, error = null)

            try {
                // Save the backend URL and API key
                secureStorage.saveBackendUrl(url)
                secureStorage.saveSessionToken(apiKey)

                // Simulate connection test
                kotlinx.coroutines.delay(500)

                _uiState.value = _uiState.value.copy(isTesting = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    error = "Failed to connect: ${e.message}"
                )
            }
        }
    }
}

data class SetupUiState(
    val backendUrl: String = "",
    val apiKey: String = "",
    val isTesting: Boolean = false,
    val error: String? = null
)
