package com.nanogpt.chat.data.local.dao

import androidx.room.*
import com.nanogpt.chat.data.local.entity.ProjectMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectMemberDao {
    @Query("SELECT * FROM project_members WHERE projectId = :projectId")
    fun getMembersByProjectId(projectId: String): Flow<List<ProjectMemberEntity>>

    @Query("SELECT * FROM project_members WHERE id = :id")
    suspend fun getMemberById(id: String): ProjectMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: ProjectMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ProjectMemberEntity>)

    @Delete
    suspend fun deleteMember(member: ProjectMemberEntity)

    @Query("DELETE FROM project_members WHERE id = :id")
    suspend fun deleteMemberById(id: String)

    @Query("DELETE FROM project_members WHERE projectId = :projectId")
    suspend fun deleteMembersByProjectId(projectId: String)

    @Query("DELETE FROM project_members WHERE projectId = :projectId AND userId = :userId")
    suspend fun deleteMemberByProjectAndUser(projectId: String, userId: String)
}
