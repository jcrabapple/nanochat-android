package com.nanogpt.chat.di

import android.content.Context
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.ui.theme.ThemeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSecureStorage(
        @ApplicationContext context: Context
    ): SecureStorage {
        return SecureStorage(context)
    }

    @Provides
    @Singleton
    fun provideThemeManager(storage: SecureStorage): ThemeManager {
        return ThemeManager(storage)
    }
}
