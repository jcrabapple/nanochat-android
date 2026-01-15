package com.nanogpt.chat.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.local.dao.ConversationDao
import com.nanogpt.chat.data.local.dao.ProjectDao
import com.nanogpt.chat.data.local.entity.ConversationEntity
import com.nanogpt.chat.data.local.entity.ProjectEntity
import com.nanogpt.chat.data.local.entity.SyncStatus
import com.nanogpt.chat.data.repository.ConversationRepository
import com.nanogpt.chat.data.sync.ConversationSyncManager
import com.nanogpt.chat.data.sync.ConversationUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents a failed operation that can be retried
 */
sealed class FailedOperation {
    abstract val operationId: String
    abstract val description: String

    data class DeleteConversation(
        override val operationId: String,
        val conversationId: String,
        val conversationTitle: String,
        override val description: String = "Failed to delete \"$conversationTitle\" from server"
    ) : FailedOperation()

    data class MoveToProject(
        override val operationId: String,
        val conversationId: String,
        val conversationTitle: String,
        val projectId: String?,
        override val description: String = "Failed to update project for \"$conversationTitle\""
    ) : FailedOperation()
}

@HiltViewModel
class ConversationsListViewModel @Inject constructor(
    private val conversationDao: ConversationDao,
    private val projectDao: ProjectDao,
    private val secureStorage: SecureStorage,
    private val conversationRepository: ConversationRepository,
    private val conversationSyncManager: ConversationSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsListUiState())
    val uiState: StateFlow<ConversationsListUiState> = _uiState.asStateFlow()

    init {
        // Always observe local database for conversation updates
        observeConversations()
        observeProjects()
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

    private fun observeProjects() {
        viewModelScope.launch {
            projectDao.getAllProjects().collect { projects ->
                _uiState.value = _uiState.value.copy(projects = projects)
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
            // Get conversation title for error messages
            val conversation = conversationDao.getConversationById(conversationId)

            // Delete from local database immediately for visual feedback
            conversationDao.deleteConversationById(conversationId)

            // Also try to delete from API
            val result = conversationRepository.deleteConversation(conversationId)
            if (result.isFailure) {
                // Add to failed operations for retry
                val failedOp = FailedOperation.DeleteConversation(
                    operationId = "delete_$conversationId",
                    conversationId = conversationId,
                    conversationTitle = conversation?.title ?: "Unknown"
                )
                val currentFailed = _uiState.value.failedOperations.toMutableList()
                currentFailed.add(failedOp)
                _uiState.value = _uiState.value.copy(
                    failedOperations = currentFailed,
                    error = "Some operations failed. See retry options below."
                )
            } else {
                // Remove any existing failed operation for this conversation
                val currentFailed = _uiState.value.failedOperations
                if (currentFailed.any { it.operationId == "delete_$conversationId" }) {
                    _uiState.value = _uiState.value.copy(
                        failedOperations = currentFailed.filter { it.operationId != "delete_$conversationId" }
                    )
                }
            }
        }
    }

    fun moveConversationToProject(conversationId: String, projectId: String?) {
        viewModelScope.launch {
            // Get conversation title for error messages
            val conversation = conversationDao.getConversationById(conversationId)

            // Update local database immediately for visual feedback
            conversationDao.updateProjectId(conversationId, projectId)

            // Also sync to backend API
            val result = conversationRepository.updateConversationProject(conversationId, projectId)
            if (result.isFailure) {
                // Add to failed operations for retry
                val failedOp = FailedOperation.MoveToProject(
                    operationId = "move_${conversationId}_${projectId}",
                    conversationId = conversationId,
                    conversationTitle = conversation?.title ?: "Unknown",
                    projectId = projectId
                )
                val currentFailed = _uiState.value.failedOperations.toMutableList()
                currentFailed.add(failedOp)
                _uiState.value = _uiState.value.copy(
                    failedOperations = currentFailed,
                    error = "Some operations failed. See retry options below."
                )
            } else {
                // Remove any existing failed operation for this conversation
                val currentFailed = _uiState.value.failedOperations
                if (currentFailed.any { it.operationId == "move_${conversationId}_${projectId}" }) {
                    _uiState.value = _uiState.value.copy(
                        failedOperations = currentFailed.filter { it.operationId != "move_${conversationId}_${projectId}" }
                    )
                }
            }
        }
    }

    /**
     * Retry a failed operation
     */
    fun retryOperation(operation: FailedOperation) {
        viewModelScope.launch {
            when (operation) {
                is FailedOperation.DeleteConversation -> {
                    val result = conversationRepository.deleteConversation(operation.conversationId)
                    if (result.isSuccess) {
                        // Remove from failed operations
                        _uiState.value = _uiState.value.copy(
                            failedOperations = _uiState.value.failedOperations.filter { it.operationId != operation.operationId },
                            error = if (_uiState.value.failedOperations.size <= 1) null else _uiState.value.error
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Retry failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        )
                    }
                }
                is FailedOperation.MoveToProject -> {
                    val result = conversationRepository.updateConversationProject(operation.conversationId, operation.projectId)
                    if (result.isSuccess) {
                        // Remove from failed operations
                        _uiState.value = _uiState.value.copy(
                            failedOperations = _uiState.value.failedOperations.filter { it.operationId != operation.operationId },
                            error = if (_uiState.value.failedOperations.size <= 1) null else _uiState.value.error
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Retry failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Dismiss a failed operation (user chooses not to retry)
     */
    fun dismissOperation(operationId: String) {
        _uiState.value = _uiState.value.copy(
            failedOperations = _uiState.value.failedOperations.filter { it.operationId != operationId },
            error = if (_uiState.value.failedOperations.size <= 1) null else _uiState.value.error
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ConversationsListUiState(
    val conversations: List<ConversationEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val failedOperations: List<FailedOperation> = emptyList()
)
