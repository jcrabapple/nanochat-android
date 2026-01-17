package com.nanogpt.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.json.JsonObject

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String, // "user" | "assistant" | "system"
    val content: String,
    val reasoning: String? = null,
    val modelId: String? = null,
    val annotationsJson: String? = null, // JSON string
    val followUpSuggestions: String? = null, // JSON array string
    val createdAt: Long,
    val tokenCount: Int? = null,
    val costUsd: Double? = null,
    val responseTimeMs: Long? = null,
    val starred: Boolean? = null,
    val localId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
