package com.nanogpt.chat.data.remote.dto

import com.nanogpt.chat.ui.chat.Message
import com.nanogpt.chat.ui.chat.Annotation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val role: String, // "user" | "assistant" | "system"
    val content: String,
    val reasoning: String? = null,
    val modelId: String? = null,
    val annotations: List<AnnotationDto>? = null,
    val followUpSuggestions: List<String>? = null,
    val images: List<ImageDto>? = null,
    val createdAt: String,
    val tokenCount: Int? = null,
    val costUsd: Double? = null,
    val starred: Boolean? = null
)

@Serializable
data class ImageDto(
    val url: String,
    val storage_id: String? = null,
    val fileName: String? = null
)

@Serializable
data class AnnotationDto(
    val type: String, // "web-search" | "image" | "video" | "youtube"
    val data: JsonElement
)

// Extension to convert DTO to Domain model
fun MessageDto.toDomain(): Message {
    return Message(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        reasoning = reasoning,
        modelId = modelId,
        createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .parse(createdAt)
            .time,
        tokenCount = tokenCount,
        annotations = annotations?.map { Annotation(it.type, it.data) },
        images = images?.map { it.url },
        starred = starred
    )
}
