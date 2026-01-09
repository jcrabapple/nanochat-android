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
        color: String? = null
    ): Result<ProjectEntity> {
        return try {
            // Create locally first
            val localProject = ProjectEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                userId = "", // Will be filled by ViewModel
                color = color,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )

            projectDao.insertProject(localProject)

            // Try to sync with server
            try {
                val response = api.createProject(CreateProjectRequest(name, color))
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    val serverProject = dto.toEntity()
                    projectDao.insertProject(serverProject)
                    Result.success(serverProject)
                } else {
                    Result.success(localProject)
                }
            } catch (e: Exception) {
                Result.success(localProject)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProject(
        id: String,
        name: String? = null,
        color: String? = null
    ): Result<Unit> {
        return try {
            val existing = projectDao.getProjectById(id)
            if (existing != null) {
                val updated = existing.copy(
                    name = name ?: existing.name,
                    color = color ?: existing.color,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING
                )
                projectDao.updateProject(updated)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(id: String): Result<Unit> {
        return try {
            projectDao.deleteProjectById(id)

            // Note: This won't delete conversations in the project
            // They will just have a null project reference

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
}

// Extension function to convert DTO to Entity
fun ProjectDto.toEntity(): ProjectEntity {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    return ProjectEntity(
        id = id,
        name = name,
        userId = userId,
        color = color,
        createdAt = sdf.parse(createdAt)?.time ?: System.currentTimeMillis(),
        updatedAt = sdf.parse(updatedAt)?.time ?: System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}
