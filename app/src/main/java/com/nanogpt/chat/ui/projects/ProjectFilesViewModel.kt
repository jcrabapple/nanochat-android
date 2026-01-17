package com.nanogpt.chat.ui.projects

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanogpt.chat.data.local.entity.ProjectEntity
import com.nanogpt.chat.data.local.entity.ProjectFileEntity
import com.nanogpt.chat.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectFilesViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectFilesUiState())
    val uiState: StateFlow<ProjectFilesUiState> = _uiState.asStateFlow()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Load project details
            val project = projectRepository.getProjectById(projectId)
            _uiState.value = _uiState.value.copy(project = project)

            // Refresh files from server to ensure we have the latest data
            // This is critical because the local database might not have all files
            // especially after app restart
            try {
                projectRepository.refreshFiles(projectId)
            } catch (e: Exception) {
                android.util.Log.e("ProjectFilesViewModel", "Failed to refresh files from server", e)
                // Continue even if refresh fails - we'll show what's in the local database
            }

            // Observe files (this will now show the refreshed data)
            projectRepository.getFilesByProjectId(projectId).collect { files ->
                _uiState.value = _uiState.value.copy(
                    files = files,
                    isLoading = false
                )
            }
        }
    }

    fun uploadFile(
        projectId: String,
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true)

            val result = projectRepository.uploadFile(
                projectId = projectId,
                fileUri = fileUri,
                fileName = fileName,
                mimeType = mimeType,
                fileBytes = fileBytes
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isUploading = false)
            }.onFailure { e ->
                android.util.Log.e("ProjectFilesViewModel", "Failed to upload file", e)
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = e.message
                )
            }
        }
    }

    fun deleteFile(projectId: String, fileId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = projectRepository.deleteFile(projectId, fileId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { e ->
                android.util.Log.e("ProjectFilesViewModel", "Failed to delete file", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun refreshFiles(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = projectRepository.refreshFiles(projectId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { e ->
                android.util.Log.e("ProjectFilesViewModel", "Failed to refresh files", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ProjectFilesUiState(
    val project: ProjectEntity? = null,
    val files: List<ProjectFileEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isUploading: Boolean = false,
    val error: String? = null
)
