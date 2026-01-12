package com.nanogpt.chat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.nanogpt.chat.data.local.SecureStorage
import com.nanogpt.chat.data.sync.ConversationSyncWorker
import com.nanogpt.chat.ui.navigation.NanoChatNavGraph
import com.nanogpt.chat.ui.theme.NanoChatTheme
import com.nanogpt.chat.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var secureStorage: SecureStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure content doesn't flow under system bars
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Schedule background conversation sync worker
        ConversationSyncWorker.schedule(this)

        setContent {
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
}
