package com.nanogpt.chat.ui.assistants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanogpt.chat.data.local.entity.AssistantEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantsScreen(
    viewModel: AssistantsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAssistantSelected: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingAssistant by remember { mutableStateOf<AssistantEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistants") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Assistant")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(
                items = uiState.assistants,
                key = { it.id }
            ) { assistant ->
                AssistantItem(
                    assistant = assistant,
                    onClick = { onAssistantSelected(assistant.id) },
                    onEdit = { editingAssistant = assistant },
                    onDelete = { viewModel.deleteAssistant(assistant) }
                )
            }
        }
    }

    if (showCreateDialog) {
        AssistantDialog(
            onCreate = { name: String, instructions: String, modelId: String, description: String?, webSearchEnabled: Boolean, provider: String?, mode: String?, temp: Double?, topP: Double?, reasoning: String, exaDepth: String?, contextSize: String?, kagiSource: String?, valyuSearchType: String? ->
                viewModel.createAssistant(name, instructions, modelId, description, webSearchEnabled, provider, mode, temp, topP, reasoning, exaDepth, contextSize, kagiSource, valyuSearchType)
                showCreateDialog = false
            },
            onUpdate = { _: String, _: String, _: String, _: String?, _: Boolean, _: String?, _: String?, _: Double?, _: Double?, _: String, _: String?, _: String?, _: String?, _: String? -> },
            onDismiss = { showCreateDialog = false },
            availableModels = uiState.availableModels
        )
    }

    if (editingAssistant != null) {
        AssistantDialog(
            assistant = editingAssistant,
            onCreate = { _: String, _: String, _: String, _: String?, _: Boolean, _: String?, _: String?, _: Double?, _: Double?, _: String, _: String?, _: String?, _: String?, _: String? -> },
            onUpdate = { name: String, instructions: String, modelId: String, description: String?, webSearchEnabled: Boolean, provider: String?, mode: String?, temp: Double?, topP: Double?, reasoning: String, exaDepth: String?, contextSize: String?, kagiSource: String?, valyuSearchType: String? ->
                viewModel.updateAssistant(
                    editingAssistant!!.id,
                    name,
                    instructions,
                    modelId,
                    description,
                    webSearchEnabled,
                    provider,
                    mode,
                    temp,
                    topP,
                    reasoning,
                    exaDepth,
                    contextSize,
                    kagiSource,
                    valyuSearchType
                )
                editingAssistant = null
            },
            onDismiss = { editingAssistant = null },
            availableModels = uiState.availableModels
        )
    }
}

@Composable
fun AssistantItem(
    assistant: AssistantEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar - use first letter of assistant name
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = assistant.name.first().toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assistant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    assistant.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Model badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = assistant.modelId,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Temperature badge (if not default)
                        if (assistant.temperature != null && assistant.temperature != 0.7) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "T: ${assistant.temperature}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        // Web search badge
                        if (assistant.webSearchEnabled) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "Web Search",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Actions
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
