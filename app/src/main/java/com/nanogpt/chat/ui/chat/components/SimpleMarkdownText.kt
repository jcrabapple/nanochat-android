package com.nanogpt.chat.ui.chat.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Simple markdown text renderer without external dependencies.
 * Supports basic formatting: bold, italic, code, and line breaks.
 */
@Composable
fun SimpleMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val text = markdown

        while (currentIndex < text.length) {
            when {
                // Bold: **text**
                text.substring(currentIndex).startsWith("**") -> {
                    val endIndex = text.indexOf("**", currentIndex + 2)
                    if (endIndex != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(currentIndex + 2, endIndex))
                        }
                        currentIndex = endIndex + 2
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Italic: *text*
                text.substring(currentIndex).startsWith("*") -> {
                    val endIndex = text.indexOf("*", currentIndex + 1)
                    if (endIndex != -1) {
                        withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                            append(text.substring(currentIndex + 1, endIndex))
                        }
                        currentIndex = endIndex + 1
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Code: `text`
                text.substring(currentIndex).startsWith("`") -> {
                    val endIndex = text.indexOf("`", currentIndex + 1)
                    if (endIndex != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            append(text.substring(currentIndex + 1, endIndex))
                        }
                        currentIndex = endIndex + 1
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Code block: ```text```
                text.substring(currentIndex).startsWith("```") -> {
                    val endIndex = text.indexOf("```", currentIndex + 3)
                    if (endIndex != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            append(text.substring(currentIndex + 3, endIndex).trimMargin())
                        }
                        currentIndex = endIndex + 3
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Line break
                text[currentIndex] == '\n' -> {
                    append("\n")
                    currentIndex++
                }
                else -> {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
        }
    }

    Text(
        annotatedString,
        modifier = modifier
    )
}
