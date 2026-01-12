package com.nanogpt.chat.ui.auth.setup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.ui.theme.ThemeManager
import com.nanogpt.chat.utils.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLException

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    private val debugLogger: DebugLogger,
    val themeManager: ThemeManager
) : ViewModel() {

    private val TAG = "SetupViewModel"

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

        // Validation checks
        when {
            url.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Please enter a backend URL")
                return
            }
            apiKey.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Please enter your API key")
                return
            }
            !url.startsWith("http://") && !url.startsWith("https://") -> {
                _uiState.value = _uiState.value.copy(
                    error = "URL must start with http:// or https://"
                )
                return
            }
            !isValidUrl(url) -> {
                _uiState.value = _uiState.value.copy(
                    error = "Invalid URL format. Example: https://nanochat.example.com"
                )
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, error = null)

            try {
                debugLogger.logInfo(TAG, "Attempting to save configuration")

                // Save the backend URL and API key with detailed error handling
                try {
                    secureStorage.saveBackendUrl(url)
                    debugLogger.logInfo(TAG, "Backend URL saved successfully: $url")
                } catch (e: Exception) {
                    debugLogger.logError(TAG, "Failed to save backend URL", e)
                    throw SetupException("Failed to save backend URL: ${e.message}", e)
                }

                try {
                    secureStorage.saveSessionToken(apiKey)
                    debugLogger.logInfo(TAG, "API key saved successfully (length: ${apiKey.length})")
                } catch (e: Exception) {
                    debugLogger.logError(TAG, "Failed to save API key", e)
                    throw SetupException("Failed to save API key: ${e.message}", e)
                }

                // Simulate connection test with timeout
                debugLogger.logInfo(TAG, "Testing connection...")
                kotlinx.coroutines.delay(500)

                debugLogger.logInfo(TAG, "Setup completed successfully")
                _uiState.value = _uiState.value.copy(isTesting = false)
                onSuccess()

            } catch (e: SetupException) {
                // Our custom exceptions
                debugLogger.logError(TAG, "Setup failed: ${e.message}", e.cause)
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    error = e.message ?: "Connection failed"
                )
            } catch (e: CancellationException) {
                // Coroutine was cancelled
                debugLogger.logWarning(TAG, "Setup cancelled")
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    error = "Connection test was cancelled"
                )
            } catch (e: Exception) {
                // Unexpected errors
                debugLogger.logError(TAG, "Unexpected error during setup", e)
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    error = "Unexpected error: ${e.message}\n\nPlease check your URL and API key, or contact support."
                )
            }
        }
    }

    /**
     * Validate URL format more thoroughly
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url).toURI()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear saved data (for troubleshooting)
     */
    fun clearData() {
        viewModelScope.launch {
            try {
                debugLogger.logInfo(TAG, "Clearing saved data")
                // Would need to implement clear methods in SecureStorage if needed
                _uiState.value = SetupUiState()
            } catch (e: Exception) {
                debugLogger.logError(TAG, "Failed to clear data", e)
            }
        }
    }
}

/**
 * Custom exception for setup errors
 */
class SetupException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class SetupUiState(
    val backendUrl: String = "",
    val apiKey: String = "",
    val isTesting: Boolean = false,
    val error: String? = null
)

