package com.nanogpt.chat.ui.auth.setup

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanogpt.chat.ui.theme.ThemeManager

@Composable
fun SetupScreen(
    onComplete: () -> Unit
) {
    val viewModel: SetupViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    // Get theme from ViewModel's themeManager reference
    val colorScheme = viewModel.themeManager.getAppColorScheme()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.background
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
                tint = colorScheme.primary
            )

            Text(
                "Welcome to NanoChat",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                "Connect to your NanoChat instance",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
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
                        color = colorScheme.onPrimary
                    )
                } else {
                    Text("Connect")
                }
            }

            if (uiState.error != null) {
                Text(
                    uiState.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
