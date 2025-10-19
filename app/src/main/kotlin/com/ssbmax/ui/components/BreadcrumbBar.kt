package com.ssbmax.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Represents a single item in the breadcrumb trail
 */
data class BreadcrumbItem(
    val title: String,
    val route: String? = null, // null means it's the current page
    val isClickable: Boolean = true
)

/**
 * Breadcrumb navigation component for hierarchical navigation
 * Example: Phase 1 > OIR > Practice Test
 *
 * @param items List of breadcrumb items from root to current page
 * @param onItemClick Callback when a breadcrumb item is clicked
 * @param modifier Modifier for the breadcrumb bar
 */
@Composable
fun BreadcrumbBar(
    items: List<BreadcrumbItem>,
    onItemClick: (BreadcrumbItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            val isClickable = item.isClickable && !isLast && item.route != null
            
            // Breadcrumb item
            Text(
                text = item.title,
                style = if (isLast) {
                    MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = if (isLast) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = if (isClickable) {
                    Modifier
                        .clickable { onItemClick(item) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else {
                    Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                }
            )
            
            // Separator
            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Compact breadcrumb navigation for smaller spaces
 * Shows: ... > Parent > Current
 *
 * @param items List of breadcrumb items
 * @param onItemClick Callback when a breadcrumb item is clicked
 * @param modifier Modifier for the breadcrumb bar
 */
@Composable
fun CompactBreadcrumbBar(
    items: List<BreadcrumbItem>,
    onItemClick: (BreadcrumbItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            items.size == 1 -> {
                // Just show current
                BreadcrumbText(items[0], isLast = true, onItemClick)
            }
            items.size == 2 -> {
                // Show both
                BreadcrumbText(items[0], isLast = false, onItemClick)
                BreadcrumbSeparator()
                BreadcrumbText(items[1], isLast = true, onItemClick)
            }
            else -> {
                // Show: ... > Parent > Current
                Text(
                    text = "...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                BreadcrumbSeparator()
                BreadcrumbText(items[items.size - 2], isLast = false, onItemClick)
                BreadcrumbSeparator()
                BreadcrumbText(items.last(), isLast = true, onItemClick)
            }
        }
    }
}

@Composable
private fun BreadcrumbText(
    item: BreadcrumbItem,
    isLast: Boolean,
    onItemClick: (BreadcrumbItem) -> Unit
) {
    val isClickable = item.isClickable && !isLast && item.route != null
    
    Text(
        text = item.title,
        style = if (isLast) {
            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        } else {
            MaterialTheme.typography.bodyMedium
        },
        color = if (isLast) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = if (isClickable) {
            Modifier
                .clickable { onItemClick(item) }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        } else {
            Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        }
    )
}

@Composable
private fun BreadcrumbSeparator() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(20.dp)
    )
}

/**
 * Helper to build breadcrumb items easily
 */
object BreadcrumbBuilder {
    /**
     * Create breadcrumbs for Phase > Topic flow
     */
    fun phaseTopic(phase: String, topic: String, phaseRoute: String? = null): List<BreadcrumbItem> {
        return listOf(
            BreadcrumbItem(phase, phaseRoute, isClickable = phaseRoute != null),
            BreadcrumbItem(topic, null, isClickable = false)
        )
    }
    
    /**
     * Create breadcrumbs for Phase > Topic > Subpage flow
     */
    fun phaseTopicSubpage(
        phase: String,
        topic: String,
        subpage: String,
        phaseRoute: String? = null,
        topicRoute: String? = null
    ): List<BreadcrumbItem> {
        return listOf(
            BreadcrumbItem(phase, phaseRoute, isClickable = phaseRoute != null),
            BreadcrumbItem(topic, topicRoute, isClickable = topicRoute != null),
            BreadcrumbItem(subpage, null, isClickable = false)
        )
    }
    
    /**
     * Create breadcrumbs for Study Materials flow
     */
    fun studyMaterial(
        category: String,
        materialTitle: String,
        categoryRoute: String? = null
    ): List<BreadcrumbItem> {
        return listOf(
            BreadcrumbItem("Study Materials", "study_materials", isClickable = true),
            BreadcrumbItem(category, categoryRoute, isClickable = categoryRoute != null),
            BreadcrumbItem(materialTitle, null, isClickable = false)
        )
    }
}

