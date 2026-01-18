package com.nanogpt.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistants")
data class AssistantEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val instructions: String,
    val modelId: String,
    val webSearchEnabled: Boolean,
    val webSearchProvider: String? = null,
    // New fields for advanced assistant configuration
    val icon: String? = null,  // Emoji or base64 image data URI
    val temperature: Double? = null,  // 0.0 - 2.0, controls randomness (null = backend default)
    val topP: Double? = null,  // 0.0 - 1.0, controls diversity (null = backend default)
    val maxTokens: Int? = null,  // Maximum tokens in response (null = backend default)
    val contextSize: Int? = null,  // Number of historical messages to include (null = backend default)
    val reasoningEffort: String? = null,  // "off" | "auto" | "light" | "medium" | "heavy" (thinking budget)
    val webSearchMode: String? = null,  // "off" | "standard" | "deep"
    // Provider-specific web search options
    val webSearchExaDepth: String? = null,  // "fast" | "auto" | "neural" | "deep"
    val webSearchContextSize: String? = null,  // "low" | "medium" | "high"
    val webSearchKagiSource: String? = null,  // "web" | "news" | "search"
    val webSearchValyuSearchType: String? = null,  // "all" | "web"
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
