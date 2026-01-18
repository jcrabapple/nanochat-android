package com.nanogpt.chat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nanogpt.chat.data.local.dao.AssistantDao
import com.nanogpt.chat.data.local.dao.ConversationDao
import com.nanogpt.chat.data.local.dao.MessageDao
import com.nanogpt.chat.data.local.dao.ProjectDao
import com.nanogpt.chat.data.local.dao.ProjectFileDao
import com.nanogpt.chat.data.local.dao.ProjectMemberDao
import com.nanogpt.chat.data.local.entity.AssistantEntity
import com.nanogpt.chat.data.local.entity.ConversationEntity
import com.nanogpt.chat.data.local.entity.MessageEntity
import com.nanogpt.chat.data.local.entity.ProjectEntity
import com.nanogpt.chat.data.local.entity.ProjectFileEntity
import com.nanogpt.chat.data.local.entity.ProjectMemberEntity

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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add starred column to messages table
        database.execSQL(
            """
            ALTER TABLE messages ADD COLUMN starred INTEGER DEFAULT NULL
            """
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add description and systemPrompt columns to projects table
        database.execSQL(
            """
            ALTER TABLE projects ADD COLUMN description TEXT DEFAULT NULL
            """
        )
        database.execSQL(
            """
            ALTER TABLE projects ADD COLUMN systemPrompt TEXT DEFAULT NULL
            """
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create project_files table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS project_files (
                id TEXT PRIMARY KEY NOT NULL,
                projectId TEXT NOT NULL,
                storageId TEXT NOT NULL,
                fileName TEXT NOT NULL,
                fileType TEXT NOT NULL,
                extractedContent TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
            )
            """
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_project_files_projectId ON project_files(projectId)
            """
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create project_members table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS project_members (
                id TEXT PRIMARY KEY NOT NULL,
                projectId TEXT NOT NULL,
                userId TEXT NOT NULL,
                role TEXT NOT NULL,
                userName TEXT NOT NULL,
                userEmail TEXT NOT NULL,
                userImage TEXT,
                FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
            )
            """
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_project_members_projectId ON project_members(projectId)
            """
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_project_members_userId ON project_members(userId)
            """
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add responseTimeMs column to messages table for performance metrics
        database.execSQL(
            """
            ALTER TABLE messages ADD COLUMN responseTimeMs INTEGER DEFAULT NULL
            """
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add localId column to messages table to preserve UI identity during sync
        database.execSQL(
            """
            ALTER TABLE messages ADD COLUMN localId TEXT DEFAULT NULL
            """
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add provider-specific web search options to assistants table
        database.execSQL(
            """
            ALTER TABLE assistants ADD COLUMN webSearchExaDepth TEXT DEFAULT NULL
            """
        )
        database.execSQL(
            """
            ALTER TABLE assistants ADD COLUMN webSearchContextSize TEXT DEFAULT NULL
            """
        )
        database.execSQL(
            """
            ALTER TABLE assistants ADD COLUMN webSearchKagiSource TEXT DEFAULT NULL
            """
        )
        database.execSQL(
            """
            ALTER TABLE assistants ADD COLUMN webSearchValyuSearchType TEXT DEFAULT NULL
            """
        )
    }
}

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        AssistantEntity::class,
        ProjectEntity::class,
        ProjectFileEntity::class,
        ProjectMemberEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class NanoChatDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun assistantDao(): AssistantDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectFileDao(): ProjectFileDao
    abstract fun projectMemberDao(): ProjectMemberDao
}
