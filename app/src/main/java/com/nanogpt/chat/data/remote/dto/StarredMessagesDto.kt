package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable
import com.nanogpt.chat.ui.chat.Message

@Serializable
data class StarredMessagesResponse(
    val messages: List<MessageDto>,
    val total: Int
)

@Serializable
data class UpdateMessageRequest(
    val action: String,  // "setStarred"
    val messageId: String,
    val starred: Boolean
)

@Serializable
data class SimpleSuccessResponse(
    val ok: Boolean
)
