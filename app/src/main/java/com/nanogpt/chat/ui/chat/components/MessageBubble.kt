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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
    onImageClick: (String) -> Unit = {},
    onImageDownload: (String) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    onVideoDownload: (String) -> Unit = {},
    backendUrl: String? = null,
    isGenerating: Boolean = false,
    isImageGenerationModel: Boolean = false,
    isVideoGenerationModel: Boolean = false,
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
                        MaterialTheme.colorScheme.primaryContainer
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
                modifier = Modifier,
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )

            // Show loading placeholder when generating an image
            if (isGenerating && !isUser && message.images.isNullOrEmpty()) {
                // Check if this is an image generation model based on capabilities
                val isImageGeneration = isImageGenerationModel ||
                        message.content.equals("Generated Image", ignoreCase = true) ||
                        message.content.equals("Generating image...", ignoreCase = true) ||
                        message.annotations?.any { it.type == "image" } == true

                if (isImageGeneration) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Generating image...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Subtle indicator for text generation
                    Spacer(modifier = Modifier.size(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            // Show loading placeholder when generating a video
            if (isGenerating && !isUser && message.videos.isNullOrEmpty()) {
                // Check if this is a video generation model based on capabilities
                val isVideoGeneration = isVideoGenerationModel ||
                        message.content.equals("Generated Video", ignoreCase = true) ||
                        message.content.equals("Generating video...", ignoreCase = true) ||
                        message.annotations?.any { it.type == "video" } == true

                if (isVideoGeneration) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Generating video...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Images from message.images array
            message.images?.forEach { imageUrl ->
                Spacer(modifier = Modifier.size(8.dp))
                // Construct full URL if imageUrl is a relative path
                val fullImageUrl = if (imageUrl.startsWith("/")) {
                    // Relative path - prepend backend URL
                    val baseUrl = backendUrl?.trimEnd('/')
                    if (baseUrl != null) {
                        "$baseUrl$imageUrl"
                    } else {
                        imageUrl
                    }
                } else {
                    // Already a full URL
                    imageUrl
                }

                // Image display with download button overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .clickable { onImageClick(fullImageUrl) }
                ) {
                    AsyncImage(
                        model = fullImageUrl,
                        contentDescription = "Generated image (tap to view full screen)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Translucent download button
                    FloatingActionButton(
                        onClick = { onImageDownload(fullImageUrl) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(40.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download image",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Videos from message.videos array
            message.videos?.forEach { videoUrl ->
                Spacer(modifier = Modifier.size(8.dp))
                // Construct full URL if videoUrl is a relative path
                val fullVideoUrl = if (videoUrl.startsWith("/")) {
                    // Relative path - prepend backend URL
                    val baseUrl = backendUrl?.trimEnd('/')
                    if (baseUrl != null) {
                        "$baseUrl$videoUrl"
                    } else {
                        videoUrl
                    }
                } else {
                    // Already a full URL
                    videoUrl
                }

                // Inline video player
                InlineVideoPlayer(
                    videoUrl = fullVideoUrl,
                    onFullscreen = { onVideoClick(fullVideoUrl) },
                    onDownload = { onVideoDownload(fullVideoUrl) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

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

            // Image annotations (only render if NOT in message.images to avoid duplicates)
            if (message.images.isNullOrEmpty()) {
                message.annotations?.filter { it.type == "image" }?.forEach { annotation ->
                    Spacer(modifier = Modifier.size(8.dp))
                    ImageAnnotation(
                        annotation = annotation,
                        backendUrl = backendUrl
                    )
                }
            }

            // Video annotations (render from annotations when not in videos array)
            if (message.videos.isNullOrEmpty()) {
                message.annotations?.filter { it.type == "video" }?.forEach { annotation ->
                    Spacer(modifier = Modifier.size(8.dp))
                    VideoAnnotation(
                        annotation = annotation,
                        backendUrl = backendUrl,
                        onFullscreen = onVideoClick,
                        onDownload = onVideoDownload
                    )
                }
            }

            // Action buttons for assistant messages
            if (!isUser) {
                Spacer(modifier = Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Metadata (model, tokens, cost, response time)
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
                        message.costUsd?.let { cost ->
                            val costString = if (cost < 0.01) {
                                String.format("%.4f", cost)
                            } else {
                                String.format("%.2f", cost)
                            }
                            Text(
                                "$$costString",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        message.responseTimeMs?.let { timeMs ->
                            val timeSeconds = timeMs / 1000.0
                            Text(
                                String.format("%.1fs", timeSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Regenerate button
                    if (onRegenerate != null) {
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Regenerate",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Star button
                    if (onStar != null) {
                        IconButton(
                            onClick = { onStar(!(message.starred == true)) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                if (message.starred == true) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (message.starred == true) "Unstar" else "Star",
                                modifier = Modifier.size(20.dp),
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
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(20.dp),
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

@Composable
private fun ImageAnnotation(
    annotation: Annotation,
    backendUrl: String? = null
) {
    // Extract image URL from annotation data
    val imageUrl = if (annotation.data is JsonObject) {
        val dataObj = annotation.data as JsonObject
        // Try to get the URL field - it might be a string primitive or nested object
        dataObj["url"]?.toString()?.replace("\"", "")  // Remove quotes if it's a string primitive
            ?: dataObj["image"]?.toString()?.replace("\"", "")
    } else {
        null
    }

    imageUrl?.let { url ->
        // Construct full URL if imageUrl is a relative path
        val fullImageUrl = if (url.startsWith("/")) {
            val baseUrl = backendUrl?.trimEnd('/')
            if (baseUrl != null) {
                "$baseUrl$url"
            } else {
                url
            }
        } else {
            url
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Image display
            AsyncImage(
                model = fullImageUrl,
                contentDescription = "Generated image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            )
        }
    }
}

@Composable
private fun VideoAnnotation(
    annotation: Annotation,
    backendUrl: String? = null,
    onFullscreen: (String) -> Unit,
    onDownload: (String) -> Unit
) {
    // Extract video URL from annotation data
    val videoUrl = if (annotation.data is JsonObject) {
        val dataObj = annotation.data as JsonObject
        dataObj["url"]?.toString()?.replace("\"", "")  // Remove quotes if it's a string primitive
            ?: dataObj["video"]?.toString()?.replace("\"", "")
    } else {
        null
    }

    videoUrl?.let { url ->
        // Construct full URL if videoUrl is a relative path
        val fullVideoUrl = if (url.startsWith("/")) {
            val baseUrl = backendUrl?.trimEnd('/')
            if (baseUrl != null) {
                "$baseUrl$url"
            } else {
                url
            }
        } else {
            url
        }

        // Video display with inline player
        InlineVideoPlayer(
            videoUrl = fullVideoUrl,
            onFullscreen = { onFullscreen(fullVideoUrl) },
            onDownload = { onDownload(fullVideoUrl) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
