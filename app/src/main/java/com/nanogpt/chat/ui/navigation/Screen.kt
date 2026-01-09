package com.nanogpt.chat.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Conversations : Screen("conversations")
    object Chat : Screen("chat") {
        fun createRoute(conversationId: String?) = if (conversationId != null) "chat/$conversationId" else "chat"
    }
    object Assistants : Screen("assistants")
    object Projects : Screen("projects")
    object Settings : Screen("settings")
}
