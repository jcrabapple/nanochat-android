package com.nanogpt.chat.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.nanogpt.chat.data.sync.ConversationSyncWorker
import com.nanogpt.chat.ui.navigation.NanoChatNavGraph
import com.nanogpt.chat.ui.theme.NanoChatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure content doesn't flow under system bars
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Schedule background conversation sync worker
        ConversationSyncWorker.schedule(this)

        setContent {
            NanoChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NanoChatNavGraph()
                }
            }
        }
    }
}
