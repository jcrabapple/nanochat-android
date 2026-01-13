package com.nanogpt.chat.ui.assistants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nanogpt.chat.data.local.entity.AssistantEntity
import androidx.compose.ui.unit.dp

// Predefined emoji icons for assistants
val ASSISTANT_ICONS = listOf(
    "🤖" to "Robot",
    "🎨" to "Artist",
    "💡" to "Idea",
    "📝" to "Writer",
    "🔧" to "Developer",
    "🎵" to "Musician",
    "📚" to "Teacher",
    "🔬" to "Scientist",
    "🎯" to "Goal",
    "⚡" to "Fast",
    "🌟" to "Star",
    "💼" to "Business",
    "🎮" to "Gaming",
    "🍳" to "Chef",
    "✈️" to "Travel",
    "🏥" to "Doctor",
    "⚖️" to "Legal",
    "🎪" to "Creative",
    "📊" to "Analyst"
)

@Composable
fun AssistantDialog(
    assistant: AssistantEntity? = null,
    onCreate: (String, String, String, Boolean, String?, String?, Double?, Double?, String) -> Unit,
    onUpdate: (String, String, String, Boolean, String?, String?, Double?, Double?, String) -> Unit,
    onDismiss: () -> Unit,
    availableModels: List<Pair<String, String>> = emptyList() // List of (modelId, modelName)
) {
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()

    var name by remember { mutableStateOf(assistant?.name ?: "") }
    var instructions by remember { mutableStateOf(assistant?.instructions ?: "") }
    var modelId by remember { mutableStateOf(assistant?.modelId ?: "gpt-4o-mini") }
    var webSearchEnabled by remember { mutableStateOf(assistant?.webSearchEnabled ?: false) }
    var webSearchProvider by remember { mutableStateOf(assistant?.webSearchProvider ?: "") }
    var webSearchMode by remember { mutableStateOf(assistant?.webSearchMode ?: "standard") }

    var temperature by remember { mutableStateOf(assistant?.temperature?.toFloat() ?: 0.7f) }
    var topP by remember { mutableStateOf(assistant?.topP?.toFloat() ?: 1.0f) }
    var reasoningEffort by remember { mutableStateOf(assistant?.reasoningEffort ?: "auto") }

    var showModelDropdown by remember { mutableStateOf(false) }
    var showProviderDropdown by remember { mutableStateOf(false) }
    var showSearchModeDropdown by remember { mutableStateOf(false) }
    var showReasoningDropdown by remember { mutableStateOf(false) }

    val providers = listOf(
        "linkup" to "Linkup",
        "tavily" to "Tavily",
        "exa" to "Exa",
        "kagi" to "Kagi"
    )

    val searchModes = listOf(
        "standard" to "Standard",
        "deep" to "Deep"
    )

    val reasoningOptions = listOf(
        "off" to "Off",
        "auto" to "Auto",
        "light" to "Light",
        "medium" to "Medium",
        "heavy" to "Heavy"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = navigationBarsPadding.calculateBottomPadding())
                .height(600.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = if (assistant == null) "Create Assistant" else "Edit Assistant",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it.replaceFirstChar { char ->
                                if (char.isLowerCase()) char.titlecase() else char.toString()
                            }
                        },
                        label = { Text("Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Instructions
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = {
                            instructions = it.replaceFirstChar { char ->
                                if (char.isLowerCase()) char.titlecase() else char.toString()
                            }
                        },
                        label = { Text("Instructions *") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )

                    // Model Selector
                    Column {
                        Text(
                            text = "Model *",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            val displayModelName = availableModels.find { it.first == modelId }?.second
                                ?: if (availableModels.isNotEmpty()) availableModels.firstOrNull { it.first == modelId }?.second ?: modelId
                                else modelId

                            OutlinedTextField(
                                value = displayModelName,
                                onValueChange = { },
                                label = { Text("Select Model") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showModelDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showModelDropdown,
                                onDismissRequest = { showModelDropdown = false }
                            ) {
                                if (availableModels.isNotEmpty()) {
                                    availableModels.forEach { (id, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                modelId = id
                                                showModelDropdown = false
                                            }
                                        )
                                    }
                                } else {
                                    // Fallback to hardcoded list if no user models available
                                    listOf("gpt-4o-mini", "gpt-4o", "gpt-4-turbo", "gpt-4", "claude-3-haiku", "claude-3-sonnet", "claude-3-opus").forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model) },
                                            onClick = {
                                                modelId = model
                                                showModelDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Temperature Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Temperature",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%.2f", temperature),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0.0f..2.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Controls randomness (0.0 = focused, 2.0 = creative)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Top P Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Top P",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%.2f", topP),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = topP,
                            onValueChange = { topP = it },
                            valueRange = 0.0f..1.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Controls diversity (0.0 = focused, 1.0 = diverse)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Thinking Budget (Reasoning Effort)
                    Column {
                        Text(
                            text = "Thinking Budget",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            val reasoningDisplay = reasoningOptions.find { it.first == reasoningEffort }?.second
                                ?: "Auto"
                            OutlinedTextField(
                                value = reasoningDisplay,
                                onValueChange = { },
                                label = { Text("Select Level") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showReasoningDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showReasoningDropdown,
                                onDismissRequest = { showReasoningDropdown = false }
                            ) {
                                reasoningOptions.forEach { (key, value) ->
                                    DropdownMenuItem(
                                        text = { Text(value) },
                                        onClick = {
                                            reasoningEffort = key
                                            showReasoningDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Web Search Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = webSearchEnabled,
                            onCheckedChange = { webSearchEnabled = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Web Search")
                    }

                    // Web Search Mode
                    if (webSearchEnabled) {
                        Column {
                            Text(
                                text = "Search Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                val searchModeDisplay = searchModes.find { it.first == webSearchMode }?.second
                                    ?: "Standard"
                                OutlinedTextField(
                                    value = searchModeDisplay,
                                    onValueChange = { },
                                    label = { Text("Select Mode") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showSearchModeDropdown = true }
                                )
                                DropdownMenu(
                                    expanded = showSearchModeDropdown,
                                    onDismissRequest = { showSearchModeDropdown = false }
                                ) {
                                    searchModes.forEach { (key, value) ->
                                        DropdownMenuItem(
                                            text = { Text(value) },
                                            onClick = {
                                                webSearchMode = key
                                                showSearchModeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Web Search Provider
                    if (webSearchEnabled) {
                        Column {
                            Text(
                                text = "Search Provider",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                OutlinedTextField(
                                    value = providers.find { it.first == webSearchProvider }?.second
                                        ?: "Linkup",
                                    onValueChange = { },
                                    label = { Text("Select Provider") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showProviderDropdown = true }
                                )
                                DropdownMenu(
                                    expanded = showProviderDropdown,
                                    onDismissRequest = { showProviderDropdown = false }
                                ) {
                                    providers.forEach { (key, value) ->
                                        DropdownMenuItem(
                                            text = { Text(value) },
                                            onClick = {
                                                webSearchProvider = key
                                                showProviderDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val tempDouble = if (temperature == 0.7f) null else temperature.toDouble()
                                val topPDouble = if (topP == 1.0f) null else topP.toDouble()

                                if (assistant == null) {
                                    onCreate(
                                        name,
                                        instructions,
                                        modelId,
                                        webSearchEnabled,
                                        webSearchProvider.takeIf { it.isNotBlank() },
                                        webSearchMode,
                                        tempDouble,
                                        topPDouble,
                                        reasoningEffort
                                    )
                                } else {
                                    onUpdate(
                                        name,
                                        instructions,
                                        modelId,
                                        webSearchEnabled,
                                        webSearchProvider.takeIf { it.isNotBlank() },
                                        webSearchMode,
                                        tempDouble,
                                        topPDouble,
                                        reasoningEffort
                                    )
                                }
                            },
                            enabled = name.isNotBlank() && instructions.isNotBlank() && modelId.isNotBlank()
                        ) {
                            Text(if (assistant == null) "Create" else "Update")
                        }
                    }
                }
            }
        }
    }
}
