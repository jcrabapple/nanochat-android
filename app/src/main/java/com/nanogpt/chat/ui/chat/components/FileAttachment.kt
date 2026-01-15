package com.nanogpt.chat.ui.chat.components

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Immutable

@Immutable
data class FileAttachment(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uploadedUrl: String? = null,
    val storageId: String? = null,
    val uploadError: String? = null,
    val estimatedTokens: Int? = null,
    val fileType: FileType? = null
) {
    val isUploaded: Boolean
        get() = uploadedUrl != null && storageId != null

    val isUploading: Boolean
        get() = uploadError == null && !isUploaded

    val hasError: Boolean
        get() = uploadError != null

    companion object {
        // Maximum file size: 10MB
        const val MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L

        // Supported MIME types
        private val SUPPORTED_MIME_TYPES = setOf(
            // Text files
            "text/plain",
            "text/csv",
            "text/markdown",
            "text/md",
            "application/json",
            // PDF
            "application/pdf",
            // EPUB
            "application/epub+zip"
        )

        fun isValidMimeType(mimeType: String): Boolean {
            return SUPPORTED_MIME_TYPES.contains(mimeType.lowercase())
        }

        fun isValidFileSize(sizeBytes: Long): Boolean {
            return sizeBytes <= MAX_FILE_SIZE_BYTES
        }

        fun getFileType(mimeType: String, fileName: String): FileType? {
            return when {
                mimeType.contains("pdf") -> FileType.PDF
                mimeType.contains("epub") -> FileType.EPUB
                mimeType.contains("markdown") || mimeType.contains("text/md") ||
                fileName.endsWith(".md", ignoreCase = true) -> FileType.MARKDOWN
                mimeType.contains("csv") || fileName.endsWith(".csv", ignoreCase = true) -> FileType.CSV
                mimeType.contains("json") || fileName.endsWith(".json", ignoreCase = true) -> FileType.JSON
                mimeType.contains("text") || fileName.endsWith(".txt", ignoreCase = true) -> FileType.TEXT
                else -> null
            }
        }

        /**
         * Estimate token count for text content.
         * Rough approximation: 1 token ≈ 4 characters for English text.
         * This is a conservative estimate that may overcount slightly.
         */
        fun estimateTokens(text: String): Int {
            // Simple character-based estimation
            return (text.length / 4).coerceAtLeast(1)
        }

        /**
         * Get file extension from MIME type
         */
        fun getExtension(mimeType: String): String {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            return extension ?: when {
                mimeType.contains("markdown") || mimeType.contains("text/md") -> "md"
                mimeType.contains("json") -> "json"
                else -> "txt"
            }
        }
    }
}

enum class FileType(val displayName: String, val supportsText: Boolean = true) {
    PDF("PDF", false),
    EPUB("EPUB", false),
    MARKDOWN("Markdown", true),
    TEXT("Text", true),
    CSV("CSV", true),
    JSON("JSON", true)
}
