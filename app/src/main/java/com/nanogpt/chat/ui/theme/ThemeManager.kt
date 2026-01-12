package com.nanogpt.chat.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.nanogpt.chat.data.local.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages theme preferences and provides color schemes for the application.
 * This is a singleton that persists theme choices and provides reactive state.
 */
@Singleton
class ThemeManager @Inject constructor(
    private val storage: SecureStorage
) {
    // StateFlow for reactive UI updates
    // Use lazy initialization to avoid issues during Hilt injection
    private val _isDarkMode = MutableStateFlow<Boolean>(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private var initialized = false
    private val lock = Any()

    private fun ensureInitialized() {
        synchronized(lock) {
            if (!initialized) {
                val savedPreference = storage.getUseDarkMode()
                _isDarkMode.value = savedPreference ?: isSystemInDarkTheme(storage)
                initialized = true
            }
        }
    }

    private val _lightTheme = MutableStateFlow(
        ThemeChoice.fromString(storage.getLightTheme(), ThemeChoice.defaultLightTheme())
    )
    val lightTheme: StateFlow<ThemeChoice> = _lightTheme.asStateFlow()

    private val _darkTheme = MutableStateFlow(
        ThemeChoice.fromString(storage.getDarkTheme(), ThemeChoice.defaultDarkTheme())
    )
    val darkTheme: StateFlow<ThemeChoice> = _darkTheme.asStateFlow()

    /**
     * Get the color scheme for the current theme configuration.
     * This should be called from a Composable context.
     */
    @Composable
    fun getAppColorScheme(): ColorScheme {
        // Ensure initialization before accessing theme
        ensureInitialized()

        val context = LocalContext.current
        val isDark by isDarkMode.collectAsState()
        val lightTheme by lightTheme.collectAsState()
        val darkTheme by darkTheme.collectAsState()
        val currentTheme = if (isDark) darkTheme else lightTheme

        return when {
            // Material You - Dark mode
            isDark && currentTheme == ThemeChoice.MATERIAL_YOU_DARK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                dynamicDarkColorScheme(context)
            }
            // Material You - Light mode
            !isDark && currentTheme == ThemeChoice.MATERIAL_YOU_LIGHT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                dynamicLightColorScheme(context)
            }
            // Material You - Fallback for older Android versions
            currentTheme == ThemeChoice.MATERIAL_YOU_DARK -> {
                androidx.compose.material3.darkColorScheme(
                    primary = NanoChatPrimary,
                    secondary = NanoChatSecondary,
                    background = NanoChatBackground,
                    surface = NanoChatSurface
                )
            }
            currentTheme == ThemeChoice.MATERIAL_YOU_LIGHT -> {
                androidx.compose.material3.lightColorScheme(
                    primary = Purple40,
                    secondary = PurpleGrey40,
                    tertiary = Pink40
                )
            }
            // Tokyo Night themes
            else -> {
                // Try Tokyo Night first
                getTokyoNightColorScheme(currentTheme) ?:
                // Fall back to Catppuccin
                getCatppuccinColorScheme(currentTheme) ?: run {
                    // Final fallback to default if something goes wrong
                    if (isDark) {
                        androidx.compose.material3.darkColorScheme(
                            primary = NanoChatPrimary,
                            secondary = NanoChatSecondary,
                            background = NanoChatBackground,
                            surface = NanoChatSurface
                        )
                    } else {
                        androidx.compose.material3.lightColorScheme(
                            primary = Purple40,
                            secondary = PurpleGrey40,
                            tertiary = Pink40
                        )
                    }
                }
            }
        }
    }

    /**
     * Set the light theme preference
     */
    fun setLightTheme(theme: ThemeChoice) {
        require(!theme.isDark) { "Theme must be a light theme" }
        _lightTheme.value = theme
        storage.saveLightTheme(theme.name)
    }

    /**
     * Set the dark theme preference
     */
    fun setDarkTheme(theme: ThemeChoice) {
        require(theme.isDark) { "Theme must be a dark theme" }
        _darkTheme.value = theme
        storage.saveDarkTheme(theme.name)
    }

    /**
     * Toggle between light and dark mode
     */
    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        storage.saveUseDarkMode(newValue)
    }

    /**
     * Set dark mode directly
     */
    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        storage.saveUseDarkMode(isDark)
    }

    /**
     * Get status bar color for the current theme
     */
    @Composable
    fun getStatusBarColor(): Int {
        val isDark by isDarkMode.collectAsState()
        return if (isDark) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }
    }

    /**
     * Check if we should use light status bar icons (for light themes)
     */
    @Composable
    fun useLightStatusBarIcons(): Boolean {
        return !isDarkMode.value
    }

    /**
     * Detect if the system is currently in dark mode
     */
    private fun isSystemInDarkTheme(storage: SecureStorage): Boolean {
        return try {
            val context = storage.getContext()
            val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        } catch (e: Exception) {
            false // Default to light mode if we can't detect
        }
    }

    /**
     * Setup the window insets and status bar for the given activity
     */
    fun setupWindowInsets(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }
}
