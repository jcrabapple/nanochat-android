package com.nanogpt.chat.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.entity.ProjectEntity
import com.nanogpt.chat.data.local.entity.ProjectMemberEntity
import com.nanogpt.chat.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectMembersViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectMembersUiState())
    val uiState: StateFlow<ProjectMembersUiState> = _uiState.asStateFlow()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Load project details
            val project = projectRepository.getProjectById(projectId)
            _uiState.value = _uiState.value.copy(project = project)

            // Refresh members from server to ensure we have the latest data
            // This is critical because the local database might not have all members
            // especially after app restart
            try {
                projectRepository.refreshMembers(projectId)
            } catch (e: Exception) {
                android.util.Log.e("ProjectMembersViewModel", "Failed to refresh members from server", e)
                // Continue even if refresh fails - we'll show what's in the local database
            }

            // Observe members (this will now show the refreshed data)
            projectRepository.getMembersByProjectId(projectId).collect { members ->
                _uiState.value = _uiState.value.copy(
                    members = members,
                    isLoading = false
                )
            }
        }
    }

    fun addMember(projectId: String, email: String, role: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = projectRepository.addMember(projectId, email, role)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { e ->
                // Parse error message for better UX
                val errorMessage = when {
                    e.message?.contains("cannot add owner", ignoreCase = true) == true ->
                        "Cannot add the project owner as a member"
                    e.message?.contains("already a member", ignoreCase = true) == true ->
                        "This user is already a member of the project"
                    e.message?.contains("not found", ignoreCase = true) == true ->
                        "User not found"
                    else -> e.message ?: "Failed to add member"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    fun removeMember(projectId: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = projectRepository.removeMember(projectId, userId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { e ->
                // Parse error message for better UX
                val errorMessage = when {
                    e.message?.contains("cannot remove owner", ignoreCase = true) == true ->
                        "Cannot remove the project owner"
                    e.message?.contains("not a member", ignoreCase = true) == true ->
                        "User is not a member of this project"
                    else -> e.message ?: "Failed to remove member"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ProjectMembersUiState(
    val project: ProjectEntity? = null,
    val members: List<ProjectMemberEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
