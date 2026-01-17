package com.nanogpt.chat.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Conversations : Screen("conversations")
    object Chat : Screen("chat") {
        fun createRoute(conversationId: String?) = if (conversationId != null) "chat/$conversationId" else "chat"
    }
    object Assistants : Screen("assistants")
    object Projects : Screen("projects")
    object ProjectFiles : Screen("projectFiles") {
        fun createRoute(projectId: String) = "projectFiles/$projectId"
    }
    object ProjectMembers : Screen("projectMembers") {
        fun createRoute(projectId: String) = "projectMembers/$projectId"
    }
    object Settings : Screen("settings")
}
