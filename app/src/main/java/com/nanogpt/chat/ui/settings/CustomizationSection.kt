package com.nanogpt.chat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanogpt.chat.ui.theme.ThemeChoice
import com.nanogpt.chat.ui.theme.ThemeManager
import com.nanogpt.chat.ui.theme.getCatppuccinColorScheme
import com.nanogpt.chat.ui.theme.getTokyoNightColorScheme

/**
 * Customization section for theme settings.
 * Allows users to choose light/dark mode and select specific themes.
 */
@Composable
fun CustomizationSection(
    themeManager: ThemeManager,
    modifier: Modifier = Modifier
) {
    val isDarkMode by themeManager.isDarkMode.collectAsState()
    val lightTheme by themeManager.lightTheme.collectAsState()
    val darkTheme by themeManager.darkTheme.collectAsState()

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme Mode Toggle
        Text(
            text = "Theme Mode",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))

        ThemeModeToggleCard(
            isDarkMode = isDarkMode,
            onToggleChange = { themeManager.toggleDarkMode() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Light Theme Selection
        if (!isDarkMode) {
            Text(
                text = "Light Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            ThemeCards(
                selectedTheme = lightTheme,
                onThemeSelected = { themeManager.setLightTheme(it) }
            )
        } else {
            // Dark Theme Selection
            Text(
                text = "Dark Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            ThemeCards(
                selectedTheme = darkTheme,
                onThemeSelected = { themeManager.setDarkTheme(it) }
            )
        }
    }
}

@Composable
private fun ThemeModeToggleCard(
    isDarkMode: Boolean,
    onToggleChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = if (isDarkMode) "Dark Mode" else "Light Mode",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isDarkMode) "Currently using dark theme" else "Currently using light theme",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isDarkMode,
                onCheckedChange = onToggleChange
            )
        }
    }
}

@Composable
private fun ThemeCards(
    selectedTheme: ThemeChoice,
    onThemeSelected: (ThemeChoice) -> Unit
) {
    val themes = if (selectedTheme.isDark) {
        ThemeChoice.darkThemes()
    } else {
        ThemeChoice.lightThemes()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        themes.forEach { theme ->
            ThemeCard(
                theme = theme,
                isSelected = theme == selectedTheme,
                onClick = { onThemeSelected(theme) }
            )
        }
    }
}

@Composable
private fun ThemeCard(
    theme: ThemeChoice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = getCatppuccinColorScheme(theme)
    val tokyoNightScheme = getTokyoNightColorScheme(theme)
    val isMaterialYou = colorScheme == null && tokyoNightScheme == null

    val themeDescription = when {
        isMaterialYou -> "Dynamic colors from wallpaper"
        tokyoNightScheme != null -> when (theme) {
            ThemeChoice.TOKYO_NIGHT_LIGHT -> "Clean light theme inspired by Tokyo at dawn"
            ThemeChoice.TOKYO_NIGHT -> "Dark theme with vibrant neon accents"
            ThemeChoice.TOKYO_NIGHT_STORM -> "Deep blue-gray stormy night aesthetic"
            else -> "Tokyo Night color palette"
        }
        else -> "Pastel color palette with soft aesthetics"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme preview colors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    when {
                        isMaterialYou -> {
                            // Material You indicator
                            MaterialYouPreview(isDark = theme.isDark)
                        }
                        tokyoNightScheme != null -> {
                            // Tokyo Night color swatches
                            ColorSwatch(tokyoNightScheme.primary)
                            ColorSwatch(tokyoNightScheme.secondary)
                            ColorSwatch(tokyoNightScheme.tertiary)
                            ColorSwatch(tokyoNightScheme.surface)
                        }
                        else -> {
                            // Catppuccin color swatches
                            colorScheme?.let { scheme ->
                                ColorSwatch(scheme.primary)
                                ColorSwatch(scheme.secondary)
                                ColorSwatch(scheme.tertiary)
                                ColorSwatch(scheme.surface)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(2f)) {
                    Text(
                        text = theme.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                    Text(
                        text = themeDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Selected indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun MaterialYouPreview(isDark: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (isDark) {
                    Color(0xFF6750A4) // Material You dark purple
                } else {
                    Color(0xFF6750A4) // Material You light purple
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Small dot to indicate dynamic color
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
