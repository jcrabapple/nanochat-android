package com.nanogpt.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val userId: String,
    val assistantId: String? = null,
    val projectId: String? = null,
    val modelId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val pinned: Boolean = false,
    val costUsd: Double? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

enum class SyncStatus {
    SYNCED,
    PENDING,
    FAILED
}
