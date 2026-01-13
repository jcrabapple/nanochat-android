package com.nanogpt.chat.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanogpt.chat.ui.chat.Annotation
import com.nanogpt.chat.ui.chat.Message
import kotlinx.serialization.json.JsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    onCopy: () -> Unit = {},
    onRegenerate: (() -> Unit)? = null,
    onStar: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = if (isUser) {
            Alignment.End
        } else {
            Alignment.Start
        }
    ) {
        // Avatar
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = if (isUser) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.primary
            }
        ) {
            Icon(
                if (isUser) Icons.Filled.Person else Icons.Filled.SmartToy,
                contentDescription = if (isUser) "User" else "AI",
                tint = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.size(4.dp))

        // Message bubble
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isUser) {
                        Color(0xFF2196F3) // Blue for user messages
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(12.dp)
        ) {
            // Show reasoning if present
            if (!message.reasoning.isNullOrBlank()) {
                var reasoningExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Header row (clickable to toggle)
                    Row(
                        modifier = Modifier
                            .clickable { reasoningExpanded = !reasoningExpanded }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Thinking",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            if (reasoningExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (reasoningExpanded) "Collapse" else "Expand",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Reasoning content (only show when expanded)
                    if (reasoningExpanded) {
                        Text(
                            message.reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
            }

            // Main content with markdown rendering
            SimpleMarkdownText(
                markdown = message.content,
                modifier = Modifier
            )

            // Web search annotations
            message.annotations?.filter { it.type == "web-search" }?.forEach { annotation ->
                Spacer(modifier = Modifier.size(8.dp))
                WebSearchAnnotation(
                    annotation = annotation,
                    onUrlClick = { url ->
                        // Handle URL click
                    }
                )
            }

            // Action buttons for assistant messages
            if (!isUser) {
                Spacer(modifier = Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Metadata (model, tokens)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        message.modelId?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        message.tokenCount?.let {
                            Text(
                                "$it tokens",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Regenerate button
                    if (onRegenerate != null) {
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Regenerate",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Star button
                    if (onStar != null) {
                        IconButton(
                            onClick = { onStar(!(message.starred == true)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (message.starred == true) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (message.starred == true) "Unstar" else "Star",
                                modifier = Modifier.size(16.dp),
                                tint = if (message.starred == true) {
                                    Color(0xFFFFD700) // Gold color for starred
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                        }
                    }

                    // Copy button
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebSearchAnnotation(
    annotation: Annotation,
    onUrlClick: (String) -> Unit
) {
    // The annotation.data should be a JsonObject with web search results
    val searchData = if (annotation.data is JsonObject) {
        annotation.data
    } else {
        null
    }

    searchData?.let { data ->
        WebSearchResultCard(
            jsonData = data,
            onUrlClick = onUrlClick
        )
    }
}
