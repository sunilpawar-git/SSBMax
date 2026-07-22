package com.ssbmax.shared.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * KMP port of the Android `app/.../ui/components/BreadcrumbBar.kt` --
 * hierarchical navigation trail (e.g. "Study Materials > Category > Article").
 * Only `BreadcrumbBar`/`BreadcrumbItem` ported -- `CompactBreadcrumbBar`/
 * `BreadcrumbBuilder` (same file in the Android original) confirmed dead code,
 * zero call sites anywhere in `app/ui`, correctly not ported.
 */
data class BreadcrumbItem(
    val title: String,
    val route: String? = null,
    val isClickable: Boolean = true
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreadcrumbBar(
    items: List<BreadcrumbItem>,
    onItemClick: (BreadcrumbItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            val isClickable = item.isClickable && !isLast && item.route != null

            Text(
                text = item.title,
                style = if (isLast) {
                    MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (isClickable) {
                    Modifier.clickable { onItemClick(item) }.padding(horizontal = 4.dp, vertical = 2.dp)
                } else {
                    Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                }
            )

            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp).align(Alignment.CenterVertically)
                )
            }
        }
    }
}
