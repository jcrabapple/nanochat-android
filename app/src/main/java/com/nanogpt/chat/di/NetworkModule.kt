package com.nanogpt.chat.di

import android.util.Log
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
            level = HttpLoggingInterceptor.Level.BODY
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

                // Add API key as Bearer token for NanoGPT
                // The nanochat backend expects: Authorization: Bearer <api-key>
                secureStorage.getSessionToken()?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                    Log.d("OkHttp", "Adding Authorization header: Bearer ${token.take(20)}...")
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
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        secureStorage: SecureStorage
    ): Retrofit {
        val baseUrl = secureStorage.getBackendUrl() ?: "https://localhost:3000"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideNanoChatApi(retrofit: Retrofit): NanoChatApi {
        return retrofit.create(NanoChatApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWebSearchApi(retrofit: Retrofit): WebSearchApi {
        return retrofit.create(WebSearchApi::class.java)
    }
}
