package com.nanogpt.chat.ui.auth.setup

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.BuildConfig
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.local.dao.AssistantDao
import com.nanogpt.chat.data.repository.toEntity
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
    private val assistantDao: AssistantDao,
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
                    debugLogger.logInfo(TAG, "Backend URL saved successfully")
                } catch (e: Exception) {
                    debugLogger.logError(TAG, "Failed to save backend URL", e)
                    throw SetupException("Failed to save backend URL: ${e.message}", e)
                }

                try {
                    secureStorage.saveSessionToken(apiKey)
                    // Don't log key details - just confirm it was saved
                    debugLogger.logInfo(TAG, "Session token saved successfully")
                } catch (e: Exception) {
                    debugLogger.logError(TAG, "Failed to save session token", e)
                    throw SetupException("Failed to save session token: ${e.message}", e)
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

                    // Sync assistants from server immediately after successful connection
                    try {
                        debugLogger.logInfo(TAG, "Fetching assistants from server...")
                        val assistantsResponse = tempApi.getAssistants()
                        if (assistantsResponse.isSuccessful && assistantsResponse.body() != null) {
                            val assistants = assistantsResponse.body()!!
                            debugLogger.logInfo(TAG, "Successfully fetched ${assistants.size} assistants")

                            // Save assistants to database so they're available after app restart
                            val entities = assistants.map { it.toEntity() }
                            assistantDao.insertAssistants(entities)
                            debugLogger.logInfo(TAG, "Saved ${entities.size} assistants to database")
                        } else {
                            debugLogger.logWarning(TAG, "Failed to fetch assistants (HTTP ${assistantsResponse.code()})")
                        }
                    } catch (e: Exception) {
                        // Don't fail setup if assistant fetch fails, just log it
                        debugLogger.logWarning(TAG, "Failed to fetch assistants: ${e.message}")
                    }

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
     * Validate URL format with security checks to prevent SSRF attacks
     * - Only allows http/https protocols
     * - Blocks localhost and private IP addresses in release builds
     * - Validates host format
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            val scheme = uri.scheme ?: return false

            // Only allow HTTP/HTTPS protocols
            if (scheme !in listOf("http", "https")) {
                debugLogger.logWarning(TAG, "URL rejected: invalid protocol '$scheme'")
                return false
            }

            val host = uri.host ?: return false

            // In release builds, prevent connection to localhost/private IPs (SSRF protection)
            if (!BuildConfig.DEBUG) {
                // Block localhost variants
                if (host.equals("localhost", ignoreCase = true)) {
                    debugLogger.logWarning(TAG, "URL rejected: localhost not allowed in release builds")
                    return false
                }

                // Block IPv4 loopback (127.x.x.x)
                if (host.matches(Regex("^127\\."))) {
                    debugLogger.logWarning(TAG, "URL rejected: IPv4 loopback address not allowed")
                    return false
                }

                // Block private IPv4 ranges:
                // - 10.0.0.0/8
                // - 172.16.0.0/12 (172.16.x.x to 172.31.x.x)
                // - 192.168.0.0/16
                if (host.matches(Regex("^10\\.")) ||
                    host.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\.")) ||
                    host.matches(Regex("^192\\.168\\."))) {
                    debugLogger.logWarning(TAG, "URL rejected: private IP address not allowed")
                    return false
                }

                // Block IPv6 loopback (::1)
                if (host.equals("::1", ignoreCase = true) || host.equals("[::1]", ignoreCase = true)) {
                    debugLogger.logWarning(TAG, "URL rejected: IPv6 loopback not allowed")
                    return false
                }
            }

            // Validate port range if specified
            val port = uri.port
            if (port != -1 && port !in 1..65535) {
                debugLogger.logWarning(TAG, "URL rejected: invalid port $port")
                return false
            }

            true
        } catch (e: Exception) {
            debugLogger.logWarning(TAG, "URL rejected: parsing error - ${e.message}")
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

