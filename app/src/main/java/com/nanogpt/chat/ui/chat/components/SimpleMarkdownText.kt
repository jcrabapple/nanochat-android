package com.nanogpt.chat.ui.chat.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri

/**
 * Enhanced markdown text renderer without external dependencies.
 * Supports headings, bold, italic, code, code blocks, math, tables, block quotes,
 * strikethrough, horizontal rules, checkbox lists, and clickable links.
 */
@Composable
fun SimpleMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    // Normalize various quote characters to standard backtick
    val normalizedMarkdown = markdown
        .replace("\u2018", "`")  // Left single quote
        .replace("\u2019", "`")  // Right single quote
        .replace("\u201C", "\"") // Left double quote
        .replace("\u201D", "\"") // Right double quote
        .replace("\u00B4", "`")  // Acute accent
        .replace("\u2035", "`")  // Backtick
        .replace("\u2032", "`")  // Prime

    val blocks = parseMarkdownBlocks(normalizedMarkdown)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> TerminalCodeBlock(
                    code = block.code,
                    language = block.language
                )
                is MarkdownBlock.MathBlock -> MathBlock(
                    math = block.math,
                    textColor = color
                )
                is MarkdownBlock.Heading -> HeadingBlock(
                    text = block.text,
                    level = block.level,
                    color = color
                )
                is MarkdownBlock.Table -> MarkdownTable(
                    headers = block.headers,
                    rows = block.rows,
                    textColor = color
                )
                is MarkdownBlock.BlockQuote -> BlockQuoteBlock(
                    text = block.text,
                    textColor = color
                )
                is MarkdownBlock.Checkbox -> CheckboxBlock(
                    text = block.text,
                    checked = block.checked,
                    textColor = color
                )
                is MarkdownBlock.HorizontalRule -> HorizontalRuleBlock()
                is MarkdownBlock.Paragraph -> {
                    val context = LocalContext.current
                    val annotatedString = buildAnnotatedString {
                        appendInlineMarkdown(
                            text = block.text,
                            surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant,
                            tertiaryContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            onSurfaceColor = color // Use the passed color here
                        )
                    }
                    ClickableText(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = color // Use the passed color here
                        ),
                        onClick = { offset ->
                            // Check if click was on a link
                            annotatedString.getStringAnnotations("url", offset, offset)
                                .firstOrNull()?.let { annotation ->
                                    val intent = Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                                    context.startActivity(intent)
                                }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Terminal-style code block with header bar
 */
@Composable
private fun TerminalCodeBlock(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column {
            // Terminal header with darker background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Terminal control buttons (macOS style)
                    Icon(
                        Icons.Filled.Circle,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFFFF5F57),
                        modifier = Modifier.size(12.dp)
                    )
                    Icon(
                        Icons.Filled.Circle,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFFFFBD2E),
                        modifier = Modifier.size(12.dp)
                    )
                    Icon(
                        Icons.Filled.Circle,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFF28C840),
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Language label
                if (language.isNotEmpty()) {
                    Text(
                        text = language.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Code content with better contrast - fill max width to ensure background covers
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp)
            ) {
                Text(
                    text = code.trimEnd(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Math block with subtle styling
 */
@Composable
private fun MathBlock(
    math: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    ) {
        Text(
            text = math.trim(),
            modifier = Modifier.padding(12.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = textColor
        )
    }
}

/**
 * Heading with appropriate size and color
 */
@Composable
private fun HeadingBlock(
    text: String,
    level: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val (fontSize, fontWeight) = when (level) {
        1 -> 24.sp to FontWeight.Bold
        2 -> 20.sp to FontWeight.Bold
        else -> 18.sp to FontWeight.Bold
    }

    Text(
        text = text,
        modifier = modifier.padding(vertical = 4.dp),
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color
        )
    )
}

/**
 * Markdown table with borders and headers
 */
@Composable
private fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        // Wrap in horizontal scroll to allow wide tables
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(1.dp)  // Account for border
        ) {
            Column(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                // Header row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    headers.forEach { header ->
                        Column(modifier = Modifier.width(200.dp)) {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }

                // Data rows
                rows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { cell ->
                            Column(modifier = Modifier.width(200.dp)) {
                                Text(
                                    text = cell,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor
                                )
                            }
                        }
                    }

                    // Add separator line between rows (except after last row)
                    if (index < rows.size - 1) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        ) {}
                    }
                }
            }
        }
    }
}

/**
 * Block quote with left border styling
 */
@Composable
private fun BlockQuoteBlock(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                drawRect(color = bgColor, size = size)
                drawRect(color = borderColor, size = Size(10f, size.height))
            }
            .padding(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            ),
            color = textColor
        )
    }
}

/**
 * Checkbox with optional checkmark icon
 */
@Composable
private fun CheckboxBlock(
    text: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(20.dp)
        ) {
            Box(
                modifier = Modifier.padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

/**
 * Horizontal rule divider
 */
@Composable
private fun HorizontalRuleBlock(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}

/**
 * Parse markdown into structured blocks
 */
private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.split("\n")
    var i = 0
    var loopCount = 0
    val maxLoops = lines.size * 3  // Safety limit

    while (i < lines.size) {
        loopCount++
        if (loopCount > maxLoops) {
            // Safety limit reached, add remaining as plain text
            if (i < lines.size) {
                blocks.add(MarkdownBlock.Paragraph(lines.subList(i, lines.size).joinToString("\n")))
            }
            break
        }

        val line = lines[i]
        val trimmedLine = line.trim()

        when {
            // Checkbox list items - check FIRST before paragraph accumulation
            trimmedLine.matches(Regex("^\\s*[-*+]\\s+\\[([ xX])\\]\\s+.+")) -> {
                val matchResult = Regex("^\\s*[-*+]\\s+\\[([ xX])\\]\\s+(.+)").find(trimmedLine)
                if (matchResult != null) {
                    val isChecked = matchResult.groupValues[1].lowercase() == "x"
                    val text = matchResult.groupValues[2]
                    blocks.add(MarkdownBlock.Checkbox(text, isChecked))
                }
            }
            // Regular list items (unordered)
            trimmedLine.matches(Regex("^\\s*[-*+]\\s+.+")) -> {
                val matchResult = Regex("^\\s*[-*+]\\s+(.+)").find(trimmedLine)
                if (matchResult != null) {
                    val text = matchResult.groupValues[1]
                    blocks.add(MarkdownBlock.Paragraph(text))
                }
            }
            // Ordered list items
            trimmedLine.matches(Regex("^\\s*\\d+\\.\\s+.+")) -> {
                val matchResult = Regex("^\\s*\\d+\\.\\s+(.+)").find(trimmedLine)
                if (matchResult != null) {
                    val text = matchResult.groupValues[1]
                    blocks.add(MarkdownBlock.Paragraph(text))
                }
            }
            // Table detection - starts with | and contains multiple |
            trimmedLine.startsWith("|") && trimmedLine.endsWith("|") && trimmedLine.count { it == '|' } >= 4 -> {
                // This is a table row, check if next line is separator
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1].trim()
                    if (nextLine.startsWith("|") && nextLine.endsWith("|") &&
                        nextLine.contains("---") || nextLine.contains("===")) {
                        // This is a table! Parse it
                        val headers = parseTableRow(trimmedLine)
                        val rows = mutableListOf<List<String>>()
                        var j = i + 2
                        while (j < lines.size) {
                            val tableLine = lines[j].trim()
                            if (tableLine.startsWith("|") && tableLine.endsWith("|") &&
                                tableLine.count { it == '|' } >= 4) {
                                rows.add(parseTableRow(tableLine))
                                j++
                            } else {
                                break
                            }
                        }
                        blocks.add(MarkdownBlock.Table(headers, rows))
                        i = j - 1  // Will be incremented to j
                    } else {
                        // Not a table, treat as regular text
                        blocks.add(MarkdownBlock.Paragraph(line))
                    }
                } else {
                    blocks.add(MarkdownBlock.Paragraph(line))
                }
            }
            // Code block
            trimmedLine.startsWith("```") -> {
                val language = trimmedLine.substring(3).trim()
                var codeContent = ""
                var j = i + 1
                while (j < lines.size && !lines[j].trim().startsWith("```")) {
                    codeContent += if (codeContent.isEmpty()) "" else "\n"
                    codeContent += lines[j]
                    j++
                }
                if (j < lines.size) {
                    blocks.add(MarkdownBlock.CodeBlock(codeContent, language))
                    i = j
                } else {
                    blocks.add(MarkdownBlock.Paragraph(line))
                }
            }
            // Math block
            trimmedLine.startsWith("$$") -> {
                var mathContent = if (trimmedLine.length > 2) {
                    trimmedLine.substring(2)
                } else {
                    ""
                }
                var j = i + 1
                while (j < lines.size && !lines[j].trim().startsWith("$$")) {
                    mathContent += "\n" + lines[j]
                    j++
                }
                if (j < lines.size) {
                    blocks.add(MarkdownBlock.MathBlock(mathContent))
                    i = j
                } else {
                    blocks.add(MarkdownBlock.Paragraph(line))
                }
            }
            // Heading 1
            trimmedLine.startsWith("# ") && !trimmedLine.startsWith("##") -> {
                blocks.add(MarkdownBlock.Heading(trimmedLine.substring(2).trim(), 1))
            }
            // Heading 2
            trimmedLine.startsWith("## ") && !trimmedLine.startsWith("###") -> {
                blocks.add(MarkdownBlock.Heading(trimmedLine.substring(3).trim(), 2))
            }
            // Heading 3+
            trimmedLine.startsWith("###") -> {
                val level = trimmedLine.takeWhile { it == '#' }.length
                val text = trimmedLine.substring(level + 1).trim()
                blocks.add(MarkdownBlock.Heading(text, minOf(level, 3)))
            }
            // Horizontal rule
            trimmedLine.matches(Regex("^(\\*{3,}|-{3,}|_{3,})\\s*$")) -> {
                blocks.add(MarkdownBlock.HorizontalRule)
            }
            // Block quote
            trimmedLine.startsWith(">") -> {
                val quoteLines = mutableListOf<String>()
                var j = i
                while (j < lines.size) {
                    val quoteLine = lines[j]
                    val quoteTrimmed = quoteLine.trim()
                    if (quoteTrimmed.startsWith(">")) {
                        // Remove the ">" and any following space
                        val content = if (quoteTrimmed.length > 1) {
                            quoteTrimmed.substring(1).trimStart()
                        } else {
                            ""
                        }
                        quoteLines.add(content)
                        j++
                    } else if (quoteLine.isBlank()) {
                        // Allow blank lines between quote lines
                        quoteLines.add("")
                        j++
                    } else {
                        break
                    }
                }
                // Filter trailing empty lines
                while (quoteLines.isNotEmpty() && quoteLines.last().isBlank()) {
                    quoteLines.removeAt(quoteLines.lastIndex)
                }
                if (quoteLines.isNotEmpty()) {
                    blocks.add(MarkdownBlock.BlockQuote(quoteLines.joinToString("\n")))
                    i = j - 1
                }
            }
            // Regular text - accumulate into paragraphs
            else -> {
                val paragraphLines = mutableListOf<String>()
                var j = i
                while (j < lines.size) {
                    val nextLine = lines[j]
                    val nextTrimmed = nextLine.trim()
                    // Stop accumulating at block boundaries or list items
                    if (nextTrimmed.isEmpty() ||
                        nextTrimmed.startsWith("```") ||
                        nextTrimmed.startsWith("$$") ||
                        nextTrimmed.startsWith("#") ||
                        nextTrimmed.startsWith(">") ||
                        (nextTrimmed.startsWith("|") && nextTrimmed.endsWith("|")) ||
                        nextTrimmed.matches(Regex("^\\s*[-*+]\\s+")) ||
                        nextTrimmed.matches(Regex("^\\s*\\d+\\.\\s+"))) {
                        break
                    }
                    paragraphLines.add(nextLine)
                    j++
                }
                if (paragraphLines.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString("\n")))
                    i = j  // Skip to the last line we processed
                }
                // If paragraphLines is empty, the normal i++ will advance
            }
        }
        i++
    }

    return blocks
}

/**
 * Parse a single table row, returning list of cell contents
 */
private fun parseTableRow(row: String): List<String> {
    // Remove leading and trailing |
    val content = row.removePrefix("|").removeSuffix("|")
    // Split by | and trim each cell
    return content.split("|").map { it.trim() }
}

/**
 * Parse inline markdown (bold, italic, code, math)
 * Prevents infinite loops by limiting recursion depth and iterations
 */
private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    surfaceVariantColor: androidx.compose.ui.graphics.Color,
    tertiaryContainerColor: androidx.compose.ui.graphics.Color,
    onSurfaceColor: androidx.compose.ui.graphics.Color,
    depth: Int = 0
) {
    // Prevent stack overflow from deep recursion
    if (depth > 50) {
        append(text)
        return
    }

    var currentIndex = 0
    var iterations = 0
    val maxIterations = text.length * 2  // Safety limit

    while (currentIndex < text.length) {
        iterations++
        if (iterations > maxIterations) {
            // Safety limit reached, append rest and exit
            append(text.substring(currentIndex))
            break
        }

        val char = text[currentIndex]

        when {
            // Link: [text](url)
            char == '[' -> {
                val closingBracket = text.indexOf(']', currentIndex + 1)
                if (closingBracket != -1 && closingBracket > currentIndex + 1 &&
                    text.length > closingBracket + 1 && text[closingBracket + 1] == '(') {
                    val closingParen = text.indexOf(')', closingBracket + 2)
                    if (closingParen != -1 && closingParen > closingBracket + 2) {
                        val linkText = text.substring(currentIndex + 1, closingBracket)
                        val linkUrl = text.substring(closingBracket + 2, closingParen)
                        val linkStart = length
                        withStyle(
                            SpanStyle(
                                color = androidx.compose.ui.graphics.Color(0xFF3B82F6),
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            appendInlineMarkdown(
                                linkText,
                                surfaceVariantColor,
                                tertiaryContainerColor,
                                onSurfaceColor,
                                depth + 1
                            )
                        }
                        val linkEnd = length
                        // Store URL as annotation for click handling
                        addStringAnnotation("url", linkUrl, linkStart, linkEnd)
                        currentIndex = closingParen + 1
                    } else {
                        append(char)
                        currentIndex++
                    }
                } else {
                    append(char)
                    currentIndex++
                }
            }
            // Strikethrough: ~~text~~
            char == '~' && currentIndex + 1 < text.length && text[currentIndex + 1] == '~' -> {
                val endIndex = text.indexOf("~~", currentIndex + 2)
                if (endIndex != -1 && endIndex > currentIndex + 2) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        appendInlineMarkdown(
                            text.substring(currentIndex + 2, endIndex),
                            surfaceVariantColor,
                            tertiaryContainerColor,
                            onSurfaceColor,
                            depth + 1
                        )
                    }
                    currentIndex = endIndex + 2
                } else {
                    append(char)
                    currentIndex++
                }
            }
            // Code inline: `text` - check first for priority
            char == '`' -> {
                val endIndex = text.indexOf('`', currentIndex + 1)
                if (endIndex != -1 && endIndex > currentIndex + 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            background = surfaceVariantColor,
                            color = onSurfaceColor
                        )
                    ) {
                        append(text.substring(currentIndex + 1, endIndex))
                    }
                    currentIndex = endIndex + 1
                } else {
                    append(char)
                    currentIndex++
                }
            }
            // Bold: **text**
            char == '*' && currentIndex + 1 < text.length && text[currentIndex + 1] == '*' -> {
                val endIndex = text.indexOf("**", currentIndex + 2)
                if (endIndex != -1 && endIndex > currentIndex + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInlineMarkdown(
                            text.substring(currentIndex + 2, endIndex),
                            surfaceVariantColor,
                            tertiaryContainerColor,
                            onSurfaceColor,
                            depth + 1
                        )
                    }
                    currentIndex = endIndex + 2
                } else {
                    append(char)
                    currentIndex++
                }
            }
            // Italic: *text* (single asterisk, not part of **)
            char == '*' -> {
                val endIndex = findClosingSingleAsterisk(text, currentIndex + 1)
                if (endIndex != -1 && endIndex > currentIndex) {
                    withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        appendInlineMarkdown(
                            text.substring(currentIndex + 1, endIndex),
                            surfaceVariantColor,
                            tertiaryContainerColor,
                            onSurfaceColor,
                            depth + 1
                        )
                    }
                    currentIndex = endIndex + 1
                } else {
                    append(char)
                    currentIndex++
                }
            }
            // Math inline: $text$
            char == '$' -> {
                val endIndex = findClosingMath(text, currentIndex + 1)
                if (endIndex != -1 && endIndex > currentIndex) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            background = tertiaryContainerColor.copy(alpha = 0.3f)
                        )
                    ) {
                        append(text.substring(currentIndex + 1, endIndex))
                    }
                    currentIndex = endIndex + 1
                } else {
                    append(char)
                    currentIndex++
                }
            }
            else -> {
                append(char)
                currentIndex++
            }
        }
    }
}

/**
 * Find closing $ for inline math, ignoring $$ patterns
 */
private fun findClosingMath(text: String, startIndex: Int): Int {
    var i = startIndex
    while (i < text.length) {
        if (text[i] == '$' && !text.substring(i).startsWith("$$")) {
            return i
        }
        i++
    }
    return -1
}

/**
 * Find closing * for italic, ignoring ** patterns
 */
private fun findClosingSingleAsterisk(text: String, startIndex: Int): Int {
    var i = startIndex
    while (i < text.length) {
        if (text[i] == '*' && !text.substring(i).startsWith("**")) {
            if (i + 1 >= text.length || text[i + 1] != '*') {
                return i
            }
        }
        i++
    }
    return -1
}

/**
 * Sealed class representing different markdown block types
 */
private sealed class MarkdownBlock {
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock()
    data class MathBlock(val math: String) : MarkdownBlock()
    data class Heading(val text: String, val level: Int) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class Checkbox(val text: String, val checked: Boolean) : MarkdownBlock()
    data object HorizontalRule : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}
