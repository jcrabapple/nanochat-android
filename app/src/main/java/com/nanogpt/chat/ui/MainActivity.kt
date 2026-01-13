package com.nanogpt.chat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.repository.AssistantRepository
import com.nanogpt.chat.data.sync.ConversationSyncWorker
import com.nanogpt.chat.ui.navigation.NanoChatNavGraph
import com.nanogpt.chat.ui.theme.NanoChatTheme
import com.nanogpt.chat.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var secureStorage: SecureStorage

    @Inject
    lateinit var assistantRepository: AssistantRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure content doesn't flow under system bars
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Schedule background conversation sync worker
        ConversationSyncWorker.schedule(this)

        // Sync assistants from backend at launch
        lifecycleScope.launch {
            assistantRepository.refreshAssistants()
        }

        setContent {
            var isDarkMode by mutableStateOf(themeManager.isDarkMode.value)

            // Listen for theme changes and update status bar
            LaunchedEffect(themeManager.isDarkMode) {
                themeManager.isDarkMode.collect { isDark ->
                    isDarkMode = isDark
                    setStatusBarAppearance(isDark)
                }
            }

            NanoChatTheme(themeManager) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NanoChatNavGraph(
                        themeManager = themeManager,
                        secureStorage = secureStorage
                    )
                }
            }
        }
    }

    private fun setStatusBarAppearance(isDark: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        // isAppearanceLightStatusBars = true means dark icons (for light backgrounds)
        // isAppearanceLightStatusBars = false means light icons (for dark backgrounds)
        controller.isAppearanceLightStatusBars = !isDark
    }
}
