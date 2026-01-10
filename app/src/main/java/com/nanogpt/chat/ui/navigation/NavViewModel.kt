package com.nanogpt.chat.ui.navigation

import androidx.lifecycle.ViewModel
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.ui.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    val themeManager: ThemeManager
) : ViewModel() {

    fun getStartDestination(): String {
        val backendUrl = secureStorage.getBackendUrl()
        val apiKey = secureStorage.getSessionToken()

        return when {
            backendUrl == null || apiKey == null -> Screen.Setup.route
            else -> {
                // Navigate to the last conversation or start a new chat
                val lastConversationId = secureStorage.getLastConversationId()
                if (lastConversationId != null) {
                    Screen.Chat.createRoute(lastConversationId)
                } else {
                    "chat" // New chat
                }
            }
        }
    }
}
