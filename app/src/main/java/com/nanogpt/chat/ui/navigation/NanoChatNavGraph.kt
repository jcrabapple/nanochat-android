package com.nanogpt.chat.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.ui.auth.setup.SetupScreen
import com.nanogpt.chat.ui.assistants.AssistantsScreen
import com.nanogpt.chat.ui.chat.ChatScreen
import com.nanogpt.chat.ui.conversations.ConversationsListScreen
import com.nanogpt.chat.ui.projects.ProjectsScreen
import com.nanogpt.chat.ui.settings.SettingsScreen
import com.nanogpt.chat.ui.theme.ThemeManager

@Composable
fun NanoChatNavGraph(
    navController: NavHostController = rememberNavController(),
    themeManager: ThemeManager,
    secureStorage: SecureStorage
) {
    // Determine start destination directly from SecureStorage
    val startDestination = remember {
        val backendUrl = secureStorage.getBackendUrl()
        val apiKey = secureStorage.getSessionToken()

        when {
            backendUrl == null || apiKey == null -> Screen.Setup.route
            else -> {
                val lastConversationId = secureStorage.getLastConversationId()
                if (lastConversationId != null) {
                    Screen.Chat.createRoute(lastConversationId)
                } else {
                    "chat" // New chat
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(
                themeManager = themeManager,
                onComplete = {
                    navController.navigate("chat") {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        // Route for new conversation (no ID)
        composable(
            route = "chat"
        ) {
            ChatScreen(
                conversationId = null,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToConversation = { convId ->
                    if (convId != null) {
                        navController.navigate(Screen.Chat.createRoute(convId)) {
                            popUpTo("chat") { inclusive = true }
                        }
                    } else {
                        navController.navigate("chat") {
                            popUpTo("chat") { inclusive = true }
                        }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAssistants = {
                    navController.navigate(Screen.Assistants.route)
                },
                onNavigateToProjects = {
                    navController.navigate(Screen.Projects.route)
                },
                themeManager = themeManager
            )
        }

        // Route for existing conversation (with ID)
        composable(
            route = "chat/{conversationId}"
        ) {
            ChatScreen(
                conversationId = it.arguments?.getString("conversationId") ?: "",
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToConversation = { convId ->
                    if (convId != null) {
                        navController.navigate(Screen.Chat.createRoute(convId)) {
                            popUpTo("chat/{conversationId}") { inclusive = true }
                        }
                    } else {
                        navController.navigate("chat") {
                            popUpTo("chat/{conversationId}") { inclusive = true }
                        }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAssistants = {
                    navController.navigate(Screen.Assistants.route)
                },
                onNavigateToProjects = {
                    navController.navigate(Screen.Projects.route)
                },
                themeManager = themeManager
            )
        }

        composable(Screen.Assistants.route) {
            AssistantsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Projects.route) {
            ProjectsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAssistants = {
                    navController.navigate(Screen.Assistants.route)
                }
            )
        }
    }
}
