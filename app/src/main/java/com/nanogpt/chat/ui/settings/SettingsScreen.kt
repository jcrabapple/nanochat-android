package com.nanogpt.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler
import androidx.paging.compose.*

/**
 * Enum representing all settings sections
 */
enum class SettingsSection(
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    ACCOUNT("Account", "Privacy, notifications, and data management", Icons.Default.AccountCircle),
    ASSISTANTS("Assistants", "Manage your AI assistants", Icons.Default.Psychology),
    CUSTOMIZATION("Customization", "Appearance and behavior", Icons.Default.Palette),
    MODELS("Models", "Manage your favorite models", Icons.Default.SmartToy),
    API_KEYS("API Keys", "Manage your API keys", Icons.Default.Key),
    ANALYTICS("Analytics", "Usage statistics and insights", Icons.Default.BarChart),
    STARRED("Starred", "Starred messages and conversations", Icons.Default.Star),
    DEVELOPER("Developer", "Advanced settings and tools", Icons.Default.DeveloperMode)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToAssistants: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }

    // If a section is selected, show that section's detail screen
    if (selectedSection != null) {
        SettingsDetailScreen(
            section = selectedSection!!,
            viewModel = viewModel,
            settings = uiState.settings,
            onNavigateBack = { selectedSection = null },
            onNavigateToAssistants = onNavigateToAssistants
        )
    } else {
        // Main settings list screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Settings sections list
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsSection.entries.forEach { section ->
                            SettingsSectionCard(
                                icon = section.icon,
                                title = section.displayName,
                                description = section.description,
                                onClick = { selectedSection = section }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Error banner
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Arrow icon
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    section: SettingsSection,
    viewModel: SettingsViewModel,
    settings: com.nanogpt.chat.data.remote.dto.UserSettingsDto?,
    onNavigateBack: () -> Unit,
    onNavigateToAssistants: () -> Unit = {}
) {
    // Handle Android back button/gesture
    BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section.displayName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            when (section) {
                SettingsSection.ACCOUNT -> AccountSection(
                    viewModel = viewModel,
                    settings = settings
                )
                SettingsSection.ASSISTANTS -> AssistantsSection(
                    onNavigateToAssistants = onNavigateToAssistants
                )
                SettingsSection.CUSTOMIZATION -> CustomizationSection(
                    themeManager = viewModel.themeManager
                )
                SettingsSection.MODELS -> ModelsSection(
                    viewModel = viewModel
                )
                else -> PlaceholderSection(section.displayName)
            }
        }
    }
}

@Composable
fun AccountSection(
    viewModel: SettingsViewModel,
    settings: com.nanogpt.chat.data.remote.dto.UserSettingsDto?
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteSuccessDialog by remember { mutableStateOf(false) }

    settings?.let {
        // Privacy Section
        SettingsSection(title = "Privacy") {
            PrivacySettings(
                contextMemory = settings.contextMemoryEnabled,
                persistentMemory = settings.persistentMemoryEnabled,
                onContextMemoryChange = { viewModel.updateContextMemory(it) },
                onPersistentMemoryChange = { viewModel.updatePersistentMemory(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content Processing Section
        SettingsSection(title = "Content Processing") {
            ContentProcessingSettings(
                youtubeTranscripts = settings.youtubeTranscriptsEnabled,
                webScraping = settings.webScrapingEnabled,
                followUpQuestions = settings.followUpQuestionsEnabled,
                onYoutubeTranscriptsChange = { viewModel.updateYoutubeTranscripts(it) },
                onWebScrapingChange = { viewModel.updateWebScraping(it) },
                onFollowUpQuestionsChange = { viewModel.updateFollowUpQuestions(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // MCP Section
        SettingsSection(title = "MCP Integration") {
            McpSettings(
                mcpEnabled = settings.mcpEnabled,
                onMcpEnabledChange = { viewModel.updateMcpEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Karakeep Section
        SettingsSection(title = "Karakeep Integration") {
            KarakeepSettings(
                url = settings.karakeepUrl ?: "",
                apiKey = settings.karakeepApiKey ?: "",
                isTesting = uiState.isTestingKarakeep,
                testResult = uiState.karakeepTestResult,
                onSave = { url, apiKey ->
                    viewModel.updateKarakeepSettings(url, apiKey)
                },
                onTest = { viewModel.testKarakeepConnection { success, message -> } }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Model Preferences Section
        SettingsSection(title = "Model Preferences") {
            ModelPreferencesSettings(
                chatTitleModel = settings.titleModelId,
                followUpQuestionsModel = settings.followUpModelId,
                models = uiState.allModels,
                onChatTitleModelChange = { viewModel.updateChatTitleModel(it) },
                onFollowUpQuestionsModelChange = { viewModel.updateFollowUpQuestionsModel(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Text-to-Speech Section
        SettingsSection(title = "Text-to-Speech") {
            TtsSettings(
                model = uiState.ttsModel,
                voice = uiState.ttsVoice,
                speed = uiState.ttsSpeed,
                onModelChange = { viewModel.updateTtsModel(it) },
                onVoiceChange = { viewModel.updateTtsVoice(it) },
                onSpeedChange = { viewModel.updateTtsSpeed(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Speech-to-Text Section
        SettingsSection(title = "Speech-to-Text") {
            SttSettings(
                model = uiState.sttModel,
                onModelChange = { viewModel.updateSttModel(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Data Management Section
        SettingsSection(title = "Data Management") {
            DataManagementSettings(
                onDeleteAllChats = { showDeleteDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All Chats") },
            text = {
                Text("Are you sure you want to delete all chats? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllChats(
                            onSuccess = {
                                showDeleteDialog = false
                                showDeleteSuccessDialog = true
                            },
                            onError = { error ->
                                viewModel.clearError()
                                // Show error via uiState
                            }
                        )
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success dialog
    if (showDeleteSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSuccessDialog = false },
            title = { Text("Success") },
            text = { Text("All chats have been deleted successfully.") },
            confirmButton = {
                TextButton(onClick = { showDeleteSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun PlaceholderSection(sectionName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = sectionName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "This section is coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun PrivacySettings(
    contextMemory: Boolean,
    persistentMemory: Boolean,
    onContextMemoryChange: (Boolean) -> Unit,
    onPersistentMemoryChange: (Boolean) -> Unit
) {
    SettingSwitch(
        title = "Context Memory",
        description = "Compress long conversations for better context",
        checked = contextMemory,
        onCheckedChange = onContextMemoryChange
    )
    SettingSwitch(
        title = "Persistent Memory",
        description = "Remember facts across conversations",
        checked = persistentMemory,
        onCheckedChange = onPersistentMemoryChange
    )
}

@Composable
fun ContentProcessingSettings(
    youtubeTranscripts: Boolean,
    webScraping: Boolean,
    followUpQuestions: Boolean,
    onYoutubeTranscriptsChange: (Boolean) -> Unit,
    onWebScrapingChange: (Boolean) -> Unit,
    onFollowUpQuestionsChange: (Boolean) -> Unit
) {
    SettingSwitch(
        title = "YouTube Transcripts",
        description = "Automatically fetch YouTube video transcripts ($0.01 each)",
        checked = youtubeTranscripts,
        onCheckedChange = onYoutubeTranscriptsChange
    )
    SettingSwitch(
        title = "Web Scraping",
        description = "Automatically scrape web page content",
        checked = webScraping,
        onCheckedChange = onWebScrapingChange
    )
    SettingSwitch(
        title = "Follow-up Questions",
        description = "Show suggested questions after responses",
        checked = followUpQuestions,
        onCheckedChange = onFollowUpQuestionsChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPreferencesSettings(
    chatTitleModel: String?,
    followUpQuestionsModel: String?,
    models: List<com.nanogpt.chat.data.remote.dto.ModelDto>,
    onChatTitleModelChange: (String?) -> Unit,
    onFollowUpQuestionsModelChange: (String?) -> Unit
) {
    var chatTitleExpanded by remember { mutableStateOf(false) }
    var followUpExpanded by remember { mutableStateOf(false) }

    // Chat Title Generation Model
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "Chat Title Generation Model",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Select the model used to generate chat titles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(modifier = Modifier.padding(top = 4.dp)) {
            ExposedDropdownMenuBox(
                expanded = chatTitleExpanded,
                onExpandedChange = { chatTitleExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (chatTitleModel.isNullOrEmpty()) "GLM-4.5-Air" else chatTitleModel,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = chatTitleExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = chatTitleExpanded,
                    onDismissRequest = { chatTitleExpanded = false }
                ) {
                    // Default option
                    DropdownMenuItem(
                        text = { Text("GLM-4.5-Air") },
                        onClick = {
                            onChatTitleModelChange(null)
                            chatTitleExpanded = false
                        }
                    )
                    // User models
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.name) },
                            onClick = {
                                onChatTitleModelChange(model.id)
                                chatTitleExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Follow-up Questions Model
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "Follow-up Questions Model",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Select the model used to generate follow-up questions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(modifier = Modifier.padding(top = 4.dp)) {
            ExposedDropdownMenuBox(
                expanded = followUpExpanded,
                onExpandedChange = { followUpExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (followUpQuestionsModel.isNullOrEmpty()) "GLM-4.5-Air" else followUpQuestionsModel,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = followUpExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = followUpExpanded,
                    onDismissRequest = { followUpExpanded = false }
                ) {
                    // Default option
                    DropdownMenuItem(
                        text = { Text("GLM-4.5-Air") },
                        onClick = {
                            onFollowUpQuestionsModelChange(null)
                            followUpExpanded = false
                        }
                    )
                    // User models
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.name) },
                            onClick = {
                                onFollowUpQuestionsModelChange(model.id)
                                followUpExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsSettings(
    model: String?,
    voice: String?,
    speed: Float,
    onModelChange: (String?) -> Unit,
    onVoiceChange: (String?) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    var modelExpanded by remember { mutableStateOf(false) }
    var voiceExpanded by remember { mutableStateOf(false) }

    // Available TTS models with pricing
    val ttsModels = listOf(
        "tts-1" to "TTS-1 (OpenAI) - $0.015/1k chars",
        "Kokoro-82m" to "Kokoro-82m - $0.001/1k chars",
        "Elevenlabs-Turbo-V2.5" to "Elevenlabs Turbo V2.5 - $0.06/1k chars",
        "tts-1-hd" to "TTS-1 HD (OpenAI) - $0.030/1k chars",
        "gpt-4o-mini-tts" to "GPT-4o Mini TTS - $0.0006/1k chars"
    )

    // Voices available for each model
    val kokoroVoices = listOf(
        "af_alloy" to "Alloy (Female US)",
        "af_aoede" to "Aoede (Female US)",
        "af_bella" to "Bella (Female US)",
        "af_jessica" to "Jessica (Female US)",
        "af_nova" to "Nova (Female US)",
        "am_adam" to "Adam (Male US)",
        "am_echo" to "Echo (Male US)",
        "am_eric" to "Eric (Male US)",
        "am_liam" to "Liam (Male US)",
        "am_onyx" to "Onyx (Male US)",
        "bf_alice" to "Alice (Female UK)",
        "bf_emma" to "Emma (Female UK)",
        "bf_isabella" to "Isabella (Female UK)",
        "bf_lily" to "Lily (Female UK)",
        "bm_daniel" to "Daniel (Male UK)",
        "bm_fable" to "Fable (Male UK)",
        "bm_george" to "George (Male UK)",
        "bm_lewis" to "Lewis (Male UK)",
        "jf_alpha" to "Alpha (Female JP)",
        "jf_gongitsune" to "Gongitsune (Female JP)",
        "jf_nezumi" to "Nezumi (Female JP)",
        "jf_tebukuro" to "Tebukuro (Female JP)",
        "zf_xiaobei" to "Xiaobei (Female CN)",
        "zf_xiaoni" to "Xiaoni (Female CN)",
        "zf_xiaoxiao" to "Xiaoxiao (Female CN)",
        "zf_xiaoyi" to "Xiaoyi (Female CN)",
        "ff_siwis" to "Siwis (Female FR)",
        "im_nicola" to "Nicola (Male IT)",
        "hf_alpha" to "Alpha (Female HI)",
        "hf_beta" to "Beta (Female HI)"
    )

    val elevenlabsVoices = listOf(
        "Adam" to "Adam",
        "Alice" to "Alice",
        "Antoni" to "Antoni",
        "Aria" to "Aria",
        "Arnold" to "Arnold",
        "Bella" to "Bella",
        "Bill" to "Bill",
        "Brian" to "Brian",
        "Callum" to "Callum",
        "Charlie" to "Charlie",
        "Charlotte" to "Charlotte",
        "Chris" to "Chris",
        "Daniel" to "Daniel",
        "Domi" to "Domi",
        "Dorothy" to "Dorothy",
        "Drew" to "Drew",
        "Elli" to "Elli",
        "Emily" to "Emily",
        "Eric" to "Eric",
        "Ethan" to "Ethan",
        "Fin" to "Fin",
        "Freya" to "Freya",
        "George" to "George",
        "Gigi" to "Gigi",
        "Giovanni" to "Giovanni",
        "Grace" to "Grace",
        "James" to "James",
        "Jeremy" to "Jeremy",
        "Jessica" to "Jessica",
        "Joseph" to "Joseph",
        "Josh" to "Josh",
        "Laura" to "Laura",
        "Liam" to "Liam",
        "Lily" to "Lily",
        "Matilda" to "Matilda",
        "Matthew" to "Matthew",
        "Michael" to "Michael",
        "Nicole" to "Nicole",
        "Rachel" to "Rachel",
        "River" to "River",
        "Roger" to "Roger",
        "Ryan" to "Ryan",
        "Sam" to "Sam",
        "Sarah" to "Sarah",
        "Thomas" to "Thomas",
        "Will" to "Will"
    )

    val openaiVoices = listOf(
        "alloy" to "Alloy",
        "ash" to "Ash",
        "ballad" to "Ballad",
        "coral" to "Coral",
        "echo" to "Echo",
        "fable" to "Fable",
        "onyx" to "Onyx",
        "nova" to "Nova",
        "sage" to "Sage",
        "shimmer" to "Shimmer",
        "verse" to "Verse"
    )

    // Get voices for selected model
    val availableVoices = when (model) {
        "Kokoro-82m" -> kokoroVoices
        "Elevenlabs-Turbo-V2.5" -> elevenlabsVoices
        "tts-1", "tts-1-hd", "gpt-4o-mini-tts" -> openaiVoices
        else -> openaiVoices // Default
    }

    // Get default voice for model
    val defaultVoiceForModel = when (model) {
        "Kokoro-82m" -> "af_alloy"
        "Elevenlabs-Turbo-V2.5" -> "Rachel"
        "tts-1", "tts-1-hd", "gpt-4o-mini-tts" -> "alloy"
        else -> "alloy"
    }

    // Current display voice
    val currentVoice = voice ?: defaultVoiceForModel

    // TTS Model
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "Model",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Choose a TTS model. Pricing varies significantly",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(modifier = Modifier.padding(top = 4.dp)) {
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = it }
            ) {
                OutlinedTextField(
                    value = model ?: "tts-1",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    ttsModels.forEach { (modelId, displayName) ->
                        DropdownMenuItem(
                            text = { Text(displayName) },
                            onClick = {
                                onModelChange(modelId)
                                // Reset voice to default for new model
                                val newDefaultVoice = when (modelId) {
                                    "Kokoro-82m" -> "af_alloy"
                                    "Elevenlabs-Turbo-V2.5" -> "Rachel"
                                    "tts-1", "tts-1-hd", "gpt-4o-mini-tts" -> "alloy"
                                    else -> "alloy"
                                }
                                onVoiceChange(newDefaultVoice)
                                modelExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Voice Selection
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "Voice",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Select the voice used for reading messages aloud",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(modifier = Modifier.padding(top = 4.dp)) {
            ExposedDropdownMenuBox(
                expanded = voiceExpanded,
                onExpandedChange = { voiceExpanded = it }
            ) {
                OutlinedTextField(
                    value = availableVoices.find { it.first == currentVoice }?.second ?: currentVoice,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = voiceExpanded,
                    onDismissRequest = { voiceExpanded = false }
                ) {
                    availableVoices.forEach { (voiceId, displayName) ->
                        DropdownMenuItem(
                            text = { Text(displayName) },
                            onClick = {
                                onVoiceChange(voiceId)
                                voiceExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Speed Slider
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "Speed: ${String.format("%.1f", speed)}x",
            style = MaterialTheme.typography.bodyMedium
        )
        androidx.compose.material3.Slider(
            value = speed,
            onValueChange = onSpeedChange,
            valueRange = 0.5f..2.0f,
            steps = 5
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SttSettings(
    model: String?,
    onModelChange: (String?) -> Unit
) {
    var modelExpanded by remember { mutableStateOf(false) }

    // Available STT models
    val sttModels = listOf(
        "Whisper-Large-V3" to "Whisper Large V3 (OpenAI) - $0.01/min",
        "Wizper" to "Wizper - Fast & Efficient - $0.01/min",
        "Elevenlabs-STT" to "Elevenlabs STT - Premium - $0.03/min"
    )

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "Model",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Choose an STT model for voice transcription",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(modifier = Modifier.padding(top = 4.dp)) {
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = it }
            ) {
                OutlinedTextField(
                    value = model ?: "Whisper-Large-V3",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    sttModels.forEach { (modelId, displayName) ->
                        DropdownMenuItem(
                            text = { Text(displayName) },
                            onClick = {
                                onModelChange(modelId)
                                modelExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun McpSettings(
    mcpEnabled: Boolean,
    onMcpEnabledChange: (Boolean) -> Unit
) {
    SettingSwitch(
        title = "Nano-GPT MCP",
        description = "Supports Vision, YouTube Transcripts, Web Scraping, Nano-GPT Balance, Image Generation, and Model Lists",
        checked = mcpEnabled,
        onCheckedChange = onMcpEnabledChange
    )
}

@Composable
fun KarakeepSettings(
    url: String,
    apiKey: String,
    isTesting: Boolean,
    testResult: String?,
    onSave: (String, String) -> Unit,
    onTest: () -> Unit
) {
    var urlInput by remember { mutableStateOf(url) }
    var apiKeyInput by remember { mutableStateOf(apiKey) }
    var hasChanges by remember { mutableStateOf(false) }

    // Update local state when props change
    androidx.compose.runtime.LaunchedEffect(url, apiKey) {
        urlInput = url
        apiKeyInput = apiKey
        hasChanges = false
    }

    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Configure your Karakeep instance to save chats as bookmarks.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = urlInput,
            onValueChange = {
                urlInput = it
                hasChanges = it != url || apiKeyInput != apiKey
            },
            label = { Text("Karakeep URL") },
            placeholder = { Text("https://karakeep.example.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = {
                apiKeyInput = it
                hasChanges = urlInput != url || it != apiKey
            },
            label = { Text("API Key") },
            placeholder = { Text("your-api-key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        // Test result message
        testResult?.let { result ->
            val isSuccess = result == "Connection successful!"
            Surface(
                color = if (isSuccess) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = result,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSuccess) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Test Connection button
            Button(
                onClick = {
                    onTest()
                },
                enabled = !isTesting && urlInput.isNotBlank() && apiKeyInput.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Test Connection")
                }
            }

            // Save Settings button
            Button(
                onClick = {
                    onSave(urlInput, apiKeyInput)
                    hasChanges = false
                },
                enabled = hasChanges,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
fun DataManagementSettings(
    onDeleteAllChats: () -> Unit
) {
    Button(
        onClick = onDeleteAllChats,
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Delete All Chats")
    }
}

@Composable
fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun AssistantsSection(
    onNavigateToAssistants: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description
        Text(
            text = "Manage your AI assistants. Create custom assistants with specific instructions, models, and web search settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Assistant info cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "What are Assistants?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Assistants are AI personas with custom instructions and settings. You can create assistants for specific tasks like coding, writing, analysis, and more.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quick Selection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Use the assistant icon in the chat sidebar to quickly switch between assistants without leaving the conversation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Manage button
        Button(
            onClick = onNavigateToAssistants,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Manage Assistants")
        }
    }
}

enum class ModelFilter(val displayName: String) {
    ENABLED("Enabled"),
    ALL("All Models"),
    SUBSCRIPTION("Subscription"),
    IMAGE("Image"),
    VIDEO("Video")
}

@Composable
fun ModelsSection(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(ModelFilter.ENABLED) }
    var searchQuery by remember { mutableStateOf("") }

    // Build filter function based on selected filter and search query
    val filterFunction: (com.nanogpt.chat.data.remote.dto.ModelDto) -> Boolean = remember(selectedFilter, searchQuery, uiState.enabledModelIds) {
        { model ->
            // Exclude image and video models from ALL and SUBSCRIPTION filters
            val isImageOrVideoModel = model.capabilities.images || model.capabilities.video

            // First apply the selected filter
            val passesFilter = when (selectedFilter) {
                ModelFilter.ENABLED -> model.id in uiState.enabledModelIds
                ModelFilter.ALL -> !isImageOrVideoModel
                ModelFilter.SUBSCRIPTION -> model.subscription?.included == true && !isImageOrVideoModel
                ModelFilter.IMAGE -> model.capabilities.images
                ModelFilter.VIDEO -> model.capabilities.video
            }

            // Then apply search filter if query is not blank
            val passesSearch = searchQuery.isBlank() ||
                model.name.contains(searchQuery, ignoreCase = true) ||
                model.id.contains(searchQuery, ignoreCase = true) ||
                (!model.description.isNullOrBlank() && model.description.contains(searchQuery, ignoreCase = true))

            passesFilter && passesSearch
        }
    }

    // Apply filter to all models (for initial load display)
    val filteredModels = remember(selectedFilter, searchQuery, uiState.enabledModelIds) {
        uiState.allModels.filter(filterFunction)
    }

    // Use regular Column for now to avoid nested scrolling
    // In the future, we could restructure SettingsDetailScreen to use LazyColumn for MODELS section
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Manage your AI models. Enable or disable models to control which ones appear in the model selector.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${filteredModels.size} models",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search models by name or description...") },
            leadingIcon = {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        // Filter chips
        FilterChipsRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        // Models list
        if (uiState.allModels.isEmpty()) {
            // Loading
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading models...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (filteredModels.isEmpty()) {
            // No models found
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            "No models match \"$searchQuery\""
                        } else {
                            "No ${selectedFilter.displayName.lowercase()} found"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Display models (limit to first 50 for performance)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredModels.take(50).forEach { model ->
                    EnhancedModelCard(
                        model = model,
                        onEnabledToggle = { viewModel.toggleModelEnabled(model) }
                    )
                }

                if (filteredModels.size > 50) {
                    // Show message about more models
                    Text(
                        text = "Showing first 50 of ${filteredModels.size} models. Use search to find specific models.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Refresh button
        Button(
            onClick = { viewModel.refreshModels() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh Models")
        }
    }
}

// Helper function to infer provider from model ID
fun inferProviderFromModelId(modelId: String): String {
    return when {
        modelId.startsWith("gpt", ignoreCase = true) -> "OpenAI"
        modelId.startsWith("claude", ignoreCase = true) -> "Anthropic"
        modelId.startsWith("gemini", ignoreCase = true) -> "Google"
        modelId.contains("llama", ignoreCase = true) -> "Meta"
        modelId.contains("mistral", ignoreCase = true) -> "Mistral"
        modelId.contains("deepseek", ignoreCase = true) -> "DeepSeek"
        modelId.contains("zhipu", ignoreCase = true) || modelId.contains("glm", ignoreCase = true) -> "Zhipu AI"
        else -> "Other"
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: ModelFilter,
    onFilterSelected: (ModelFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModelFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) }
            )
        }
    }
}

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (selected) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            label()
        }
    }
}

@Composable
fun ModelCard(
    model: com.nanogpt.chat.data.remote.dto.UserModelDto,
    onEnabledToggle: () -> Unit,
    onPinnedToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (model.pinned) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (model.pinned) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Provider icon or fallback
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Model info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = model.modelId,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (model.pinned) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (model.pinned) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = model.provider.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Pin button
            IconButton(
                onClick = onPinnedToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (model.pinned) Icons.Default.Star else Icons.Default.Star,
                    contentDescription = if (model.pinned) "Unpin" else "Pin",
                    modifier = Modifier.size(20.dp),
                    tint = if (model.pinned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }

            // Enabled switch
            Switch(
                checked = model.enabled,
                onCheckedChange = { onEnabledToggle() }
            )
        }
    }
}

@Composable
fun EnhancedModelCard(
    model: com.nanogpt.chat.data.remote.dto.ModelDto,
    onEnabledToggle: () -> Unit
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Provider icon
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Model info (compact)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Capabilities badges (compact)
                if (model.capabilities.vision || model.capabilities.reasoning ||
                    model.capabilities.images || model.capabilities.video) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (model.capabilities.vision) {
                            CapabilityBadge("V")
                        }
                        if (model.capabilities.reasoning) {
                            CapabilityBadge("R")
                        }
                        if (model.capabilities.images) {
                            CapabilityBadge("I")
                        }
                        if (model.capabilities.video) {
                            CapabilityBadge("Vid")
                        }
                    }
                }
            }

            // Info button
            IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Model Info",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Enabled switch
            Switch(
                checked = model.enabled,
                onCheckedChange = { onEnabledToggle() },
                modifier = Modifier.size(40.dp, 24.dp)
            )
        }
    }

    // Info dialog
    if (showInfoDialog) {
        ModelInfoDialog(
            model = model,
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
fun ModelInfoDialog(
    model: com.nanogpt.chat.data.remote.dto.ModelDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(model.name)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Description
                model.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Model ID
                Text(
                    text = "ID: ${model.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Capabilities
                if (model.capabilities.vision || model.capabilities.reasoning ||
                    model.capabilities.images || model.capabilities.video) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Capabilities:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (model.capabilities.vision) {
                            Text("• Vision", style = MaterialTheme.typography.bodySmall)
                        }
                        if (model.capabilities.reasoning) {
                            Text("• Reasoning", style = MaterialTheme.typography.bodySmall)
                        }
                        if (model.capabilities.images) {
                            Text("• Image Generation", style = MaterialTheme.typography.bodySmall)
                        }
                        if (model.capabilities.video) {
                            Text("• Video", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Pricing information
                model.pricing?.let { pricing ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Pricing:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        pricing.prompt?.let {
                            PricingItem("Prompt", it, roundToDecimals = true)
                        }
                        pricing.completion?.let {
                            PricingItem("Completion", it, roundToDecimals = true)
                        }
                        pricing.image?.let {
                            PricingItem("Image", it, roundToDecimals = false)
                        }
                        pricing.request?.let {
                            PricingItem("Request", it, roundToDecimals = false)
                        }
                    }
                }

                // Subscription info
                model.subscription?.let { sub ->
                    Text(
                        text = if (sub.included) "✓ Included in subscription" else "Not included in subscription",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (sub.included) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (sub.included) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CapabilityBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 10.sp
        )
    }
}

@Composable
fun PricingItem(label: String, value: String, roundToDecimals: Boolean = false) {
    val displayValue = if (roundToDecimals) {
        // Try to parse as number and round to 3 decimal places
        value.toDoubleOrNull()?.let { num ->
            String.format("%.3f", num)
        } ?: value
    } else {
        value
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}
