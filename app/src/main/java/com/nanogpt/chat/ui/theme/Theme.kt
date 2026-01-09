package com.nanogpt.chat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NanoChatPrimary,
    secondary = NanoChatSecondary,
    background = NanoChatBackground,
    surface = NanoChatSurface
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun NanoChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalView.current.context
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Use fully opaque status bar color
            val statusBarColor = if (darkTheme) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
            window.statusBarColor = statusBarColor

            // Control status bar icon appearance
            // For dark mode (dark background), we want LIGHT icons
            // For light mode (light background), we want DARK icons
            @Suppress("DEPRECATION")
            val decorView = window.decorView
            @Suppress("DEPRECATION")
            val flags = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (darkTheme) {
                // Dark mode: Remove light status bar flag (use dark/light icons based on system)
                decorView.systemUiVisibility = decorView.systemUiVisibility and flags.inv()
            } else {
                // Light mode: Add light status bar flag (use dark icons)
                decorView.systemUiVisibility = decorView.systemUiVisibility or flags
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
