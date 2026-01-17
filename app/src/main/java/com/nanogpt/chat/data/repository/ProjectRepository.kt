package com.nanogpt.chat.data.repository

import android.net.Uri
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.local.dao.ProjectDao
import com.nanogpt.chat.data.local.dao.ProjectFileDao
import com.nanogpt.chat.data.local.dao.ProjectMemberDao
import com.nanogpt.chat.data.local.entity.ProjectEntity
import com.nanogpt.chat.data.local.entity.ProjectFileEntity
import com.nanogpt.chat.data.local.entity.ProjectMemberEntity
import com.nanogpt.chat.data.local.entity.SyncStatus
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.CreateProjectRequest
import com.nanogpt.chat.data.remote.dto.ProjectDto
import com.nanogpt.chat.data.remote.dto.ProjectFileDto
import com.nanogpt.chat.data.remote.dto.AddProjectMemberRequest
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val projectFileDao: ProjectFileDao,
    private val projectMemberDao: ProjectMemberDao,
    private val api: NanoChatApi,
    private val secureStorage: SecureStorage
) {

    fun getProjects(): Flow<List<ProjectEntity>> {
        return projectDao.getAllProjects()
    }

    suspend fun getProjectById(id: String): ProjectEntity? {
        return projectDao.getProjectById(id)
    }

    suspend fun createProject(
        name: String,
        description: String? = null,
        systemPrompt: String? = null,
        color: String? = null
    ): Result<ProjectEntity> {
        android.util.Log.d("ProjectRepository", "Creating project: name=$name, description=$description, systemPrompt=$systemPrompt, color=$color")
        return try {
            // Create locally first
            val localProject = ProjectEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                userId = "", // Will be filled by server
                description = description,
                systemPrompt = systemPrompt,
                color = color,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )

            android.util.Log.d("ProjectRepository", "Inserting local project with id: ${localProject.id}")
            projectDao.insertProject(localProject)

            // Try to sync with server
            try {
                android.util.Log.d("ProjectRepository", "Calling API to create project on server")
                val response = api.createProject(
                    CreateProjectRequest(
                        name = name,
                        description = description,
                        systemPrompt = systemPrompt,
                        color = color
                    )
                )
                android.util.Log.d("ProjectRepository", "API response: successful=${response.isSuccessful}, code=${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    android.util.Log.d("ProjectRepository", "Server returned project with id: ${dto.id}")
                    // Delete local temp and insert server version
                    projectDao.deleteProjectById(localProject.id)
                    val serverProject = dto.toEntity()
                    projectDao.insertProject(serverProject)

                    // Refresh members to ensure the creator (owner) is stored locally
                    refreshMembers(dto.id)

                    android.util.Log.d("ProjectRepository", "Project created successfully from server")
                    Result.success(serverProject)
                } else {
                    android.util.Log.e("ProjectRepository", "API call failed: code=${response.code()}, message=${response.message()}")
                    Result.success(localProject)
                }
            } catch (e: Exception) {
                android.util.Log.e("ProjectRepository", "Exception during API call", e)
                Result.success(localProject)
            }
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Exception during project creation", e)
            Result.failure(e)
        }
    }

    suspend fun updateProject(
        id: String,
        name: String? = null,
        description: String? = null,
        systemPrompt: String? = null,
        color: String? = null
    ): Result<Unit> {
        return try {
            val existing = projectDao.getProjectById(id)
            if (existing != null) {
                val updated = existing.copy(
                    name = name ?: existing.name,
                    description = description ?: existing.description,
                    systemPrompt = systemPrompt ?: existing.systemPrompt,
                    color = color ?: existing.color,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING
                )
                projectDao.updateProject(updated)

                // Sync with backend
                try {
                    api.updateProject(
                        id = id,
                        updates = com.nanogpt.chat.data.remote.dto.ProjectUpdates(
                            name = name,
                            description = description,
                            systemPrompt = systemPrompt,
                            color = color
                        )
                    )
                    // Mark as synced if successful
                    projectDao.updateProject(updated.copy(syncStatus = SyncStatus.SYNCED))
                } catch (e: Exception) {
                    // Keep local changes even if sync fails
                    android.util.Log.e("ProjectRepository", "Failed to sync project update", e)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(id: String): Result<Unit> {
        return try {
            projectDao.deleteProjectById(id)

            // Also delete from server
            try {
                api.deleteProject(id)
            } catch (e: Exception) {
                // Ignore network errors
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshProjects(): Result<Unit> {
        return try {
            val response = api.getProjects()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val entities = dtos.map { it.toEntity() }
                entities.forEach { projectDao.insertProject(it) }

                // Refresh members for all projects to ensure owner info is up to date
                entities.forEach { project ->
                    try {
                        refreshMembers(project.id)
                    } catch (e: Exception) {
                        android.util.Log.e("ProjectRepository", "Failed to refresh members for project ${project.id}", e)
                    }
                }

                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to refresh: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync all pending projects to the server.
     * Called on app startup and when user refreshes the project list.
     */
    suspend fun syncPendingProjects(): Result<Int> {
        return try {
            val pendingProjects = projectDao.getPendingProjects()
            var syncedCount = 0

            for (project in pendingProjects) {
                try {
                    val response = api.updateProject(
                        id = project.id,
                        updates = com.nanogpt.chat.data.remote.dto.ProjectUpdates(
                            name = project.name,
                            description = project.description,
                            systemPrompt = project.systemPrompt,
                            color = project.color
                        )
                    )

                    if (response.isSuccessful) {
                        // Update successful, mark as synced
                        projectDao.updateProject(project.copy(syncStatus = SyncStatus.SYNCED))
                        syncedCount++
                    } else if (response.code() == 404) {
                        // Project doesn't exist on server (new project), create it
                        val createResponse = api.createProject(
                            CreateProjectRequest(
                                name = project.name,
                                description = project.description,
                                systemPrompt = project.systemPrompt,
                                color = project.color
                            )
                        )

                        if (createResponse.isSuccessful && createResponse.body() != null) {
                            // Delete the local temporary project and insert with server data
                            projectDao.deleteProjectById(project.id)
                            val serverProject = createResponse.body()!!.toEntity()
                            projectDao.insertProject(serverProject)
                            syncedCount++
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProjectRepository", "Failed to sync project ${project.id}", e)
                    // Continue with next project
                }
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============== Project Files ==============

    fun getFilesByProjectId(projectId: String): Flow<List<ProjectFileEntity>> {
        return projectFileDao.getFilesByProjectId(projectId)
    }

    suspend fun getFileCount(projectId: String): Int {
        return projectFileDao.getFileCountByProjectId(projectId)
    }

    suspend fun uploadFile(
        projectId: String,
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray
    ): Result<ProjectFileEntity> {
        return try {
            android.util.Log.d("ProjectRepository", "Uploading file: $fileName to project $projectId, mimeType: $mimeType")

            // Normalize .txt files to .md for project uploads
            // Backend's project file endpoint seems to validate by file extension, not MIME type
            // It accepts .md files but rejects .txt files even though both are text/plain
            val normalizedFileName = when {
                fileName.endsWith(".txt") -> fileName.removeSuffix(".txt") + ".md"
                else -> fileName
            }
            val normalizedMimeType = when {
                mimeType == "text/plain" -> "text/markdown"
                else -> mimeType
            }

            // Create RequestBody for the file
            val mediaType = normalizedMimeType.toMediaType()
            val requestBody = okhttp3.RequestBody.create(mediaType, fileBytes)

            // Create MultipartBody with the file part
            val multipartBody = okhttp3.MultipartBody.Builder()
                .setType("multipart/form-data".toMediaType()!!)
                .addFormDataPart("file", normalizedFileName, requestBody)
                .build()

            android.util.Log.d("ProjectRepository", "Sending multipart request to /api/projects/$projectId/files")
            android.util.Log.d("ProjectRepository", "Original: $fileName ($mimeType), Normalized: $normalizedFileName ($normalizedMimeType)")

            // Get backend URL for Origin header (to satisfy CSRF protection)
            val backendUrl = secureStorage.getBackendUrl() ?: ""

            // Upload to server - send the entire multipart body with Origin header
            val response = api.uploadProjectFile(
                projectId = projectId,
                body = multipartBody,
                origin = backendUrl
            )

            android.util.Log.d("ProjectRepository", "Upload response: HTTP ${response.code()}, successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val uploadResponse = response.body()!!
                val entity = ProjectFileEntity(
                    id = uploadResponse.id,
                    projectId = uploadResponse.projectId,
                    storageId = uploadResponse.storageId,
                    fileName = uploadResponse.fileName,
                    fileType = uploadResponse.fileType,
                    extractedContent = uploadResponse.extractedContent,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.SYNCED
                )
                projectFileDao.insertFile(entity)
                android.util.Log.d("ProjectRepository", "File uploaded successfully: ${uploadResponse.id}")
                Result.success(entity)
            } else {
                val errorBody = response.errorBody()?.string()
                val error = "Failed to upload file: HTTP ${response.code()} - $errorBody"
                android.util.Log.e("ProjectRepository", error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Exception during file upload", e)
            Result.failure(e)
        }
    }

    suspend fun deleteFile(projectId: String, fileId: String): Result<Unit> {
        return try {
            android.util.Log.d("ProjectRepository", "Deleting file: $fileId from project $projectId")

            // Delete from local database
            projectFileDao.deleteFileById(fileId)

            // Delete from server
            try {
                val response = api.deleteProjectFile(projectId, fileId)
                if (!response.isSuccessful) {
                    android.util.Log.e("ProjectRepository", "Failed to delete file from server: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProjectRepository", "Exception during server file deletion", e)
                // Keep local deletion even if server deletion fails
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Exception during file deletion", e)
            Result.failure(e)
        }
    }

    suspend fun refreshFiles(projectId: String): Result<Unit> {
        return try {
            val response = api.getProjectFiles(projectId)

            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val entities = dtos.map { it.toEntity() }

                // Insert all files from server (REPLACE strategy will update existing)
                entities.forEach { projectFileDao.insertFile(it) }

                // Delete files that are no longer on server
                val serverIds = dtos.map { it.id }
                projectFileDao.deleteFilesNotInList(serverIds)

                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val error = "Failed to refresh files: HTTP ${response.code()} - $errorBody"
                android.util.Log.e("ProjectRepository", error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Exception during file refresh", e)
            Result.failure(e)
        }
    }

    // ============== Project Members ==============

    fun getMembersByProjectId(projectId: String): Flow<List<ProjectMemberEntity>> {
        return projectMemberDao.getMembersByProjectId(projectId)
    }

    suspend fun addMember(
        projectId: String,
        email: String,
        role: String
    ): Result<ProjectMemberEntity> {
        return try {
            android.util.Log.d("ProjectRepository", "Adding member: $email to project $projectId with role $role")

            val response = api.addProjectMember(
                projectId = projectId,
                request = AddProjectMemberRequest(email = email, role = role)
            )

            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val entity = ProjectMemberEntity(
                    id = dto.id,
                    projectId = dto.projectId,
                    userId = dto.userId,
                    role = dto.role,
                    userName = dto.user.name,
                    userEmail = dto.user.email,
                    userImage = dto.user.image
                )
                projectMemberDao.insertMember(entity)
                android.util.Log.d("ProjectRepository", "Member added successfully: ${dto.id}")
                Result.success(entity)
            } else {
                val errorBody = response.errorBody()?.string()
                val error = "Failed to add member: HTTP ${response.code()} - $errorBody"
                android.util.Log.e("ProjectRepository", error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Exception during member addition", e)
            Result.failure(e)
        }
    }

    suspend fun removeMember(projectId: String, userId: String): Result<Unit> {
        return try {
            android.util.Log.d("ProjectRepository", "Removing member: $userId from project $projectId")

            // Delete from local database first
            projectMemberDao.deleteMemberByProjectAndUser(projectId, userId)

            // Delete from server
            try {
                val response = api.removeProjectMember(projectId, userId)
                if (!response.isSuccessful) {
                    android.util.Log.e("ProjectRepository", "Failed to remove member from server: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProjectRepository", "Exception during server member removal", e)
                // Keep local deletion even if server deletion fails
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Exception during member removal", e)
            Result.failure(e)
        }
    }

    suspend fun updateMemberRole(projectId: String, memberId: String, newRole: String): Result<Unit> {
        // Note: The API doesn't seem to have an update endpoint, so we need to remove and re-add
        // This is a limitation of the current API
        return Result.failure(Exception("Update member role not supported by API"))
    }

    suspend fun refreshMembers(projectId: String): Result<Unit> {
        return try {
            android.util.Log.d("ProjectRepository", "Refreshing members for project: $projectId")
            val response = api.getProjectMembers(projectId)

            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val entities = dtos.map { it.toEntity(projectId) }
                entities.forEach { projectMemberDao.insertMember(it) }

                android.util.Log.d("ProjectRepository", "Refreshed ${entities.size} members for project $projectId")
                Result.success(Unit)
            } else {
                val error = "Failed to refresh members: HTTP ${response.code()}"
                android.util.Log.e("ProjectRepository", error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Exception during member refresh", e)
            Result.failure(e)
        }
    }
}

// Extension function to convert String to MediaType
private fun String.toMediaType(): okhttp3.MediaType? {
    val clazz = okhttp3.MediaType::class.java
    val method = clazz.getDeclaredMethod("get", String::class.java)
    return method.invoke(null, this) as? okhttp3.MediaType
}

// Extension function to convert DTO to Entity
fun ProjectDto.toEntity(): ProjectEntity {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    return ProjectEntity(
        id = id,
        name = name,
        userId = userId,
        description = description,
        systemPrompt = systemPrompt,
        color = color,
        createdAt = sdf.parse(createdAt)?.time ?: System.currentTimeMillis(),
        updatedAt = sdf.parse(updatedAt)?.time ?: System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}

// Extension function to convert ProjectFileDto to Entity
fun ProjectFileDto.toEntity(): ProjectFileEntity {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    return ProjectFileEntity(
        id = id,
        projectId = projectId,
        storageId = storageId,
        fileName = fileName,
        fileType = fileType,
        extractedContent = extractedContent,
        createdAt = sdf.parse(createdAt)?.time ?: System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}

// Extension function to convert ProjectMemberDto to Entity
fun com.nanogpt.chat.data.remote.dto.ProjectMemberDto.toEntity(projectId: String): ProjectMemberEntity {
    return ProjectMemberEntity(
        id = id,
        projectId = projectId,
        userId = userId,
        role = role,
        userName = user.name,
        userEmail = user.email,
        userImage = user.image
    )
}
