package com.nanogpt.chat.di

import android.content.Context
import androidx.room.Room
import com.nanogpt.chat.data.local.MIGRATION_1_2
import com.nanogpt.chat.data.local.NanoChatDatabase
import com.nanogpt.chat.data.local.dao.AssistantDao
import com.nanogpt.chat.data.local.dao.ConversationDao
import com.nanogpt.chat.data.local.dao.MessageDao
import com.nanogpt.chat.data.local.dao.ProjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNanoChatDatabase(
        @ApplicationContext context: Context
    ): NanoChatDatabase {
        return Room.databaseBuilder(
            context,
            NanoChatDatabase::class.java,
            "nanochat.db"
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideConversationDao(database: NanoChatDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: NanoChatDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideAssistantDao(database: NanoChatDatabase): AssistantDao {
        return database.assistantDao()
    }

    @Provides
    @Singleton
    fun provideProjectDao(database: NanoChatDatabase): ProjectDao {
        return database.projectDao()
    }
}
