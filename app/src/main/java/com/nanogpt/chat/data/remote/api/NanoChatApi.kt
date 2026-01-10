package com.nanogpt.chat.data.remote.api

import com.nanogpt.chat.data.remote.dto.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface NanoChatApi {

    // ============== Conversations ==============
    @GET("/api/db/conversations")
    suspend fun getConversations(
        @Query("projectId") projectId: String? = null,
        @Query("search") search: String? = null,
        @Query("mode") mode: String? = null
    ): Response<List<ConversationDto>>

    @GET("/api/db/conversations")
    suspend fun getConversation(
        @Query("id") id: String
    ): Response<ConversationDto>

    @DELETE("/api/db/conversations")
    suspend fun deleteConversation(
        @Query("id") id: String? = null,
        @Query("all") all: String? = null
    ): Response<Unit>

    // ============== Messages ==============
    @GET("/api/db/messages")
    suspend fun getMessages(
        @Query("conversationId") conversationId: String
    ): Response<List<MessageDto>>

    @POST("/api/generate-message")
    suspend fun generateMessage(@Body request: GenerateMessageRequest): Response<GenerateMessageResponse>

    @Streaming
    @POST("/api/generate-message")
    fun streamMessage(@Body request: GenerateMessageRequest): Call<ResponseBody>

    // ============== Assistants ==============
    @GET("/api/assistants")
    suspend fun getAssistants(): Response<List<AssistantDto>>

    @POST("/api/assistants")
    suspend fun createAssistant(@Body request: CreateAssistantRequest): Response<AssistantDto>

    @PATCH("/api/assistants/{id}")
    suspend fun updateAssistant(
        @Path("id") id: String,
        @Body updates: AssistantUpdates
    ): Response<AssistantDto>

    @DELETE("/api/assistants/{id}")
    suspend fun deleteAssistant(@Path("id") id: String): Response<Unit>

    // ============== Projects ==============
    @GET("/api/projects")
    suspend fun getProjects(): Response<List<ProjectDto>>

    @POST("/api/projects")
    suspend fun createProject(@Body request: CreateProjectRequest): Response<ProjectDto>

    @GET("/api/projects/{id}")
    suspend fun getProject(@Path("id") id: String): Response<ProjectDto>

    @PATCH("/api/projects/{id}")
    suspend fun updateProject(
        @Path("id") id: String,
        @Body updates: ProjectUpdates
    ): Response<ProjectDto>

    @DELETE("/api/projects/{id}")
    suspend fun deleteProject(@Path("id") id: String): Response<Unit>

    // ============== Settings ==============
    @GET("/api/db/user-settings")
    suspend fun getUserSettings(): Response<UserSettingsDto>

    @POST("/api/db/user-settings")
    suspend fun updateSettings(@Body settings: SettingsUpdates): Response<UserSettingsDto>

    // ============== User Models ==============
    @GET("/api/db/user-models")
    suspend fun getUserModels(
        @Query("provider") provider: String? = null,
        @Query("modelId") modelId: String? = null
    ): Response<okhttp3.ResponseBody>

    @POST("/api/db/user-models")
    suspend fun updateUserModel(@Body request: UpdateUserModelRequest): Response<Unit>

    // ============== Model Providers ==============
    @GET("/api/model-providers")
    suspend fun getModelProviders(
        @Query("modelId") modelId: String
    ): Response<ModelProvidersResponse>

    // ============== Message Interactions ==============
    @POST("/api/db/message-interactions")
    suspend fun logMessageInteraction(@Body request: MessageInteractionRequest): Response<MessageInteractionResponse>

    // ============== Karakeep Integration ==============
    @POST("/api/karakeep/save-chat")
    suspend fun saveChatToKarakeep(@Body request: SaveChatToKarakeepRequest): Response<SaveChatToKarakeepResponse>
}
