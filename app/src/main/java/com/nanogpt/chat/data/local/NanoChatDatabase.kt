package com.nanogpt.chat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nanogpt.chat.data.local.dao.AssistantDao
import com.nanogpt.chat.data.local.dao.ConversationDao
import com.nanogpt.chat.data.local.dao.MessageDao
import com.nanogpt.chat.data.local.dao.ProjectDao
import com.nanogpt.chat.data.local.entity.AssistantEntity
import com.nanogpt.chat.data.local.entity.ConversationEntity
import com.nanogpt.chat.data.local.entity.MessageEntity
import com.nanogpt.chat.data.local.entity.ProjectEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns to assistants table
        database.execSQL(
            """
            ALTER TABLE assistants ADD COLUMN icon TEXT DEFAULT NULL
            """
        )
        database.execSQL(
            """
            ALTER TABLE assistants ADD COLUMN temperature REAL DEFAULT NULL
            """
        )
        database.execSQL(
            """
            ALTER TABLE assistants ADD COLUMN topP REAL DEFAULT NULL
            """
        )
        database.execSQL(
            """
            ALTER TABLE assistants ADD COLUMN maxTokens INTEGER DEFAULT NULL
            """
        )
        database.execSQL(
            """
            ALTER TABLE assistants ADD COLUMN contextSize INTEGER DEFAULT NULL
            """
        )
        database.execSQL(
            """
            ALTER TABLE assistants ADD COLUMN reasoningEffort TEXT DEFAULT NULL
            """
        )
        database.execSQL(
            """
            ALTER TABLE assistants ADD COLUMN webSearchMode TEXT DEFAULT NULL
            """
        )
    }
}

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        AssistantEntity::class,
        ProjectEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class NanoChatDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun assistantDao(): AssistantDao
    abstract fun projectDao(): ProjectDao
}
