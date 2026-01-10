package com.nanogpt.chat.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Custom nanoChat dark color scheme (legacy, for fallback)
 */
private val DarkColorScheme = lightColorScheme(
    primary = NanoChatPrimary,
    secondary = NanoChatSecondary,
    background = NanoChatBackground,
    surface = NanoChatSurface
)

/**
 * Custom nanoChat light color scheme (legacy, for fallback)
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Main theme composable for NanoChat app.
 * Uses ThemeManager to provide dynamic color schemes based on user preferences.
 *
 * @param themeManager The theme manager that provides color schemes
 * @param content The content to be themed
 */
@Composable
fun NanoChatTheme(
    themeManager: ThemeManager,
    content: @Composable () -> Unit
) {
    val colorScheme = themeManager.getAppColorScheme()
    val isDarkMode by themeManager.isDarkMode.collectAsState()
    val statusBarColor = themeManager.getStatusBarColor()
    val useLightStatusBarIcons = themeManager.useLightStatusBarIcons()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Set status bar color
            window.statusBarColor = statusBarColor

            // Control status bar icon appearance
            // For dark mode (dark background), we want LIGHT icons
            // For light mode (light background), we want DARK icons
            @Suppress("DEPRECATION")
            val decorView = window.decorView
            @Suppress("DEPRECATION")
            val flags = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (useLightStatusBarIcons) {
                // Light mode: Add light status bar flag (use dark icons)
                decorView.systemUiVisibility = decorView.systemUiVisibility or flags
            } else {
                // Dark mode: Remove light status bar flag (use light icons)
                decorView.systemUiVisibility = decorView.systemUiVisibility and flags.inv()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
