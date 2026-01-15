package com.nanogpt.chat.ui.auth.login

import android.util.Log
import androidx.lifecycle.ViewModel
import com.nanogpt.chat.BuildConfig
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.remote.api.NanoChatApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: NanoChatApi,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onApiKeyChange(apiKey: String) {
        _uiState.value = _uiState.value.copy(apiKey = apiKey, error = null)
    }

    fun signIn(onSuccess: () -> Unit) {
        val apiKey = _uiState.value.apiKey.trim()

        // Validation checks
        when {
            apiKey.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Please enter your API key")
                return
            }
            !isValidSessionToken(apiKey) -> {
                _uiState.value = _uiState.value.copy(
                    error = "Invalid API key format. API keys should be at least 16 characters and contain only letters, numbers, hyphens, and underscores."
                )
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                if (BuildConfig.DEBUG) {
                    Log.d("LoginViewModel", "Saving API key...")
                }
                // Save the API key to secure storage
                secureStorage.saveSessionToken(apiKey)

                _uiState.value = _uiState.value.copy(isLoading = false)
                if (BuildConfig.DEBUG) {
                    Log.d("LoginViewModel", "API key saved successfully!")
                }
                onSuccess()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("LoginViewModel", "Error saving API key", e)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error saving API key: ${e.message}"
                )
            }
        }
    }

    /**
     * Validate session token format before storing.
     * Checks for minimum length and valid characters to prevent malformed tokens.
     */
    private fun isValidSessionToken(token: String): Boolean {
        // Remove any whitespace that user might have accidentally included
        val cleanToken = token.replace(Regex("\\s+"), "")

        // Basic validation:
        // - Minimum 16 characters (common for JWT and similar tokens)
        // - Only contains valid characters (alphanumeric, hyphens, underscores)
        // - For JWT tokens, should have 3 parts separated by dots (but we're lenient here)
        return cleanToken.length >= 16 &&
                cleanToken.matches(Regex("^[a-zA-Z0-9._-]+$"))
    }
}

data class LoginUiState(
    val apiKey: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
