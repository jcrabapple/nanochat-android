package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProjectFileDto(
    val id: String,
    val projectId: String,
    val storageId: String,
    val fileName: String,
    val fileType: String, // "pdf" | "markdown" | "text" | "epub"
    val extractedContent: String? = null,
    val createdAt: String,
    val storage: StorageInfo? = null
)

@Serializable
data class StorageInfo(
    val id: String,
    val url: String? = null
)

@Serializable
data class CreateProjectFileResponse(
    val id: String,
    val projectId: String,
    val storageId: String,
    val fileName: String,
    val fileType: String,
    val extractedContent: String? = null,
    val createdAt: String
)

@Serializable
data class DeleteProjectFileResponse(
    val success: Boolean
)
