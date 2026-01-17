package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProjectMemberDto(
    val id: String,
    val userId: String,
    val role: String, // "owner" | "editor" | "viewer"
    val user: ProjectMemberUser
)

@Serializable
data class ProjectMemberUser(
    val id: String,
    val name: String,
    val email: String,
    val image: String? = null
)

@Serializable
data class AddProjectMemberRequest(
    val email: String,
    val role: String
)

@Serializable
data class AddProjectMemberResponse(
    val id: String,
    val projectId: String,
    val userId: String,
    val role: String,
    val user: ProjectMemberUser
)

@Serializable
data class RemoveProjectMemberResponse(
    val success: Boolean
)
