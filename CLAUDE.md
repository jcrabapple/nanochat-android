# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**nanochat-android** is a native Android companion app for the self-hostable [nanochat](https://github.com/nanogpt-community/nanochat) backend. Users deploy their own nanochat backend (SvelteKit + Drizzle + Better Auth) and connect this Android app to their instance.

**Important Architecture Decisions:**
- Users self-host the backend; the app connects to user-provided URLs
- API keys are stored server-side, not in the app (security by design)
- Uses polling pattern for message streaming (NOT SSE - backend `/api/generate-message` returns JSON, not streams)
- All data cached locally in Room database for offline access

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (signed with release.keystore)
./gradlew assembleRelease

# Install to connected devices
adb install app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/release/app-release.apk

# Install to all connected devices via adb
adb devices | grep -v "List" | cut -f1 | while read line; do
  adb -s $line install -r app/build/outputs/apk/debug/app-debug.apk
done

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Lint (if configured)
./gradlew lint
```

## Architecture

### Layer Structure

```
┌─────────────────────────────────────────────┐
│         UI Layer (Compose)                  │
│  - Screens (Chat, Settings, etc.)           │
│  - ViewModels (State Management)            │
│  - Navigation Component                     │
├─────────────────────────────────────────────┤
│         Data Layer                          │
│  - Repository Implementations               │
│  - Local Cache (Room Database)              │
│  - Remote APIs (Retrofit + OkHttp)          │
└─────────────────────────────────────────────┘
```

### Key Modules

- **`di/`** - Hilt dependency injection modules (NetworkModule, DatabaseModule, AppModule, WorkManagerModule)
- **`data/`** - Data layer with repositories, local storage, remote APIs, and background sync
- **`ui/`** - Jetpack Compose screens and ViewModels

### Backend Integration Pattern

**Critical**: The backend uses a **polling pattern**, not Server-Sent Events (SSE):

1. POST to `/api/generate-message` → Returns `{ok: boolean, conversation_id: string}`
2. Poll GET `/api/messages/{conversationId}` every 500ms for actual message content
3. Stop polling when:
   - Assistant message content exists AND
   - No content changes for 3 seconds (6 consecutive empty polls)

See `ChatViewModel.kt:pollForMessages()` for implementation details.

### Message Streaming Architecture

The `StreamingManager` class exists but is **not currently used** for message generation because the backend doesn't return SSE streams from `/api/generate-message`. It's kept for potential future SSE endpoints.

Current flow in `ChatViewModel.kt`:
1. `sendMessage()` → Calls `api.generateMessage()` (gets conversation_id)
2. `pollForMessages()` → Polls `api.getMessages()` every 500ms
3. Updates UI with latest messages from API
4. Saves to Room database as content arrives
5. Stops when content stops changing for 3 seconds

### State Management

- **ViewModels** use `StateFlow` and `MutableStateFlow` for reactive state
- **UI observes** state with `collectAsState()`
- **isGenerating flag** controls send/stop button toggle
- Use `copy()` to update immutable state data classes

### Navigation

- Uses Jetpack Navigation Compose with type-safe routes
- Routes defined in `ui/navigation/Screen.kt`
- `NavHost` in `ui/navigation/NanoChatNavGraph.kt`
- Start destination determined by authentication state (Setup vs Chat)

### Database Schema

Room database with 4 entities:
- **ConversationEntity** - Conversations with title, assistantId, projectId, syncStatus
- **MessageEntity** - Messages with role, content, reasoning, annotations (JSON)
- **AssistantEntity** - Custom AI assistants with instructions
- **ProjectEntity** - Project organization for conversations

**Message Annotations:**
The `annotationsJson` field stores JSON-serialized metadata including:
- Web search results (citations, sources)
- Image attachments
- File references
- Custom metadata from plugins

### DAO Merge Behavior

When fetching from API, DAOs perform intelligent merge:
- Compares `updatedAt` timestamps
- Preserves local changes (syncStatus = PENDING)
- Updates stale remote data
- Prevents overwriting user edits with server data

### Secure Storage

Uses `EncryptedSharedPreferences` for sensitive data:
- Session tokens (Bearer auth)
- Backend URL
- User ID and email
- Last used model/conversation IDs
- TTS/STT settings (local only)

**Never store API keys** - those live on the backend server.

## Common Patterns

### Adding a New Feature

1. Create DTO in `data/remote/dto/`
2. Add API endpoint in `data/remote/api/NanoChatApi.kt`
3. Create Entity in `data/local/entity/` (if caching needed)
4. Create DAO in `data/local/dao/`
5. Create Repository in `data/repository/`
6. Create ViewModel in `ui/[feature]/`
7. Create Composable screen in `ui/[feature]/`
8. Update `Screen.kt` and `NanoChatNavGraph.kt` for navigation

### Repository Pattern

Repositories abstract data sources:
```kotlin
class SomeRepository @Inject constructor(
    private val api: NanoChatApi,
    private val dao: SomeDao,
    private val secureStorage: SecureStorage
) {
    // Combine API calls with local caching
    suspend fun getData(): Result<Data> {
        // Try API first, fallback to cache
    }
}
```

### ViewModel State Management

```kotlin
private val _uiState = MutableStateFlow(FeatureUiState())
val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

// Update state
_uiState.value = _uiState.value.copy(isLoading = false)
```

### Dependency Injection

All ViewModels use `@HiltViewModel`:
```kotlin
@HiltViewModel
class SomeViewModel @Inject constructor(
    private val repository: SomeRepository
) : ViewModel()
```

Provide dependencies in `di/` modules with `@Provides @Singleton`.

### API Integration

API calls return Retrofit `Response<T>`:
```kotlin
val response = api.someMethod()
if (response.isSuccessful && response.body() != null) {
    Result.success(response.body()!!)
} else {
    Result.failure(Exception("HTTP ${response.code()}"))
}
```

Always handle both success and failure cases.

## Configuration

### Backend URL Configuration

On first launch, user enters their nanochat backend URL via `SetupScreen`. This URL is:
- Stored in `SecureStorage`
- Used to build Retrofit instance in `NetworkModule`
- Must include protocol (https://) and port if non-standard

### Release Signing

Release builds are signed with `release.keystore`:
- Store password: `nanochat`
- Key alias: `nanochat`
- Key password: `nanochat`

**Never commit the actual keystore file** to version control.

## Important Constraints

### Message Polling Timeout

When modifying message polling logic:
- **3-second timeout** (`maxEmptyPolls = 6`) feels immediate but allows brief pauses
- Too short (< 2s): Cuts off slow models
- Too long (> 10s): Unresponsive UI, stop button doesn't reset
- Always track both message count AND content changes

### Navigation Handling

All screens must handle back navigation properly:
- Chat screen can navigate to conversations list
- Settings/Assistants/Projects screens navigate back to calling screen
- Use `navController.popBackStack()` for back navigation

### Material 3 Design

Follow Material 3 guidelines:
- Use `MaterialTheme.colorScheme` for colors
- Use `MaterialTheme.typography` for text styles
- Surface colors for cards/containers
- Proper elevation and spacing

### Error Handling

- Use `Result<T>` for repository operations
- Show errors in UI via `error: String?` in state
- Clear errors with `clearError()` methods
- Log errors with `android.util.Log.e()` for debugging

## Tech Stack Details

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Kotlin**: 1.9.22
- **Gradle**: 8.7
- **Compose BOM**: 2024.08.00
- **Hilt**: 2.51
- **Room**: 2.6.1
- **Retrofit**: 2.11.0
- **OkHttp**: 4.12.0
- **Kotlinx Serialization**: 1.6.3

## Development Notes

### Testing

Tests are configured in `build.gradle.kts` but no test files exist yet:
- Unit tests: `app/src/test/`
- Instrumented tests: `app/src/androidTest/`
- Libraries: JUnit, MockK, Turbine, Espresso

When adding tests, follow the pattern in dependencies and use:
- `@RunWith(MockKJUnitRunner::class)` for unit tests
- `@HiltAndroidTest` for instrumented tests

### Coil Image Loading

Coil is initialized in `NanoChatApplication` with SVG decoder support:
```kotlin
val imageLoader = ImageLoader.Builder(this)
    .components { add(SvgDecoder.Factory()) }
    .build()
```

Use `AsyncImage()` composable for loading images from URLs.

### Markdown Rendering

Uses `multiplatform-markdown-renderer` library for message content. Apply `MarkdownText()` composable for assistant messages.

### Web Search Integration

Multiple providers supported: Linkup, Tavily, Exa, Kagi. Provider selection in UI, API calls through `WebSearchRepository`.

### Conversation Title Auto-Generation

After the first message in a conversation, the backend auto-generates a title. The app uses `ConversationTitlePoller` to poll for this title:
- Polls `/api/conversations/{id}` every 500ms
- Stops when title changes from "New Chat" or timeout after 30 seconds
- Updates local database and UI in real-time

### Background Sync

WorkManager manages periodic background sync:
- `ConversationSyncWorker` runs periodically to sync conversations
- Configured in `WorkManagerModule`
- Manual refresh available in UI
- Respects network and battery constraints

### Karakeep Integration

Bookmarking service integration for saving chats:
- API endpoint: `/api/karakeep/save-chat`
- Allows users to save conversations to Karakeep
- Triggered from UI action in chat screen

### Passkey Authentication

Passkey support via Android Credentials API:
- `CredentialsApiClient` for biometric auth
- Alternative to password-based login
- Configured in `LoginScreen`

### Camera and Media Features

CameraX integration for potential image features:
- CameraX 1.3.4 dependency
- Prepared for future image capture functionality
- ExoPlayer for media playback

## File Organization

```
app/src/main/java/com/nanogpt/chat/
├── NanoChatApplication.kt          # App entry point
├── di/                             # Dependency injection
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── WorkManagerModule.kt
├── data/
│   ├── local/                      # Local storage
│   │   ├── NanoChatDatabase.kt
│   │   ├── dao/                    # Room DAOs
│   │   ├── entity/                 # Room entities
│   │   └── SecureStorage.kt
│   ├── remote/                     # API layer
│   │   ├── api/
│   │   │   ├── NanoChatApi.kt
│   │   │   └── WebSearchApi.kt
│   │   ├── dto/                    # API DTOs
│   │   └── StreamingManager.kt     # SSE (unused currently)
│   ├── repository/                 # Data repositories
│   └── sync/                       # Background sync workers
└── ui/
    ├── navigation/
    │   ├── Screen.kt               # Route definitions
    │   ├── NanoChatNavGraph.kt     # NavHost setup
    │   └── NavViewModel.kt
    ├── auth/                       # Authentication flows
    │   ├── login/
    │   ├── setup/
    │   └── passkey/                # Passkey/biometric auth
    ├── chat/                       # Main chat screen
    │   ├── ChatScreen.kt
    │   ├── ChatViewModel.kt
    │   └── components/
    ├── conversations/              # Conversation list
    ├── assistants/                 # Assistant management
    ├── projects/                   # Project management
    └── settings/                   # App settings
```

## Common Issues

### "No response from assistant"

- Check backend URL is correct
- Verify session token is valid
- Check `pollForMessages()` is completing properly
- Ensure `isGenerating` flag is reset

### "Messages getting cut off"

- `maxEmptyPolls` too small in `pollForMessages()`
- Slow AI models need longer timeout
- Check network connectivity
- Verify backend is streaming messages correctly

### "Stop button not changing back"

- `isGenerating` flag not reset to false
- Polling timeout too long
- Check completion detection logic (content changes + timeout)

### "Navigation not working"

- Ensure all routes defined in `Screen.kt`
- Check `NavHost` includes all composable routes
- Verify back navigation callbacks are wired correctly
