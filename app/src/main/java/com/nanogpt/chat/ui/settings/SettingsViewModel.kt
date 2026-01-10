package com.nanogpt.chat.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.ui.theme.ThemeManager
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
    private val secureStorage: SecureStorage,
    val themeManager: ThemeManager
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

    fun updateKarakeepSettings(url: String, apiKey: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Sync to backend
                val response = api.updateSettings(
                    SettingsUpdates(
                        karakeepUrl = url.ifEmpty { null },
                        karakeepApiKey = apiKey.ifEmpty { null }
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        settings = response.body()!!,
                        isLoading = false,
                        error = null,
                        karakeepTestResult = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to update Karakeep settings: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error updating Karakeep settings: ${e.message}"
                )
            }
        }
    }

    fun testKarakeepConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(isTestingKarakeep = true, karakeepTestResult = null)
                val settings = _uiState.value.settings
                var url = settings?.karakeepUrl
                val apiKey = settings?.karakeepApiKey

                android.util.Log.d("KarakeepTest", "Starting test connection...")
                android.util.Log.d("KarakeepTest", "URL from settings: $url")
                android.util.Log.d("KarakeepTest", "API Key from settings: ${apiKey?.take(10)}...")

                if (url.isNullOrBlank() || apiKey.isNullOrBlank()) {
                    val msg = "Please enter both URL and API Key"
                    android.util.Log.e("KarakeepTest", msg)
                    _uiState.value = _uiState.value.copy(
                        isTestingKarakeep = false,
                        karakeepTestResult = msg
                    )
                    onResult(false, msg)
                    return@launch
                }

                // Clean up URL
                url = url.trim()
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                val testUrl = if (url.endsWith("/")) url else "$url/"

                android.util.Log.d("KarakeepTest", "Testing connection to: ${testUrl}api/v1/bookmarks")

                // Test connection to Karakeep API using GET request
                val client = okhttp3.OkHttpClient().newBuilder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url("${testUrl}api/v1/bookmarks")
                    .get()
                    .header("Authorization", "Bearer $apiKey")
                    .header("Accept", "application/json")
                    .build()

                android.util.Log.d("KarakeepTest", "Sending request...")
                val response = client.newCall(request).execute()
                android.util.Log.d("KarakeepTest", "Response code: ${response.code}, successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val successMsg = "Connection successful!"
                    android.util.Log.d("KarakeepTest", successMsg)
                    _uiState.value = _uiState.value.copy(
                        isTestingKarakeep = false,
                        karakeepTestResult = successMsg
                    )
                    onResult(true, successMsg)
                } else {
                    val errorMsg = "Connection failed: HTTP ${response.code}"
                    val responseBody = response.body?.string()
                    android.util.Log.e("KarakeepTest", "$errorMsg, body: $responseBody")
                    _uiState.value = _uiState.value.copy(
                        isTestingKarakeep = false,
                        karakeepTestResult = errorMsg
                    )
                    onResult(false, errorMsg)
                }
            } catch (e: Exception) {
                val exceptionType = e.javaClass.simpleName
                val exceptionMessage = e.message ?: "No message"
                val stackTrace = e.stackTraceToString().take(500)

                android.util.Log.e("KarakeepTest", "Exception type: $exceptionType")
                android.util.Log.e("KarakeepTest", "Exception message: $exceptionMessage")
                android.util.Log.e("KarakeepTest", "Stack trace: $stackTrace", e)

                val errorMsg = when (e) {
                    is java.net.UnknownHostException -> "Connection failed: Host not found. Check the URL."
                    is java.net.SocketTimeoutException -> "Connection failed: Request timed out"
                    is javax.net.ssl.SSLException -> "Connection failed: SSL error. Check if URL uses HTTPS."
                    is java.net.MalformedURLException -> "Connection failed: Invalid URL format."
                    else -> "Connection failed: $exceptionType - $exceptionMessage"
                }

                android.util.Log.e("KarakeepTest", "Showing error to user: $errorMsg")
                _uiState.value = _uiState.value.copy(
                    isTestingKarakeep = false,
                    karakeepTestResult = errorMsg
                )
                onResult(false, errorMsg)
            }
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
    val sttModel: String? = null,
    // Karakeep testing
    val isTestingKarakeep: Boolean = false,
    val karakeepTestResult: String? = null
)
