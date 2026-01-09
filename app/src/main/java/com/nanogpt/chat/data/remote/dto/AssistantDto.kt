package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AssistantDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val instructions: String,
    val modelId: String,
    val webSearchEnabled: Boolean,
    val webSearchProvider: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateAssistantRequest(
    val name: String,
    val description: String? = null,
    val instructions: String,
    val modelId: String,
    val webSearchEnabled: Boolean,
    val webSearchProvider: String? = null
)

@Serializable
data class AssistantUpdates(
    val name: String? = null,
    val description: String? = null,
    val instructions: String? = null,
    val modelId: String? = null,
    val webSearchEnabled: Boolean? = null,
    val webSearchProvider: String? = null
)
