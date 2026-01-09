package com.nanogpt.chat.data.remote

import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.remote.dto.GenerateMessageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val secureStorage: SecureStorage
) {

    sealed class StreamEvent {
        data class TokenReceived(val token: String) : StreamEvent()
        data class ContentDelta(val content: String) : StreamEvent()
        data class ReasoningDelta(val reasoning: String) : StreamEvent()
        data class Complete(val messageId: String? = null) : StreamEvent()
        data class Error(val error: String) : StreamEvent()
        data class ConversationCreated(val conversationId: String, val title: String) : StreamEvent()
    }

    suspend fun streamMessage(
        request: GenerateMessageRequest,
        onEvent: (StreamEvent) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backendUrl = secureStorage.getBackendUrl() ?: return@withContext Result.failure(
                Exception("Backend URL not configured")
            )

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            val requestBody = json.encodeToString(
                kotlinx.serialization.serializer<GenerateMessageRequest>(),
                request
            ).toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url("$backendUrl/api/generate-message")
                .post(requestBody)
                .header("Authorization", "Bearer ${secureStorage.getSessionToken()}")
                .header("Accept", "text/event-stream")
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(
                Exception("Empty response body")
            )

            BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                var line: String?
                var conversationId: String? = request.conversation_id

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line
                    when {
                        currentLine?.startsWith("data: ") == true -> {
                            val data = currentLine.removePrefix("data: ").trim()

                            if (data == "[DONE]") {
                                onEvent(StreamEvent.Complete(conversationId))
                                break
                            }

                            try {
                                val jsonElement = Json.parseToJsonElement(data)
                                handleSseData(jsonElement, conversationId, onEvent) { newConvId ->
                                    conversationId = newConvId
                                }
                            } catch (e: Exception) {
                                // Log parse error but continue
                                println("Error parsing SSE data: ${e.message}")
                            }
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun handleSseData(
        json: kotlinx.serialization.json.JsonElement,
        conversationId: String?,
        onEvent: (StreamEvent) -> Unit,
        updateConversationId: (String) -> Unit
    ) {
        val jsonObject = json.jsonObject

        // Check for conversation creation
        if (jsonObject.containsKey("conversationId") && jsonObject.containsKey("conversationTitle")) {
            val convId = jsonObject["conversationId"]?.jsonPrimitive?.content
            val convTitle = jsonObject["conversationTitle"]?.jsonPrimitive?.content
            if (convId != null && convTitle != null) {
                updateConversationId(convId)
                onEvent(StreamEvent.ConversationCreated(convId, convTitle))
            }
        }

        // Check for different content types
        when {
            "token" in jsonObject -> {
                val token = jsonObject["token"]?.jsonPrimitive?.content ?: ""
                onEvent(StreamEvent.TokenReceived(token))
            }
            "content" in jsonObject -> {
                val content = jsonObject["content"]?.jsonPrimitive?.content ?: ""
                onEvent(StreamEvent.ContentDelta(content))
            }
            "reasoning" in jsonObject -> {
                val reasoning = jsonObject["reasoning"]?.jsonPrimitive?.content ?: ""
                onEvent(StreamEvent.ReasoningDelta(reasoning))
            }
            "error" in jsonObject -> {
                val error = jsonObject["error"]?.jsonPrimitive?.content ?: "Unknown error"
                onEvent(StreamEvent.Error(error))
            }
        }
    }

    fun stopStreaming() {
        // Cancel any ongoing streaming
        // Implementation depends on how we track active streams
    }
}
