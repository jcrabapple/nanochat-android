package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FileUploadResponse(
    val url: String,
    val storageId: String
)
