package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProjectDto(
    val id: String,
    val name: String,
    val userId: String,
    val color: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateProjectRequest(
    val name: String,
    val color: String? = null
)

@Serializable
data class ProjectUpdates(
    val name: String? = null,
    val color: String? = null
)
