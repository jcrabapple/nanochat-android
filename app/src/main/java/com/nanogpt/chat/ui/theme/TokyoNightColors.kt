package com.nanogpt.chat.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Tokyo Night color palettes mapped to Material 3 color schemes.
 * Reference: https://github.com/tokyo-night/tokyo-night-vscode-theme
 */

// ===== TOKYO NIGHT LIGHT =====
private val TokyoNightLightBlue = Color(0xFF2E58EB)
private val TokyoNightLightBlue0 = Color(0xFF2E58EB)
private val TokyoNightLightBlue1 = Color(0xFF0066CC)
private val TokyoNightLightCyan = Color(0xFF008080)
private val TokyoNightLightMagenta = Color(0xFFBB00BB)
private val TokyoNightLightPurple = Color(0xFF800080)
private val TokyoNightLightRed = Color(0xFFCC0052)
private val TokyoNightLightRed1 = Color(0xFFCC0052)
private val TokyoNightLightGreen = Color(0xFF006633)
private val TokyoNightLightGreen1 = Color(0xFF008060)
private val TokyoNightLightGreen2 = Color(0xFF009977)
private val TokyoNightLightOrange = Color(0xFFE67300)
private val TokyoNightLightYellow = Color(0xFFB28800)
private val TokyoNightLightTeal = Color(0xFF009999)

private val TokyoNightLightBg = Color(0xFFE1E2E7)
private val TokyoNightLightBgDark = Color(0xFFD4D5DD)
private val TokyoNightLightBgHighlight = Color(0xFFC8CAD4)
private val TokyoNightLightFg = Color(0xFF373A43)
private val TokyoNightLightFgDark = Color(0xFF4B4E58)
private val TokyoNightLightFgGutter = Color(0xFF8E92A8)
private val TokyoNightLightComment = Color(0xFF9699A8)
private val TokyoNightLightDark3 = Color(0xFF8E92A8)
private val TokyoNightLightDark5 = Color(0xFF595D78)

// ===== TOKYO NIGHT (DARK) =====
private val TokyoNightBlue = Color(0xFF7AA2F7)
private val TokyoNightBlue0 = Color(0xFF3D59A1)
private val TokyoNightBlue1 = Color(0xFF2AC3DE)
private val TokyoNightBlue2 = Color(0xFF0DB9D7)
private val TokyoNightBlue5 = Color(0xFF89DDFF)
private val TokyoNightBlue6 = Color(0xFFB4F9F8)
private val TokyoNightBlue7 = Color(0xFF394B70)
private val TokyoNightCyan = Color(0xFF7DCFFF)
private val TokyoNightMagenta = Color(0xFFBB9AF7)
private val TokyoNightMagenta2 = Color(0xFFFF007C)
private val TokyoNightPurple = Color(0xFF9D7CD8)
private val TokyoNightRed = Color(0xFFF7768E)
private val TokyoNightRed1 = Color(0xFFDB4B4B)
private val TokyoNightGreen = Color(0xFF9ECE6A)
private val TokyoNightGreen1 = Color(0xFF73DACA)
private val TokyoNightGreen2 = Color(0xFF41A6B5)
private val TokyoNightOrange = Color(0xFFFF9E64)
private val TokyoNightYellow = Color(0xFFE0AF68)
private val TokyoNightTeal = Color(0xFF1ABC9C)

private val TokyoNightBg = Color(0xFF1A1B26)
private val TokyoNightBgDark = Color(0xFF16161E)
private val TokyoNightBgDark1 = Color(0xFF0C0E14)
private val TokyoNightBgHighlight = Color(0xFF292E42)
private val TokyoNightFg = Color(0xFFC0CAF5)
private val TokyoNightFgDark = Color(0xFFA9B1D6)
private val TokyoNightFgGutter = Color(0xFF3B4261)
private val TokyoNightComment = Color(0xFF565F89)
private val TokyoNightDark3 = Color(0xFF545C7E)
private val TokyoNightDark5 = Color(0xFF737AA2)
private val TokyoNightTerminalBlack = Color(0xFF414868)

// ===== TOKYO NIGHT STORM (DARK) =====
private val TokyoStormBlue = Color(0xFF7AA2F7)
private val TokyoStormBlue0 = Color(0xFF3D59A1)
private val TokyoStormBlue1 = Color(0xFF2AC3DE)
private val TokyoStormBlue2 = Color(0xFF0DB9D7)
private val TokyoStormBlue5 = Color(0xFF89DDFF)
private val TokyoStormBlue6 = Color(0xFFB4F9F8)
private val TokyoStormBlue7 = Color(0xFF394B70)
private val TokyoStormCyan = Color(0xFF7DCFFF)
private val TokyoStormMagenta = Color(0xFFBB9AF7)
private val TokyoStormMagenta2 = Color(0xFFFF007C)
private val TokyoStormPurple = Color(0xFF9D7CD8)
private val TokyoStormRed = Color(0xFFF7768E)
private val TokyoStormRed1 = Color(0xFFDB4B4B)
private val TokyoStormGreen = Color(0xFF9ECE6A)
private val TokyoStormGreen1 = Color(0xFF73DACA)
private val TokyoStormGreen2 = Color(0xFF41A6B5)
private val TokyoStormOrange = Color(0xFFFF9E64)
private val TokyoStormYellow = Color(0xFFE0AF68)
private val TokyoStormTeal = Color(0xFF1ABC9C)

private val TokyoStormBg = Color(0xFF24283B)
private val TokyoStormBgDark = Color(0xFF1F2335)
private val TokyoStormBgDark1 = Color(0xFF1B1E2D)
private val TokyoStormBgHighlight = Color(0xFF292E42)
private val TokyoStormFg = Color(0xFFC0CAF5)
private val TokyoStormFgDark = Color(0xFFA9B1D6)
private val TokyoStormFgGutter = Color(0xFF3B4261)
private val TokyoStormComment = Color(0xFF565F89)
private val TokyoStormDark3 = Color(0xFF545C7E)
private val TokyoStormDark5 = Color(0xFF737AA2)
private val TokyoStormTerminalBlack = Color(0xFF414868)

/**
 * Light theme color scheme for Tokyo Night Light
 */
val TokyoNightLightColorScheme: ColorScheme = lightColorScheme(
    primary = TokyoNightLightBlue,
    onPrimary = Color.White,
    primaryContainer = TokyoNightLightBlue0,
    onPrimaryContainer = Color.White,

    secondary = TokyoNightLightCyan,
    onSecondary = Color.White,
    secondaryContainer = TokyoNightLightGreen1,
    onSecondaryContainer = Color.White,

    tertiary = TokyoNightLightMagenta,
    onTertiary = Color.White,
    tertiaryContainer = TokyoNightLightPurple,
    onTertiaryContainer = Color.White,

    background = TokyoNightLightBg,
    onBackground = TokyoNightLightFg,

    surface = TokyoNightLightBgDark,
    onSurface = TokyoNightLightFgDark,

    surfaceVariant = TokyoNightLightBgHighlight,
    onSurfaceVariant = TokyoNightLightFgDark,

    outline = TokyoNightLightDark5,
    outlineVariant = TokyoNightLightDark3,

    error = TokyoNightLightRed,
    onError = Color.White,
    errorContainer = TokyoNightLightRed1,
    onErrorContainer = Color.White
)

/**
 * Dark theme color scheme for Tokyo Night
 */
val TokyoNightDarkColorScheme: ColorScheme = darkColorScheme(
    primary = TokyoNightBlue,
    onPrimary = TokyoNightBg,
    primaryContainer = TokyoNightBlue0,
    onPrimaryContainer = TokyoNightBg,

    secondary = TokyoNightCyan,
    onSecondary = TokyoNightBg,
    secondaryContainer = TokyoNightGreen1,
    onSecondaryContainer = TokyoNightBg,

    tertiary = TokyoNightMagenta,
    onTertiary = TokyoNightBg,
    tertiaryContainer = TokyoNightPurple,
    onTertiaryContainer = TokyoNightBg,

    background = TokyoNightBg,
    onBackground = TokyoNightFg,

    surface = TokyoNightBgDark,
    onSurface = TokyoNightFgDark,

    surfaceVariant = TokyoNightBgHighlight,
    onSurfaceVariant = TokyoNightFgDark,

    outline = TokyoNightDark5,
    outlineVariant = TokyoNightDark3,

    error = TokyoNightRed,
    onError = TokyoNightBg,
    errorContainer = TokyoNightRed1,
    onErrorContainer = TokyoNightBg
)

/**
 * Dark theme color scheme for Tokyo Night Storm
 */
val TokyoNightStormColorScheme: ColorScheme = darkColorScheme(
    primary = TokyoStormBlue,
    onPrimary = TokyoStormBg,
    primaryContainer = TokyoStormBlue0,
    onPrimaryContainer = TokyoStormBg,

    secondary = TokyoStormCyan,
    onSecondary = TokyoStormBg,
    secondaryContainer = TokyoStormGreen1,
    onSecondaryContainer = TokyoStormBg,

    tertiary = TokyoStormMagenta,
    onTertiary = TokyoStormBg,
    tertiaryContainer = TokyoStormPurple,
    onTertiaryContainer = TokyoStormBg,

    background = TokyoStormBg,
    onBackground = TokyoStormFg,

    surface = TokyoStormBgDark,
    onSurface = TokyoStormFgDark,

    surfaceVariant = TokyoStormBgHighlight,
    onSurfaceVariant = TokyoStormFgDark,

    outline = TokyoStormDark5,
    outlineVariant = TokyoStormDark3,

    error = TokyoStormRed,
    onError = TokyoStormBg,
    errorContainer = TokyoStormRed1,
    onErrorContainer = TokyoStormBg
)

/**
 * Returns the appropriate Tokyo Night color scheme for the given theme choice.
 * Returns null for non-Tokyo Night themes.
 */
fun getTokyoNightColorScheme(theme: ThemeChoice): ColorScheme? {
    return when (theme) {
        ThemeChoice.TOKYO_NIGHT_LIGHT -> TokyoNightLightColorScheme
        ThemeChoice.TOKYO_NIGHT -> TokyoNightDarkColorScheme
        ThemeChoice.TOKYO_NIGHT_STORM -> TokyoNightStormColorScheme
        else -> null
    }
}
