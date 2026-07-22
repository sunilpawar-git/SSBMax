package com.ssbmax.shared.ui.common

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
 * KMP port of the Android `app/.../ui/components/MarkdownText.kt`. Shared by
 * every screen that renders long-form educational content (SSB Overview,
 * Study Material Detail, Topic) -- ported once here rather than duplicated
 * per screen, matching this phase's "reusable building block" precedent
 * ([com.ssbmax.shared.ui.common.HapticFeedbackHelper] etc).
 *
 * Only deviation from the Android original: the `@androidx.annotation.VisibleForTesting`
 * annotation on [parseInlineBold] (Android-only, no common-target equivalent)
 * was dropped -- `internal` visibility alone already achieves the same
 * "testable but not part of the public API" intent.
 *
 * Renders markdown-formatted text: headings (#, ##, ###), bullet lists
 * (-, *, •, ✓), numbered lists, inline bold (**text**).
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
        content.split("\n\n").forEach { block ->
            val lines = block.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

            var inBulletList = false
            var inNumberedList = false
            val bulletItems = mutableListOf<String>()
            val numberedItems = mutableListOf<String>()

            lines.forEach { line ->
                when {
                    line.startsWith("# ") -> {
                        if (inBulletList) { renderBulletList(bulletItems, textColor); bulletItems.clear(); inBulletList = false }
                        if (inNumberedList) { renderNumberedList(numberedItems, textColor); numberedItems.clear(); inNumberedList = false }
                        Text(
                            text = line.removePrefix("# ").trim(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    line.startsWith("## ") -> {
                        if (inBulletList) { renderBulletList(bulletItems, textColor); bulletItems.clear(); inBulletList = false }
                        if (inNumberedList) { renderNumberedList(numberedItems, textColor); numberedItems.clear(); inNumberedList = false }
                        Text(
                            text = line.removePrefix("## ").trim(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    line.startsWith("### ") -> {
                        if (inBulletList) { renderBulletList(bulletItems, textColor); bulletItems.clear(); inBulletList = false }
                        if (inNumberedList) { renderNumberedList(numberedItems, textColor); numberedItems.clear(); inNumberedList = false }
                        Text(
                            text = line.removePrefix("### ").trim(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }

                    line.startsWith("- ") || line.startsWith("* ") ||
                        line.startsWith("• ") || line.startsWith("✓ ") -> {
                        if (inNumberedList) { renderNumberedList(numberedItems, textColor); numberedItems.clear(); inNumberedList = false }
                        inBulletList = true
                        bulletItems.add(
                            line.trim()
                                .removePrefix("- ").removePrefix("* ")
                                .removePrefix("• ").removePrefix("✓ ")
                                .trim()
                        )
                    }

                    line.matches(Regex("^\\d+\\..*")) -> {
                        if (inBulletList) { renderBulletList(bulletItems, textColor); bulletItems.clear(); inBulletList = false }
                        inNumberedList = true
                        numberedItems.add(line.trim().replaceFirst(Regex("^\\d+\\.\\s*"), ""))
                    }

                    else -> {
                        if (inBulletList) { renderBulletList(bulletItems, textColor); bulletItems.clear(); inBulletList = false }
                        if (inNumberedList) { renderNumberedList(numberedItems, textColor); numberedItems.clear(); inNumberedList = false }
                        Text(
                            text = parseInlineBold(line),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
            }

            if (inBulletList && bulletItems.isNotEmpty()) {
                renderBulletList(bulletItems, textColor)
            }
            if (inNumberedList && numberedItems.isNotEmpty()) {
                renderNumberedList(numberedItems, textColor)
            }
        }
    }
}

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

internal fun parseInlineBold(text: String): AnnotatedString =
    buildAnnotatedString { appendWithInlineBold(text) }
