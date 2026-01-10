package com.nanogpt.chat.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nanogpt.chat.ui.auth.setup.SetupScreen
import com.nanogpt.chat.ui.assistants.AssistantsScreen
import com.nanogpt.chat.ui.chat.ChatScreen
import com.nanogpt.chat.ui.conversations.ConversationsListScreen
import com.nanogpt.chat.ui.projects.ProjectsScreen
import com.nanogpt.chat.ui.settings.SettingsScreen

@Composable
fun NanoChatNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navViewModel: NavViewModel = hiltViewModel()
    var startDestination by remember { mutableStateOf<String?>(null) }

    // Determine start destination
    LaunchedEffect(Unit) {
        startDestination = navViewModel.getStartDestination()
    }

    if (startDestination != null) {
        NavHost(
            navController = navController,
            startDestination = startDestination!!
        ) {
            composable(Screen.Setup.route) {
                SetupScreen(
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
                    themeManager = navViewModel.themeManager
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
                    themeManager = navViewModel.themeManager
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
                    }
                )
            }
        }
    }
}
