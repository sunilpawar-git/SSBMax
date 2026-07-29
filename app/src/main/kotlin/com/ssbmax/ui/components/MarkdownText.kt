package com.ssbmax.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Renders markdown-formatted text content with proper styling
 * Supports: headings (#, ##, ###), bullet lists (-, *, •, ✓), numbered lists, bold (**text**)
 * 
 * This is the centralized utility for displaying long-form content across the app.
 * Use this instead of custom text parsing to ensure consistent formatting.
 * 
 * Updated to process each line individually for robust inline bold support.
 */
@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Split by double newlines to identify blocks, then process each line within blocks
        content.split("\n\n").forEach { block ->
            MarkdownBlock(block, textColor)
        }
    }
}

/**
 * Tracks pending bullet/numbered list items within a block so they can be
 * flushed as a single grouped list right before a heading or plain line.
 */
private class MarkdownListState(private val textColor: Color) {
    private var inBulletList = false
    private var inNumberedList = false
    private val bulletItems = mutableListOf<String>()
    private val numberedItems = mutableListOf<String>()

    @Composable
    fun flush() {
        if (inBulletList) {
            renderBulletList(bulletItems, textColor)
            bulletItems.clear()
            inBulletList = false
        }
        if (inNumberedList) {
            renderNumberedList(numberedItems, textColor)
            numberedItems.clear()
            inNumberedList = false
        }
    }

    @Composable
    fun addBulletItem(item: String) {
        if (inNumberedList) {
            renderNumberedList(numberedItems, textColor)
            numberedItems.clear()
            inNumberedList = false
        }
        inBulletList = true
        bulletItems.add(item)
    }

    @Composable
    fun addNumberedItem(item: String) {
        if (inBulletList) {
            renderBulletList(bulletItems, textColor)
            bulletItems.clear()
            inBulletList = false
        }
        inNumberedList = true
        numberedItems.add(item)
    }
}

@Composable
private fun MarkdownBlock(block: String, textColor: Color) {
    val lines = block.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    val listState = MarkdownListState(textColor)

    lines.forEach { line ->
        when {
            line.startsWith("# ") -> {
                listState.flush()
                MarkdownHeading(
                    line.removePrefix("# ").trim(), MaterialTheme.typography.headlineSmall,
                    FontWeight.Bold, 8.dp, 4.dp, textColor
                )
            }
            line.startsWith("## ") -> {
                listState.flush()
                MarkdownHeading(
                    line.removePrefix("## ").trim(), MaterialTheme.typography.titleLarge,
                    FontWeight.SemiBold, 8.dp, 4.dp, textColor
                )
            }
            line.startsWith("### ") -> {
                listState.flush()
                MarkdownHeading(
                    line.removePrefix("### ").trim(), MaterialTheme.typography.titleMedium,
                    FontWeight.Medium, 6.dp, 2.dp, textColor
                )
            }
            line.startsWith("- ") || line.startsWith("* ") ||
                line.startsWith("• ") || line.startsWith("✓ ") -> {
                val cleanedItem = line.trim()
                    .removePrefix("- ").removePrefix("* ")
                    .removePrefix("• ").removePrefix("✓ ")
                    .trim()
                listState.addBulletItem(cleanedItem)
            }
            line.matches(Regex("^\\d+\\..*")) -> {
                val cleanedItem = line.trim().replaceFirst(Regex("^\\d+\\.\\s*"), "")
                listState.addNumberedItem(cleanedItem)
            }
            else -> {
                listState.flush()
                Text(
                    text = parseInlineBold(line),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }

    listState.flush()
}

@Composable
private fun MarkdownHeading(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    textColor: Color
) {
    Text(
        text = text,
        style = style,
        fontWeight = fontWeight,
        color = textColor,
        modifier = Modifier.padding(top = topPadding, bottom = bottomPadding)
    )
}

/**
 * Render a bullet list with inline bold support
 */
@Composable
private fun renderBulletList(items: List<String>, textColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            if (item.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = textColor)
                    Text(
                        text = parseInlineBold(item),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Render a numbered list with inline bold support
 */
@Composable
private fun renderNumberedList(items: List<String>, textColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { index, item ->
            if (item.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = parseInlineBold(item),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Extension function to parse and render inline bold text (**text**)
 * Splits by ** and applies bold styling to odd-indexed parts
 * 
 * Example: "This is **bold** text" -> "This is <b>bold</b> text"
 */
private fun AnnotatedString.Builder.appendWithInlineBold(text: String) {
    val parts = text.split("**")
    parts.forEachIndexed { index, part ->
        if (part.isNotEmpty()) {
            if (index % 2 == 1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(part)
                pop()
            } else {
                append(part)
            }
        }
    }
}

@androidx.annotation.VisibleForTesting
internal fun parseInlineBold(text: String): AnnotatedString =
    buildAnnotatedString { appendWithInlineBold(text) }
