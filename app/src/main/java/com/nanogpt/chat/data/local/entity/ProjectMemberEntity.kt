package com.nanogpt.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "project_members",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["userId"])
    ]
)
data class ProjectMemberEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val userId: String,
    val role: String, // "owner" | "editor" | "viewer"
    val userName: String,
    val userEmail: String,
    val userImage: String? = null
)
