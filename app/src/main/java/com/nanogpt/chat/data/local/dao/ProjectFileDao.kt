package com.nanogpt.chat.data.local.dao

import androidx.room.*
import com.nanogpt.chat.data.local.entity.ProjectFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectFileDao {
    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getFilesByProjectId(projectId: String): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files WHERE id = :id")
    suspend fun getFileById(id: String): ProjectFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ProjectFileEntity>)

    @Update
    suspend fun updateFile(file: ProjectFileEntity)

    @Delete
    suspend fun deleteFile(file: ProjectFileEntity)

    @Query("DELETE FROM project_files WHERE id = :id")
    suspend fun deleteFileById(id: String)

    @Query("DELETE FROM project_files WHERE projectId = :projectId")
    suspend fun deleteFilesByProjectId(projectId: String)

    @Query("SELECT COUNT(*) FROM project_files WHERE projectId = :projectId")
    suspend fun getFileCountByProjectId(projectId: String): Int

    @Query("DELETE FROM project_files WHERE id NOT IN (:ids)")
    suspend fun deleteFilesNotInList(ids: List<String>)
}
