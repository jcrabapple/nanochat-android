package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GenerateFollowUpQuestionsRequest(
    val conversationId: String,
    val messageId: String
)

@Serializable
data class GenerateFollowUpQuestionsResponse(
    val ok: Boolean,
    val suggestions: List<String> = emptyList()
)
