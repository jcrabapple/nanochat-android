package com.nanogpt.chat.data.repository

import android.util.Log
import com.nanogpt.chat.data.local.dao.ConversationDao
import com.nanogpt.chat.data.remote.api.NanoChatApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Polls the backend for auto-generated conversation titles.
 *
 * The backend generates conversation titles asynchronously after the first message is sent.
 * This poller checks every 2 seconds for up to 30 seconds to see if a meaningful title
 * has been generated, then updates the local database.
 */
@Singleton
class ConversationTitlePoller @Inject constructor(
    private val conversationDao: ConversationDao,
    private val api: NanoChatApi
) {
    // Track active polling jobs to avoid duplicate polls
    private val activePolls = ConcurrentHashMap<String, Job>()

    // Use a dedicated scope for polling operations
    private val scope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    companion object {
        private const val DEFAULT_TITLE = "New Chat"
        private const val POLL_INTERVAL = 2000L  // 2 seconds between polls
        private const val MAX_POLL_ATTEMPTS = 15 // 30 seconds total (15 * 2s)

        // Heuristic: meaningful titles are typically longer than 10 characters
        private const val MEANINGFUL_TITLE_MIN_LENGTH = 10
    }

    /**
     * Start polling for a conversation's title.
     * If already polling for this conversation, the existing poll is cancelled first.
     *
     * @param conversationId The conversation to poll for title updates
     */
    fun startPolling(conversationId: String) {
        // Cancel any existing poll for this conversation
        stopPolling(conversationId)

        activePolls[conversationId] = scope.launch {
            var attempts = 0

            while (attempts < MAX_POLL_ATTEMPTS && isActive) {
                delay(POLL_INTERVAL)

                try {
                    val response = api.getConversation(conversationId)
                    if (response.isSuccessful && response.body() != null) {
                        val conversationDto = response.body()!!
                        val newTitle = conversationDto.title

                        // Check if title has been generated (not default, meaningful length)
                        val isMeaningfulTitle = newTitle != DEFAULT_TITLE &&
                            newTitle.isNotBlank() &&
                            newTitle.length > MEANINGFUL_TITLE_MIN_LENGTH

                        if (isMeaningfulTitle) {
                            // Update the database with the new title
                            val existing = conversationDao.getConversationById(conversationId)
                            existing?.let { currentEntity ->
                                val updated = currentEntity.copy(title = newTitle)
                                conversationDao.updateConversation(updated)

                                Log.d(
                                    "ConversationTitlePoller",
                                    "Updated title for $conversationId: \"$newTitle\""
                                )
                            }

                            // Title generated successfully, stop polling
                            stopPolling(conversationId)
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    // Log error but continue polling
                    Log.e(
                        "ConversationTitlePoller",
                        "Error polling for title ($conversationId): ${e.message}"
                    )
                }

                attempts++

                if (attempts >= MAX_POLL_ATTEMPTS) {
                    Log.d(
                        "ConversationTitlePoller",
                        "Max polling attempts reached for $conversationId, title may not have been generated"
                    )
                }
            }

            // Max attempts reached or job was cancelled, stop polling
            stopPolling(conversationId)
        }
    }

    /**
     * Stop polling for a specific conversation.
     *
     * @param conversationId The conversation to stop polling for
     */
    fun stopPolling(conversationId: String) {
        activePolls[conversationId]?.cancel()
        activePolls.remove(conversationId)
    }

    /**
     * Stop all active polling operations.
     * Useful for cleanup in ViewModel.onCleared()
     */
    fun stopAllPolls() {
        activePolls.values.forEach { job ->
            job.cancel()
        }
        activePolls.clear()
    }

    /**
     * Check if currently polling for a conversation.
     *
     * @param conversationId The conversation to check
     * @return true if actively polling, false otherwise
     */
    fun isPolling(conversationId: String): Boolean {
        return activePolls.containsKey(conversationId)
    }

    /**
     * Get the number of conversations currently being polled.
     *
     * @return The count of active polls
     */
    fun getActivePollCount(): Int {
        return activePolls.size
    }
}
