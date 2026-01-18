package com.nanogpt.chat.di

import android.util.Log
import com.nanogpt.chat.BuildConfig
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.remote.api.NanoChatApi
import com.nanogpt.chat.data.remote.api.WebSearchApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            // Only log in debug builds to prevent sensitive data exposure in release
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        secureStorage: SecureStorage,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")

                // Add API key as Bearer token for API authentication
                // Backend uses Bearer token for API key auth (not cookies)
                // Cookies are only for Better Auth session-based web login
                secureStorage.getSessionToken()?.let { token ->
                    // Clean token - remove any whitespace/newlines
                    val cleanToken = token.trim().replace(Regex("\\s+"), "")

                    // Add as Bearer token for API key authentication
                    requestBuilder.header("Authorization", "Bearer $cleanToken")
                }

                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS) // Longer timeout for streaming
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        secureStorage: SecureStorage
    ): Retrofit {
        // If no URL is configured, use a placeholder to allow the app to launch
        // and navigate to the SetupScreen. The actual API calls will fail until
        // the user configures the URL and restarts the app or the graph is recreated.
        val baseUrl = secureStorage.getBackendUrl() ?: "https://setup.needed/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    fun provideNanoChatApi(retrofit: Retrofit): NanoChatApi {
        return retrofit.create(NanoChatApi::class.java)
    }

    @Provides
    fun provideWebSearchApi(retrofit: Retrofit): WebSearchApi {
        return retrofit.create(WebSearchApi::class.java)
    }
}
