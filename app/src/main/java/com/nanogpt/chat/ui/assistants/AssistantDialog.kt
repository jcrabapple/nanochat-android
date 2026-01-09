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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nanogpt.chat.data.local.entity.AssistantEntity
import androidx.compose.ui.unit.dp

@Composable
fun AssistantDialog(
    assistant: AssistantEntity? = null,
    onCreate: (String, String, String, String, Boolean, String?) -> Unit,
    onUpdate: (String, String, String, String, Boolean, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(assistant?.name ?: "") }
    var description by remember { mutableStateOf(assistant?.description ?: "") }
    var instructions by remember { mutableStateOf(assistant?.instructions ?: "") }
    var modelId by remember { mutableStateOf(assistant?.modelId ?: "gpt-4o-mini") }
    var webSearchEnabled by remember { mutableStateOf(assistant?.webSearchEnabled ?: false) }
    var webSearchProvider by remember { mutableStateOf(assistant?.webSearchProvider ?: "") }

    var showModelDropdown by remember { mutableStateOf(false) }
    var showProviderDropdown by remember { mutableStateOf(false) }

    val models = listOf(
        "gpt-4o-mini",
        "gpt-4o",
        "gpt-4-turbo",
        "gpt-4",
        "claude-3-haiku",
        "claude-3-sonnet",
        "claude-3-opus"
    )

    val providers = listOf(
        "" to "None",
        "linkup" to "Linkup",
        "tavily" to "Tavily",
        "exa" to "Exa",
        "kagi" to "Kagi"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Instructions
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
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
                            OutlinedTextField(
                                value = modelId,
                                onValueChange = { },
                                label = { Text("Select Model") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            )
                            DropdownMenu(
                                expanded = showModelDropdown,
                                onDismissRequest = { showModelDropdown = false }
                            ) {
                                models.forEach { model ->
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

                    // Web Search Provider
                    if (webSearchEnabled) {
                        Column {
                            Text(
                                text = "Web Search Provider",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                OutlinedTextField(
                                    value = providers.find { it.first == webSearchProvider }?.second
                                        ?: "None",
                                    onValueChange = { },
                                    label = { Text("Select Provider") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true
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
                                if (assistant == null) {
                                    onCreate(name, description, instructions, modelId, webSearchEnabled, webSearchProvider.takeIf { it.isNotBlank() })
                                } else {
                                    onUpdate(name, description, instructions, modelId, webSearchEnabled, webSearchProvider.takeIf { it.isNotBlank() })
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
