package com.nanogpt.chat.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

data class ModelCapabilities(
    val vision: Boolean = false,
    val reasoning: Boolean = false,
    val images: Boolean = false,
    val video: Boolean = false
)

data class ModelInfo(
    val id: String,
    val name: String,
    val provider: String? = null,
    val providerLogo: String? = null,
    val capabilities: ModelCapabilities = ModelCapabilities()
)

@Composable
fun ModelSelector(
    selectedModel: ModelInfo,
    onModelSelected: (ModelInfo) -> Unit,
    models: List<ModelInfo> = defaultModels,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    if (onDismiss != null) {
        // Dialog mode
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Model") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Provider logo or fallback icon
                                    if (model.providerLogo != null) {
                                        SubcomposeAsyncImage(
                                            model = model.providerLogo,
                                            contentDescription = model.provider ?: "Provider logo",
                                            modifier = Modifier.size(24.dp),
                                            loading = {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            },
                                            error = {
                                                // Fallback to provider badge/emoji if image fails
                                                ProviderBadge(provider = model.provider)
                                            }
                                        )
                                    } else {
                                        // Show provider badge if no logo URL
                                        ProviderBadge(provider = model.provider)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Model info
                                    Column {
                                        Text(model.name)
                                        model.provider?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        // Capability badges
                                        if (model.capabilities.vision || model.capabilities.reasoning ||
                                            model.capabilities.images || model.capabilities.video) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                ModelCapabilityBadge("Vision", Icons.Default.Visibility, model.capabilities.vision)
                                                ModelCapabilityBadge("Reason", Icons.Default.Psychology, model.capabilities.reasoning)
                                                ModelCapabilityBadge("Image", Icons.Default.Image, model.capabilities.images)
                                                ModelCapabilityBadge("Video", Icons.Default.Videocam, model.capabilities.video)
                                            }
                                        }
                                    }
                                }
                            },
                            onClick = {
                                onModelSelected(model)
                                onDismiss()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    } else {
        // Dropdown mode
        Box {
            Card(
                modifier = modifier,
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                onClick = { expanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedModel.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select model",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(model.name)
                                model.provider?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Capability badges
                                if (model.capabilities.vision || model.capabilities.reasoning ||
                                    model.capabilities.images || model.capabilities.video) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ModelCapabilityBadge("Vision", Icons.Default.Visibility, model.capabilities.vision)
                                        ModelCapabilityBadge("Reason", Icons.Default.Psychology, model.capabilities.reasoning)
                                        ModelCapabilityBadge("Image", Icons.Default.Image, model.capabilities.images)
                                        ModelCapabilityBadge("Video", Icons.Default.Videocam, model.capabilities.video)
                                    }
                                }
                            }
                        },
                        onClick = {
                            onModelSelected(model)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderBadge(provider: String?) {
    val providerLower = provider?.lowercase() ?: ""
    val (badgeColor, initials) = when {
        providerLower.contains("openai") -> Color(0xFF10A37F) to "AI"
        providerLower.contains("anthropic") || providerLower.contains("claude") -> Color(0xFFD4915D) to "AC"
        providerLower.contains("google") || providerLower.contains("gemini") -> Color(0xFF4285F4) to "G"
        providerLower.contains("deepseek") -> Color(0xFF4E94E9) to "DS"
        providerLower.contains("moonshot") || providerLower.contains("kimi") -> Color(0xFF4096FF) to "MS"
        providerLower.contains("zai") || providerLower.contains("z.ai") || providerLower.contains("glm") || providerLower.contains("zhipu") -> Color(0xFF6B46C1) to "Z"
        providerLower.contains("minimax") || providerLower.contains("mini") -> Color(0xFFF59E0B) to "MM"
        else -> MaterialTheme.colorScheme.primary to "AI"
    }

    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = badgeColor
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initials,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ModelCapabilityBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean
) {
    if (!enabled) return

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.height(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

val defaultModels = listOf(
    ModelInfo("gpt-4o-mini", "GPT-4o Mini"),
    ModelInfo("gpt-4o", "GPT-4o"),
    ModelInfo("gpt-4-turbo", "GPT-4 Turbo"),
    ModelInfo("gpt-4", "GPT-4"),
    ModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo"),
    ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet"),
    ModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku"),
    ModelInfo("claude-3-opus-20240229", "Claude 3 Opus"),
    ModelInfo("claude-3-sonnet-20240229", "Claude 3 Sonnet"),
    ModelInfo("claude-3-haiku-20240307", "Claude 3 Haiku")
)
