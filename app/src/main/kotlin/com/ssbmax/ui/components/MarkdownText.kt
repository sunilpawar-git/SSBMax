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
            val lines = block.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            
            // Track if we're in a multi-line list
            var inBulletList = false
            var inNumberedList = false
            val bulletItems = mutableListOf<String>()
            val numberedItems = mutableListOf<String>()
            
            lines.forEach { line ->
                when {
                    // Main heading (#)
                    line.startsWith("# ") -> {
                        // Flush any pending lists
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
                        
                        Text(
                            text = line.removePrefix("# ").trim(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    
                    // Subheading (##)
                    line.startsWith("## ") -> {
                        // Flush any pending lists
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
                        
                        Text(
                            text = line.removePrefix("## ").trim(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    
                    // Small heading (###)
                    line.startsWith("### ") -> {
                        // Flush any pending lists
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
                        
                        Text(
                            text = line.removePrefix("### ").trim(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    
                    // Bullet list item
                    line.startsWith("- ") || line.startsWith("* ") || 
                    line.startsWith("• ") || line.startsWith("✓ ") -> {
                        // Flush numbered list if switching types
                        if (inNumberedList) {
                            renderNumberedList(numberedItems, textColor)
                            numberedItems.clear()
                            inNumberedList = false
                        }
                        
                        inBulletList = true
                        val cleanedItem = line.trim()
                            .removePrefix("- ").removePrefix("* ")
                            .removePrefix("• ").removePrefix("✓ ")
                            .trim()
                        bulletItems.add(cleanedItem)
                    }
                    
                    // Numbered list item
                    line.matches(Regex("^\\d+\\..*")) -> {
                        // Flush bullet list if switching types
                        if (inBulletList) {
                            renderBulletList(bulletItems, textColor)
                            bulletItems.clear()
                            inBulletList = false
                        }
                        
                        inNumberedList = true
                        val cleanedItem = line.trim().replaceFirst(Regex("^\\d+\\.\\s*"), "")
                        numberedItems.add(cleanedItem)
                    }
                    
                    // Regular line with inline bold support
                    else -> {
                        // Flush any pending lists
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
                        
                        Text(
                            text = buildAnnotatedString { appendWithInlineBold(line) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
            }
            
            // Flush any remaining lists at end of block
            if (inBulletList && bulletItems.isNotEmpty()) {
                renderBulletList(bulletItems, textColor)
            }
            if (inNumberedList && numberedItems.isNotEmpty()) {
                renderNumberedList(numberedItems, textColor)
            }
        }
    }
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
                        text = buildAnnotatedString { appendWithInlineBold(item) },
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
                        text = buildAnnotatedString { appendWithInlineBold(item) },
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
