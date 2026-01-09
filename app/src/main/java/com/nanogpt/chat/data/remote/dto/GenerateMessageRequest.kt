package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GenerateMessageRequest(
    val message: String? = null,  // optional if conversation_id exists
    val model_id: String,
    val assistant_id: String? = null,
    val project_id: String? = null,
    val session_token: String? = null,
    val conversation_id: String? = null,
    val web_search_enabled: Boolean? = null,
    val web_search_mode: String? = null,  // "off" | "standard" | "deep"
    val web_search_provider: String? = null,  // "linkup" | "tavily" | "exa" | "kagi"
    val images: List<MessageImageDto>? = null,
    val documents: List<MessageDocumentDto>? = null,
    val reasoning_effort: String? = null,  // "low" | "medium" | "high"
    val temporary: Boolean? = null,
    val provider_id: String? = null
)

@Serializable
data class MessageImageDto(
    val url: String,
    val storage_id: String,
    val fileName: String? = null
)

@Serializable
data class MessageDocumentDto(
    val url: String,
    val storage_id: String,
    val fileName: String? = null,
    val fileType: String? = null  // "pdf" | "markdown" | "text" | "epub"
)

@Serializable
data class GenerateMessageResponse(
    val ok: Boolean,
    val conversation_id: String
)
