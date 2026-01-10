package com.nanogpt.chat.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.local.dao.ConversationDao
import com.nanogpt.chat.data.local.entity.ConversationEntity
import com.nanogpt.chat.data.local.entity.SyncStatus
import com.nanogpt.chat.data.repository.ConversationRepository
import com.nanogpt.chat.data.sync.ConversationSyncManager
import com.nanogpt.chat.data.sync.ConversationUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationsListViewModel @Inject constructor(
    private val conversationDao: ConversationDao,
    private val secureStorage: SecureStorage,
    private val conversationRepository: ConversationRepository,
    private val conversationSyncManager: ConversationSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsListUiState())
    val uiState: StateFlow<ConversationsListUiState> = _uiState.asStateFlow()

    init {
        // Always observe local database for conversation updates
        observeConversations()
        // Also fetch fresh conversations from API
        loadConversationsFromApi()
        // Listen for sync events from ChatViewModel and background worker
        listenForSyncEvents()
    }

    private fun observeConversations() {
        viewModelScope.launch {
            conversationDao.getAllConversations().collect { conversations ->
                _uiState.value = _uiState.value.copy(
                    conversations = conversations,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Listen for conversation sync events from ChatViewModel and background sync worker.
     * Triggers a refresh from API when conversations are created/updated.
     */
    private fun listenForSyncEvents() {
        viewModelScope.launch {
            conversationSyncManager.conversationUpdates.collect { update ->
                when (update) {
                    is ConversationUpdate.Created,
                    is ConversationUpdate.Updated,
                    is ConversationUpdate.Refreshed -> {
                        // Refresh from API to get the latest data
                        loadConversationsFromApi()
                    }
                    is ConversationUpdate.Deleted -> {
                        // Refresh to show deleted conversations are gone
                        loadConversationsFromApi()
                    }
                }
            }
        }
    }

    private fun loadConversationsFromApi() {
        viewModelScope.launch {
            val result = conversationRepository.fetchConversationsFromApi()
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun createNewConversation(onSuccess: (String?) -> Unit) {
        // Instead of creating a conversation locally, navigate to chat with null conversationId
        // The chat screen will handle creating the conversation implicitly via the API
        onSuccess(null)
    }

    fun togglePin(conversationId: String) {
        viewModelScope.launch {
            val conversation = conversationDao.getConversationById(conversationId)
            conversation?.let {
                conversationDao.updatePinnedStatus(conversationId, !it.pinned)
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            // Delete from local database immediately for visual feedback
            conversationDao.deleteConversationById(conversationId)

            // Also try to delete from API
            val result = conversationRepository.deleteConversation(conversationId)
            if (result.isFailure) {
                // Show error to user since the conversation will come back when app restarts
                _uiState.value = _uiState.value.copy(
                    error = "Warning: Could not delete from server. Conversation will reappear on restart."
                )
            }
        }
    }
}

data class ConversationsListUiState(
    val conversations: List<ConversationEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
