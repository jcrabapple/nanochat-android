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

    companion object {
        /** Maximum time to wait for data before timing out the stream (5 minutes) */
        private const val STREAM_TIMEOUT_MS = 5 * 60 * 1000L
    }

    // Track the current call for cancellation
    private var currentCall: okhttp3.Call? = null

    sealed class StreamEvent {
        data class TokenReceived(val token: String) : StreamEvent()
        data class ContentDelta(val content: String) : StreamEvent()
        data class ReasoningDelta(val reasoning: String) : StreamEvent()
        data class Complete(
            val messageId: String? = null,
            val tokenCount: Int? = null,
            val costUsd: Double? = null,
            val responseTimeMs: Long? = null
        ) : StreamEvent()
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
                .url("$backendUrl/api/generate-message/stream")  // Use streaming endpoint
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
                var messageId: String? = null
                var lastEventTime = System.currentTimeMillis()
                var eventCount = 0
                var currentEventType: String? = null
                var currentData: String? = null

                android.util.Log.d("StreamingManager", "Starting SSE stream reading...")

                while (currentCall?.isCanceled() != true && reader.readLine().also { line = it } != null) {
                    // Check for timeout (no data for 5 minutes)
                    val now = System.currentTimeMillis()
                    if (now - lastEventTime > STREAM_TIMEOUT_MS) {
                        android.util.Log.e("StreamingManager", "Stream timeout after 5 minutes")
                        onEvent(StreamEvent.Error("Stream timeout after 5 minutes"))
                        break
                    }

                    val currentLine = line
                    android.util.Log.v("StreamingManager", "SSE line: $currentLine")

                    when {
                        // Empty line marks the end of an event
                        currentLine?.isEmpty() == true -> {
                            // Process the complete event
                            if (currentEventType != null && currentData != null) {
                                lastEventTime = now
                                eventCount++
                                android.util.Log.d("StreamingManager", "SSE event: $currentEventType, data: $currentData")

                                handleSseEvent(currentEventType, currentData, conversationId, messageId, onEvent) { newConvId, newMsgId ->
                                    conversationId = newConvId
                                    messageId = newMsgId
                                }
                            }
                            // Reset for next event
                            currentEventType = null
                            currentData = null
                        }
                        currentLine?.startsWith("event: ") == true -> {
                            currentEventType = currentLine.removePrefix("event: ").trim()
                        }
                        currentLine?.startsWith("data: ") == true -> {
                            currentData = currentLine.removePrefix("data: ").trim()
                        }
                    }
                }

                android.util.Log.d("StreamingManager", "SSE stream ended. Received $eventCount events, cancelled=${currentCall?.isCanceled()}")

                // If we exited the loop without [DONE], handle appropriately
                if (currentCall?.isCanceled() == true) {
                    onEvent(StreamEvent.Error("Generation cancelled"))
                } else if (eventCount == 0) {
                    // No events received - this might be a JSON response instead of SSE
                    android.util.Log.e("StreamingManager", "No SSE events received - might be JSON response")
                    onEvent(StreamEvent.Error("No SSE events received"))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            currentCall = null
        }
    }

    private fun handleSseEvent(
        eventType: String,
        data: String,
        conversationId: String?,
        messageId: String?,
        onEvent: (StreamEvent) -> Unit,
        updateIds: (String?, String?) -> Unit
    ) {
        try {
            val jsonElement = Json.parseToJsonElement(data)
            val jsonObject = jsonElement.jsonObject

            when (eventType) {
                "message_start" -> {
                    // Extract conversation_id and message_id
                    val convId = jsonObject["conversation_id"]?.jsonPrimitive?.content
                    val msgId = jsonObject["message_id"]?.jsonPrimitive?.content
                    android.util.Log.d("StreamingManager", "Message started: conv=$convId, msg=$msgId")

                    // If this is a new conversation (different from request), emit ConversationCreated event
                    if (convId != null && convId != conversationId) {
                        android.util.Log.d("StreamingManager", "===== CONVERSATION CREATED =====")
                        android.util.Log.d("StreamingManager", "New conversationId: $convId")
                        onEvent(StreamEvent.ConversationCreated(convId, ""))
                    }

                    updateIds(convId, messageId ?: msgId)
                }
                "delta" -> {
                    // Extract content and reasoning
                    val content = jsonObject["content"]?.jsonPrimitive?.content ?: ""
                    val reasoning = jsonObject["reasoning"]?.jsonPrimitive?.content ?: ""

                    when {
                        content.isNotEmpty() -> {
                            onEvent(StreamEvent.ContentDelta(content))
                        }
                        reasoning.isNotEmpty() -> {
                            onEvent(StreamEvent.ReasoningDelta(reasoning))
                        }
                    }
                }
                "message_complete" -> {
                    android.util.Log.d("StreamingManager", "Message complete: $data")
                    val tokenCount = jsonObject["token_count"]?.jsonPrimitive?.content?.toIntOrNull()
                    val costUsd = jsonObject["cost_usd"]?.jsonPrimitive?.content?.toDoubleOrNull()
                    val responseTimeMs = jsonObject["response_time_ms"]?.jsonPrimitive?.content?.toLongOrNull()
                    onEvent(StreamEvent.Complete(conversationId, tokenCount, costUsd, responseTimeMs))
                }
                "error" -> {
                    val error = jsonObject["error"]?.jsonPrimitive?.content ?: "Unknown error"
                    android.util.Log.e("StreamingManager", "Stream error: $error")
                    onEvent(StreamEvent.Error(error))
                }
                else -> {
                    android.util.Log.d("StreamingManager", "Unknown event type: $eventType")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StreamingManager", "Error parsing SSE event: $eventType - $data", e)
        }
    }

    fun stopStreaming() {
        // Cancel the ongoing streaming request
        currentCall?.cancel()
        currentCall = null
    }
}
