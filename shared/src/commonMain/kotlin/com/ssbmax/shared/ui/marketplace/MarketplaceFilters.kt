package com.ssbmax.shared.ui.marketplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.InstituteType
import com.ssbmax.shared.domain.model.PriceRange
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.marketplace_all
import ssbmax.shared.generated.resources.marketplace_class_type
import ssbmax.shared.generated.resources.marketplace_clear_all
import ssbmax.shared.generated.resources.marketplace_filters_title
import ssbmax.shared.generated.resources.marketplace_price_range

/**
 * Title-cased chip label for a [PriceRange] (e.g. "Budget", not the enum's
 * own longer `displayName` like "Budget (₹5k-15k)", which is too long for a
 * filter chip). Matches the Android original's inline
 * `range.name.split("_").joinToString(...) { titlecase }` -- `PriceRange`'s
 * enum constants have no underscores, so this reduces to a plain title-case
 * of the single word; kept as a named function instead of inlining the same
 * one-liner twice (search + filters chips would otherwise duplicate it).
 */
private fun PriceRange.chipLabel(): String =
    name.lowercase().replaceFirstChar { it.titlecase() }

/**
 * Type/price-range filter chips for [MarketplaceScreen]. Split out to keep
 * both files under this repo's 300-line Quality Limit.
 */
@Composable
internal fun FiltersSection(
    filterType: InstituteType?,
    filterPriceRange: PriceRange?,
    filterCity: String?,
    onTypeChange: (InstituteType?) -> Unit,
    onPriceRangeChange: (PriceRange?) -> Unit,
    onCityChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(Res.string.marketplace_filters_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearFilters) {
                    Text(stringResource(Res.string.marketplace_clear_all))
                }
            }

            Text(stringResource(Res.string.marketplace_class_type), style = MaterialTheme.typography.labelMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = filterType == null,
                    onClick = { onTypeChange(null) },
                    label = { Text(stringResource(Res.string.marketplace_all)) }
                )
                InstituteType.entries.forEach { type ->
                    FilterChip(
                        selected = filterType == type,
                        onClick = { onTypeChange(if (filterType == type) null else type) },
                        label = { Text(type.displayName) }
                    )
                }
            }

            Text(stringResource(Res.string.marketplace_price_range), style = MaterialTheme.typography.labelMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = filterPriceRange == null,
                    onClick = { onPriceRangeChange(null) },
                    label = { Text(stringResource(Res.string.marketplace_all)) }
                )
                PriceRange.entries.forEach { range ->
                    FilterChip(
                        selected = filterPriceRange == range,
                        onClick = { onPriceRangeChange(if (filterPriceRange == range) null else range) },
                        label = { Text(range.chipLabel()) }
                    )
                }
            }
        }
    }
}
