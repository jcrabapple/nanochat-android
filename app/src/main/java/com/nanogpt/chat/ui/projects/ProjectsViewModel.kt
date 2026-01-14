package com.nanogpt.chat.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.local.entity.ProjectEntity
import com.nanogpt.chat.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val secureStorage: SecureStorage,
    private val conversationRepository: com.nanogpt.chat.data.repository.ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        observeProjects()
        syncPendingProjects()
    }

    private fun observeProjects() {
        viewModelScope.launch {
            projectRepository.getProjects().collect { projects ->
                _uiState.value = _uiState.value.copy(
                    projects = projects,
                    isLoading = false
                )
            }
        }
    }

    fun createProject(
        name: String,
        description: String? = null,
        systemPrompt: String? = null,
        color: String?
    ) {
        android.util.Log.d("ProjectsViewModel", "createProject called: name=$name, description=$description, systemPrompt=$systemPrompt, color=$color")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = projectRepository.createProject(
                name = name,
                description = description,
                systemPrompt = systemPrompt,
                color = color
            )

            result.onSuccess {
                android.util.Log.d("ProjectsViewModel", "Project created successfully: ${it.id}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { e ->
                android.util.Log.e("ProjectsViewModel", "Failed to create project", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateProject(
        id: String,
        name: String,
        description: String? = null,
        systemPrompt: String? = null,
        color: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = projectRepository.updateProject(
                id = id,
                name = name.takeIf { it.isNotBlank() },
                description = description,
                systemPrompt = systemPrompt,
                color = color
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            projectRepository.deleteProject(project.id)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    suspend fun getConversationCountForProject(projectId: String): Int {
        return conversationRepository.getConversationCountForProject(projectId)
    }

    fun deleteProjectWithConversations(project: ProjectEntity, deleteConversations: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                if (deleteConversations) {
                    // Delete all conversations in the project
                    val result = conversationRepository.deleteConversationsForProject(project.id)
                    result.onFailure { e ->
                        android.util.Log.e("ProjectsViewModel", "Failed to delete conversations", e)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to delete some conversations: ${e.message}"
                        )
                    }
                }

                // Delete the project
                projectRepository.deleteProject(project.id)
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(error = e.message)
                    }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                projectRepository.refreshProjects()
                syncPendingProjects()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun syncPendingProjects() {
        viewModelScope.launch {
            try {
                val result = projectRepository.syncPendingProjects()
                result.onSuccess { count ->
                    if (count > 0) {
                        android.util.Log.d("ProjectsViewModel", "Synced $count pending projects")
                    }
                }.onFailure { e ->
                    android.util.Log.e("ProjectsViewModel", "Failed to sync pending projects", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("ProjectsViewModel", "Error syncing pending projects", e)
            }
        }
    }
}

data class ProjectsUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
