package com.nanogpt.chat.ui.projects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nanogpt.chat.data.local.entity.ProjectEntity

@Composable
fun ProjectDialog(
    project: ProjectEntity? = null,
    onCreate: (String, String?, String?, String?) -> Unit,
    onUpdate: (String, String?, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(project?.name ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }
    var systemPrompt by remember { mutableStateOf(project?.systemPrompt ?: "") }
    var selectedColor by remember { mutableStateOf(project?.color ?: "") }

    val projectColors = listOf(
        "" to "Default",
        "#F44336" to "Red",
        "#E91E63" to "Pink",
        "#9C27B0" to "Purple",
        "#673AB7" to "Deep Purple",
        "#3F51B5" to "Indigo",
        "#2196F3" to "Blue",
        "#03A9F4" to "Light Blue",
        "#00BCD4" to "Cyan",
        "#009688" to "Teal",
        "#4CAF50" to "Green",
        "#8BC34A" to "Light Green",
        "#CDDC39" to "Lime",
        "#FFEB3B" to "Yellow",
        "#FFC107" to "Amber",
        "#FF9800" to "Orange",
        "#FF5722" to "Deep Orange"
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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (project == null) "Create Project" else "Edit Project",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Project Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    // System Prompt
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text("System Prompt (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )

                    // Color Selection
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        projectColors.forEach { (colorValue, colorName) ->
                            val isSelected = selectedColor == colorValue
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { selectedColor = colorValue },
                                shape = CircleShape,
                                color = if (colorValue.isNotBlank()) {
                                    Color(android.graphics.Color.parseColor(colorValue))
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = if (isSelected) {
                                    BorderStroke(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    null
                                }
                            ) {}
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
                                val color = selectedColor.takeIf { it.isNotBlank() }
                                val desc = description.takeIf { it.isNotBlank() }
                                val prompt = systemPrompt.takeIf { it.isNotBlank() }
                                if (project == null) {
                                    onCreate(name, desc, prompt, color)
                                } else {
                                    onUpdate(name, desc, prompt, color)
                                }
                            },
                            enabled = name.isNotBlank()
                        ) {
                            Text(if (project == null) "Create" else "Update")
                        }
                    }
                }
            }
        }
    }
}
