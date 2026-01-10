package com.nanogpt.chat.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Catppuccin color palettes mapped to Material 3 color schemes.
 * Reference: https://github.com/catppuccin/catppuccin
 */

// ===== LATTE (Light) =====
private val LatteRosewater = Color(0xFFdc8a78)
private val LatteFlamingo = Color(0xFFdd7878)
private val LattePink = Color(0xFFea76cb)
private val LatteMauve = Color(0xFF8839ef)
private val LatteRed = Color(0xFFd20f39)
private val LatteMaroon = Color(0xFFe64553)
private val LattePeach = Color(0xFFfe640b)
private val LatteYellow = Color(0xFFdf8e1d)
private val LatteGreen = Color(0xFF40a02b)
private val LatteTeal = Color(0xFF179299)
private val LatteSky = Color(0xFF04a5e5)
private val LatteSapphire = Color(0xFF209fb5)
private val LatteBlue = Color(0xFF1e66f5)
private val LatteLavender = Color(0xFF7287fd)

private val LatteText = Color(0xFF4c4f69)
private val LatteSubtext1 = Color(0xFF5c5f77)
private val LatteSubtext0 = Color(0xFF6c6f85)
private val LatteOverlay2 = Color(0xFF7c7f93)
private val LatteOverlay1 = Color(0xFF8c8fa1)
private val LatteOverlay0 = Color(0xFF9ca0b0)
private val LatteSurface2 = Color(0xFFacb0be)
private val LatteSurface1 = Color(0xFFbcc0cc)
private val LatteSurface0 = Color(0xFFccd0da)
private val LatteBase = Color(0xFFeff1f5)
private val LatteMantle = Color(0xFFe6e9ef)
private val LatteCrust = Color(0xFFdce0e8)

// ===== FRAPPE (Dark) =====
private val FrappeRosewater = Color(0xFFf2d5cf)
private val FrappeFlamingo = Color(0xFFeebebe)
private val FrappePink = Color(0xFFf4b8e4)
private val FrappeMauve = Color(0xFFca9ee6)
private val FrappeRed = Color(0xFFe78284)
private val FrappeMaroon = Color(0xFFea999c)
private val FrappePeach = Color(0xFFef9f76)
private val FrappeYellow = Color(0xFFe5c890)
private val FrappeGreen = Color(0xFFa6d189)
private val FrappeTeal = Color(0xFF81c8be)
private val FrappeSky = Color(0xFF99d1db)
private val FrappeSapphire = Color(0xFF85c1dc)
private val FrappeBlue = Color(0xFF8caaee)
private val FrappeLavender = Color(0xFFbabbf1)

private val FrappeText = Color(0xFFc6d0f5)
private val FrappeSubtext1 = Color(0xFFb5bfe2)
private val FrappeSubtext0 = Color(0xFFa5adce)
private val FrappeOverlay2 = Color(0xFF949cbb)
private val FrappeOverlay1 = Color(0xFF838ba7)
private val FrappeOverlay0 = Color(0xFF737994)
private val FrappeSurface2 = Color(0xFF626880)
private val FrappeSurface1 = Color(0xFF51576d)
private val FrappeSurface0 = Color(0xFF414559)
private val FrappeBase = Color(0xFF303446)
private val FrappeMantle = Color(0xFF292c3c)
private val FrappeCrust = Color(0xFF232634)

// ===== MACCHIATO (Dark) =====
private val MacchiatoRosewater = Color(0xFFf4dbd6)
private val MacchiatoFlamingo = Color(0xFFf0c6c6)
private val MacchiatoPink = Color(0xFFf5bde6)
private val MacchiatoMauve = Color(0xFFc6a0f6)
private val MacchiatoRed = Color(0xFFed8796)
private val MacchiatoMaroon = Color(0xFFee99a0)
private val MacchiatoPeach = Color(0xFFf5a97f)
private val MacchiatoYellow = Color(0xFFeed49f)
private val MacchiatoGreen = Color(0xFFa6da95)
private val MacchiatoTeal = Color(0xFF8bd5ca)
private val MacchiatoSky = Color(0xFF91d7e3)
private val MacchiatoSapphire = Color(0xFF7dc4e4)
private val MacchiatoBlue = Color(0xFF8aadf4)
private val MacchiatoLavender = Color(0xFFb7bdf8)

private val MacchiatoText = Color(0xFFcad3f5)
private val MacchiatoSubtext1 = Color(0xFFb8c0e0)
private val MacchiatoSubtext0 = Color(0xFFa5adcb)
private val MacchiatoOverlay2 = Color(0xFF939ab7)
private val MacchiatoOverlay1 = Color(0xFF8087a2)
private val MacchiatoOverlay0 = Color(0xFF6e738d)
private val MacchiatoSurface2 = Color(0xFF5b6078)
private val MacchiatoSurface1 = Color(0xFF494d64)
private val MacchiatoSurface0 = Color(0xFF363a4f)
private val MacchiatoBase = Color(0xFF24273a)
private val MacchiatoMantle = Color(0xFF1e2030)
private val MacchiatoCrust = Color(0xFF181926)

// ===== MOCHA (Dark) =====
private val MochaRosewater = Color(0xFFf5e0dc)
private val MochaFlamingo = Color(0xFFf2cdcd)
private val MochaPink = Color(0xFFf5c2e7)
private val MochaMauve = Color(0xFFcba6f7)
private val MochaRed = Color(0xFFf38ba8)
private val MochaMaroon = Color(0xFFeba0ac)
private val MochaPeach = Color(0xFFfab387)
private val MochaYellow = Color(0xFFf9e2af)
private val MochaGreen = Color(0xFFa6e3a1)
private val MochaTeal = Color(0xFF94e2d5)
private val MochaSky = Color(0xFF89dceb)
private val MochaSapphire = Color(0xFF74c7ec)
private val MochaBlue = Color(0xFF89b4fa)
private val MochaLavender = Color(0xFFb4befe)

private val MochaText = Color(0xFFcdd6f4)
private val MochaSubtext1 = Color(0xFFbac2de)
private val MochaSubtext0 = Color(0xFFa6adc8)
private val MochaOverlay2 = Color(0xFF9399b2)
private val MochaOverlay1 = Color(0xFF7f849c)
private val MochaOverlay0 = Color(0xFF6c7086)
private val MochaSurface2 = Color(0xFF585b70)
private val MochaSurface1 = Color(0xFF45475a)
private val MochaSurface0 = Color(0xFF313244)
private val MochaBase = Color(0xFF1e1e2e)
private val MochaMantle = Color(0xFF181825)
private val MochaCrust = Color(0xFF11111b)

/**
 * Light theme color scheme for Catppuccin Latte
 */
val LatteLightColorScheme: ColorScheme = lightColorScheme(
    primary = LatteMauve,
    onPrimary = LatteBase,
    primaryContainer = Color(0xFF9D8CD1),
    onPrimaryContainer = LatteBase,

    secondary = LatteLavender,
    onSecondary = LatteBase,
    secondaryContainer = Color(0xFFB6BCF2),
    onSecondaryContainer = LatteBase,

    tertiary = LattePink,
    onTertiary = LatteBase,
    tertiaryContainer = Color(0xFFF2B3DE),
    onTertiaryContainer = LatteBase,

    background = LatteBase,
    onBackground = LatteText,

    surface = LatteMantle,
    onSurface = LatteSubtext1,

    surfaceVariant = LatteSurface0,
    onSurfaceVariant = LatteSubtext0,

    outline = LatteOverlay1,
    outlineVariant = LatteSurface2,

    error = LatteRed,
    onError = LatteBase,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = LatteRed
)

/**
 * Dark theme color scheme for Catppuccin Frappé
 */
val FrappeDarkColorScheme: ColorScheme = darkColorScheme(
    primary = FrappeMauve,
    onPrimary = FrappeBase,
    primaryContainer = Color(0xFFB3A8D4),
    onPrimaryContainer = FrappeBase,

    secondary = FrappeLavender,
    onSecondary = FrappeBase,
    secondaryContainer = Color(0xFF989EBA),
    onSecondaryContainer = FrappeBase,

    tertiary = FrappeRosewater,
    onTertiary = FrappeBase,
    tertiaryContainer = Color(0xFFC9AA9E),
    onTertiaryContainer = FrappeBase,

    background = FrappeBase,
    onBackground = FrappeText,

    surface = FrappeMantle,
    onSurface = FrappeSubtext1,

    surfaceVariant = FrappeSurface0,
    onSurfaceVariant = FrappeSubtext0,

    outline = FrappeOverlay1,
    outlineVariant = FrappeSurface2,

    error = FrappeRed,
    onError = FrappeBase,
    errorContainer = Color(0xFF9F2B33),
    onErrorContainer = FrappeRosewater
)

/**
 * Dark theme color scheme for Catppuccin Macchiato
 */
val MacchiatoDarkColorScheme: ColorScheme = darkColorScheme(
    primary = MacchiatoMauve,
    onPrimary = MacchiatoBase,
    primaryContainer = Color(0xFFA888C9),
    onPrimaryContainer = MacchiatoBase,

    secondary = MacchiatoLavender,
    onSecondary = MacchiatoBase,
    secondaryContainer = Color(0xFF9295C2),
    onSecondaryContainer = MacchiatoBase,

    tertiary = MacchiatoRosewater,
    onTertiary = MacchiatoBase,
    tertiaryContainer = Color(0xFFCAACA7),
    onTertiaryContainer = MacchiatoBase,

    background = MacchiatoBase,
    onBackground = MacchiatoText,

    surface = MacchiatoMantle,
    onSurface = MacchiatoSubtext1,

    surfaceVariant = MacchiatoSurface0,
    onSurfaceVariant = MacchiatoSubtext0,

    outline = MacchiatoOverlay1,
    outlineVariant = MacchiatoSurface2,

    error = MacchiatoRed,
    onError = MacchiatoBase,
    errorContainer = Color(0xFFA8403E),
    onErrorContainer = MacchiatoRosewater
)

/**
 * Dark theme color scheme for Catppuccin Mocha
 */
val MochaDarkColorScheme: ColorScheme = darkColorScheme(
    primary = MochaMauve,
    onPrimary = MochaBase,
    primaryContainer = Color(0xFFA28BC9),
    onPrimaryContainer = MochaBase,

    secondary = MochaLavender,
    onSecondary = MochaBase,
    secondaryContainer = Color(0xFF8F92BF),
    onSecondaryContainer = MochaBase,

    tertiary = MochaRosewater,
    onTertiary = MochaBase,
    tertiaryContainer = Color(0xFFC9B9B7),
    onTertiaryContainer = MochaBase,

    background = MochaBase,
    onBackground = MochaText,

    surface = MochaMantle,
    onSurface = MochaSubtext1,

    surfaceVariant = MochaSurface0,
    onSurfaceVariant = MochaSubtext0,

    outline = MochaOverlay1,
    outlineVariant = MochaSurface2,

    error = MochaRed,
    onError = MochaBase,
    errorContainer = Color(0xFFA63B40),
    onErrorContainer = MochaRosewater
)

/**
 * Returns the appropriate Catppuccin color scheme for the given theme choice.
 * Returns null for Material You themes (those are handled dynamically).
 */
fun getCatppuccinColorScheme(theme: ThemeChoice): ColorScheme? {
    return when (theme) {
        ThemeChoice.LATTE -> LatteLightColorScheme
        ThemeChoice.FRAPPE -> FrappeDarkColorScheme
        ThemeChoice.MACCHIATO -> MacchiatoDarkColorScheme
        ThemeChoice.MOCHA -> MochaDarkColorScheme
        else -> null // Material You themes handled separately
    }
}
