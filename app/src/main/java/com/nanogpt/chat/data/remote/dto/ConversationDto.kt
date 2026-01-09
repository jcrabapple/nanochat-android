package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    val id: String,
    val title: String,
    val userId: String,
    val assistantId: String? = null,
    val projectId: String? = null,
    val modelId: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val messageCount: Int = 0,
    val pinned: Boolean = false,
    val costUsd: Double? = null,
    val messages: List<MessageDto>? = null
)

@Serializable
data class CreateConversationRequest(
    val title: String,
    val assistantId: String? = null,
    val projectId: String? = null
)

@Serializable
data class ConversationUpdates(
    val title: String? = null,
    val assistantId: String? = null,
    val projectId: String? = null,
    val pinned: Boolean? = null
)
