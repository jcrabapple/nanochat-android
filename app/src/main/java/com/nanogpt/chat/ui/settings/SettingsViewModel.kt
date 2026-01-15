package com.nanogpt.chat.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.paging.ModelsPagingSource
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.ModelDto
import com.nanogpt.chat.data.remote.dto.NanoGptBalanceDto
import com.nanogpt.chat.data.remote.dto.NanoGptSubscriptionDto
import com.nanogpt.chat.data.remote.dto.SettingsUpdates
import com.nanogpt.chat.data.remote.dto.UserSettingsDto
import com.nanogpt.chat.data.remote.dto.parseUserModelsResponse
import com.nanogpt.chat.data.remote.dto.MessageDto
import com.nanogpt.chat.data.remote.dto.toDomain
import com.nanogpt.chat.ui.theme.ThemeManager
import com.nanogpt.chat.utils.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import kotlinx.coroutines.flow.Flow
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
    val themeManager: ThemeManager,
    private val debugLogger: DebugLogger,
    private val messageRepository: com.nanogpt.chat.data.repository.MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        fetchSettings()
        fetchUserModels()
        fetchEnabledModelIds()
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
                val response = api.getModels()
                if (response.isSuccessful && response.body() != null) {
                    val allModels = response.body()!!
                    _uiState.value = _uiState.value.copy(allModels = allModels)
                }
            } catch (e: Exception) {
                // Log error but don't show to user - models are optional for settings
                android.util.Log.e("SettingsViewModel", "Error fetching models", e)
                _uiState.value = _uiState.value.copy(allModels = emptyList())
            }
        }
    }

    private fun fetchEnabledModelIds() {
        viewModelScope.launch {
            try {
                val response = api.getUserModels()
                if (response.isSuccessful && response.body() != null) {
                    val json = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                    val jsonElement = json.parseToJsonElement(response.body()!!.string())
                    val userModels = parseUserModelsResponse(jsonElement)
                    val enabledModelIds = userModels.map { it.modelId }.toSet()
                    _uiState.value = _uiState.value.copy(enabledModelIds = enabledModelIds)
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error fetching enabled models", e)
                _uiState.value = _uiState.value.copy(enabledModelIds = emptySet())
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
                // Don't log API keys - even partial keys can be used for brute force attacks
                android.util.Log.d("KarakeepTest", "API Key present: ${!apiKey.isNullOrBlank()}")

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

    fun toggleModelEnabled(model: com.nanogpt.chat.data.remote.dto.ModelDto) {
        viewModelScope.launch {
            try {
                val provider = inferProviderFromModelId(model.id)
                val response = api.updateUserModel(
                    com.nanogpt.chat.data.remote.dto.UpdateUserModelRequest(
                        action = "set",
                        provider = provider,
                        modelId = model.id,
                        enabled = !model.enabled
                    )
                )
                if (response.isSuccessful) {
                    // Update local state for the cached allModels
                    val newEnabledState = !model.enabled
                    val updatedAllModels = _uiState.value.allModels.map { m ->
                        if (m.id == model.id) {
                            m.copy(enabled = newEnabledState)
                        } else {
                            m
                        }
                    }

                    // Update enabledModelIds set
                    val updatedEnabledIds = if (newEnabledState) {
                        _uiState.value.enabledModelIds + model.id
                    } else {
                        _uiState.value.enabledModelIds - model.id
                    }

                    _uiState.value = _uiState.value.copy(
                        allModels = updatedAllModels,
                        enabledModelIds = updatedEnabledIds
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update model: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error updating model: ${e.message}"
                )
            }
        }
    }

    // Helper to infer provider from model ID
    private fun inferProviderFromModelId(modelId: String): String {
        return when {
            modelId.startsWith("gpt", ignoreCase = true) -> "openai"
            modelId.startsWith("claude", ignoreCase = true) -> "anthropic"
            modelId.startsWith("gemini", ignoreCase = true) -> "google"
            modelId.contains("llama", ignoreCase = true) -> "meta"
            modelId.contains("mistral", ignoreCase = true) -> "mistral"
            modelId.contains("deepseek", ignoreCase = true) -> "deepseek"
            modelId.contains("zhipu", ignoreCase = true) || modelId.contains("glm", ignoreCase = true) -> "zhipu"
            else -> "nanogpt"
        }
    }

    fun refreshModels() {
        fetchUserModels()
        fetchEnabledModelIds()
    }

    fun fetchNanoGptData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingNanoGpt = true, nanoGptError = null)
            try {
                // Fetch balance
                val balanceResponse = api.getNanoGptBalance()
                val balance = if (balanceResponse.isSuccessful && balanceResponse.body() != null) {
                    balanceResponse.body()
                } else {
                    null
                }

                // Fetch subscription
                val subscriptionResponse = api.getNanoGptSubscriptionUsage()
                val subscription = if (subscriptionResponse.isSuccessful && subscriptionResponse.body() != null) {
                    subscriptionResponse.body()
                } else {
                    null
                }

                _uiState.value = _uiState.value.copy(
                    nanoGptBalance = balance,
                    nanoGptSubscription = subscription,
                    isLoadingNanoGpt = false,
                    nanoGptError = if (balance == null && subscription == null) {
                        "Failed to load NanoGPT data"
                    } else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingNanoGpt = false,
                    nanoGptError = "Error: ${e.message}"
                )
            }
        }
    }

    fun fetchModelPerformance(recalculate: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingModelPerformance = true, modelPerformanceError = null)
            try {
                val response = api.getModelPerformance(
                    recalculate = if (recalculate) "true" else null
                )
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!.stats

                    // Calculate overall stats
                    val totalMessages = stats.sumOf { it.totalMessages }
                    val totalCost = stats.sumOf { it.totalCost }

                    // Calculate weighted average rating (only for models that have ratings)
                    val modelsWithRatings = stats.filter { it.avgRating != null }
                    val avgRating = if (modelsWithRatings.isNotEmpty()) {
                        modelsWithRatings.sumOf { (it.avgRating ?: 0.0) * it.totalMessages } / totalMessages
                    } else null

                    // Find most used model
                    val mostUsedModel = stats.maxByOrNull { it.totalMessages }?.modelId

                    // Find best rated model
                    val bestRatedModel = stats
                        .filter { it.avgRating != null && it.avgRating!! > 0 }
                        .maxByOrNull { it.avgRating ?: 0.0 }?.modelId

                    // Find most cost effective (lowest cost per message, must have at least 1 message)
                    val mostCostEffective = stats
                        .filter { it.totalMessages > 0 }
                        .minByOrNull { it.totalCost / it.totalMessages }?.modelId

                    // Find fastest model (lowest response time, must have at least 1 message)
                    val fastestModel = stats
                        .filter { it.totalMessages > 0 && it.avgResponseTime > 0 }
                        .minByOrNull { it.avgResponseTime }?.modelId

                    val overallStats = com.nanogpt.chat.data.remote.dto.OverallStatsDto(
                        totalMessages = totalMessages,
                        totalCost = totalCost,
                        avgRating = avgRating,
                        mostUsedModel = mostUsedModel,
                        bestRatedModel = bestRatedModel,
                        mostCostEffective = mostCostEffective,
                        fastestModel = fastestModel
                    )

                    _uiState.value = _uiState.value.copy(
                        modelPerformance = stats,
                        overallStats = overallStats,
                        isLoadingModelPerformance = false,
                        modelPerformanceError = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingModelPerformance = false,
                        modelPerformanceError = "Failed to load model performance: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingModelPerformance = false,
                    modelPerformanceError = "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * Returns a Flow of PagingData for models with the given filter.
     * This uses the cached allModels list and applies the filter function.
     */
    fun getModelsPagingFlow(
        filter: (ModelDto) -> Boolean
    ): Flow<PagingData<ModelDto>> {
        return Pager(
            config = PagingConfig(
                pageSize = ModelsPagingSource.PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = 10
            ),
            pagingSourceFactory = {
                ModelsPagingSource(
                    models = _uiState.value.allModels,
                    filter = filter
                )
            }
        ).flow.cachedIn(viewModelScope)
    }

    /**
     * Share debug logs via intent
     */
    fun shareDebugLogs(context: Context) {
        viewModelScope.launch {
            try {
                val logs = debugLogger.collectDebugLogs()
                debugLogger.shareDebugLogs(logs)
            } catch (e: Exception) {
                // If we can't collect logs, at least try to share basic info
                val basicInfo = """
                    NanoChat Mobile Debug Report

                    App Version: ${com.nanogpt.chat.BuildConfig.VERSION_NAME}
                    Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                    Android: ${android.os.Build.VERSION.RELEASE}

                    Error collecting logs: ${e.message}
                """.trimIndent()
                debugLogger.shareDebugLogs(basicInfo)
            }
        }
    }

    fun fetchStarredMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStarred = true, starredError = null)
            try {
                messageRepository.getStarredMessages()
                    .onSuccess { messagesDto ->
                        val messages = messagesDto.map { it.toDomain() }
                        _uiState.value = _uiState.value.copy(
                            starredMessages = messages,
                            isLoadingStarred = false
                        )
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingStarred = false,
                            starredError = e.message
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingStarred = false,
                    starredError = e.message
                )
            }
        }
    }

    fun toggleStarFromSettings(messageId: String, starred: Boolean) {
        viewModelScope.launch {
            messageRepository.toggleMessageStar(messageId, starred)
                .onSuccess {
                    // Refresh the starred messages list
                    fetchStarredMessages()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        starredError = "Failed to ${if (starred) "star" else "unstar"} message: ${e.message}"
                    )
                }
        }
    }

    fun clearStarredError() {
        _uiState.value = _uiState.value.copy(starredError = null)
    }
}

data class SettingsUiState(
    val settings: UserSettingsDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val allModels: List<com.nanogpt.chat.data.remote.dto.ModelDto> = emptyList(), // Cached full list
    val enabledModelIds: Set<String> = emptySet(), // Model IDs that are enabled (from /api/db/user-models)
    // Local TTS/STT settings (not synced to backend)
    val ttsModel: String? = null,
    val ttsVoice: String? = null,
    val ttsSpeed: Float = 1.0f,
    val sttModel: String? = null,
    // Karakeep testing
    val isTestingKarakeep: Boolean = false,
    val karakeepTestResult: String? = null,
    // NanoGPT API
    val nanoGptBalance: NanoGptBalanceDto? = null,
    val nanoGptSubscription: NanoGptSubscriptionDto? = null,
    val isLoadingNanoGpt: Boolean = false,
    val nanoGptError: String? = null,
    // Model Performance Analytics
    val modelPerformance: List<com.nanogpt.chat.data.remote.dto.ModelPerformanceStatsDto> = emptyList(),
    val overallStats: com.nanogpt.chat.data.remote.dto.OverallStatsDto? = null,
    val isLoadingModelPerformance: Boolean = false,
    val modelPerformanceError: String? = null,
    // Starred Messages
    val starredMessages: List<com.nanogpt.chat.ui.chat.Message> = emptyList(),
    val isLoadingStarred: Boolean = false,
    val starredError: String? = null
)
