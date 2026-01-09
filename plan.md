---
share_link: https://share.note.sx/veufuhdg
share_updated: 2026-01-08T21:27:50-05:00
---
# Native Android Companion App Development Plan for nanochat

## Executive Summary

Developing a native Android app for nanochat requires careful consideration of the existing web application's architecture, API design, and feature set. This plan outlines a phased approach to build a production-ready Android application that mirrors web functionality while leveraging native platform capabilities.

---

## 1. Architecture Overview

### Recommended Architecture Pattern

```
┌─────────────────────────────────────────────────────────────────┐
│                    Android App Architecture                       │
├─────────────────────────────────────────────────────────────────┤
│  UI Layer                                                        │
│  ├── Jetpack Compose (Declarative UI)                            │
│  ├── ViewModels (State Management)                               │
│  └── Navigation Component (Screen Transitions)                   │
├─────────────────────────────────────────────────────────────────┤
│  Domain Layer                                                    │
│  ├── Use Cases (Business Logic)                                  │
│  ├── Domain Models (Clean Architecture)                          │
│  └── Repository Interfaces                                       │
├─────────────────────────────────────────────────────────────────┤
│  Data Layer                                                      │
│  ├── Repository Implementations                                  │
│  ├── Local Data Sources (Room, DataStore, Preferences)           │
│  ├── Remote Data Sources (Retrofit, WebSocket)                   │
│  └── Data Mappers                                                │
├─────────────────────────────────────────────────────────────────┤
│  Network Layer                                                   │
│  ├── OkHttp (HTTP Client)                                        │
│  ├── Retrofit (REST API)                                         │
│  └── WebSocket (Real-time Streaming)                             │
└─────────────────────────────────────────────────────────────────┘
```

### Communication with Backend

```
┌──────────────┐         HTTPS/REST          ┌──────────────┐
│   Android    │  ◄────────────────────────►  │  nanochat    │
│    App       │         WebSocket            │   Backend    │
└──────────────┘                              └──────────────┘
       │                                             │
       │  • Session cookies (stored locally)          │
       │  • API key authentication                    │
       │  • Bearer token for programmatic access      │
       │  • Streaming responses for chat              │
       └──────────────────────────────────────────────┘
```

---

## 2. Technology Stack Recommendations

### Core Dependencies

| Category | Library | Purpose |
|----------|---------|---------|
| **UI Framework** | Jetpack Compose | Declarative UI, modern toolkit |
| **Architecture** | Hilt + MVVM | Dependency injection, state management |
| **Networking** | Retrofit + OkHttp | REST API calls, interceptors |
| **WebSocket** | OkHttp WebSocket | Real-time chat streaming |
| **Local Storage** | Room Database | Caching, offline support |
| **Preferences** | DataStore | User settings, small data |
| **Image Loading** | Coil | Async image loading, caching |
| **File Handling** | Android SAF + Coroutines | Document/attachment handling |
| **Audio Recording** | MediaRecorder + ExoPlayer | STT input, TTS playback |
| **Navigation** | Navigation Compose | Screen navigation |
| **Serialization** | Kotlinx Serialization | JSON parsing, WebSocket messages |
| **Authentication** | SessionManager + EncryptedSharedPreferences | Secure credential storage |

### Gradle Configuration

```kotlin
// build.gradle.kts (app level)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("kotlinx-serialization")
}

android {
    namespace = "com.nanogpt.chat"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    
    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-android-compiler:2.51")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    
    // WebSocket (built into OkHttp)
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Media
    implementation("androidx.media3:exoplayer:1.3.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## 3. Feature Implementation Plan

### Phase 1: Foundation (Weeks 1-3)

#### 3.1.1 Project Setup & Configuration

**Tasks:**
- Initialize Android project with appropriate Gradle/Kotlin versions
- Configure build variants (debug, release)
- Set up Hilt dependency injection
- Implement ProGuard rules for release builds
- Configure lint checks and unit tests
- Set up CI/CD pipeline (GitHub Actions)

**Deliverables:**
- Fully configured build environment
- Dependency injection setup
- Code quality checks

**Key Files to Create:**
```
app/
├── src/main/
│   ├── java/com/nanogpt/chat/
│   │   ├── NanoChatApplication.kt
│   │   ├── di/                    # Hilt modules
│   │   │   ├── AppModule.kt
│   │   │   ├── NetworkModule.kt
│   │   │   ├── DatabaseModule.kt
│   │   │   └── StorageModule.kt
│   │   └── NanoChatApp.kt
│   ├── res/
│   │   ├── values/
│   │   │   ├── themes.xml
│   │   │   ├── colors.xml
│   │   │   └── strings.xml
│   │   └── drawable/
│   └── AndroidManifest.xml
```

#### 3.1.2 Authentication System

**API Integration:**

```kotlin
// data/remote/api/AuthApi.kt
interface AuthApi {
    @POST("auth/sign-in")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthResponse>
    
    @POST("auth/sign-out")
    suspend fun signOut(): Response<Unit>
    
    @GET("auth/session")
    suspend fun getSession(): Response<User>
    
    @POST("auth/passkey/register")
    suspend fun registerPasskey(): Response<PasskeyChallenge>
    
    @POST("auth/passkey/authenticate")
    suspend fun authenticatePasskey(
        @Body credential: PasskeyCredential
    ): Response<AuthResponse>
}

// data/remote/dto/AuthDto.kt
@kotlinx.serialization.Serializable
data class SignInRequest(
    val email: String,
    val password: String
)

@kotlinx.serialization.Serializable
data class AuthResponse(
    val user: UserDto,
    val session: SessionDto,
    val accessToken: String? = null
)
```

**Session Management:**

```kotlin
// data/local/SessionManager.kt
@Singleton
class SessionManager @Inject constructor(
    private val encryptedPrefs: EncryptedSharedPreferences
) {
    companion object {
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_API_KEY = "api_key"
    }
    
    fun saveSession(session: SessionDto) {
        encryptedPrefs.edit()
            .putString(KEY_SESSION_TOKEN, session.token)
            .putString(KEY_USER_ID, session.userId)
            .apply()
    }
    
    fun getSessionToken(): String? = 
        encryptedPrefs.getString(KEY_SESSION_TOKEN, null)
    
    fun clearSession() {
        encryptedPrefs.edit().clear().apply()
    }
}
```

**Authentication Flow:**
```
User opens app
    ↓
Check for existing session in encrypted storage
    ↓
┌─────────────────────────────────────────┐
│ Session valid?                          │
├─────────────────────────────────────────┤
│ YES → Restore session, fetch user data  │
│ NO  → Show login screen                  │
└─────────────────────────────────────────┘
    ↓
User authenticates (email/password or passkey)
    ↓
Save session to encrypted storage
    ↓
Navigate to home screen
```

#### 3.1.3 Network Layer Setup

**Retrofit Configuration:**

```kotlin
// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionManager: SessionManager,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .apply {
                        sessionManager.getSessionToken()?.let { token ->
                            header("Authorization", "Bearer $token")
                        }
                    }
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://your-nanochat-domain.com/")
            .client(okHttpClient)
            .addConverterFactory(
                Json.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }
}
```

**API Service Setup:**

```kotlin
// data/remote/api/NanoChatApi.kt
interface NanoChatApi {
    // Conversations
    @GET("conversations")
    suspend fun getConversations(
        @Query("limit") limit: Int = 50
    ): Response<List<ConversationDto>>
    
    @GET("conversations/{id}")
    suspend fun getConversation(
        @Path("id") id: String
    ): Response<ConversationDto>
    
    @POST("conversations")
    suspend fun createConversation(
        @Body request: CreateConversationRequest
    ): Response<ConversationDto>
    
    @DELETE("conversations/{id}")
    suspend fun deleteConversation(
        @Path("id") id: String
    ): Response<Unit>
    
    // Messages
    @GET("conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") conversationId: String
    ): Response<List<MessageDto>>
    
    // Generation
    @POST("generate-message")
    suspend fun generateMessage(
        @Body request: GenerateMessageRequest
    ): Response<GenerateMessageResponse>
    
    // Streaming (separate WebSocket handling)
    
    // Storage
    @Multipart
    @POST("storage")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("mimeType") mimeType: String
    ): Response<StorageResponse>
    
    @GET("storage/{id}")
    suspend fun getFile(
        @Path("id") id: String
    ): Response<ResponseBody>
    
    // Settings
    @GET("user/settings")
    suspend fun getUserSettings(): Response<UserSettingsDto>
    
    @PATCH("user/settings")
    suspend fun updateSettings(
        @Body settings: UpdateSettingsRequest
    ): Response<UserSettingsDto>
    
    // Assistants
    @GET("assistants")
    suspend fun getAssistants(): Response<List<AssistantDto>>
}
```

---

### Phase 2: Core Chat Features (Weeks 4-6)

#### 3.2.1 Conversation List & Management

**Data Layer:**

```kotlin
// data/local/dao/ConversationDao.kt
@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeConversation(id: String): Flow<ConversationEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)
    
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)
    
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)
    
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}

// data/local/entity/ConversationEntity.kt
@Entity(tableName = "conversations")
@kotlinx.serialization.Serializable
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val updatedAt: Long,
    val pinned: Boolean = false,
    val generating: Boolean = false,
    val costUsd: Double? = null,
    val assistantId: String? = null,
    val projectId: String? = null,
    val createdAt: Long
)
```

**Repository Implementation:**

```kotlin
// data/repository/ConversationRepositoryImpl.kt
@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val api: NanoChatApi,
    private val dao: ConversationDao
) : ConversationRepository {
    
    override fun getConversations(): Flow<List<Conversation>> {
        return dao.getAllConversations().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun refreshConversations() {
        try {
            val response = api.getConversations()
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    val entities = dtos.map { it.toEntity() }
                    dao.insertConversations(entities)
                }
            }
        } catch (e: Exception) {
            // Handle network error, use cached data
            throw e
        }
    }
    
    override suspend fun createConversation(
        title: String,
        assistantId: String?
    ): Result<Conversation> {
        return try {
            val response = api.createConversation(
                CreateConversationRequest(title, assistantId)
            )
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    dao.insertConversation(dto.toEntity())
                    Result.success(dto.toDomain())
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**UI Implementation:**

```kotlin
// ui/conversations/ConversationsScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    viewModel: ConversationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("nanochat") },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createNewChat() }
            ) {
                Icon(Icons.Add, contentDescription = "New Chat")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.conversations.isEmpty()) {
            EmptyConversationsView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onNewChat = { viewModel.createNewChat() }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(
                    items = uiState.conversations,
                    key = { it.id }
                ) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = { 
                            viewModel.navigateToChat(conversation.id) 
                        },
                        onDelete = { 
                            viewModel.deleteConversation(conversation.id) 
                        }
                    )
                }
            }
        }
    }
}

// ui/conversations/ConversationsViewModel.kt
@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val navigator: Navigator
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()
    
    init {
        observeConversations()
    }
    
    private fun observeConversations() {
        viewModelScope.launch {
            conversationRepository.getConversations()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { conversations ->
                    _uiState.update {
                        it.copy(
                            conversations = conversations,
                            isLoading = false
                        )
                    }
                }
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                conversationRepository.refreshConversations()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun createNewChat() {
        viewModelScope.launch {
            conversationRepository.createConversation("New Chat", null)
                .onSuccess { conversation ->
                    navigator.navigateToChat(conversation.id)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }
    
    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(id)
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }
    
    fun navigateToChat(id: String) {
        navigator.navigateToChat(id)
    }
}
```

#### 3.2.2 Chat Interface with Streaming

**WebSocket Manager:**

```kotlin
// data/remote/WebSocketManager.kt
@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager
) {
    private var webSocket: WebSocket? = null
    private var currentConversationId: String? = null
    
    sealed class WebSocketEvent {
        data class TokenReceived(val token: String) : WebSocketEvent()
        data class ContentDelta(val content: String) : WebSocketEvent()
        data class ReasoningDelta(val reasoning: String) : WebSocketEvent()
        data class GenerationIdReceived(val id: String) : WebSocketEvent()
        data class UsageReceived(val promptTokens: Int, val completionTokens: Int) : WebSocketEvent()
        data object StreamComplete : WebSocketEvent()
        data class ErrorReceived(val error: String) : WebSocketEvent()
    }
    
    fun connect(
        conversationId: String,
        onEvent: (WebSocketEvent) -> Unit
    ): Result<Unit> {
        return try {
            currentConversationId = conversationId
            
            val request = Request.Builder()
                .url("wss://your-domain.com/ws/chat/${conversationId}")
                .addHeader("Authorization", "Bearer ${sessionManager.getSessionToken()}")
                .build()
            
            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    onEvent(WebSocketEvent.StreamComplete) // Connection established
                }
                
                override fun onMessage(webSocket: WebSocket, text: String) {
                    parseMessage(text, onEvent)
                }
                
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                }
                
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onEvent(WebSocketEvent.ErrorReceived(t.message ?: "WebSocket error"))
                }
            })
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun sendMessage(message: String) {
        webSocket?.send(message)
    }
    
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        currentConversationId = null
    }
    
    private fun parseMessage(text: String, onEvent: (WebSocketEvent) -> Unit) {
        // Parse SSE-like format from the web app
        // Example: "data: {...}\n\n"
        val lines = text.lines()
        for (line in lines) {
            if (line.startsWith("data:")) {
                val json = line.removePrefix("data:").trim()
                try {
                    val event = parseJsonEvent(json)
                    onEvent(event)
                } catch (e: Exception) {
                    // Log parsing error
                }
            }
        }
    }
    
    private fun parseJsonEvent(json: String): WebSocketEvent {
        // Implement JSON parsing for different event types
        // This mirrors the SSE handling in the web app
        return when {
            json.contains("\"token\"") -> {
                val token = parseJsonField(json, "token")
                WebSocketEvent.TokenReceived(token)
            }
            json.contains("\"content\"") -> {
                val content = parseJsonField(json, "content")
                WebSocketEvent.ContentDelta(content)
            }
            // ... other event types
            else -> WebSocketEvent.ErrorReceived("Unknown event type")
        }
    }
    
    private fun parseJsonField(json: String, field: String): String {
        val regex = "\"$field\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: ""
    }
}
```

**Chat ViewModel with Streaming:**

```kotlin
// ui/chat/ChatViewModel.kt
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val conversationId: String,
    private val messageRepository: MessageRepository,
    private val webSocketManager: WebSocketManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()
    
    private var isGenerating = false
    
    init {
        loadConversation()
        observeMessages()
    }
    
    private fun loadConversation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val conversation = messageRepository.getConversation(conversationId)
                _uiState.update { it.copy(conversation = conversation) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    private fun observeMessages() {
        viewModelScope.launch {
            messageRepository.observeMessages(conversationId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { msgs ->
                    _messages.value = msgs
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }
    
    fun updateInput(text: String) {
        _inputText.value = text
    }
    
    fun sendMessage() {
        if (isGenerating || _inputText.value.isBlank()) return
        
        val message = _inputText.value
        _inputText.value = ""
        
        viewModelScope.launch {
            isGenerating = true
            _uiState.update { it.copy(isGenerating = true) }
            
            // Add user message immediately
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "user",
                content = message,
                createdAt = System.currentTimeMillis()
            )
            _messages.value = _messages.value + userMessage
            
            // Connect to WebSocket for streaming
            webSocketManager.connect(conversationId) { event ->
                handleWebSocketEvent(event)
            }
            
            // Send message to WebSocket
            webSocketManager.sendMessage(
                Json.encodeToString(GenerateMessageSerializer, request)
            )
        }
    }
    
    private fun handleWebSocketEvent(event: WebSocketManager.WebSocketEvent) {
        when (event) {
            is WebSocketManager.WebSocketEvent.TokenReceived -> {
                // Update last assistant message with new token
                updateLastAssistantMessage { it.copy(
                    content = it.content + event.token
                )}
            }
            is WebSocketManager.WebSocketEvent.ReasoningDelta -> {
                updateLastAssistantMessage { it.copy(
                    reasoning = it.reasoning + event.reasoning
                )}
            }
            is WebSocketManager.WebSocketEvent.StreamComplete -> {
                isGenerating = false
                _uiState.update { it.copy(isGenerating = false) }
                webSocketManager.disconnect()
            }
            is WebSocketManager.WebSocketEvent.ErrorReceived -> {
                isGenerating = false
                _uiState.update { it.copy(
                    isGenerating = false,
                    error = event.error
                )}
                webSocketManager.disconnect()
            }
            else -> { /* Handle other events */ }
        }
    }
    
    fun stopGeneration() {
        viewModelScope.launch {
            webSocketManager.disconnect()
            isGenerating = false
            _uiState.update { it.copy(isGenerating = false) }
        }
    }
    
    fun regenerateMessage() {
        // Implement regeneration logic
    }
    
    fun copyMessage(message: Message) {
        // Copy to clipboard
    }
    
    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
```

**Chat Screen UI:**

```kotlin
// ui/chat/ChatScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, uiState.isGenerating) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.conversation?.title ?: "Chat",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.isGenerating) {
                            Text(
                                text = "Generating...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Model selector
                    ModelSelectorButton(
                        selectedModel = uiState.selectedModel,
                        onModelSelected = { /* Update model */ }
                    )
                    // Settings menu
                    IconButton(onClick = { /* Open settings */ }) {
                        Icon(Icons.MoreVert, contentDescription = "More")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = scrollState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    MessageBubble(
                        message = message,
                        onCopy = { viewModel.copyMessage(message) },
                        onRegenerate = { viewModel.regenerateMessage() },
                        onEdit = { /* Edit message */ }
                    )
                }
            }
            
            // Error banner
            uiState.error?.let { error ->
                ErrorBanner(
                    message = error,
                    onDismiss = { /* Clear error */ }
                )
            }
            
            // Input area
            ChatInputBar(
                text = inputText,
                onTextChange = viewModel::updateInput,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopGeneration,
                isGenerating = uiState.isGenerating,
                selectedModel = uiState.selectedModel,
                onModelClick = { /* Show model picker */ },
                onAttachmentsClick = { /* Show attachment options */ },
                onVoiceClick = { /* Show voice input */ }
            )
        }
    }
}

// ui/components/MessageBubble.kt
@Composable
fun MessageBubble(
    message: Message,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            // Show reasoning if available
            message.reasoning?.let { reasoning ->
                if (reasoning.isNotBlank()) {
                    Text(
                        text = reasoning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            
            // Message content (Markdown rendered)
            MarkdownText(
                content = message.content,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Metadata (model, tokens, cost)
            if (message.role == "assistant") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    message.modelId?.let { model ->
                        Text(
                            text = model,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    message.tokenCount?.let { tokens ->
                        Text(
                            text = "${tokens}t",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        
        // Action buttons for assistant messages
        if (!isUser) {
            Column(
                modifier = Modifier.padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onRegenerate, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Refresh,
                        contentDescription = "Regenerate",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
```

#### 3.2.3 Message Input with Attachments

```kotlin
// ui/chat/ChatInputBar.kt
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean,
    selectedModel: Model?,
    onModelClick: () -> Unit,
    onAttachmentsClick: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAttachmentOptions by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Attachment preview row
        // ... (show selected images/documents)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attachment button
            IconButton(onClick = onAttachmentsClick) {
                Icon(
                    Icons.AttachFile,
                    contentDescription = "Attach files"
                )
            }
            
            // Voice input button
            IconButton(onClick = onVoiceClick) {
                Icon(
                    Icons.Mic,
                    contentDescription = "Voice input"
                )
            }
            
            // Text input
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Send or Stop button
            if (isGenerating) {
                FilledIconButton(
                    onClick = onStop,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Stop, contentDescription = "Stop")
                }
            } else {
                FilledIconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank()
                ) {
                    Icon(Icons.Send, contentDescription = "Send")
                }
            }
        }
    }
}
```

---

### Phase 3: Advanced Features (Weeks 7-10)

#### 3.3.1 File Upload & Media Handling

**Image Upload with Compression:**

```kotlin
// data/repository/StorageRepositoryImpl.kt
@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val api: NanoChatApi,
    private val context: Context
) : StorageRepository {
    
    override suspend fun uploadImage(
        imageUri: Uri,
        maxSizeBytes: Long = 1024 * 1024 // 1MB
    ): Result<StorageItem> {
        return try {
            // Get original file
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return Result.failure(Exception("Cannot open file"))
            
            // Compress image
            val compressedFile = compressImage(inputStream, maxSizeBytes)
            inputStream.close()
            
            // Create multipart request
            val fileBody = compressedFile.asRequestBody("image/*".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData(
                "file",
                compressedFile.name,
                fileBody
            )
            
            // Upload
            val response = api.uploadFile(
                multipartBody,
                "image/${compressedFile.extension}"
            )
            
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    Result.success(dto.toDomain())
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun compressImage(
        inputStream: InputStream,
        maxSizeBytes: Long
    ): File {
        val bitmap = BitmapFactory.decodeStream(inputStream)
        
        // Calculate scale factor
        val width = bitmap.width
        val height = bitmap.height
        val maxDimension = 1024
        
        val scale = if (width > height) {
            if (width > maxDimension) maxDimension.toFloat() / width
            else 1f
        } else {
            if (height > maxDimension) maxDimension.toFloat() / height
            else 1f
        }
        
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap, scaledWidth, scaledHeight, true
        )
        
        // Compress to JPEG with quality
        val outputFile = File.createTempFile(
            "image_",
            ".jpg",
            context.cacheDir
        )
        
        var quality = 90
        var outputStream = FileOutputStream(outputFile)
        
        do {
            outputStream.close()
            outputStream = FileOutputStream(outputFile)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 10
        } while (outputFile.length() > maxSizeBytes && quality > 10)
        
        outputStream.close()
        bitmap.recycle()
        scaledBitmap.recycle()
        
        return outputFile
    }
}
```

**Document Upload:**

```kotlin
override suspend fun uploadDocument(
    documentUri: Uri
): Result<StorageItem> {
    return try {
        val mimeType = context.contentResolver.getType(documentUri)
            ?: return Result.failure(Exception("Unknown file type"))
        
        val fileName = getFileName(documentUri) ?: "document"
        val fileType = when {
            mimeType == "application/pdf" -> "pdf"
            mimeType.startsWith("text/") -> "text"
            else -> return Result.failure(Exception("Unsupported file type: $mimeType"))
        }
        
        // Get file size
        val size = getFileSize(documentUri)
        if (size > 20 * 1024 * 1024) {
            return Result.failure(Exception("File too large (max 20MB)"))
        }
        
        // Copy to temp file for upload
        val inputStream = context.contentResolver.openInputStream(documentUri)
            ?: return Result.failure(Exception("Cannot open file"))
        
        val tempFile = File.createTempFile("doc_", ".tmp", context.cacheDir)
        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        // Create multipart
        val fileBody = tempFile.asRequestBody(mimeType.toMediaType())
        val multipartBody = MultipartBody.Part.createFormData(
            "file",
            fileName,
            fileBody
        )
        
        val response = api.uploadFile(multipartBody, mimeType)
        
        // Clean up temp file
        tempFile.delete()
        
        if (response.isSuccessful) {
            response.body()?.let { dto ->
                Result.success(dto.toDomain())
            } ?: Result.failure(Exception("Empty response"))
        } else {
            Result.failure(Exception("Upload failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

#### 3.3.2 Text-to-Speech (TTS)

```kotlin
// data/repository/TtsRepositoryImpl.kt
@Singleton
class TtsRepositoryImpl @Inject constructor(
    private val api: TtsApi,
    private val context: Context
) : TtsRepository {
    
    private var mediaPlayer: MediaPlayer? = null
    
    override suspend fun generateSpeech(
        text: String,
        model: TtsModel,
        speed: Float
    ): Result<Uri> {
        return try {
            val response = api.generateSpeech(
                TtsRequest(
                    text = text,
                    model = model.id,
                    speed = speed
                )
            )
            
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    // Download audio file
                    val audioUri = downloadAudio(dto.audioUrl)
                    Result.success(audioUri)
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("TTS failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun playAudio(uri: Uri) {
        stopPlayback()
        
        mediaPlayer = MediaPlayer().apply {
            setDataSource(uri.toString())
            prepareAsync()
            setOnPreparedListener { start() }
            setOnCompletionListener {
                stopPlayback()
            }
            setOnErrorListener { _, _, _ ->
                stopPlayback()
                true
            }
        }
    }
    
    override fun pausePlayback() {
        mediaPlayer?.pause()
    }
    
    override fun resumePlayback() {
        mediaPlayer?.start()
    }
    
    override fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }
    
    override fun getPlaybackPosition(): Int = mediaPlayer?.currentPosition ?: 0
    
    override fun getPlaybackDuration(): Int = mediaPlayer?.duration ?: 0
    
    private suspend fun downloadAudio(url: String): Uri {
        val response = api.downloadAudio(url)
        val bytes = response.bytes()
        
        val file = File(context.cacheDir, "tts_${System.currentTimeMillis()}.mp3")
        file.writeBytes(bytes)
        
        return Uri.fromFile(file)
    }
}
```

**TTS UI Component:**

```kotlin
// ui/components/TtsPlayer.kt
@Composable
fun TtsPlayer(
    text: String,
    modifier: Modifier = Modifier
) {
    val viewModel: TtsPlayerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(text) {
        viewModel.loadText(text)
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Play/Pause/Stop button
        IconButton(
            onClick = {
                when (uiState.playbackState) {
                    PlaybackState.IDLE -> viewModel.play()
                    PlaybackState.PLAYING -> viewModel.pause()
                    PlaybackState.PAUSED -> viewModel.resume()
                    PlaybackState.PLAYING -> viewModel.stop()
                }
            },
            enabled = uiState.isReady
        ) {
            Icon(
                when (uiState.playbackState) {
                    PlaybackState.IDLE -> Icons.PlayArrow
                    PlaybackState.PLAYING -> Icons.Pause
                    PlaybackState.PAUSED -> Icons.PlayArrow
                },
                contentDescription = "Play/Pause"
            )
        }
        
        // Progress slider (if playing)
        if (uiState.playbackState != PlaybackState.IDLE) {
            Slider(
                value = uiState.progress.toFloat(),
                onValueChange = { viewModel.seekTo(it.toInt()) },
                modifier = Modifier.weight(1f)
            )
            
            // Time display
            Text(
                text = "${uiState.currentPosition / 1000}s / ${uiState.duration / 1000}s",
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Speed selector
        var speedExpanded by remember { mutableStateOf(false) }
        
        Box {
            TextButton(onClick = { speedExpanded = true }) {
                Text("${uiState.speed}x")
            }
            
            DropdownMenu(
                expanded = speedExpanded,
                onDismissRequest = { speedExpanded = false }
            ) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    DropdownMenuItem(
                        text = { Text("${speed}x") },
                        onClick = {
                            viewModel.setSpeed(speed)
                            speedExpanded = false
                        }
                    )
                }
            }
        }
    }
}
```

#### 3.3.3 Speech-to-Text (STT)

```kotlin
// data/repository/SttRepositoryImpl.kt
@Singleton
class SttRepositoryImpl @Inject constructor(
    private val api: SttApi,
    private val context: Context
) : SttRepository {
    
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    
    override fun startRecording(): Result<Unit> {
        return try {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate, channelConfig, audioFormat
            ) * 2
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return Result.failure(Exception("Failed to initialize audio recorder"))
            }
            
            audioRecord?.startRecording()
            
            recordingThread = Thread {
                recordAudio()
            }
            recordingThread?.start()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun stopRecording(): Result<SttResult> {
        return try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            recordingThread?.join()
            recordingThread = null
            
            // Upload recorded audio
            val audioFile = File(context.cacheDir, "recording.wav")
            val result = transcribeAudio(audioFile)
            
            // Clean up
            audioFile.delete()
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun recordAudio() {
        val buffer = ShortArray(4096)
        val outputFile = File(context.cacheDir, "recording.wav")
        
        try {
            val outputStream = FileOutputStream(outputFile)
            val dataOutputStream = DataOutputStream(outputStream)
            
            // Write WAV header (will be updated at the end)
            val header = ByteArray(44)
            dataOutputStream.write(header)
            
            // Write audio data
            val sampleRate = 16000
            while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readCount > 0) {
                    for (i in 0 until readCount) {
                        dataOutputStream.writeShort(buffer[i].toInt())
                    }
                }
            }
            
            // Update WAV header with file size
            val fileSize = outputFile.length()
            updateWavHeader(outputFile, fileSize, sampleRate)
            
            dataOutputStream.close()
        } catch (e: Exception) {
            outputFile.delete()
        }
    }
    
    private suspend fun transcribeAudio(audioFile: File): Result<SttResult> {
        return try {
            val fileBody = audioFile.asRequestBody("audio/wav".toMediaType())
            val multipart = MultipartBody.Part.createFormData(
                "file",
                "audio.wav",
                fileBody
            )
            
            val response = api.transcribe(
                multipart,
                SttModel.WHISPER_LARGE_V3.id
            )
            
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    Result.success(
                        SttResult(
                            text = dto.text,
                            model = dto.model,
                            duration = dto.duration
                        )
                    )
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Transcription failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

#### 3.3.4 Settings & User Preferences

```kotlin
// ui/settings/SettingsViewModel.kt
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val settings = settingsRepository.getSettings()
                _uiState.update { 
                    it.copy(
                        settings = settings,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun updateWebSearchEnabled(enabled: Boolean) {
        updateSettings { it.copy(webSearchEnabled = enabled) }
    }
    
    fun updateFollowUpQuestionsEnabled(enabled: Boolean) {
        updateSettings { it.copy(followUpQuestionsEnabled = enabled) }
    }
    
    fun updatePersistentMemoryEnabled(enabled: Boolean) {
        updateSettings { it.copy(persistentMemoryEnabled = enabled) }
    }
    
    fun updateContextMemoryEnabled(enabled: Boolean) {
        updateSettings { it.copy(contextMemoryEnabled = enabled) }
    }
    
    fun updateTtsModel(model: TtsModel) {
        updateSettings { it.copy(ttsModel = model) }
    }
    
    fun updateTtsSpeed(speed: Float) {
        updateSettings { it.copy(ttsSpeed = speed) }
    }
    
    fun updateSttModel(model: SttModel) {
        updateSettings { it.copy(sttModel = model) }
    }
    
    fun updateTheme(theme: AppTheme) {
        updateSettings { it.copy(theme = theme) }
    }
    
    private fun updateSettings(
        transform: (UserSettings) -> UserSettings
    ) {
        viewModelScope.launch {
            val current = _uiState.value.settings ?: return@launch
            val updated = transform(current)
            
            try {
                settingsRepository.updateSettings(updated)
                _uiState.update { it.copy(settings = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            // Navigate to login screen
        }
    }
}
```

---

### Phase 4: Polish & Production (Weeks 11-12)

#### 3.4.1 Push Notifications

```kotlin
// service/NanoChatFirebaseMessagingService.kt
@AndroidEntryPoint
class NanoChatMessagingService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server
        viewModelScope.launch {
            notificationRepository.updatePushToken(token)
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val data = remoteMessage.data
        when (data["type"]) {
            "generation_complete" -> {
                showNotification(
                    title = "Response Ready",
                    body = "Your AI response is ready",
                    conversationId = data["conversationId"]
                )
            }
            "new_message" -> {
                showNotification(
                    title = "New Message",
                    body = data["preview"] ?: "You have a new message",
                    conversationId = data["conversationId"]
                )
            }
        }
    }
    
    private fun showNotification(
        title: String,
        body: String,
        conversationId: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            conversationId?.let { putExtra("conversationId", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }
}
```

#### 3.4.2 Offline Support & Caching

```kotlin
// data/local/NanoChatDatabase.kt
@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        UserSettingsEntity::class,
        AssistantEntity::class,
        LocalMessageCache::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NanoChatDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun settingsDao(): SettingsDao
    abstract fun assistantDao(): AssistantDao
}

// data/repository/OfflineSupport.kt
@Singleton
class OfflineSupportManager @Inject constructor(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val connectivityManager: ConnectivityManager
) {
    
    val isOnline: StateFlow<Boolean> = MutableStateFlow(true)
    
    init {
        // Monitor connectivity
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                (isOnline as MutableStateFlow).value = true
                syncOfflineChanges()
            }
            
            override fun onLost(network: Network) {
                (isOnline as MutableStateFlow).value = false
            }
        }
        
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }
    
    private fun syncOfflineChanges() {
        viewModelScope.launch {
            // Upload any pending messages
            val pendingMessages = messageDao.getPendingMessages()
            pendingMessages.forEach { message ->
                try {
                    // Upload to server
                    messageDao.markAsSynced(message.id)
                } catch (e: Exception) {
                    // Keep as pending
                }
            }
        }
    }
    
    fun queueMessageForSync(message: MessageEntity) {
        viewModelScope.launch {
            messageDao.insert(message.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }
}
```

#### 3.4.3 Deep Linking

```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle deep links
        intent?.data?.let { uri ->
            handleDeepLink(uri)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { handleDeepLink(it) }
    }
    
    private fun handleDeepLink(uri: Uri) {
        when (uri.host) {
            "chat.nanogpt.com" -> {
                val conversationId = uri.getQueryParameter("id")
                conversationId?.let {
                    // Navigate to chat screen
                    navController.navigate("chat/$it")
                }
            }
            "share.nanogpt.com" -> {
                val sharedConversationId = uri.getQueryParameter("conversation")
                sharedConversationId?.let {
                    // Show shared conversation preview
                    showSharedConversation(it)
                }
            }
        }
    }
}
```

**AndroidManifest.xml deep link configuration:**
```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="https"
            android:host="chat.nanogpt.com" />
        <data
            android:scheme="https"
            android:host="share.nanogpt.com" />
    </intent-filter>
</activity>
```

---

## 4. API Compatibility Layer

### API Adapter for Web App Endpoints

Since the web app uses specific endpoint patterns, create adapters:

```kotlin
// data/remote/adapter/WebApiAdapter.kt
class WebApiAdapter(
    private val retrofit: Retrofit
) {
    // The web app uses specific URL patterns
    // Map them to proper REST endpoints
    
    fun getConversations(): Call<List<ConversationDto>> {
        return retrofit.create(DynamicApi::class.java)
            .getConversations()
    }
    
    fun getMessages(conversationId: String): Call<List<MessageDto>> {
        return retrofit.create(DynamicApi::class.java)
            .getMessages(conversationId)
    }
    
    // The generate-message endpoint is complex
    // It handles both new and existing conversations
    fun generateMessage(request: GenerateMessageRequest): Call<GenerateMessageResponse> {
        return retrofit.create(DynamicApi::class.java)
            .generateMessage(request)
    }
}

// Handle the SSE-like streaming from the web app
// The web app returns Server-Sent Events format
interface StreamingApi {
    @Streaming
    @GET("generate-message")
    fun streamMessage(@QueryMap params: Map<String, String>): Call<ResponseBody>
}
```

---

## 5. Testing Strategy

### Unit Tests

```kotlin
// MessageRepositoryTest.kt
class MessageRepositoryTest {
    private lateinit var repository: MessageRepository
    private lateinit var mockApi: NanoChatApi
    private lateinit var mockDao: MessageDao
    
    @BeforeEach
    fun setup() {
        repository = MessageRepositoryImpl(mockApi, mockDao)
    }
    
    @Test
    fun `getMessages returns cached data when offline`() = runTest {
        // Given
        whenever(mockDao.getMessages(any())).thenReturn(flowOf(cachedMessages))
        whenever(mockApi.getMessages(any())).thenThrow(IOException())
        
        // When
        val result = repository.getMessages("conv123")
        
        // Then
        result.first() shouldBeEqualTo cachedMessages
    }
    
    @Test
    fun `sendMessage creates user message and starts generation`() = runTest {
        // Given
        val message = "Hello, AI!"
        val conversationId = "conv123"
        
        whenever(mockApi.createMessage(any())).thenReturn(Response.success(MessageDto(...)))
        whenever(mockDao.insertMessage(any())).thenReturn(Unit)
        
        // When
        val result = repository.sendMessage(conversationId, message)
        
        // Then
        result.isSuccess shouldBe true
        verify(mockDao).insertMessage(any())
    }
}

// ViewModel Tests
class ChatViewModelTest {
    @Test
    fun `sendMessage updates input and triggers generation`() = runTest {
        // Given
        val viewModel = ChatViewModel(...)
        val testMessage = "Test message"
        
        // When
        viewModel.updateInput(testMessage)
        viewModel.sendMessage()
        
        // Then
        viewModel.inputText.first() shouldBe ""
        viewModel.uiState.first().isGenerating shouldBe true
    }
}
```

### Integration Tests

```kotlin
// ChatFlowTest.kt
@RunWith(AndroidJUnit4::class)
class ChatFlowTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var repository: MessageRepository
    
    @Test
    fun endToEndChatFlow() {
        // 1. Create conversation
        val conversation = repository.createConversation("Test Chat", null)
        
        // 2. Send message
        val message = repository.sendMessage(
            conversation.id,
            "What is 2+2?"
        )
        
        // 3. Wait for generation to complete
        waitForGenerationComplete(conversation.id)
        
        // 4. Verify response
        val messages = repository.getMessages(conversation.id)
        val lastMessage = messages.last()
        lastMessage.role shouldBe "assistant"
        lastMessage.content shouldContain "4"
    }
}
```

---

## 6. Security Considerations

### Certificate Pinning

```kotlin
// di/NetworkModule.kt
@Provides
@Singleton
fun providePinnedOkHttpClient(): OkHttpClient {
    val certificatePinner = CertificatePinner.Builder()
        .add("your-domain.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        .build()
    
    return OkHttpClient.Builder()
        .certificatePinner(certificatePinner)
        // ... other configs
        .build()
}
```

### Biometric Authentication

```kotlin
// ui/settings/SecuritySettings.kt
@Composable
fun BiometricAuthSetting(
    onBiometricEnabledChange: (Boolean) -> Unit
) {
    val biometricManager = BiometricManager.from(LocalContext.current)
    
    val canAuthenticate = biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    )
    
    var enabled by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_enabled", false)) }
    
    Switch(
        checked = enabled && canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS,
        onCheckedChange = { newValue ->
            if (newValue && canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                // Show error - biometric not available
                return@Switch
            }
            enabled = newValue
            sharedPrefs.edit().putBoolean("biometric_enabled", newValue).apply()
            onBiometricEnabledChange(newValue)
        },
        enabled = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    )
    
    if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
        Text(
            text = "Please enroll biometric credentials in device settings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
```

### Secure Storage for API Keys

```kotlin
// data/local/SecureKeyStorage.kt
@Singleton
class SecureKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveApiKey(key: String) {
        encryptedPrefs.edit()
            .putString("api_key", key)
            .apply()
    }
    
    fun getApiKey(): String? {
        return encryptedPrefs.getString("api_key", null)
    }
    
    fun clearApiKey() {
        encryptedPrefs.edit().clear().apply()
    }
}
```

---

## 7. Development Timeline

### Phase 1: Foundation (Weeks 1-3)
| Week | Tasks | Deliverables |
|------|-------|--------------|
| 1 | Project setup, DI, network layer | Build environment configured |
| 2 | Authentication (session, login) | Login flow working |
| 3 | Basic navigation, theme | App skeleton with navigation |

### Phase 2: Core Features (Weeks 4-6)
| Week | Tasks | Deliverables |
|------|-------|--------------|
| 4 | Conversation list, CRUD | Conversations screen |
| 5 | Chat interface, WebSocket | Real-time chat working |
| 6 | Message input, attachments | File upload/attachment support |

### Phase 3: Advanced Features (Weeks 7-10)
| Week | Tasks | Deliverables |
|------|-------|--------------|
| 7 | TTS integration | Voice playback |
| 8 | STT integration | Voice input |
| 9 | Settings screen | User preferences |
| 10 | Deep linking, sharing | Share functionality |

### Phase 4: Polish (Weeks 11-12)
| Week | Tasks | Deliverables |
|------|-------|--------------|
| 11 | Push notifications, offline | Notifications working |
| 12 | Testing, optimization, release prep | Production-ready app |

---

## 8. Potential Challenges & Solutions

| Challenge | Solution |
|-----------|----------|
| **WebSocket streaming format** | Parse SSE-like format from web app, adapt to real-time UI updates |
| **API compatibility** | Create adapter layer to map web endpoints to REST patterns |
| **Large file uploads** | Compress images client-side, use background upload with progress |
| **Offline message queue** | Store pending messages locally, sync when online |
| **Battery consumption** | Use WorkManager for background sync, optimize WebSocket heartbeat |
| **Different screen sizes** | Responsive layouts with Compose, test on various devices |
| **API rate limiting** | Implement exponential backoff for retries |
| **Secure credential storage** | Use EncryptedSharedPreferences + Android Keystore |

---

## 9. Success Metrics

- ✅ App launches in < 2 seconds
- ✅ Chat messages sent/received with < 500ms latency
- ✅ 99.9% uptime for WebSocket connections
- ✅ All web app features functional on Android
- ✅ Pass Play Store review (privacy, permissions)
- ✅ Crash-free rate > 99.5%
- ✅ Battery impact < 5% per hour of active usage

---

This plan provides a comprehensive roadmap for building a production-quality Android companion app for nanochat. The phased approach allows for iterative development and testing, with clear milestones and deliverables at each stage.

---
