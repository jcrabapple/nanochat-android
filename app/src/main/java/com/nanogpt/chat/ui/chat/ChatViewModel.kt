package com.nanogpt.chat.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.remote.StreamingManager
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.GenerateMessageRequest
import com.nanogpt.chat.data.remote.dto.ModelDto
import com.nanogpt.chat.data.remote.dto.UserModelDto
import com.nanogpt.chat.data.remote.dto.parseUserModelsResponse
import com.nanogpt.chat.data.remote.dto.toDomain
import com.nanogpt.chat.data.repository.ConversationRepository
import com.nanogpt.chat.data.repository.MessageRepository
import com.nanogpt.chat.data.repository.WebSearchRepository
import com.nanogpt.chat.data.repository.ConversationTitlePoller
import com.nanogpt.chat.data.repository.AssistantRepository
import com.nanogpt.chat.data.repository.VideoGenerationRepository
import com.nanogpt.chat.data.repository.toEntity
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.sync.ConversationSyncManager
import com.nanogpt.chat.data.local.dao.MessageDao
import com.nanogpt.chat.data.local.entity.ConversationEntity
import com.nanogpt.chat.data.local.entity.AssistantEntity
import com.nanogpt.chat.ui.chat.components.ModelInfo
import com.nanogpt.chat.ui.chat.components.ModelCapabilities
import com.nanogpt.chat.ui.chat.components.FileAttachment
import com.nanogpt.chat.ui.chat.components.FileType
import com.nanogpt.chat.data.local.entity.MessageEntity
import com.nanogpt.chat.ui.chat.components.WebSearchMode
import com.nanogpt.chat.ui.chat.components.WebSearchProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val streamingManager: StreamingManager,
    private val webSearchRepository: WebSearchRepository,
    private val assistantRepository: AssistantRepository,
    private val secureStorage: SecureStorage,
    private val api: NanoChatApi,
    private val messageDao: MessageDao,
    private val conversationSyncManager: ConversationSyncManager,
    private val conversationTitlePoller: ConversationTitlePoller,
    private val videoGenerationRepository: VideoGenerationRepository
) : ViewModel() {

    companion object {
        /** Interval between message polling requests */
        private const val POLLING_INTERVAL_MS = 500L

        /** Max consecutive empty polls before considering generation complete (3 seconds total) */
        private const val MAX_EMPTY_POLLS = 6

        /** Default fallback model ID when no model is selected */
        private const val DEFAULT_MODEL_ID = "zai-org/glm-4.7"
    }

    private var conversationId: String? = savedStateHandle["conversationId"]
        set(value) {
            val oldValue = field
            field = value
            // Update savedStateHandle when conversationId changes
            if (value != oldValue) {
                savedStateHandle["conversationId"] = value
                android.util.Log.d("ChatViewModel", "conversationId updated: $value (was $oldValue)")
            }
        }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Expose backend URL for image loading
    val backendUrl: String?
        get() = secureStorage.getBackendUrl()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // Cache for model capabilities (modelId -> ModelDto with full info including capabilities)
    private val modelCapabilitiesCache = MutableStateFlow<Map<String, ModelDto>>(emptyMap())

    private var currentAssistantMessage: StringBuilder = StringBuilder()
    private var currentReasoning: StringBuilder = StringBuilder()
    private var isGenerating = false
    private var streamingMessageId: String? = null
    private var titleGenerated = false // Track if title has been generated for this conversation
    
    /** Creates the default fallback models list when API fetch fails */
    private fun createDefaultModels(): List<ModelInfo> = listOf(
        ModelInfo("zai-org/glm-4.7", "GLM-4.7", "zai-org", getProviderLogoUrl("zai-org")),
        ModelInfo("zai-org/glm-4.6v", "GLM-4.6v", "zai-org", getProviderLogoUrl("zai-org")),
        ModelInfo("deepseek/deepseek-v3.2", "DeepSeek V3.2", "deepseek", getProviderLogoUrl("deepseek")),
        ModelInfo("moonshotai/kimi-k2-thinking", "Kimi K2 Thinking", "moonshotai", getProviderLogoUrl("moonshotai"))
    )
    
    /** Applies default models to UI state when API fetch fails */
    private fun applyDefaultModels() {
        val defaultModels = createDefaultModels()
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

        // If an assistant is already selected, re-apply its model settings now that models are available
        _uiState.value.selectedAssistant?.let { assistant ->
            android.util.Log.d("ChatViewModel", "applyDefaultModels: Re-applying assistant model settings")
            applyAssistantSettings(assistant)
        }
    }

    init {
        fetchUserModels()
        fetchAssistants()
        if (conversationId != null) {
            titleGenerated = true // Existing conversations already have titles
            loadConversation()
            observeMessages()
        }
    }

    private fun loadConversation(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
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
                    val domainMessages = messageEntities.map { it.toDomain() }

                    _messages.update { currentList ->
                        // If we are generating, preserve the in-memory streaming message state
                        // This prevents the DB emission from overwriting our partial stream
                        // We must read the partial message from currentList inside update{} to ensure atomicity
                        if (isGenerating && streamingMessageId != null) {
                            val partialMessage = currentList.find { it.id == streamingMessageId }
                            if (partialMessage != null) {
                                val mergedMessages = domainMessages.toMutableList()
                                val dbIndex = mergedMessages.indexOfFirst { it.id == streamingMessageId }

                                if (dbIndex != -1) {
                                    // Message exists in DB, override with our partial state
                                    mergedMessages[dbIndex] = partialMessage
                                } else {
                                    // Message not in DB yet, append it
                                    mergedMessages.add(partialMessage)
                                }
                                mergedMessages
                            } else {
                                domainMessages
                            }
                        } else {
                            domainMessages
                        }
                    }
                }
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    // ============== File Attachment Management ==============

    /**
     * Add a file attachment and validate it
     */
    fun addFileAttachment(uri: android.net.Uri, fileName: String, mimeType: String, sizeBytes: Long) {
        viewModelScope.launch {
            try {
                // Validate file type
                if (!FileAttachment.isValidMimeType(mimeType)) {
                    _uiState.value = _uiState.value.copy(
                        error = "Unsupported file type: $mimeType. Supported types: TXT, CSV, PDF, Markdown, JSON"
                    )
                    return@launch
                }

                // Validate file size
                if (!FileAttachment.isValidFileSize(sizeBytes)) {
                    val sizeMB = sizeBytes / (1024.0 * 1024.0)
                    _uiState.value = _uiState.value.copy(
                        error = "File too large: ${String.format("%.2f", sizeMB)}MB. Maximum size is 10MB."
                    )
                    return@launch
                }

                val fileType = FileAttachment.getFileType(mimeType, fileName)

                // For text-based files, read content and estimate tokens
                val estimatedTokens = if (fileType?.supportsText == true) {
                    try {
                        val content = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            readTextContent(uri)
                        }
                        FileAttachment.estimateTokens(content)
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "Failed to read file content: ${e.message}")
                        null
                    }
                } else {
                    null
                }

                // Check token count against model context window
                if (estimatedTokens != null) {
                    val maxTokens = getModelContextWindow()
                    if (estimatedTokens > maxTokens) {
                        _uiState.value = _uiState.value.copy(
                            error = "File is too large (~$estimatedTokens tokens). Model supports maximum $maxTokens tokens."
                        )
                        return@launch
                    }
                }

                val attachment = FileAttachment(
                    uri = uri,
                    fileName = fileName,
                    mimeType = mimeType,
                    sizeBytes = sizeBytes,
                    estimatedTokens = estimatedTokens,
                    fileType = fileType
                )

                val currentAttachments = _uiState.value.fileAttachments.toMutableList()
                currentAttachments.add(attachment)
                _uiState.value = _uiState.value.copy(fileAttachments = currentAttachments)

                // Clear error after adding file
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to add file: ${e.message}"
                )
            }
        }
    }

    /**
     * Remove a file attachment
     */
    fun removeFileAttachment(uri: android.net.Uri) {
        val currentAttachments = _uiState.value.fileAttachments.toMutableList()
        currentAttachments.removeAll { it.uri == uri }
        _uiState.value = _uiState.value.copy(fileAttachments = currentAttachments)
    }

    /**
     * Upload all pending file attachments
     */
    @Suppress("DEPRECATION")
    private suspend fun uploadFileAttachments(): List<com.nanogpt.chat.data.remote.dto.MessageDocumentDto> {
        val uploadedDocuments = mutableListOf<com.nanogpt.chat.data.remote.dto.MessageDocumentDto>()

        _uiState.value.fileAttachments.forEach { attachment ->
            if (!attachment.isUploaded && !attachment.hasError) {
                try {
                    // Create binary request body
                    val contentResolver = getApplicationContext().contentResolver
                    val inputStream = contentResolver.openInputStream(attachment.uri)
                    val fileBytes = inputStream?.readBytes() ?: throw Exception("Failed to read file")

                    // Create RequestBody with the file bytes (null MediaType is okay since we set it via header)
                    val mediaType = attachment.mimeType.toMediaType()
                    val requestBody = okhttp3.RequestBody.create(
                        mediaType,
                        fileBytes
                    )

                    // Upload file to /api/storage endpoint
                    android.util.Log.d("ChatViewModel", "Uploading file: ${attachment.fileName}, MIME: ${attachment.mimeType}")

                    // Normalize MIME type for TXT files
                    // Some backends reject "text/plain", so we try:
                    // 1. "text/plain; charset=utf-8" first
                    // 2. Fall back to "text/markdown" if that fails
                    val normalizedMimeType = when {
                        attachment.mimeType == "text/plain" && attachment.fileType == FileType.TEXT -> "text/markdown"
                        else -> attachment.mimeType
                    }

                    android.util.Log.d("ChatViewModel", "Using MIME type: $normalizedMimeType (original: ${attachment.mimeType})")

                    val response = api.uploadFile(
                        file = requestBody,
                        contentType = normalizedMimeType,
                        fileName = attachment.fileName
                    )

                    android.util.Log.d("ChatViewModel", "Upload response: HTTP ${response.code()}, success: ${response.isSuccessful}")
                    if (!response.isSuccessful) {
                        val errorBody = response.errorBody()?.string()
                        android.util.Log.e("ChatViewModel", "Upload failed. Response: $errorBody")
                    }

                    if (response.isSuccessful && response.body() != null) {
                        val uploadResponse = response.body()!!

                        // Determine file type for MessageDocumentDto
                        val fileTypeStr = when (attachment.fileType) {
                            FileType.PDF -> "pdf"
                            FileType.MARKDOWN -> "markdown"
                            FileType.TEXT -> "text"
                            FileType.CSV -> "text"
                            FileType.JSON -> "text"
                            FileType.EPUB -> "epub"
                            null -> "text"
                            else -> "text"  // Fallback for any other types
                        }

                        uploadedDocuments.add(
                            com.nanogpt.chat.data.remote.dto.MessageDocumentDto(
                                url = uploadResponse.url,
                                storage_id = uploadResponse.storageId,
                                fileName = attachment.fileName,
                                fileType = fileTypeStr
                            )
                        )
                    } else {
                        throw Exception("Upload failed: HTTP ${response.code()}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Failed to upload file: ${e.message}")
                    throw e
                }
            } else if (attachment.isUploaded) {
                // Already uploaded, add to list
                uploadedDocuments.add(
                    com.nanogpt.chat.data.remote.dto.MessageDocumentDto(
                        url = attachment.uploadedUrl!!,
                        storage_id = attachment.storageId!!,
                        fileName = attachment.fileName,
                        fileType = attachment.fileType?.name?.lowercase()
                    )
                )
            }
        }

        return uploadedDocuments
    }

    /**
     * Convert MIME type string to MediaType using OkHttp's extension function
     */
    private fun String.toMediaType(): okhttp3.MediaType? {
        // Use reflection to call the extension function
        // This avoids the deprecation error while maintaining compatibility
        val clazz = okhttp3.MediaType::class.java
        val method = clazz.getDeclaredMethod("get", String::class.java)
        return method.invoke(null, this) as? okhttp3.MediaType
    }

    /**
     * Clear all file attachments after sending
     */
    private fun clearFileAttachments() {
        _uiState.value = _uiState.value.copy(fileAttachments = emptyList())
    }

    /**
     * Read text content from URI
     */
    private suspend fun readTextContent(uri: android.net.Uri): String {
        // This will be implemented with proper ContentResolver in the actual usage
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplicationContext()
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                inputStream?.bufferedReader().use { it?.readText() } ?: ""
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Failed to read text: ${e.message}")
                ""
            }
        }
    }

    /**
     * Get model's maximum context window size
     * Returns a conservative default (128k) since backend doesn't provide this info yet
     */
    private fun getModelContextWindow(): Int {
        // Most modern models support at least 128k context
        // Some models support more (e.g., GPT-4o: 128k, Claude 3.5: 200k, GLM-4: 128k)
        return 128000
    }

    private lateinit var applicationContext: android.content.Context

    private fun getApplicationContext(): android.content.Context {
        if (!::applicationContext.isInitialized) {
            throw IllegalStateException("ApplicationContext not set")
        }
        return applicationContext
    }

    fun setApplicationContext(context: android.content.Context) {
        applicationContext = context
    }

    fun sendMessage() {
        if (_inputText.value.isBlank() || isGenerating) return

        val message = _inputText.value
        _inputText.value = ""

        viewModelScope.launch {
            sendMessageInternal(message)
        }
    }

    private suspend fun sendMessageInternal(message: String, skipUserMessage: Boolean = false) {
        try {
            // Start generation
            isGenerating = true
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                error = null
            )
            currentAssistantMessage = StringBuilder()
            currentReasoning = StringBuilder()

            // For existing conversations, add user message locally (unless skipping for regeneration)
            val currentConversationId = conversationId
            if (!skipUserMessage) {
                if (currentConversationId != null) {
                    val userMessageResult = messageRepository.createUserMessage(currentConversationId, message)
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
            }

            // Add placeholder for assistant message
            val assistantMessageId = java.util.UUID.randomUUID().toString()
            streamingMessageId = assistantMessageId

            val assistantMessage = Message(
                id = assistantMessageId,
                conversationId = conversationId ?: "",
                role = "assistant",
                content = "",
                createdAt = System.currentTimeMillis()
            )
            _messages.update { it + assistantMessage }

            val webSearchMode = _uiState.value.webSearchMode.name.lowercase()
            val webSearchProvider = if (_uiState.value.webSearchMode != WebSearchMode.OFF) {
                _uiState.value.webSearchProvider.value
            } else {
                null
            }

            // Upload file attachments if present
            val documents = if (_uiState.value.fileAttachments.isNotEmpty()) {
                uploadFileAttachments()
            } else {
                null
            }

            val request = GenerateMessageRequest(
                message = message,
                model_id = _uiState.value.selectedModel?.id ?: "gpt-4o-mini",
                conversation_id = conversationId,
                assistant_id = _uiState.value.selectedAssistant?.id,
                web_search_enabled = _uiState.value.webSearchMode != WebSearchMode.OFF,
                web_search_mode = webSearchMode,
                web_search_provider = webSearchProvider,
                documents = documents
            )

            android.util.Log.d("ChatViewModel", "===== SEND MESSAGE =====")
            android.util.Log.d("ChatViewModel", "Local conversationId: $conversationId")
            android.util.Log.d("ChatViewModel", "Request conversation_id: ${request.conversation_id}")
            android.util.Log.d("ChatViewModel", "Message: ${message.take(50)}...")

            // Try SSE streaming first with timeout, fall back to polling if it fails
            android.util.Log.d("ChatViewModel", "Attempting SSE streaming...")

            val streamSuccess = try {
                var streamCompleted = false

                streamingManager.streamMessage(request) { event ->
                    android.util.Log.d("ChatViewModel", "SSE event received: $event")

                    when (event) {
                        is StreamingManager.StreamEvent.ConversationCreated -> {
                            android.util.Log.d("ChatViewModel", "===== SSE CONVERSATION CREATED =====")
                            android.util.Log.d("ChatViewModel", "Event conversationId: ${event.conversationId}")
                            android.util.Log.d("ChatViewModel", "Local conversationId: $conversationId")
                            conversationId = event.conversationId
                            secureStorage.saveLastConversationId(event.conversationId)

                            // Fetch and save conversation details to database BEFORE notifying listeners
                            viewModelScope.launch {
                                try {
                                    android.util.Log.d("ChatViewModel", "Fetching conversation details from backend...")
                                    val convResponse = api.getConversation(event.conversationId)
                                    if (convResponse.isSuccessful && convResponse.body() != null) {
                                        val conversationDto = convResponse.body()!!
                                        val conversationEntity = conversationDto.toEntity()

                                        android.util.Log.d("ChatViewModel", "Inserting conversation to database: ${conversationEntity.id}, title: ${conversationEntity.title}")
                                        conversationRepository.insertConversation(conversationEntity)
                                        android.util.Log.d("ChatViewModel", "Conversation inserted to database")

                                        // Update UI state
                                        _uiState.value = _uiState.value.copy(conversation = conversationEntity)

                                        // Notify AFTER database insert so conversation is visible in sidebar
                                        android.util.Log.d("ChatViewModel", "Notifying conversation created: ${event.conversationId}")
                                        conversationSyncManager.notifyConversationCreated(event.conversationId)
                                        android.util.Log.d("ChatViewModel", "Notification complete")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ChatViewModel", "Failed to save new conversation to DB: ${e.message}", e)
                                }
                            }

                            // Update title for UI
                            _messages.value = _messages.value.map { msg ->
                                if (msg.role == "assistant" && msg.content.isEmpty()) {
                                    msg.copy(conversationId = event.conversationId)
                                } else {
                                    msg
                                }
                            }
                        }
                        is StreamingManager.StreamEvent.ContentDelta -> {
                            currentAssistantMessage.append(event.content)
                            updateAssistantMessage(currentAssistantMessage.toString())
                            streamCompleted = false
                        }
                        is StreamingManager.StreamEvent.ReasoningDelta -> {
                            currentReasoning.append(event.reasoning)
                            updateAssistantMessage(
                                currentAssistantMessage.toString(),
                                currentReasoning.toString()
                            )
                            streamCompleted = false
                        }
                        is StreamingManager.StreamEvent.TokenReceived -> {
                            currentAssistantMessage.append(event.token)
                            updateAssistantMessage(currentAssistantMessage.toString())
                            streamCompleted = false
                        }
                        is StreamingManager.StreamEvent.Complete -> {
                            android.util.Log.d("ChatViewModel", "SSE stream complete: ${event.messageId}, tokens=${event.tokenCount}, cost=$${event.costUsd}, time=${event.responseTimeMs}ms")
                            streamCompleted = true

                            // Update the last assistant message with metrics
                            _messages.value = _messages.value.map { msg ->
                                if (msg.role == "assistant" && msg.id == _messages.value.lastOrNull { it.role == "assistant" }?.id) {
                                    msg.copy(
                                        tokenCount = event.tokenCount,
                                        costUsd = event.costUsd,
                                        responseTimeMs = event.responseTimeMs
                                    )
                                } else {
                                    msg
                                }
                            }

                            // Fetch final message state from server and complete generation
                            val finalConvId = event.messageId ?: conversationId
                            if (finalConvId != null) {
                                viewModelScope.launch {
                                    fetchAndSaveMessages(finalConvId)
                                    finishGeneration(finalConvId)
                                }
                            } else {
                                isGenerating = false
                                _uiState.value = _uiState.value.copy(isGenerating = false)
                            }
                        }
                        is StreamingManager.StreamEvent.Error -> {
                            android.util.Log.e("ChatViewModel", "SSE stream error: ${event.error}")
                            _uiState.value = _uiState.value.copy(
                                error = "Error: ${event.error}",
                                isGenerating = false
                            )
                            isGenerating = false
                            streamCompleted = true
                        }
                    }
                }

                streamCompleted
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "SSE streaming failed, falling back to polling: ${e.message}", e)
                false
            }

            if (!streamSuccess) {
                // Fallback to polling approach
                android.util.Log.d("ChatViewModel", "Using polling fallback")
                usePollingApproach(request)
            }

            // Clear file attachments after successful send
            clearFileAttachments()

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isGenerating = false,
                error = "Error: ${e.message}"
            )
            isGenerating = false
        }
    }

    private suspend fun pollForMessages(convId: String) {
        var polling = true
        var lastMessageCount = 0
        var emptyPolls = 0
        var lastContent = ""
        val seenMessageIds = mutableSetOf<String>()

        while (polling && isGenerating) {
            try {
                kotlinx.coroutines.delay(POLLING_INTERVAL_MS)

                // Fetch messages from API
                val messagesResponse = api.getMessages(convId)
                if (messagesResponse.isSuccessful && messagesResponse.body() != null) {
                    val messages = messagesResponse.body()!!

                    // Debug logging for image generation
                    val lastMsg = messages.lastOrNull { it.role == "assistant" }
                    if (lastMsg != null) {
                        android.util.Log.d("ChatViewModel", "Last assistant message:")
                        android.util.Log.d("ChatViewModel", "  Content: ${lastMsg.content}")
                        android.util.Log.d("ChatViewModel", "  Images: ${lastMsg.images}")
                        android.util.Log.d("ChatViewModel", "  Annotations: ${lastMsg.annotations}")
                    }

                    // Get the last assistant message
                    val lastAssistantMessage = messages.lastOrNull { it.role == "assistant" }
                    val currentContent = lastAssistantMessage?.content ?: ""

                    // Check if generation is complete by tracking content changes
                    if (messages.size > lastMessageCount || currentContent != lastContent) {
                        lastMessageCount = messages.size
                        lastContent = currentContent
                        emptyPolls = 0

                        // Save messages to database atomically
                        // Use withContext to ensure database operations complete on IO thread
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val newMessageEntities = mutableListOf<MessageEntity>()
                                val newMessageIds = mutableSetOf<String>()

                                for (messageDto in messages) {
                                    if (seenMessageIds.add(messageDto.id)) {
                                        newMessageEntities.add(messageDto.toEntity(convId))
                                        newMessageIds.add(messageDto.id)
                                    }
                                }

                                if (newMessageEntities.isNotEmpty()) {
                                    messageDao.insertMessages(newMessageEntities)
                                }
                                // Update existing assistant message with new content if needed
                                lastAssistantMessage?.let { lastMsg ->
                                    if (lastMsg.id !in newMessageIds && currentContent.isNotEmpty()) {
                                        messageRepository.updateMessageContent(
                                            messageId = lastMsg.id,
                                            content = lastMsg.content,
                                            reasoning = lastMsg.reasoning
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ChatViewModel", "Failed to save messages to DB: ${e.message}")
                            }
                        }
                    } else {
                        emptyPolls++
                    }

                    // Update UI with messages (after database save for consistency)
                    _messages.value = messages.map { dto ->
                        dto.toDomain()
                    }

                    // Stop polling if we have assistant message content/images and no new content for MAX_EMPTY_POLLS consecutive polls
                    val hasImages = lastAssistantMessage?.images?.isNotEmpty() == true
                    val hasImageAnnotations = lastAssistantMessage?.annotations?.any { it.type == "image" } == true
                    val hasVisualContent = hasImages || hasImageAnnotations

                    // Detect if this is likely image generation (empty or placeholder content, no images yet)
                    val isLikelyImageGeneration = (currentContent.isEmpty() ||
                            currentContent.equals("Generated Image", ignoreCase = true) ||
                            currentContent.equals("Image", ignoreCase = true) ||
                            currentContent.length < 20) && !hasVisualContent

                    // Check if content is a placeholder that indicates generation is still in progress
                    val isPlaceholderContent = currentContent.equals("Generating image...", ignoreCase = true) ||
                                             currentContent.equals("Generating...", ignoreCase = true) ||
                                             currentContent.isEmpty()

                    // For image generation, only complete when images actually arrive AND we've had empty polls
                    // For text generation, complete when content exists and we've had empty polls
                    val shouldComplete = if (isLikelyImageGeneration) {
                        // Keep polling until images arrive AND stabilize
                        hasVisualContent && emptyPolls >= MAX_EMPTY_POLLS
                    } else {
                        // Normal text completion logic - don't complete if we're waiting for images
                        if (hasVisualContent) {
                            // We have images, wait for empty polls
                            emptyPolls >= MAX_EMPTY_POLLS
                        } else {
                            // Text only, complete when content exists and we've had empty polls
                            !isPlaceholderContent && currentContent.isNotEmpty() && emptyPolls >= MAX_EMPTY_POLLS
                        }
                    }

                    android.util.Log.d("ChatViewModel", "Poll state: hasImages=$hasImages, hasVisualContent=$hasVisualContent, isLikelyImageGeneration=$isLikelyImageGeneration, emptyPolls=$emptyPolls, shouldComplete=$shouldComplete")

                    if (lastAssistantMessage != null && shouldComplete) {
                        android.util.Log.d("ChatViewModel", "Stopping generation - shouldComplete=true")
                        polling = false
                        isGenerating = false
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false
                        )

                        // Start polling for auto-generated conversation title
                        if (!titleGenerated) {
                            conversationTitlePoller.startPolling(convId)
                            titleGenerated = true
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue polling on error
                android.util.Log.e("ChatViewModel", "Error polling messages: ${e.message}")
            }
        }
    }

    private suspend fun refreshConversationDetails(convId: String) {
        try {
            val convResponse = api.getConversation(convId)
            if (convResponse.isSuccessful && convResponse.body() != null) {
                val conversationDto = convResponse.body()!!
                val conversationEntity = conversationDto.toEntity()

                // Update in database
                conversationRepository.insertConversation(conversationEntity)

                // Update UI state with the refreshed conversation (including auto-generated title)
                _uiState.value = _uiState.value.copy(conversation = conversationEntity)
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to refresh conversation details: ${e.message}")
        }
    }

    private fun handleStreamEvent(event: StreamingManager.StreamEvent) {
        when (event) {
            is StreamingManager.StreamEvent.ContentDelta -> {
                currentAssistantMessage.append(event.content)
                updateAssistantMessage(currentAssistantMessage.toString())
            }
            is StreamingManager.StreamEvent.ReasoningDelta -> {
                currentReasoning.append(event.reasoning)
                updateAssistantMessage(
                    content = currentAssistantMessage.toString(),
                    reasoning = currentReasoning.toString()
                )
            }
            is StreamingManager.StreamEvent.TokenReceived -> {
                currentAssistantMessage.append(event.token)
                updateAssistantMessage(currentAssistantMessage.toString())
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

    fun stopGeneration() {
        streamingManager.stopStreaming()
        isGenerating = false
        _uiState.value = _uiState.value.copy(
            isGenerating = false,
            isGeneratingVideo = false,
            videoGenerationStatus = null
        )
    }

    /**
     * Updates the assistant message in the UI with new content
     */
    private fun updateAssistantMessage(content: String, reasoning: String? = null) {
        val targetId = streamingMessageId ?: return
        _messages.update { currentList ->
            currentList.map { msg ->
                if (msg.id == targetId) {
                    msg.copy(
                        content = content,
                        reasoning = reasoning ?: msg.reasoning
                    )
                } else {
                    msg
                }
            }
        }
    }

    /**
     * Fetches messages from API and saves them to database
     */
    private suspend fun fetchAndSaveMessages(convId: String) {
        try {
            val messagesResponse = api.getMessages(convId)
            if (messagesResponse.isSuccessful && messagesResponse.body() != null) {
                val messages = messagesResponse.body()!!

                // Get current video URLs to filter out redundant text messages
                val existingVideoUrls = _messages.value
                    .flatMap { it.videos ?: emptyList() }
                    .toSet()

                // Filter out redundant assistant messages
                val filteredMessages = if (existingVideoUrls.isNotEmpty()) {
                    messages.filterNot { msg ->
                        msg.role == "assistant" &&
                        existingVideoUrls.any { url ->
                            msg.content.contains(url) ||
                            msg.content.contains(url.substringAfterLast("/"))
                        }
                    }
                } else {
                    messages
                }

                // Get current pending messages for this conversation to prevent duplicates
                // This happens because we insert a local PENDING message when user sends,
                // and then fetch the same message from backend with a different ID.
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val pendingMessages = messageDao.getPendingMessages()
                        .filter { it.conversationId == convId }

                    // Find IDs to delete and map local IDs
                    val idsToDelete = mutableListOf<String>()
                    val localIdMap = mutableMapOf<String, String>() // backendId -> localId

                    pendingMessages.forEach { pending ->
                        val match = filteredMessages.find { backendMsg ->
                            backendMsg.role == pending.role &&
                                    backendMsg.content == pending.content
                        }
                        if (match != null) {
                            android.util.Log.d("ChatViewModel", "Marking pending message ${pending.id} for deletion")
                            idsToDelete.add(pending.id)
                            // Preserve the local ID (pending.id) for the new backend message
                            localIdMap[match.id] = pending.localId ?: pending.id
                        }
                    }

                    val messageEntities = filteredMessages.map { it.toEntity(convId, localIdMap[it.id]) }

                    // Perform atomic replace
                    messageDao.replaceMessages(idsToDelete, messageEntities)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to fetch and save messages: ${e.message}")
        }
    }

    /**
     * Marks generation as complete and handles post-generation tasks
     */
    private suspend fun finishGeneration(convId: String?) {
        isGenerating = false
        _uiState.value = _uiState.value.copy(isGenerating = false)

        // Trigger title generation for new conversations
        if (convId != null && !titleGenerated) {
            conversationTitlePoller.startPolling(convId)
            titleGenerated = true

            // Reload conversation after a delay to get the updated title
            kotlinx.coroutines.delay(3000)
            loadConversation(silent = true)
        }
    }

    /**
     * Legacy polling approach as fallback when SSE streaming fails
     */
    private suspend fun usePollingApproach(request: GenerateMessageRequest) {
        // Call generate-message API
        android.util.Log.d("ChatViewModel", "===== POLLING APPROACH =====")
        android.util.Log.d("ChatViewModel", "Request conversation_id: ${request.conversation_id}")
        android.util.Log.d("ChatViewModel", "Local conversationId: $conversationId")

        val response = api.generateMessage(request)
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Failed to generate message: HTTP ${response.code()}")
        }

        val generateResponse = response.body()!!
        if (!generateResponse.ok) {
            throw Exception("Failed to generate message")
        }

        val newConversationId = generateResponse.conversation_id
        android.util.Log.d("ChatViewModel", "Response conversation_id: $newConversationId")

        // Update the conversationId for new conversations
        if (conversationId == null) {
            conversationId = newConversationId
            titleGenerated = false
            android.util.Log.d("ChatViewModel", "New conversation! Set local conversationId to: $newConversationId")
        } else {
            android.util.Log.d("ChatViewModel", "Existing conversation! local=$conversationId, response=$newConversationId")
            if (conversationId != newConversationId) {
                android.util.Log.e("ChatViewModel", "WARNING: Backend returned different conversationId!")
                android.util.Log.e("ChatViewModel", "This means the backend created a NEW conversation instead of using the existing one")
            }
        }

        // Save the conversation ID
        secureStorage.saveLastConversationId(newConversationId)

        // For new conversations, fetch and save conversation details to database
        try {
            val convResponse = api.getConversation(newConversationId)
            if (convResponse.isSuccessful && convResponse.body() != null) {
                val conversationDto = convResponse.body()!!
                val conversationEntity = conversationDto.toEntity()

                android.util.Log.d("ChatViewModel", "Inserting conversation to database: ${conversationEntity.id}, title: ${conversationEntity.title}")
                conversationRepository.insertConversation(conversationEntity)
                android.util.Log.d("ChatViewModel", "Conversation inserted to database")

                // Update UI state
                _uiState.value = _uiState.value.copy(conversation = conversationEntity)

                // Notify AFTER database insert so conversation is visible in sidebar
                android.util.Log.d("ChatViewModel", "Notifying conversation created: $newConversationId")
                conversationSyncManager.notifyConversationCreated(newConversationId)
                android.util.Log.d("ChatViewModel", "Notification complete")
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to save new conversation to DB: ${e.message}", e)
        }

        // Start polling for messages
        pollForMessages(newConversationId)
    }

    fun logMessageInteraction(messageId: String, action: String) {
        viewModelScope.launch {
            try {
                val request = com.nanogpt.chat.data.remote.dto.MessageInteractionRequest(
                    messageId = messageId,
                    action = action,
                    metadata = null
                )
                api.logMessageInteraction(request)
            } catch (e: Exception) {
                // Log error but don't show to user - interaction logging is optional
                android.util.Log.e("ChatViewModel", "Failed to log interaction: ${e.message}")
            }
        }
    }

    fun regenerateLastMessage() {
        viewModelScope.launch {
            val messages = _messages.value
            val lastAssistantMessage = messages.lastOrNull { it.role == "assistant" }

            if (lastAssistantMessage != null) {
                // Log the regenerate interaction
                logMessageInteraction(lastAssistantMessage.id, "regenerate")

                // If the message was starred, unstar it first
                if (lastAssistantMessage.starred == true) {
                    messageRepository.toggleMessageStar(lastAssistantMessage.id, false)
                        .onFailure { e ->
                            android.util.Log.e("ChatViewModel", "Failed to unstar message: ${e.message}")
                        }
                }

                // Find the user message before the assistant message
                val lastUserMessageIndex = messages.indexOfLast { it.role == "user" }
                if (lastUserMessageIndex != -1 && lastUserMessageIndex < messages.indexOf(lastAssistantMessage)) {
                    val userMessage = messages[lastUserMessageIndex]

                    // Remove the last assistant message
                    _messages.value = messages.toMutableList().apply {
                        remove(lastAssistantMessage)
                    }

                    // Delete from database
                    messageRepository.deleteMessage(lastAssistantMessage.id)

                    // Regenerate with the same user message, but skip adding it to UI/database
                    sendMessageInternal(userMessage.content, skipUserMessage = true)
                }
            }
        }
    }

    fun saveChatToKarakeep(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val convId = conversationId ?: uiState.value.conversation?.id
                if (convId.isNullOrBlank()) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, "No active conversation")
                    }
                    return@launch
                }

                android.util.Log.d("Karakeep", "Saving chat $convId to Karakeep")

                val request = com.nanogpt.chat.data.remote.dto.SaveChatToKarakeepRequest(
                    conversationId = convId
                )

                val response = api.saveChatToKarakeep(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        val bookmarkId = body.bookmarkId ?: "unknown"
                        android.util.Log.d("Karakeep", "Chat saved successfully, bookmark ID: $bookmarkId")
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, "Saved to Karakeep!")
                        }
                    } else {
                        val errorMsg = body.message ?: "Unknown error"
                        android.util.Log.e("Karakeep", "Failed to save: $errorMsg")
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(false, "Failed to save: $errorMsg")
                        }
                    }
                } else {
                    val errorCode = response.code()
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("Karakeep", "Failed to save: HTTP $errorCode - $errorBody")

                    val errorMsg = when (errorCode) {
                        404 -> "Karakeep not configured"
                        400 -> {
                            // Only show "Karakeep not configured" if error body contains Karakeep-related message
                            when {
                                errorBody?.contains("Karakeep", ignoreCase = true) == true &&
                                (errorBody.contains("not configured", ignoreCase = true) ||
                                 errorBody.contains("not set up", ignoreCase = true) ||
                                 errorBody.contains("missing", ignoreCase = true)) -> "Karakeep not configured"
                                else -> "Error: $errorBody"
                            }
                        }
                        else -> "HTTP $errorCode"
                    }
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, errorMsg)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Karakeep", "Error saving to Karakeep: ${e.message}", e)
                val errorMsg = when {
                    e.message?.contains("404", ignoreCase = true) == true -> "Karakeep not configured"
                    e.message?.contains("400", ignoreCase = true) == true &&
                    (e.message?.contains("Karakeep", ignoreCase = true) == true ||
                     e.message?.contains("configured", ignoreCase = true) == true) -> "Karakeep not configured"
                    else -> "Error: ${e.message}"
                }
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, errorMsg)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun toggleStar(messageId: String, starred: Boolean) {
        viewModelScope.launch {
            messageRepository.toggleMessageStar(messageId, starred)
                .onSuccess {
                    // Update the message in the list
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(starred = starred)
                        } else {
                            msg
                        }
                    }
                    // Log the interaction
                    logMessageInteraction(messageId, if (starred) "star" else "unstar")
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to ${if (starred) "star" else "unstar"} message: ${e.message}"
                    )
                }
        }
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
                // Fetch user models (enabled models for this user)
                val response = api.getUserModels()
                if (response.isSuccessful) {
                    val body = response.body()?.string() ?: "{}"
                    val jsonElement = Json.parseToJsonElement(body)
                    val userModels = parseUserModelsResponse(jsonElement)

                    // First pass: create models without capabilities (will be updated when capabilities arrive)
                    val initialModels = userModels
                        .filter { it.enabled }
                        .sortedByDescending { it.pinned }
                        .map { userModel ->
                            createModelInfo(userModel.modelId, userModel.icon_url)
                        }

                    // Try to use the last used model
                    val lastModelId = secureStorage.getLastModelId()
                    val selectedModel = if (lastModelId != null) {
                        initialModels.find { it.id == lastModelId } ?: initialModels.firstOrNull()
                    } else {
                        initialModels.firstOrNull()
                    }

                    _uiState.value = _uiState.value.copy(
                        availableModels = initialModels,
                        selectedModel = selectedModel ?: ModelInfo("zai-org/glm-4.7", "GLM 4.7", "zai-org", getProviderLogoUrl("zai-org"))
                    )

                    // If an assistant is already selected, re-apply its model settings now that models are available
                    _uiState.value.selectedAssistant?.let { assistant ->
                        android.util.Log.d("ChatViewModel", "fetchUserModels: Re-applying assistant model settings")
                        applyAssistantSettings(assistant)
                    }
                } else {
                    // If API call fails, use default models
                    applyDefaultModels()
                }
            } catch (e: Exception) {
                // If fetch fails with exception, use default models
                applyDefaultModels()
            }
        }

        // Also fetch full models with capabilities in parallel (non-blocking)
        viewModelScope.launch {
            try {
                val response = api.getModels()
                if (response.isSuccessful && response.body() != null) {
                    val allModels = response.body()!!
                    // Build a map of modelId -> ModelDto for quick lookup
                    val capabilitiesMap = allModels.associateBy { it.id }
                    modelCapabilitiesCache.value = capabilitiesMap
                    android.util.Log.d("ChatViewModel", "Fetched ${capabilitiesMap.size} models with capabilities")

                    // Re-fetch user models to update them with capabilities
                    val userResponse = api.getUserModels()
                    if (userResponse.isSuccessful) {
                        val body = userResponse.body()?.string() ?: "{}"
                        val jsonElement = Json.parseToJsonElement(body)
                        val userModels = parseUserModelsResponse(jsonElement)
                        val updatedModels = userModels
                            .filter { it.enabled }
                            .sortedByDescending { it.pinned }
                            .map { userModel ->
                                createModelInfo(userModel.modelId, userModel.icon_url)
                            }

                        // Update UI with models that now have capabilities
                        _uiState.value = _uiState.value.copy(
                            availableModels = updatedModels
                        )
                        android.util.Log.d("ChatViewModel", "Updated models with capabilities")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ChatViewModel", "Failed to fetch model capabilities: ${e.message}")
                // Don't fail - the fallback pattern matching will still work
            }
        }
    }

    /**
     * Create a ModelInfo object from a model ID and icon URL.
     * Uses capabilities from cache if available.
     */
    private fun createModelInfo(modelId: String, iconUrl: String?): ModelInfo {
        val actualProvider = extractProviderFromModelId(modelId)
        val cachedModel = modelCapabilitiesCache.value[modelId]
        val capabilities = if (cachedModel != null) {
            ModelCapabilities(
                vision = cachedModel.capabilities.vision,
                reasoning = cachedModel.capabilities.reasoning,
                images = cachedModel.capabilities.images,
                video = cachedModel.capabilities.video
            )
        } else {
            ModelCapabilities()
        }

        return ModelInfo(
            id = modelId,
            name = extractFriendlyName(modelId),
            provider = actualProvider,
            providerLogo = iconUrl,
            capabilities = capabilities
        )
    }

    private fun fetchAssistants() {
        viewModelScope.launch {
            try {
                android.util.Log.d("ChatViewModel", "fetchAssistants: Starting to observe assistants")
                assistantRepository.getAssistants().collect { assistants ->
                    android.util.Log.d("ChatViewModel", "fetchAssistants: Received ${assistants.size} assistants")
                    _uiState.value = _uiState.value.copy(
                        availableAssistants = assistants
                    )

                    // If conversation has an assistant, load it
                    if (_uiState.value.conversation?.assistantId != null) {
                        val assistant = assistants.find { it.id == _uiState.value.conversation!!.assistantId }
                        if (assistant != null) {
                            android.util.Log.d("ChatViewModel", "fetchAssistants: Loading conversation assistant: ${assistant.name}")
                            _uiState.value = _uiState.value.copy(selectedAssistant = assistant)
                            applyAssistantSettings(assistant)
                        }
                    } else if (_uiState.value.selectedAssistant == null) {
                        // For new chats, restore the last selected assistant
                        val lastAssistantId = secureStorage.getLastAssistantId()
                        android.util.Log.d("ChatViewModel", "fetchAssistants: No assistant selected, lastAssistantId=$lastAssistantId")

                        val assistant = if (lastAssistantId != null) {
                            assistants.find { it.id == lastAssistantId }
                        } else {
                            // Default to the "Default" assistant, or first available if it doesn't exist
                            val defaultAssistant = assistants.find { it.name == "Default" } ?: assistants.firstOrNull()
                            android.util.Log.d("ChatViewModel", "fetchAssistants: Looking for Default assistant, found: ${defaultAssistant?.name}")
                            defaultAssistant
                        }

                        if (assistant != null) {
                            android.util.Log.d("ChatViewModel", "fetchAssistants: Selecting assistant: ${assistant.name}")
                            _uiState.value = _uiState.value.copy(selectedAssistant = assistant)
                            applyAssistantSettings(assistant)
                            // Persist the assistant if no assistant was previously selected
                            if (lastAssistantId == null) {
                                secureStorage.saveLastAssistantId(assistant.id)
                            }
                        } else {
                            android.util.Log.w("ChatViewModel", "fetchAssistants: No assistants available to select")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Failed to fetch assistants", e)
            }
        }
    }

    fun selectAssistant(assistant: AssistantEntity) {
        _uiState.value = _uiState.value.copy(selectedAssistant = assistant)

        // Persist the selection so new chats default to this assistant
        secureStorage.saveLastAssistantId(assistant.id)

        applyAssistantSettings(assistant)
    }

    private fun applyAssistantSettings(assistant: AssistantEntity) {
        android.util.Log.d("ChatViewModel", "applyAssistantSettings: Applying settings for ${assistant.name}")
        android.util.Log.d("ChatViewModel", "  assistant.modelId: ${assistant.modelId}")
        android.util.Log.d("ChatViewModel", "  availableModels: ${_uiState.value.availableModels.map { it.id }}")

        // Apply model
        val model = _uiState.value.availableModels.find { it.id == assistant.modelId }
        if (model != null) {
            android.util.Log.d("ChatViewModel", "  Found model: ${model.name}")
            _uiState.value = _uiState.value.copy(selectedModel = model)
        } else {
            android.util.Log.w("ChatViewModel", "  Model not found for assistant.modelId: ${assistant.modelId}")
        }

        // Apply web search settings
        if (assistant.webSearchEnabled) {
            val mode = when (assistant.webSearchMode) {
                "deep" -> WebSearchMode.DEEP
                else -> WebSearchMode.STANDARD
            }
            val provider = when (assistant.webSearchProvider) {
                "tavily" -> WebSearchProvider.TAVILY
                "exa" -> WebSearchProvider.EXA
                "kagi" -> WebSearchProvider.KAGI
                else -> WebSearchProvider.LINKUP
            }
            _uiState.value = _uiState.value.copy(
                webSearchMode = mode,
                webSearchProvider = provider
            )
        } else {
            _uiState.value = _uiState.value.copy(webSearchMode = WebSearchMode.OFF)
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

    /**
     * Extract a friendly name from a model ID.
     * Examples:
     * - "zai-org/glm-4.7" -> "GLM 4.7"
     * - "openai/gpt-4o" -> "GPT-4o"
     * - "anthropic/claude-3-5-sonnet-20241022" -> "Claude 3.5 Sonnet"
     */
    private fun extractFriendlyName(modelId: String): String {
        // Get the part after the provider slash
        val modelName = modelId.substringAfterLast("/")

        // Clean up and format the name
        return modelName
            // Replace version separators with spaces for better readability
            .replace("-", " ")
            .replace("_", " ")
            // Capitalize first letter of each word
            .split(" ")
            .joinToString(" ") { word ->
                if (word.isNotEmpty()) {
                    word[0].uppercase() + word.substring(1)
                } else {
                    word
                }
            }
            // Clean up multiple spaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Generate a video using the selected model.
     * Video generation uses a separate API from normal message generation.
     */
    fun generateVideo(prompt: String) {
        if (_inputText.value.isBlank() || isGenerating) return

        android.util.Log.d("ChatViewModel", "===== GENERATE VIDEO START =====")
        android.util.Log.d("ChatViewModel", "Prompt: $prompt")

        viewModelScope.launch {
            // Clear input text immediately
            _inputText.value = ""

            android.util.Log.d("ChatViewModel", "Setting isGeneratingVideo = true")
            _uiState.value = _uiState.value.copy(isGeneratingVideo = true, error = null)

            val model = _uiState.value.selectedModel?.id ?: DEFAULT_MODEL_ID
            val currentConversationId = conversationId

            // Step 1: Add user message to UI immediately
            val userMessage = Message(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = currentConversationId ?: "",
                role = "user",
                content = prompt,
                createdAt = System.currentTimeMillis()
            )
            _messages.value = _messages.value + userMessage

            // Step 1.5: Add placeholder assistant message for loading indicator
            val placeholderMessage = Message(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = currentConversationId ?: "",
                role = "assistant",
                content = "",
                modelId = model,
                createdAt = System.currentTimeMillis()
            )
            _messages.value = _messages.value + placeholderMessage
            android.util.Log.d("ChatViewModel", "Added placeholder message: ${placeholderMessage.id}, total messages: ${_messages.value.size}")
            android.util.Log.d("ChatViewModel", "Last assistant message: ${_messages.value.lastOrNull { it.role == "assistant" }?.id}")

            // Step 2: Create conversation on backend if it's a new chat
            val actualConversationId = if (currentConversationId == null) {
                try {
                    android.util.Log.d("ChatViewModel", "Creating new conversation for video generation")

                    // Create conversation by calling generateMessage with temporary flag
                    // We'll replace the assistant response later with our video
                    val request = GenerateMessageRequest(
                        conversation_id = null,
                        message = prompt,
                        model_id = model,
                        assistant_id = _uiState.value.selectedAssistant?.id,
                        project_id = _uiState.value.conversation?.projectId,
                        web_search_mode = _uiState.value.webSearchMode.name.lowercase(),
                        web_search_provider = _uiState.value.webSearchProvider?.name?.lowercase()
                    )

                    val response = api.generateMessage(request)
                    if (!response.isSuccessful || response.body() == null) {
                        throw Exception("Failed to create conversation: HTTP ${response.code()}")
                    }

                    val generateResponse = response.body()!!
                    if (!generateResponse.ok) {
                        throw Exception("Failed to create conversation")
                    }

                    val newConversationId = generateResponse.conversation_id
                    conversationId = newConversationId
                    secureStorage.saveLastConversationId(newConversationId)

                    // Fetch conversation details from backend
                    val convResponse = api.getConversation(newConversationId)
                    if (convResponse.isSuccessful && convResponse.body() != null) {
                        val conversationDto = convResponse.body()!!
                        val conversationEntity = conversationDto.toEntity()

                        android.util.Log.d("ChatViewModel", "Inserting conversation to database: ${conversationEntity.id}, title: ${conversationEntity.title}")
                        conversationRepository.insertConversation(conversationEntity)
                        android.util.Log.d("ChatViewModel", "Conversation inserted to database")

                        // Update UI state
                        _uiState.value = _uiState.value.copy(conversation = conversationEntity)

                        // Notify AFTER database insert so conversation is visible in sidebar
                        android.util.Log.d("ChatViewModel", "Notifying conversation created: $newConversationId")
                        conversationSyncManager.notifyConversationCreated(newConversationId)
                        android.util.Log.d("ChatViewModel", "Notification complete")
                    }

                    // Fetch messages from backend to get the real message IDs
                    // This will get the user message + any backend assistant response
                    android.util.Log.d("ChatViewModel", "About to call pollForMessagesBackend")
                    pollForMessagesBackend(newConversationId, preserveMessageId = placeholderMessage.id)
                    android.util.Log.d("ChatViewModel", "pollForMessagesBackend complete, isGeneratingVideo still true: ${_uiState.value.isGeneratingVideo}")
                    android.util.Log.d("ChatViewModel", "Total messages after pollForMessagesBackend: ${_messages.value.size}")
                    android.util.Log.d("ChatViewModel", "Last assistant message after poll: ${_messages.value.lastOrNull { it.role == "assistant" }?.id}")

                    // Remove backend's assistant message (it will be replaced with our video message)
                    val backendAssistantMessages = _messages.value.filter { it.role == "assistant" && it.id != placeholderMessage.id }
                    android.util.Log.d("ChatViewModel", "Found ${backendAssistantMessages.size} backend assistant messages")
                    if (backendAssistantMessages.isNotEmpty()) {
                        backendAssistantMessages.forEach { msg ->
                            android.util.Log.d("ChatViewModel", "Removing backend assistant message: ${msg.id}, content: ${msg.content}")
                            _messages.value = _messages.value.filter { it.id != msg.id }
                            try {
                                messageDao.deleteMessageById(msg.id)
                            } catch (e: Exception) {
                                android.util.Log.e("ChatViewModel", "Failed to delete backend assistant message: ${e.message}")
                            }
                        }
                    }
                    android.util.Log.d("ChatViewModel", "Total messages after removing backend messages: ${_messages.value.size}")
                    android.util.Log.d("ChatViewModel", "Last assistant message after removal: ${_messages.value.lastOrNull { it.role == "assistant" }?.id}")

                    // Update placeholder message's conversation ID
                    _messages.value = _messages.value.map {
                        if (it.id == placeholderMessage.id) {
                            it.copy(conversationId = newConversationId)
                        } else {
                            it
                        }
                    }

                    // Start polling for auto-generated conversation title
                    if (!titleGenerated) {
                        conversationTitlePoller.startPolling(newConversationId)
                        titleGenerated = true

                        // Reload conversation after a delay to get the updated title
                        kotlinx.coroutines.delay(3000)
                        loadConversation()
                    }

                    newConversationId
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Failed to create conversation: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isGeneratingVideo = false,
                        error = "Failed to create conversation: ${e.message}"
                    )
                    return@launch
                }
            } else {
                currentConversationId
            }

            // Step 3: Start video generation
            try {
                android.util.Log.d("ChatViewModel", "About to call videoGenerationRepository.generateVideo")
                val result = videoGenerationRepository.generateVideo(model, prompt)

                result.fold(
                    onSuccess = { runId ->
                        android.util.Log.d("ChatViewModel", "Video generation started: $runId, isGeneratingVideo: ${_uiState.value.isGeneratingVideo}")
                        pollVideoStatus(runId, model, prompt, actualConversationId ?: "", placeholderMessage.id)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isGeneratingVideo = false,
                            error = "Failed to generate video: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingVideo = false,
                    error = "Video generation error: ${e.message}"
                )
            }
        }
    }

    /**
     * Fetch messages from backend and update UI (similar to pollForMessages but without polling loop)
     * @param preserveMessageId If provided, this message ID will be preserved in the messages list
     */
    private suspend fun pollForMessagesBackend(convId: String, preserveMessageId: String? = null) {
        try {
            val messagesResponse = api.getMessages(convId)
            if (messagesResponse.isSuccessful && messagesResponse.body() != null) {
                val messages = messagesResponse.body()!!

                // Get current video URLs to filter out redundant text messages
                val existingVideoUrls = _messages.value
                    .flatMap { it.videos ?: emptyList() }
                    .toSet()

                // Filter out redundant assistant messages
                val filteredMessages = if (existingVideoUrls.isNotEmpty()) {
                    messages.filterNot { msg ->
                        msg.role == "assistant" &&
                        existingVideoUrls.any { url ->
                            msg.content.contains(url) ||
                            msg.content.contains(url.substringAfterLast("/"))
                        }
                    }
                } else {
                    messages
                }

                // Save to database
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val newMessageEntities = mutableListOf<MessageEntity>()
                    for (messageDto in filteredMessages) {
                        newMessageEntities.add(messageDto.toEntity(convId))
                    }
                    if (newMessageEntities.isNotEmpty()) {
                        messageDao.insertMessages(newMessageEntities)
                    }
                }

                // Update UI, preserving the specified message if provided
                if (preserveMessageId != null) {
                    val preservedMessage = _messages.value.find { it.id == preserveMessageId }
                    if (preservedMessage != null) {
                        val backendMessages = filteredMessages.map { it.toDomain() }
                        val backendMessageIds = backendMessages.map { it.id }.toSet()

                        // Keep backend messages that don't conflict with preserved message, or merge them
                        _messages.value = backendMessages + _messages.value.filter { it.id == preserveMessageId }
                    } else {
                        _messages.value = filteredMessages.map { it.toDomain() }
                    }
                } else {
                    _messages.value = filteredMessages.map { it.toDomain() }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to fetch messages: ${e.message}")
        }
    }

    /**
     * Poll video generation status until completion or failure.
     */
    private suspend fun pollVideoStatus(
        runId: String,
        model: String,
        prompt: String,
        conversationId: String,
        placeholderMessageId: String
    ) {
        videoGenerationRepository.pollVideoStatus(runId, model).collect { result ->
            when (result) {
                is VideoGenerationRepository.VideoGenerationResult.InQueue -> {
                    _uiState.value = _uiState.value.copy(
                        videoGenerationStatus = VideoGenerationStatus.IN_QUEUE
                    )
                }
                is VideoGenerationRepository.VideoGenerationResult.InProgress -> {
                    _uiState.value = _uiState.value.copy(
                        videoGenerationStatus = VideoGenerationStatus.IN_PROGRESS
                    )
                }
                is VideoGenerationRepository.VideoGenerationResult.Completed -> {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingVideo = false,
                        videoGenerationStatus = VideoGenerationStatus.COMPLETED
                    )

                    // Remove redundant text messages containing the video URL
                    // The backend sometimes sends a text message with the video URL, which is redundant
                    // since we are displaying the video via the placeholder message
                    val videoUrl = result.videoUrl
                    val videoFilename = videoUrl.substringAfterLast("/")
                    
                    val redundantMessages = _messages.value.filter {
                        it.role == "assistant" &&
                        it.id != placeholderMessageId &&
                        (it.content.contains(videoUrl) ||
                         it.content.contains(videoFilename) ||
                         (it.content.contains("video", ignoreCase = true) && it.content.length < 100))
                    }

                    redundantMessages.forEach { msg ->
                        android.util.Log.d("ChatViewModel", "Removing redundant video message: ${msg.id}")
                        try {
                            messageDao.deleteMessageById(msg.id)
                        } catch (e: Exception) {
                            android.util.Log.e("ChatViewModel", "Failed to delete redundant message: ${e.message}")
                        }
                    }

                    // Update local list to remove them immediately
                    if (redundantMessages.isNotEmpty()) {
                        _messages.value = _messages.value.filter { it.id !in redundantMessages.map { m -> m.id } }
                    }

                    // Create video annotation
                    val videoAnnotation = Annotation(
                        type = "video",
                        data = kotlinx.serialization.json.buildJsonObject {
                            put("url", kotlinx.serialization.json.JsonPrimitive(result.videoUrl))
                        }
                    )

                    // Update placeholder message with video
                    val updatedMessage = Message(
                        id = placeholderMessageId,
                        conversationId = conversationId,
                        role = "assistant",
                        content = "",
                        modelId = model,
                        annotations = listOf(videoAnnotation),
                        createdAt = System.currentTimeMillis()
                    )

                    // Replace placeholder in messages list
                    _messages.value = _messages.value.map {
                        if (it.id == placeholderMessageId) updatedMessage else it
                    }

                    // Save to database
                    val videoAnnotationJson = kotlinx.serialization.json.buildJsonArray {
                        add(kotlinx.serialization.json.buildJsonObject {
                            put("type", kotlinx.serialization.json.JsonPrimitive("video"))
                            put("data", kotlinx.serialization.json.buildJsonObject {
                                put("url", kotlinx.serialization.json.JsonPrimitive(result.videoUrl))
                            })
                        })
                    }.toString()

                    val assistantMessageEntity = com.nanogpt.chat.data.local.entity.MessageEntity(
                        id = placeholderMessageId,
                        conversationId = conversationId,
                        role = "assistant",
                        content = "",
                        modelId = model,
                        annotationsJson = videoAnnotationJson,
                        createdAt = System.currentTimeMillis(),
                        syncStatus = com.nanogpt.chat.data.local.entity.SyncStatus.SYNCED
                    )

                    // Check if message already exists in database, if not insert it
                    val existing = messageDao.getMessageById(placeholderMessageId)
                    if (existing == null) {
                        messageDao.insertMessages(listOf(assistantMessageEntity))
                    } else {
                        messageDao.updateMessage(assistantMessageEntity)
                    }
                }
                is VideoGenerationRepository.VideoGenerationResult.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingVideo = false,
                        videoGenerationStatus = VideoGenerationStatus.FAILED,
                        error = "Video generation failed: ${result.error}"
                    )
                    // Update placeholder with error message
                    _messages.value = _messages.value.map {
                        if (it.id == placeholderMessageId) {
                            it.copy(content = "Failed to generate video")
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if a model is an image generation model based on its capabilities.
     * Returns true if the model has image generation capability, false otherwise.
     */
    fun isImageGenerationModel(modelId: String?): Boolean {
        if (modelId == null) return false

        // Check cache first
        val cachedModel = modelCapabilitiesCache.value[modelId]
        if (cachedModel != null) {
            return cachedModel.capabilities.images
        }

        // Fallback to model ID pattern matching if not in cache
        val lowerId = modelId.lowercase()
        return lowerId.contains("flux") ||
                lowerId.contains("stable-diffusion") ||
                lowerId.contains("stablediffusion") ||
                lowerId.contains("dall-e") ||
                lowerId.contains("dalle") ||
                lowerId.contains("midjourney") ||
                lowerId.contains("imagen") ||
                lowerId.contains("kandinsky") ||
                lowerId.contains("sdxl") ||
                lowerId.contains("sd-xl") ||
                lowerId.contains("sd-") ||
                lowerId.contains("sd1.") ||
                lowerId.contains("sd2.") ||
                lowerId.contains("sd3") ||
                lowerId.contains("hidream") ||
                lowerId.contains("playground") ||
                lowerId.contains("stability-ai") ||
                lowerId.contains("black-forest-labs") ||
                lowerId.contains("ideogram") ||
                lowerId.contains("leonardo") ||
                lowerId.contains("replicate") ||
                lowerId.contains("image-gen") ||
                lowerId.contains("imagegen") ||
                lowerId.contains("txt2img") ||
                lowerId.contains("text-to-image")
    }

    /**
     * Check if a model is a video generation model based on its capabilities.
     * Returns true if the model has video generation capability, false otherwise.
     */
    fun isVideoGenerationModel(modelId: String?): Boolean {
        if (modelId == null) return false

        // Check cache first
        val cachedModel = modelCapabilitiesCache.value[modelId]
        if (cachedModel != null) {
            return cachedModel.capabilities.video
        }

        // Fallback to model ID pattern matching if not in cache
        val lowerId = modelId.lowercase()
        return lowerId.contains("sora") ||
                lowerId.contains("runway") ||
                lowerId.contains("pika") ||
                lowerId.contains("kling") ||
                lowerId.contains("video-gen") ||
                lowerId.contains("videogen") ||
                lowerId.contains("text-to-video") ||
                lowerId.contains("txt2vid") ||
                lowerId.contains("gen-2") ||
                lowerId.contains("gen_2") ||
                lowerId.contains("gen2") ||
                lowerId.contains("lumalabs") ||
                lowerId.contains("haiper") ||
                lowerId.contains("luma") ||
                lowerId.contains("luma-ai") ||
                lowerId.contains("heygen") ||
                lowerId.contains("synthesia") ||
                lowerId.contains("d-ID") ||
                lowerId.contains("d-id") ||
                lowerId.contains("animatediff") ||
                lowerId.contains("animatediffusion") ||
                lowerId.contains("stability-ai") && lowerId.contains("video") ||
                lowerId.contains("ideogram") && lowerId.contains("video")
    }

    override fun onCleared() {
        super.onCleared()
        // Stop all active title polling to prevent memory leaks
        conversationTitlePoller.stopAllPolls()
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
    val availableModels: List<ModelInfo> = emptyList(),
    val selectedAssistant: AssistantEntity? = null,
    val availableAssistants: List<AssistantEntity> = emptyList(),
    val fileAttachments: List<FileAttachment> = emptyList(),
    val isGeneratingVideo: Boolean = false,
    val videoGenerationStatus: VideoGenerationStatus? = null
)

enum class VideoGenerationStatus {
    IN_QUEUE,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

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
    val costUsd: Double? = null,
    val responseTimeMs: Long? = null,
    val annotations: List<Annotation>? = null,
    val images: List<String>? = null,
    val videos: List<String>? = null,
    val starred: Boolean? = null,
    val localId: String? = null
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

    // Extract images from image annotations
    val images = annotations?.filter { it.type == "image" }?.mapNotNull { annotation ->
        if (annotation.data is kotlinx.serialization.json.JsonObject) {
            val dataObj = annotation.data as kotlinx.serialization.json.JsonObject
            dataObj["url"]?.jsonPrimitive?.content
        } else {
            null
        }
    }

    // Extract videos from video annotations
    val videos = annotations?.filter { it.type == "video" }?.mapNotNull { annotation ->
        if (annotation.data is kotlinx.serialization.json.JsonObject) {
            val dataObj = annotation.data as kotlinx.serialization.json.JsonObject
            dataObj["url"]?.jsonPrimitive?.content
        } else {
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
        costUsd = costUsd,
        responseTimeMs = responseTimeMs,
        annotations = annotations,
        images = images,
        videos = videos,
        starred = starred,
        localId = localId
    )
}
