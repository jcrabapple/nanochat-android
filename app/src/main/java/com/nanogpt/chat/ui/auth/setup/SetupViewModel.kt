package com.nanogpt.chat.ui.auth.setup

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.utils.DebugLogger
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.SSLException

@Keep
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    private val debugLogger: DebugLogger
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
        // Trim all whitespace including newlines from API key
        val apiKey = _uiState.value.apiKey.trim().replace(Regex("\\s+"), "")

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

                // Test API connection by calling /api/models endpoint
                // Create a temporary Retrofit instance with the new URL and API key
                debugLogger.logInfo(TAG, "Testing connection with API validation...")

                val json = Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    isLenient = true
                    encodeDefaults = true
                    explicitNulls = false
                }

                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val requestBuilder = original.newBuilder()
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .header("Authorization", "Bearer $apiKey")
                        chain.proceed(requestBuilder.build())
                    }
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val tempRetrofit = Retrofit.Builder()
                    .baseUrl(url)
                    .client(okHttpClient)
                    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                    .build()

                val tempApi = tempRetrofit.create(com.nanogpt.chat.data.remote.api.NanoChatApi::class.java)
                val response = tempApi.getModels()

                if (response.isSuccessful) {
                    debugLogger.logInfo(TAG, "API validation successful - credentials are valid")
                    _uiState.value = _uiState.value.copy(isTesting = false)
                    onSuccess()
                } else {
                    val errorCode = response.code()
                    val errorMessage = when (errorCode) {
                        401 -> "Invalid API key. Please check your API key and generate a new one at /account/developer"
                        404 -> "Backend URL not found. Please check your NanoChat server URL"
                        403 -> "Access forbidden. Your API key may not have the required permissions"
                        in 500..599 -> "Server error (HTTP $errorCode). Please try again later"
                        else -> "Connection failed (HTTP $errorCode). Please check your URL and API key"
                    }
                    debugLogger.logError(TAG, "API validation failed: HTTP $errorCode", null)
                    throw SetupException(errorMessage, null)
                }

            } catch (e: SetupException) {
                // Our custom exceptions
                debugLogger.logError(TAG, "Setup failed: ${e.message}", e.cause)
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    error = e.message ?: "Connection failed"
                )
            } catch (e: UnknownHostException) {
                // DNS resolution failed
                debugLogger.logError(TAG, "Unknown host", e)
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    error = "Cannot reach server: ${e.message}\n\nPlease check your URL and internet connection"
                )
            } catch (e: SocketTimeoutException) {
                // Connection timeout
                debugLogger.logError(TAG, "Connection timeout", e)
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    error = "Connection timeout. Server took too long to respond.\n\nPlease check your internet connection and server status"
                )
            } catch (e: SSLException) {
                // SSL/TLS error
                debugLogger.logError(TAG, "SSL error", e)
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    error = "SSL/TLS error: ${e.message}\n\nPlease ensure your server uses a valid SSL certificate"
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
@Keep
class SetupException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Keep
data class SetupUiState(
    val backendUrl: String = "",
    val apiKey: String = "",
    val isTesting: Boolean = false,
    val error: String? = null
)

