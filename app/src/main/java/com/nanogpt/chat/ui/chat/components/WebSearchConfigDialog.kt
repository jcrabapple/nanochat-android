package com.nanogpt.chat.ui.chat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

enum class WebSearchMode(val displayName: String) {
    OFF("Off"),
    STANDARD("Standard"),
    DEEP("Deep")
}

enum class WebSearchProvider(val displayName: String, val value: String) {
    LINKUP("Linkup", "linkup"),
    TAVILY("Tavily", "tavily"),
    EXA("Exa", "exa"),
    KAGI("Kagi", "kagi")
}

@Composable
fun WebSearchConfigDialog(
    currentMode: WebSearchMode,
    currentProvider: WebSearchProvider,
    onModeSelected: (WebSearchMode) -> Unit,
    onProviderSelected: (WebSearchProvider) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Web Search Settings") },
        text = {
            Column(
                modifier = modifier.padding(vertical = 8.dp)
            ) {
                // Mode Selection
                Text(
                    text = "Search Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                WebSearchMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentMode == mode,
                                onClick = { onModeSelected(mode) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = null
                        )
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Provider Selection (only show if web search is enabled)
                if (currentMode != WebSearchMode.OFF) {
                    Text(
                        text = "Search Provider",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    WebSearchProvider.values().forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = currentProvider == provider,
                                    onClick = { onProviderSelected(provider) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentProvider == provider,
                                onClick = null
                            )
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
