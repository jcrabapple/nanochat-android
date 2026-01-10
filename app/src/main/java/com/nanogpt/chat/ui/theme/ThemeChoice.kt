package com.nanogpt.chat.ui.theme

/**
 * Represents available theme choices for the application.
 * Each theme has a display name and indicates whether it's a dark or light theme.
 */
enum class ThemeChoice(val displayName: String, val isDark: Boolean) {
    MATERIAL_YOU_LIGHT("Material You (Light)", false),
    LATTE("Catppuccin Latte", false),
    MATERIAL_YOU_DARK("Material You (Dark)", true),
    FRAPPE("Catppuccin Frappé", true),
    MACCHIATO("Catppuccin Macchiato", true),
    MOCHA("Catppuccin Mocha", true);

    companion object {
        /**
         * Returns the default theme for light mode
         */
        fun defaultLightTheme(): ThemeChoice = MATERIAL_YOU_LIGHT

        /**
         * Returns the default theme for dark mode
         */
        fun defaultDarkTheme(): ThemeChoice = MATERIAL_YOU_DARK

        /**
         * Get ThemeChoice from string name, or default if not found
         */
        fun fromString(value: String?, default: ThemeChoice): ThemeChoice {
            return values().find { it.name == value } ?: default
        }

        /**
         * Get all light themes
         */
        fun lightThemes(): List<ThemeChoice> = values().filter { !it.isDark }

        /**
         * Get all dark themes
         */
        fun darkThemes(): List<ThemeChoice> = values().filter { it.isDark }
    }
}
