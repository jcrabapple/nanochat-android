package com.nanogpt.chat.ui.auth.setup

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSystemInDarkTheme = isSystemInDarkTheme()

    // Set status bar color based on system theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val statusBarColor = if (isSystemInDarkTheme) {
                android.graphics.Color.parseColor("#1e1e2e") // Dark background
            } else {
                android.graphics.Color.parseColor("#EFF1F5") // Light background
            }
            window.statusBarColor = statusBarColor

            @Suppress("DEPRECATION")
            val decorView = window.decorView
            @Suppress("DEPRECATION")
            val flags = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (isSystemInDarkTheme) {
                // Dark mode: Remove light status bar flag (use light icons)
                decorView.systemUiVisibility = decorView.systemUiVisibility and flags.inv()
            } else {
                // Light mode: Add light status bar flag (use dark icons)
                decorView.systemUiVisibility = decorView.systemUiVisibility or flags
            }
        }
    }

    // Define color schemes based on system theme
    val colorScheme = if (isSystemInDarkTheme) {
        darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFFcba6f7), // Mauve
            secondary = androidx.compose.ui.graphics.Color(0xFFb4befe), // Lavender
            background = androidx.compose.ui.graphics.Color(0xFF1e1e2e), // Dark background
            surface = androidx.compose.ui.graphics.Color(0xFF181825), // Dark surface
            onBackground = androidx.compose.ui.graphics.Color(0xFFcdd6f4), // Text
            onSurface = androidx.compose.ui.graphics.Color(0xFFbac2de), // Subtext
        )
    } else {
        lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF8839ef), // Mauve
            secondary = androidx.compose.ui.graphics.Color(0xFF7287fd), // Lavender
            background = androidx.compose.ui.graphics.Color(0xFFEFF1F5), // Light background
            surface = androidx.compose.ui.graphics.Color(0xFFe6e9ef), // Light surface
            onBackground = androidx.compose.ui.graphics.Color(0xFF4c4f69), // Text
            onSurface = androidx.compose.ui.graphics.Color(0xFF5c5f77), // Subtext
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
            ) {
                Icon(
                    Icons.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    "Welcome to NanoChat",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Text(
                    "Connect to your NanoChat instance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.backendUrl,
                    onValueChange = viewModel::onBackendUrlChange,
                    label = { Text("NanoChat Instance URL") },
                    placeholder = { Text("https://nanochat.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !uiState.isTesting
                )

                OutlinedTextField(
                    value = uiState.apiKey,
                    onValueChange = viewModel::onApiKeyChange,
                    label = { Text("API Key") },
                    placeholder = { Text("nc_...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !uiState.isTesting
                )

                Button(
                    onClick = {
                        viewModel.testConnection {
                            onComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.backendUrl.isNotBlank() && !uiState.isTesting
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Connect")
                    }
                }

                if (uiState.error != null) {
                    Text(
                        uiState.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
