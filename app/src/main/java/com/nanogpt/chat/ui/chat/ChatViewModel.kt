package com.nanogpt.chat.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.remote.StreamingManager
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.GenerateMessageRequest
import com.nanogpt.chat.data.remote.dto.UserModelDto
import com.nanogpt.chat.data.remote.dto.parseUserModelsResponse
import com.nanogpt.chat.data.remote.dto.toDomain
import com.nanogpt.chat.data.repository.ConversationRepository
import com.nanogpt.chat.data.repository.MessageRepository
import com.nanogpt.chat.data.repository.WebSearchRepository
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.local.entity.ConversationEntity
import com.nanogpt.chat.ui.chat.components.ModelInfo
import com.nanogpt.chat.data.local.entity.MessageEntity
import com.nanogpt.chat.ui.chat.components.WebSearchMode
import com.nanogpt.chat.ui.chat.components.WebSearchProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val streamingManager: StreamingManager,
    private val webSearchRepository: WebSearchRepository,
    private val secureStorage: SecureStorage,
    private val api: NanoChatApi
) : ViewModel() {

    private val conversationId: String? = savedStateHandle["conversationId"]

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private var currentAssistantMessage: StringBuilder = StringBuilder()
    private var currentReasoning: StringBuilder = StringBuilder()
    private var isGenerating = false

    init {
        fetchUserModels()
        if (conversationId != null) {
            loadConversation()
            observeMessages()
        }
    }

    private fun loadConversation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val result = conversationRepository.getConversationWithMessages(conversationId!!)
                result.onSuccess { conversation ->
                    if (conversation != null) {
                        _uiState.value = _uiState.value.copy(
                            conversation = conversation,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Conversation not found",
                            isLoading = false
                        )
                    }
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            messageRepository.getMessagesForConversation(conversationId!!)
                .collect { messageEntities ->
                    _messages.value = messageEntities.map { it.toDomain() }
                }
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        if (_inputText.value.isBlank() || isGenerating) return

        val message = _inputText.value
        _inputText.value = ""

        viewModelScope.launch {
            try {
                // Start generation
                isGenerating = true
                _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
                currentAssistantMessage = StringBuilder()
                currentReasoning = StringBuilder()

                // For existing conversations, add user message locally
                if (conversationId != null) {
                    val userMessageResult = messageRepository.createUserMessage(conversationId, message)
                    userMessageResult.onSuccess { userMessage ->
                        _messages.value = _messages.value + userMessage.toDomain()
                    }
                } else {
                    // For new conversations, just show user message in UI
                    _messages.value = _messages.value + Message(
                        id = java.util.UUID.randomUUID().toString(),
                        conversationId = "",
                        role = "user",
                        content = message,
                        createdAt = System.currentTimeMillis()
                    )
                }

                // Add placeholder for assistant message
                val assistantMessage = Message(
                    id = java.util.UUID.randomUUID().toString(),
                    conversationId = conversationId ?: "",
                    role = "assistant",
                    content = "",
                    createdAt = System.currentTimeMillis()
                )
                _messages.value = _messages.value + assistantMessage

                val webSearchMode = _uiState.value.webSearchMode.name.lowercase()
                val webSearchProvider = if (_uiState.value.webSearchMode != WebSearchMode.OFF) {
                    _uiState.value.webSearchProvider.value
                } else {
                    null
                }

                val request = GenerateMessageRequest(
                    message = message,
                    model_id = _uiState.value.selectedModel?.id ?: "gpt-4o-mini",
                    conversation_id = conversationId,
                    assistant_id = _uiState.value.conversation?.assistantId,
                    web_search_enabled = _uiState.value.webSearchMode != WebSearchMode.OFF,
                    web_search_mode = webSearchMode,
                    web_search_provider = webSearchProvider
                )

                // Call generate-message API
                val response = api.generateMessage(request)
                if (!response.isSuccessful || response.body() == null) {
                    throw Exception("Failed to generate message: HTTP ${response.code()}")
                }

                val generateResponse = response.body()!!
                if (!generateResponse.ok) {
                    throw Exception("Failed to generate message")
                }

                val newConversationId = generateResponse.conversation_id

                // Save the conversation ID
                secureStorage.saveLastConversationId(newConversationId)

                // Start polling for messages with much longer timeout
                pollForMessages(newConversationId)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = "Error: ${e.message}"
                )
                isGenerating = false
            }
        }
    }

    private suspend fun pollForMessages(convId: String) {
        var polling = true
        var lastMessageCount = 0
        var emptyPolls = 0
        val maxEmptyPolls = 6 // 3 seconds (6 * 500ms) - feels immediate but allows for brief pauses
        var lastContent = ""

        while (polling && isGenerating) {
            try {
                kotlinx.coroutines.delay(500) // Poll every 500ms

                // Fetch messages from API
                val messagesResponse = api.getMessages(convId)
                if (messagesResponse.isSuccessful && messagesResponse.body() != null) {
                    val messages = messagesResponse.body()!!

                    // Get the last assistant message
                    val lastAssistantMessage = messages.lastOrNull { it.role == "assistant" }
                    val currentContent = lastAssistantMessage?.content ?: ""

                    // Update UI with messages
                    _messages.value = messages.map { dto ->
                        dto.toDomain()
                    }

                    // Check if generation is complete by tracking content changes
                    if (messages.size > lastMessageCount || currentContent != lastContent) {
                        lastMessageCount = messages.size
                        lastContent = currentContent
                        emptyPolls = 0

                        // Save to database as we receive content
                        if (lastAssistantMessage != null && currentContent.isNotEmpty()) {
                            messageRepository.createAssistantMessage(
                                conversationId = convId,
                                content = currentContent,
                                reasoning = lastAssistantMessage.reasoning
                            )
                        }
                    } else {
                        emptyPolls++
                    }

                    // Stop polling if we have assistant message content and no new content for maxEmptyPolls consecutive polls
                    if (lastAssistantMessage != null && currentContent.isNotEmpty() && emptyPolls >= maxEmptyPolls) {
                        polling = false
                        isGenerating = false
                        _uiState.value = _uiState.value.copy(isGenerating = false)
                    }
                }
            } catch (e: Exception) {
                // Continue polling on error
                android.util.Log.e("ChatViewModel", "Error polling messages: ${e.message}")
            }
        }
    }

    private fun handleStreamEvent(event: StreamingManager.StreamEvent) {
        when (event) {
            is StreamingManager.StreamEvent.ContentDelta -> {
                currentAssistantMessage.append(event.content)
                updateLastAssistantMessage(
                    content = currentAssistantMessage.toString()
                )
            }
            is StreamingManager.StreamEvent.ReasoningDelta -> {
                currentReasoning.append(event.reasoning)
                updateLastAssistantMessage(
                    content = currentAssistantMessage.toString(),
                    reasoning = currentReasoning.toString()
                )
            }
            is StreamingManager.StreamEvent.TokenReceived -> {
                currentAssistantMessage.append(event.token)
                updateLastAssistantMessage(currentAssistantMessage.toString())
            }
            is StreamingManager.StreamEvent.ConversationCreated -> {
                // Update conversation info if created
                viewModelScope.launch {
                    val conversation = conversationRepository.getConversationById(event.conversationId)
                    conversation?.let {
                        _uiState.value = _uiState.value.copy(conversation = it)
                    }
                    // Save last conversation ID
                    secureStorage.saveLastConversationId(event.conversationId)
                }
            }
            is StreamingManager.StreamEvent.Complete -> {
                isGenerating = false
                _uiState.value = _uiState.value.copy(isGenerating = false)

                // Save final assistant message to database
                if (currentAssistantMessage.isNotEmpty()) {
                    viewModelScope.launch {
                        // Use the conversation ID from the event if available, otherwise use the current one
                        val convId = event.messageId ?: conversationId
                        if (convId != null) {
                            messageRepository.createAssistantMessage(
                                conversationId = convId,
                                content = currentAssistantMessage.toString(),
                                reasoning = currentReasoning.toString().takeIf { it.isNotEmpty() }
                            )
                        }
                    }
                }
            }
            is StreamingManager.StreamEvent.Error -> {
                isGenerating = false
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = event.error
                )
            }
        }
    }

    private fun updateLastAssistantMessage(content: String, reasoning: String = "") {
        _messages.value = _messages.value.toMutableList().apply {
            if (isNotEmpty() && last().role == "assistant") {
                set(lastIndex, last().copy(
                    content = content,
                    reasoning = reasoning
                ))
            }
        }
    }

    fun stopGeneration() {
        streamingManager.stopStreaming()
        isGenerating = false
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun toggleWebSearch() {
        // If currently off, turn on with standard mode
        // If currently on, turn off
        val newMode = if (_uiState.value.webSearchMode == WebSearchMode.OFF) {
            WebSearchMode.STANDARD
        } else {
            WebSearchMode.OFF
        }
        _uiState.value = _uiState.value.copy(webSearchMode = newMode)
    }

    fun setWebSearchMode(mode: WebSearchMode) {
        _uiState.value = _uiState.value.copy(webSearchMode = mode)
    }

    fun setWebSearchProvider(provider: WebSearchProvider) {
        _uiState.value = _uiState.value.copy(webSearchProvider = provider)
    }

    fun selectModel(model: ModelInfo) {
        _uiState.value = _uiState.value.copy(
            selectedModel = model
        )
        // Save the selected model
        secureStorage.saveLastModelId(model.id)
    }

    private fun fetchUserModels() {
        viewModelScope.launch {
            try {
                val response = api.getUserModels()
                if (response.isSuccessful) {
                    val body = response.body()?.string() ?: "{}"
                    val jsonElement = Json.parseToJsonElement(body)
                    val userModels = parseUserModelsResponse(jsonElement)
                    val models = userModels
                        .filter { it.enabled }
                        .sortedByDescending { it.pinned }
                        .map { userModel ->
                            val actualProvider = extractProviderFromModelId(userModel.modelId)
                            ModelInfo(
                                id = userModel.modelId,
                                name = userModel.modelId,
                                provider = actualProvider,
                                providerLogo = userModel.icon_url  // Use icon_url from API
                            )
                        }

                    // Try to use the last used model
                    val lastModelId = secureStorage.getLastModelId()
                    val selectedModel = if (lastModelId != null) {
                        models.find { it.id == lastModelId } ?: models.firstOrNull()
                    } else {
                        models.firstOrNull()
                    }

                    _uiState.value = _uiState.value.copy(
                        availableModels = models,
                        selectedModel = selectedModel ?: ModelInfo("zai-org/glm-4.7", "GLM-4.7", "zai-org", getProviderLogoUrl("zai-org"))
                    )
                } else {
                    // If API call fails, use default models
                    val defaultModels = listOf(
                        ModelInfo("zai-org/glm-4.7", "GLM-4.7", "zai-org", getProviderLogoUrl("zai-org")),
                        ModelInfo("zai-org/glm-4.6v", "GLM-4.6v", "zai-org", getProviderLogoUrl("zai-org")),
                        ModelInfo("deepseek/deepseek-v3.2", "DeepSeek V3.2", "deepseek", getProviderLogoUrl("deepseek")),
                        ModelInfo("moonshotai/kimi-k2-thinking", "Kimi K2 Thinking", "moonshotai", getProviderLogoUrl("moonshotai"))
                    )
                    val lastModelId = secureStorage.getLastModelId()
                    val selectedModel = if (lastModelId != null) {
                        defaultModels.find { it.id == lastModelId } ?: defaultModels.firstOrNull()
                    } else {
                        defaultModels.firstOrNull()
                    }

                    _uiState.value = _uiState.value.copy(
                        availableModels = defaultModels,
                        selectedModel = selectedModel
                    )
                }
            } catch (e: Exception) {
                // If fetch fails with exception, use default models
                val defaultModels = listOf(
                    ModelInfo("zai-org/glm-4.7", "GLM-4.7", "zai-org", getProviderLogoUrl("zai-org")),
                    ModelInfo("zai-org/glm-4.6v", "GLM-4.6v", "zai-org", getProviderLogoUrl("zai-org")),
                    ModelInfo("deepseek/deepseek-v3.2", "DeepSeek V3.2", "deepseek", getProviderLogoUrl("deepseek")),
                    ModelInfo("moonshotai/kimi-k2-thinking", "Kimi K2 Thinking", "moonshotai", getProviderLogoUrl("moonshotai"))
                )
                val lastModelId = secureStorage.getLastModelId()
                val selectedModel = if (lastModelId != null) {
                    defaultModels.find { it.id == lastModelId } ?: defaultModels.firstOrNull()
                } else {
                    defaultModels.firstOrNull()
                }

                _uiState.value = _uiState.value.copy(
                    availableModels = defaultModels,
                    selectedModel = selectedModel
                )
            }
        }
    }

    private fun extractProviderFromModelId(modelId: String): String? {
        // Model IDs are in format "provider/model-name" like "zai-org/glm-4.7"
        val parts = modelId.split("/", limit = 2)
        return if (parts.size >= 2) {
            parts[0] // Return the provider part (e.g., "zai-org", "openai", etc.)
        } else {
            null // No provider in model ID
        }
    }

    private fun getProviderLogoUrl(provider: String?): String? {
        if (provider == null) return null

        val providerLower = provider.lowercase()

        // Log for debugging
        android.util.Log.d("ChatViewModel", "Provider: $provider (lowercase: $providerLower)")

        // Map of known providers to their logo URLs (using reliable, well-known sources)
        return when {
            providerLower.contains("openai") || providerLower.contains("open_router") || providerLower.contains("openrouter") ->
                "https://raw.githubusercontent.com/github/explore/main/topics/openai.png"
            providerLower.contains("anthropic") || providerLower.contains("claude") ->
                "https://raw.githubusercontent.com/github/explore/main/topics/anthropic.png"
            providerLower.contains("google") || providerLower.contains("gemini") ->
                "https://raw.githubusercontent.com/github/explore/main/topics/google.png"
            providerLower.contains("deepseek") ->
                "https://raw.githubusercontent.com/github/explore/main/topics/deepseek.png"
            providerLower.contains("moonshot") || providerLower.contains("kimi") ->
                "https://raw.githubusercontent.com/github/explore/main/topics/moonshot.png"
            providerLower.contains("zai") || providerLower.contains("z.ai") || providerLower.contains("glm") || providerLower.contains("zhipu") ->
                "https://raw.githubusercontent.com/github/explore/main/topics/deepseek.png" // Using DeepSeek logo as placeholder for Z.ai
            providerLower.contains("minimax") || providerLower.contains("mini") ->
                "https://raw.githubusercontent.com/github/explore/main/topics/deepseek.png" // Using DeepSeek logo as placeholder for Minimax
            else -> {
                android.util.Log.d("ChatViewModel", "No logo mapping for provider: $provider")
                null
            }
        }
    }
}

// UI State
data class ChatUiState(
    val conversation: ConversationEntity? = null,
    val isGenerating: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val webSearchMode: WebSearchMode = WebSearchMode.OFF,
    val webSearchProvider: WebSearchProvider = WebSearchProvider.LINKUP,
    val selectedModel: ModelInfo? = ModelInfo("gpt-4o-mini", "GPT-4o Mini"),
    val availableModels: List<ModelInfo> = emptyList()
)

// Domain model for messages
data class Message(
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val reasoning: String? = null,
    val modelId: String? = null,
    val createdAt: Long,
    val tokenCount: Int? = null,
    val annotations: List<Annotation>? = null
)

data class Annotation(
    val type: String, // "web-search" | "image" | "video" | "youtube"
    val data: kotlinx.serialization.json.JsonElement
)

// Extension to convert Entity to Domain model
fun MessageEntity.toDomain(): Message {
    // Parse annotations from JSON string if present
    val annotations = annotationsJson?.let { jsonStr ->
        try {
            kotlinx.serialization.json.Json.decodeFromString<List<kotlinx.serialization.json.JsonElement>>(jsonStr)
                .map { jsonElem ->
                    val jsonObj = jsonElem as kotlinx.serialization.json.JsonObject
                    val type = jsonObj["type"]?.jsonPrimitive?.content ?: "unknown"
                    Annotation(type, jsonObj["data"]!!)
                }
        } catch (e: Exception) {
            null
        }
    }

    return Message(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        reasoning = reasoning,
        modelId = modelId,
        createdAt = createdAt,
        tokenCount = tokenCount,
        annotations = annotations
    )
}
