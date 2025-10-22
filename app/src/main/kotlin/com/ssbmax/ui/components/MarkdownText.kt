package com.ssbmax.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Renders markdown-formatted text content with proper styling
 * Supports: headings (#, ##, ###), bullet lists (-, *, •, ✓), numbered lists, bold (**text**)
 * 
 * This is the centralized utility for displaying long-form content across the app.
 * Use this instead of custom text parsing to ensure consistent formatting.
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
        content.split("\n\n").forEach { paragraph ->
            when {
                // Main heading (#)
                paragraph.trim().startsWith("# ") -> {
                    Text(
                        text = paragraph.removePrefix("# ").trim(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                // Subheading (##)
                paragraph.trim().startsWith("## ") -> {
                    Text(
                        text = paragraph.removePrefix("## ").trim(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                // Small heading (###)
                paragraph.trim().startsWith("### ") -> {
                    Text(
                        text = paragraph.removePrefix("### ").trim(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                // Bullet list
                paragraph.trim().startsWith("- ") || 
                paragraph.trim().startsWith("* ") || 
                paragraph.trim().startsWith("• ") ||
                paragraph.trim().startsWith("✓ ") -> {
                    val items = paragraph.split("\n").filter { it.trim().isNotEmpty() }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items.forEach { item ->
                            val cleanedItem = item.trim()
                                .removePrefix("- ").removePrefix("* ")
                                .removePrefix("• ").removePrefix("✓ ")
                                .trim()
                            if (cleanedItem.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("•", style = MaterialTheme.typography.bodyMedium, color = textColor)
                                    Text(
                                        text = buildAnnotatedString { appendWithInlineBold(cleanedItem) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                // Numbered list
                paragraph.trim().matches(Regex("^\\d+\\..*")) -> {
                    val items = paragraph.split("\n").filter { it.trim().isNotEmpty() }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items.forEachIndexed { index, item ->
                            val cleanedItem = item.trim().replaceFirst(Regex("^\\d+\\.\\s*"), "")
                            if (cleanedItem.isNotEmpty()) {
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
                                        text = buildAnnotatedString { appendWithInlineBold(cleanedItem) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                // Regular paragraph (may contain inline bold)
                paragraph.trim().isNotEmpty() -> {
                    Text(
                        text = buildAnnotatedString { appendWithInlineBold(paragraph.trim()) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
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
                // Odd index = text inside ** markers = bold
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(part)
                pop()
            } else {
                // Even index = normal text
                append(part)
            }
        }
    }
}

