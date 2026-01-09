package com.nanogpt.chat.ui.auth.login

import android.util.Log
import androidx.lifecycle.ViewModel
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

        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your API key")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                Log.d("LoginViewModel", "Saving API key: ${apiKey.take(20)}...")
                // Save the API key to secure storage
                secureStorage.saveSessionToken(apiKey)

                // For now, just save the API key without validation
                // The real validation will happen when making actual API calls
                _uiState.value = _uiState.value.copy(isLoading = false)
                Log.d("LoginViewModel", "API key saved successfully!")
                onSuccess()
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error saving API key", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error saving API key: ${e.message}"
                )
            }
        }
    }
}

data class LoginUiState(
    val apiKey: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
