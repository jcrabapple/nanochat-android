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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler

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
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // If a section is selected, show that section's detail screen
    if (selectedSection != null) {
        SettingsDetailScreen(
            section = selectedSection!!,
            viewModel = viewModel,
            settings = uiState.settings,
            onNavigateBack = { selectedSection = null },
            onDeleteDialogChange = { showDeleteDialog = it }
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
                        // TODO: Implement delete all chats
                        showDeleteDialog = false
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
    onDeleteDialogChange: (Boolean) -> Unit
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
                    settings = settings,
                    showDeleteDialog = false,
                    onDeleteDialogChange = onDeleteDialogChange
                )
                else -> PlaceholderSection(section.displayName)
            }
        }
    }
}

@Composable
fun AccountSection(
    viewModel: SettingsViewModel,
    settings: com.nanogpt.chat.data.remote.dto.UserSettingsDto?,
    showDeleteDialog: Boolean,
    onDeleteDialogChange: (Boolean) -> Unit
) {
    settings?.let {
        // Privacy Section
        SettingsSection(title = "Privacy") {
            PrivacySettings(
                privacyMode = settings.privacyMode,
                contextMemory = settings.contextMemoryEnabled,
                persistentMemory = settings.persistentMemoryEnabled,
                onPrivacyModeChange = { viewModel.updatePrivacyMode(it) },
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

        // Model Preferences Section
        SettingsSection(title = "Model Preferences") {
            ModelPreferencesSettings(
                chatTitleModel = settings.chatTitleModel,
                followUpQuestionsModel = settings.followUpQuestionsModel,
                onChatTitleModelChange = { viewModel.updateChatTitleModel(it) },
                onFollowUpQuestionsModelChange = { viewModel.updateFollowUpQuestionsModel(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Text-to-Speech Section
        SettingsSection(title = "Text-to-Speech") {
            TtsSettings(
                model = settings.ttsModel,
                voice = settings.ttsVoice,
                speed = settings.ttsSpeed,
                onModelChange = { viewModel.updateTtsModel(it) },
                onVoiceChange = { viewModel.updateTtsVoice(it) },
                onSpeedChange = { viewModel.updateTtsSpeed(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Speech-to-Text Section
        SettingsSection(title = "Speech-to-Text") {
            SttSettings(
                model = settings.sttModel,
                onModelChange = { viewModel.updateSttModel(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // MCP Section
        SettingsSection(title = "MCP Integration") {
            McpSettings(
                nanoGptMcpEnabled = settings.nanoGptMcpEnabled,
                onNanoGptMcpChange = { viewModel.updateNanoGptMcp(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Data Management Section
        SettingsSection(title = "Data Management") {
            DataManagementSettings(
                onDeleteAllChats = { onDeleteDialogChange(true) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
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
    privacyMode: Boolean,
    contextMemory: Boolean,
    persistentMemory: Boolean,
    onPrivacyModeChange: (Boolean) -> Unit,
    onContextMemoryChange: (Boolean) -> Unit,
    onPersistentMemoryChange: (Boolean) -> Unit
) {
    SettingSwitch(
        title = "Privacy Mode",
        description = "Hide personal information in sidebar",
        checked = privacyMode,
        onCheckedChange = onPrivacyModeChange
    )
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

@Composable
fun ModelPreferencesSettings(
    chatTitleModel: String?,
    followUpQuestionsModel: String?,
    onChatTitleModelChange: (String?) -> Unit,
    onFollowUpQuestionsModelChange: (String?) -> Unit
) {
    var showChatTitleModelDialog by remember { mutableStateOf(false) }
    var showFollowUpModelDialog by remember { mutableStateOf(false) }

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
        Button(
            onClick = { showChatTitleModelDialog = true },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(chatTitleModel ?: "Default (GLM-4.5-Air)")
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
        Button(
            onClick = { showFollowUpModelDialog = true },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(followUpQuestionsModel ?: "Default (GLM-4.5-Air)")
        }
    }

    // TODO: Add model selection dialogs
}

@Composable
fun TtsSettings(
    model: String?,
    voice: String?,
    speed: Float,
    onModelChange: (String?) -> Unit,
    onVoiceChange: (String?) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    var showModelDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

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
        Button(
            onClick = { showModelDialog = true },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(model ?: "TTS-1 (Standard) - $0.015/1k")
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
        Button(
            onClick = { showVoiceDialog = true },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(voice ?: "Alloy")
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

    // TODO: Add model and voice selection dialogs
}

@Composable
fun SttSettings(
    model: String?,
    onModelChange: (String?) -> Unit
) {
    var showModelDialog by remember { mutableStateOf(false) }

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
        Button(
            onClick = { showModelDialog = true },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(model ?: "Whisper Large V3 (OpenAI) - $0.01/min")
        }
    }

    // TODO: Add model selection dialog
}

@Composable
fun McpSettings(
    nanoGptMcpEnabled: Boolean,
    onNanoGptMcpChange: (Boolean) -> Unit
) {
    SettingSwitch(
        title = "Nano-GPT MCP",
        description = "Supports Vision, YouTube Transcripts, Web Scraping, Nano-GPT Balance, Image Generation, and Model Lists",
        checked = nanoGptMcpEnabled,
        onCheckedChange = onNanoGptMcpChange
    )
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
