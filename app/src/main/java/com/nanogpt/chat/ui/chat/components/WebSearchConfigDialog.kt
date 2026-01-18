package com.nanogpt.chat.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    KAGI("Kagi", "kagi"),
    PERPLEXITY("Perplexity", "perplexity"),
    VALYU("Valyu", "valyu")
}

// Provider-specific options
enum class WebSearchExaDepth(val displayName: String, val value: String) {
    FAST("Fast", "fast"),
    AUTO("Auto", "auto"),
    NEURAL("Neural", "neural"),
    DEEP("Deep", "deep")
}

enum class WebSearchContextSize(val displayName: String, val value: String) {
    LOW("Low", "low"),
    MEDIUM("Medium", "medium"),
    HIGH("High", "high")
}

enum class WebSearchKagiSource(val displayName: String, val value: String) {
    WEB("Web", "web"),
    NEWS("News", "news"),
    SEARCH("Search", "search")
}

enum class WebSearchValyuSearchType(val displayName: String, val value: String) {
    ALL("All", "all"),
    WEB("Web", "web")
}

// Helper function to get color for each mode
private fun WebSearchMode.getColor(materialTheme: androidx.compose.material3.ColorScheme): androidx.compose.ui.graphics.Color {
    return when (this) {
        WebSearchMode.OFF -> androidx.compose.ui.graphics.Color.Transparent
        WebSearchMode.STANDARD -> materialTheme.primary
        WebSearchMode.DEEP -> materialTheme.tertiary
    }
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
                modifier = modifier
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = null
                        )

                        // Colored indicator dot
                        if (mode != WebSearchMode.OFF) {
                            Surface(
                                modifier = Modifier.size(12.dp),
                                shape = CircleShape,
                                color = mode.getColor(MaterialTheme.colorScheme)
                            ) {}
                        }

                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyMedium
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
