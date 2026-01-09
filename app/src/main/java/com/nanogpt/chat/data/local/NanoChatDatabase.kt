package com.nanogpt.chat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nanogpt.chat.data.local.dao.AssistantDao
import com.nanogpt.chat.data.local.dao.ConversationDao
import com.nanogpt.chat.data.local.dao.MessageDao
import com.nanogpt.chat.data.local.dao.ProjectDao
import com.nanogpt.chat.data.local.entity.AssistantEntity
import com.nanogpt.chat.data.local.entity.ConversationEntity
import com.nanogpt.chat.data.local.entity.MessageEntity
import com.nanogpt.chat.data.local.entity.ProjectEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        AssistantEntity::class,
        ProjectEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NanoChatDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun assistantDao(): AssistantDao
    abstract fun projectDao(): ProjectDao
}
