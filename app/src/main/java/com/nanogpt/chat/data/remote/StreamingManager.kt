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

    // Track the current call for cancellation
    private var currentCall: okhttp3.Call? = null

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
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .build()

            // Store the call for cancellation
            currentCall = okHttpClient.newCall(httpRequest)

            val response = currentCall?.execute()

            if (response == null || !response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response?.code ?: "unknown"}"))
            }

            val body = response.body ?: return@withContext Result.failure(
                Exception("Empty response body")
            )

            BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                var line: String? = null
                var conversationId: String? = request.conversation_id
                var lastEventTime = System.currentTimeMillis()

                while (currentCall?.isCanceled() != true && reader.readLine().also { line = it } != null) {
                    // Check for timeout (no data for 5 minutes)
                    val now = System.currentTimeMillis()
                    if (now - lastEventTime > 300000) {
                        onEvent(StreamEvent.Error("Stream timeout after 5 minutes"))
                        break
                    }

                    val currentLine = line
                    when {
                        currentLine?.startsWith("data: ") == true -> {
                            lastEventTime = now
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
                                // Log parse error but continue streaming
                                android.util.Log.e("StreamingManager", "Error parsing SSE data: ${e.message}")
                            }
                        }
                    }
                }

                // If we exited the loop without [DONE], handle appropriately
                if (currentCall?.isCanceled() == true) {
                    onEvent(StreamEvent.Error("Generation cancelled"))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            currentCall = null
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
        // Cancel the ongoing streaming request
        currentCall?.cancel()
        currentCall = null
    }
}
