package com.nanogpt.chat.data.repository

import com.nanogpt.chat.data.local.dao.ProjectDao
import com.nanogpt.chat.data.local.entity.ProjectEntity
import com.nanogpt.chat.data.local.entity.SyncStatus
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.dto.CreateProjectRequest
import com.nanogpt.chat.data.remote.dto.ProjectDto
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val api: NanoChatApi
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
