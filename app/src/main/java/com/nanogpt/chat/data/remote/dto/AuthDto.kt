package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserSessionDto
)

@Serializable
data class UserSessionDto(
    val id: String,
    val email: String,
    val name: String? = null
)

@Serializable
data class UserSettingsDto(
    val userId: String,
    // Privacy
    val privacyMode: Boolean = false,
    val contextMemoryEnabled: Boolean = true,
    val persistentMemoryEnabled: Boolean = true,
    // Content Processing
    val youtubeTranscriptsEnabled: Boolean = true,
    val webScrapingEnabled: Boolean = true,
    val followUpQuestionsEnabled: Boolean = true,
    // Model Preferences
    val chatTitleModel: String? = null,
    val followUpQuestionsModel: String? = null,
    // TTS
    val ttsModel: String? = null,
    val ttsVoice: String? = null,
    val ttsSpeed: Float = 1.0f,
    // STT
    val sttModel: String? = null,
    // MCP
    val nanoGptMcpEnabled: Boolean = false
)

@Serializable
data class SettingsUpdates(
    val action: String = "update",
    val privacyMode: Boolean? = null,
    val contextMemoryEnabled: Boolean? = null,
    val persistentMemoryEnabled: Boolean? = null,
    val youtubeTranscriptsEnabled: Boolean? = null,
    val webScrapingEnabled: Boolean? = null,
    val followUpQuestionsEnabled: Boolean? = null,
    val chatTitleModel: String? = null,
    val followUpQuestionsModel: String? = null,
    val ttsModel: String? = null,
    val ttsVoice: String? = null,
    val ttsSpeed: Float? = null,
    val sttModel: String? = null,
    val nanoGptMcpEnabled: Boolean? = null
)
