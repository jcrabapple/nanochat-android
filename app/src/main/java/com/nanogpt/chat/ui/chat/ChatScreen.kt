package com.nanogpt.chat.ui.chat

import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Surface
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCirclePlus
import com.composables.icons.lucide.Bookmark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanogpt.chat.ui.chat.components.ChatDrawer
import com.nanogpt.chat.ui.chat.components.ChatInputBar
import com.nanogpt.chat.ui.theme.ThemeManager
import com.nanogpt.chat.ui.chat.components.MessageBubble
import com.nanogpt.chat.ui.chat.components.ModelInfo
import com.nanogpt.chat.ui.chat.components.ModelSelector
import com.nanogpt.chat.ui.chat.components.WebSearchConfigDialog
import com.nanogpt.chat.ui.chat.components.WebSearchMode
import com.nanogpt.chat.ui.chat.components.WebSearchProvider
import com.nanogpt.chat.ui.chat.components.ImageViewer
import com.nanogpt.chat.util.copyToClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String?,
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToConversation: (String?) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAssistants: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {},
    themeManager: ThemeManager
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isDarkMode by themeManager.isDarkMode.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showModelSelector by remember { mutableStateOf(false) }
    var showWebSearchConfig by remember { mutableStateOf(false) }
    var showAssistantSheet by remember { mutableStateOf(false) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Also scroll when generating content
    LaunchedEffect(uiState.isGenerating) {
        if (uiState.isGenerating && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                currentConversationId = conversationId,
                onConversationClick = { convId ->
                    coroutineScope.launch {
                        drawerState.close()
                        onNavigateToConversation(convId)
                    }
                },
                onNewChat = {
                    coroutineScope.launch {
                        drawerState.close()
                        onNavigateToConversation(null)
                    }
                },
                onSettingsClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        onNavigateToSettings()
                    }
                },
                onAssistantsClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        showAssistantSheet = true
                    }
                },
                onProjectsClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        onNavigateToProjects()
                    }
                },
                onThemeToggleClick = {
                    themeManager.toggleDarkMode()
                },
                isDarkMode = isDarkMode
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = uiState.conversation?.title ?: "Chat",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                uiState.selectedAssistant?.let { assistant ->
                                    Text(
                                        text = "${assistant.name} • ${uiState.selectedModel?.name ?: "Unknown Model"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                } ?: uiState.selectedModel?.let { model ->
                                    Text(
                                        text = model.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Messages",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // Save to Karakeep button
                        Surface(
                            onClick = {
                                val currentConversationId = uiState.conversation?.id ?: conversationId
                                if (currentConversationId != null) {
                                    viewModel.saveChatToKarakeep { success, message ->
                                        Toast.makeText(
                                            context,
                                            message,
                                            if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "No active conversation to save",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Lucide.Bookmark,
                                    contentDescription = "Save to Karakeep",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Save",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        IconButton(onClick = {
                            onNavigateToConversation(null)
                        }) {
                            Icon(
                                Lucide.MessageCirclePlus,
                                contentDescription = "New chat",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    shadowElevation = 8.dp,
                    tonalElevation = 8.dp,
                    modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ) {
                    ChatInputBar(
                        text = inputText,
                        onTextChange = viewModel::updateInputText,
                        onSend = { viewModel.sendMessage() },
                        onStop = { viewModel.stopGeneration() },
                        isGenerating = uiState.isGenerating,
                        webSearchEnabled = uiState.webSearchMode != WebSearchMode.OFF,
                        onWebSearchClick = { showWebSearchConfig = true },
                        selectedModel = uiState.selectedModel,
                        onModelClick = { showModelSelector = true }
                    )
                }
            }
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Start a conversation!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        val isLastAssistantMessage = message.role == "assistant" &&
                            message.id == messages.lastOrNull { it.role == "assistant" }?.id

                        MessageBubble(
                            message = message,
                            backendUrl = viewModel.backendUrl,
                            isGenerating = uiState.isGenerating && isLastAssistantMessage,
                            onImageClick = { imageUrl -> fullScreenImageUrl = imageUrl },
                            onImageDownload = { imageUrl -> downloadImage(context, imageUrl) },
                            onCopy = {
                                context.copyToClipboard(message.content, "Message")
                                Toast.makeText(
                                    context,
                                    "Copied to clipboard",
                                    Toast.LENGTH_SHORT
                                ).show()
                                viewModel.logMessageInteraction(message.id, "copy")
                            },
                            onRegenerate = if (isLastAssistantMessage) {
                                {
                                    viewModel.regenerateLastMessage()
                                }
                            } else {
                                null
                            },
                            onStar = { starred ->
                                viewModel.toggleStar(message.id, starred)
                            }
                        )
                    }
                }
            }

            // Error banner
            uiState.error?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // Model selector dialog
    if (showModelSelector && uiState.availableModels.isNotEmpty()) {
        ModelSelector(
            selectedModel = uiState.selectedModel ?: ModelInfo("gpt-4o-mini", "GPT-4o Mini"),
            onModelSelected = { model ->
                viewModel.selectModel(model)
                showModelSelector = false
            },
            models = uiState.availableModels,
            onDismiss = { showModelSelector = false }
        )
    }

    // Web search config dialog
    if (showWebSearchConfig) {
        WebSearchConfigDialog(
            currentMode = uiState.webSearchMode,
            currentProvider = uiState.webSearchProvider,
            onModeSelected = { mode ->
                viewModel.setWebSearchMode(mode)
            },
            onProviderSelected = { provider ->
                viewModel.setWebSearchProvider(provider)
            },
            onDismiss = { showWebSearchConfig = false }
        )
    }

    // Assistant selector bottom sheet
    if (showAssistantSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAssistantSheet = false }
        ) {
            AssistantSelectorSheetContent(
                assistants = uiState.availableAssistants,
                selectedAssistant = uiState.selectedAssistant,
                onAssistantSelected = { assistant ->
                    viewModel.selectAssistant(assistant)
                    showAssistantSheet = false
                },
                onNavigateToAssistants = {
                    showAssistantSheet = false
                    onNavigateToAssistants()
                }
            )
        }
    }

    // Full-screen image viewer
    if (fullScreenImageUrl != null) {
        ImageViewer(
            imageUrl = fullScreenImageUrl!!,
            onDismiss = { fullScreenImageUrl = null }
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantSelectorSheetContent(
    assistants: List<com.nanogpt.chat.data.local.entity.AssistantEntity>,
    selectedAssistant: com.nanogpt.chat.data.local.entity.AssistantEntity?,
    onAssistantSelected: (com.nanogpt.chat.data.local.entity.AssistantEntity) -> Unit,
    onNavigateToAssistants: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Assistant",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Assistants list with scroll
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(assistants.size) { index ->
                val assistant = assistants[index]
                AssistantOptionItem(
                    name = assistant.name,
                    description = assistant.description ?: "No description",
                    firstLetter = assistant.name.first().toString(),
                    isSelected = selectedAssistant?.id == assistant.id,
                    onClick = { onAssistantSelected(assistant) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Manage Assistants button
        Button(
            onClick = onNavigateToAssistants,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Manage Assistants")
        }
    }
}

@Composable
private fun AssistantOptionItem(
    name: String,
    description: String,
    firstLetter: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = firstLetter,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun downloadImage(context: Context, imageUrl: String) {
    val imageLoader = coil.Coil.imageLoader(context)

    val request = coil.request.ImageRequest.Builder(context)
        .data(imageUrl)
        .target(
            onSuccess = { drawable ->
                // Download in background
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val bitmap = if (drawable is android.graphics.drawable.BitmapDrawable) {
                            drawable.bitmap
                        } else {
                            val width = drawable.intrinsicWidth.coerceAtLeast(800)
                            val height = drawable.intrinsicHeight.coerceAtLeast(600)
                            val bmp = android.graphics.Bitmap.createBitmap(
                                width,
                                height,
                                android.graphics.Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(bmp)
                            drawable.setBounds(0, 0, width, height)
                            drawable.draw(canvas)
                            bmp
                        }

                        // Use MediaStore to save to Downloads (works on Android 10+ without permissions)
                        val resolver = context.contentResolver
                        val fileName = "nanochat_${System.currentTimeMillis()}.png"

                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                            put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                        }

                        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                            }

                            contentValues.clear()
                            contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)

                            // Show success on main thread
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Image saved to Downloads",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            throw Exception("Failed to create MediaStore entry")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatScreen", "Failed to download image: ${e.message}", e)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                context,
                                "Failed to save image: ${e.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            onError = { errorDrawable ->
                android.util.Log.e("ChatScreen", "Failed to load image for download")
                android.widget.Toast.makeText(
                    context,
                    "Failed to load image",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
        .build()

    imageLoader.enqueue(request)
}
