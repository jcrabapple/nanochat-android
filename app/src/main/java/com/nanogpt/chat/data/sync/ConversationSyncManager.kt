package com.nanogpt.chat.data.sync

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central event bus for conversation updates across ViewModels.
 * Enables ChatViewModel to notify ConversationsListViewModel when conversations are created/updated.
 */
@Singleton
class ConversationSyncManager @Inject constructor() {

    private val _conversationUpdates = MutableSharedFlow<ConversationUpdate>(
        replay = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    val conversationUpdates: SharedFlow<ConversationUpdate> = _conversationUpdates.asSharedFlow()

    /**
     * Notify listeners that a new conversation was created.
     * Triggers ConversationsListViewModel to refresh from API.
     */
    suspend fun notifyConversationCreated(conversationId: String) {
        android.util.Log.d("ConversationSyncManager", "===== NOTIFY CONVERSATION CREATED =====")
        android.util.Log.d("ConversationSyncManager", "Emitting Created event for: $conversationId")
        _conversationUpdates.emit(ConversationUpdate.Created(conversationId))
        android.util.Log.d("ConversationSyncManager", "Event emitted successfully")
    }

    /**
     * Notify listeners that a conversation was updated.
     * Used when conversation details change (title, pin status, etc.).
     */
    suspend fun notifyConversationUpdated(conversationId: String) {
        _conversationUpdates.emit(ConversationUpdate.Updated(conversationId))
    }

    /**
     * Notify listeners that conversations were refreshed from server.
     * Called by background sync worker.
     */
    suspend fun notifyConversationsRefreshed() {
        _conversationUpdates.emit(ConversationUpdate.Refreshed)
    }

    /**
     * Notify listeners that a conversation was deleted.
     */
    suspend fun notifyConversationDeleted(conversationId: String) {
        _conversationUpdates.emit(ConversationUpdate.Deleted(conversationId))
    }
}

/**
 * Sealed class representing different types of conversation update events.
 */
sealed class ConversationUpdate {
    data class Created(val conversationId: String) : ConversationUpdate()
    data class Updated(val conversationId: String) : ConversationUpdate()
    data class Deleted(val conversationId: String) : ConversationUpdate()
    object Refreshed : ConversationUpdate()
}
