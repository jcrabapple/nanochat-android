package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AssistantDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val systemPrompt: String,  // API uses "systemPrompt", not "instructions"
    val isDefault: Boolean = false,  // Added from API docs
    val defaultModelId: String? = null,  // API uses "defaultModelId", not "modelId"
    val defaultWebSearchMode: String? = null,  // API uses "defaultWebSearchMode"
    // Advanced fields (may not be in API docs but are in web app)
    val defaultWebSearchProvider: String? = null,
    val icon: String? = null,  // Emoji or base64 image data URI
    val temperature: Double? = null,  // 0.0 - 2.0
    val topP: Double? = null,  // 0.0 - 1.0
    val maxTokens: Int? = null,  // Maximum tokens in response
    val contextSize: Int? = null,  // Number of historical messages
    val reasoningEffort: String? = null,  // "off" | "auto" | "light" | "medium" | "heavy"
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateAssistantRequest(
    val name: String,
    val description: String? = null,
    val systemPrompt: String,  // API uses "systemPrompt"
    val defaultModelId: String? = null,  // API uses "defaultModelId"
    val defaultWebSearchMode: String? = null,  // "off" | "standard" | "deep"
    val defaultWebSearchProvider: String? = null,  // "linkup" | "tavily" | "exa" | "kagi"
    // Advanced fields (may not be in API docs but are in web app)
    val icon: String? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxTokens: Int? = null,
    val contextSize: Int? = null,
    val reasoningEffort: String? = null
)

@Serializable
data class AssistantUpdates(
    val name: String? = null,
    val description: String? = null,
    val systemPrompt: String? = null,
    val defaultModelId: String? = null,
    val defaultWebSearchMode: String? = null,
    val defaultWebSearchProvider: String? = null,
    // Advanced fields
    val icon: String? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxTokens: Int? = null,
    val contextSize: Int? = null,
    val reasoningEffort: String? = null
)
