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
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
