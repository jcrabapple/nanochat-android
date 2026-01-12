package com.nanogpt.chat.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import com.nanogpt.chat.BuildConfig
import com.nanogpt.chat.data.local.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Utility class for collecting and sharing debug logs.
 * Used for troubleshooting crashes and connection issues.
 */
class DebugLogger @Inject constructor(
    private val context: Context,
    private val secureStorage: SecureStorage
) {

    companion object {
        private const val TAG = "NanoChatDebug"
        private const val MAX_LOG_LINES = 2000 // Limit to prevent OOM
    }

    /**
     * Collect comprehensive debug information
     */
    suspend fun collectDebugLogs(): String = withContext(Dispatchers.IO) {
        try {
            val logs = StringBuilder()

            // Header
            logs.append("=" .repeat(80))
            logs.append("\nNanoChat Mobile Debug Report\n")
            logs.append("Generated: ${getCurrentTimestamp()}\n")
            logs.append("=".repeat(80))
            logs.append("\n\n")

            // App Information
            logs.append("## APP INFORMATION\n")
            logs.append("-".repeat(80))
            logs.append("\n")
            logs.append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
            logs.append("Package: ${BuildConfig.APPLICATION_ID}\n")
            logs.append("Build Type: ${BuildConfig.BUILD_TYPE}\n")
            logs.append("Debug Build: ${BuildConfig.DEBUG}\n")
            logs.append("\n")

            // Device Information
            logs.append("## DEVICE INFORMATION\n")
            logs.append("-".repeat(80))
            logs.append("\n")
            logs.append("Manufacturer: ${Build.MANUFACTURER}\n")
            logs.append("Model: ${Build.MODEL}\n")
            logs.append("Device: ${Build.DEVICE}\n")
            logs.append("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            // Skip serial number - requires special permissions
            logs.append("\n")

            // Configuration
            logs.append("## APP CONFIGURATION\n")
            logs.append("-".repeat(80))
            logs.append("\n")
            try {
                val backendUrl = secureStorage.getBackendUrl()
                val hasSessionToken = !secureStorage.getSessionToken().isNullOrEmpty()
                val userId = secureStorage.getUserId()

                logs.append("Backend URL: ${backendUrl ?: "Not configured"}\n")
                logs.append("Has Session Token: $hasSessionToken\n")
                logs.append("User ID: ${userId ?: "Not logged in"}\n")
            } catch (e: Exception) {
                logs.append("Error reading configuration: ${e.message}\n")
            }
            logs.append("\n")

            // System Logs (last N lines)
            logs.append("## SYSTEM LOGS (last $MAX_LOG_LINES lines)\n")
            logs.append("-".repeat(80))
            logs.append("\n")

            try {
                val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                var lines = 0
                val allLines = mutableListOf<String>()
                reader.useLines { sequence ->
                    sequence.forEach { line ->
                        if (line.contains("nanogpt", ignoreCase = true) ||
                            line.contains("FATAL", ignoreCase = true) ||
                            line.contains("AndroidRuntime", ignoreCase = true) ||
                            line.contains("System.err", ignoreCase = true) ||
                            line.contains("Exception", ignoreCase = true)) {
                            allLines.add(line)
                        }
                    }
                }

                // Take last MAX_LOG_LINES
                val recentLogs = allLines.takeLast(MAX_LOG_LINES)
                recentLogs.forEach { logs.append(it).append("\n") }

                if (allLines.size > recentLogs.size) {
                    logs.append("\n... (${allLines.size - recentLogs.size} more lines filtered)\n")
                }
            } catch (e: SecurityException) {
                logs.append("Unable to read system logs: Missing READ_LOGS permission\n")
                logs.append("This is normal for release builds. Please run: adb logcat -d > logs.txt\n")
            } catch (e: Exception) {
                logs.append("Error reading logs: ${e.message}\n")
                logs.append("Stack trace: ${Log.getStackTraceString(e)}\n")
            }
            logs.append("\n")

            // Footer
            logs.append("=".repeat(80))
            logs.append("\nEnd of Debug Report\n")
            logs.append("=".repeat(80))

            logs.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting debug logs", e)
            "Error collecting debug logs: ${Log.getStackTraceString(e)}"
        }
    }

    /**
     * Share debug logs via intent
     */
    fun shareDebugLogs(logs: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "NanoChat Mobile Debug Logs")
                putExtra(
                    Intent.EXTRA_TEXT,
                    logs
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Create a chooser to let user select app
            val chooser = Intent.createChooser(intent, "Share Debug Logs")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing logs", e)
            // Fallback: copy to clipboard would go here
        }
    }

    /**
     * Get current timestamp as formatted string
     */
    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return formatter.format(Date())
    }

    /**
     * Log error with context for debugging
     */
    fun logError(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
        // Could also write to file here for persistent logging
    }

    /**
     * Log warning with context
     */
    fun logWarning(tag: String, message: String) {
        Log.w(tag, message)
    }

    /**
     * Log info message
     */
    fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
    }
}

/**
 * Extension function to repeat strings
 */
private operator fun String.times(count: Int): String {
    return this.repeat(count)
}
