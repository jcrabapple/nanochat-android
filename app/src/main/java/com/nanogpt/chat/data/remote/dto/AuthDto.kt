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
    val titleModelId: String? = null,
    val followUpModelId: String? = null,
    // MCP
    val mcpEnabled: Boolean = false
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
    val titleModelId: String? = null,
    val followUpModelId: String? = null,
    val mcpEnabled: Boolean? = null
)
