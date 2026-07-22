package com.ssbmax.shared.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.results.HistoricResult
import com.ssbmax.shared.ui.util.formatOneDecimal
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.historic_results_action_retry
import ssbmax.shared.generated.resources.historic_results_empty_message
import ssbmax.shared.generated.resources.historic_results_empty_title
import ssbmax.shared.generated.resources.historic_results_filter_all
import ssbmax.shared.generated.resources.historic_results_filter_close
import ssbmax.shared.generated.resources.historic_results_filter_dialog_title
import ssbmax.shared.generated.resources.historic_results_new_badge
import ssbmax.shared.generated.resources.historic_results_score_format

/**
 * Sub-composables extracted from `HistoricResultsScreen.kt` purely to stay
 * under this repo's 300-line Quality Limit -- the Android original was one
 * 339-line file; this split (result-card/filter-dialog/filter-option/empty/
 * error/score-color) is a structural-only change, zero behavior difference,
 * same precedent as this session's other `*Components.kt` splits.
 */
@Composable
internal fun HistoricResultCard(
    result: HistoricResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = result.testType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (result.isRecent) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = stringResource(Res.string.historic_results_new_badge),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = result.getFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            result.overallScore?.let { score ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getScoreColor(score)
                ) {
                    Text(
                        text = stringResource(Res.string.historic_results_score_format, formatOneDecimal(score)),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
internal fun FilterDialog(
    selectedFilter: TestType?,
    onDismiss: () -> Unit,
    onFilterSelected: (TestType?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.historic_results_filter_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterOption(
                    label = stringResource(Res.string.historic_results_filter_all),
                    isSelected = selectedFilter == null,
                    onClick = { onFilterSelected(null) }
                )

                TestType.entries.filter {
                    it in listOf(TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD)
                }.forEach { testType ->
                    FilterOption(
                        label = testType.displayName,
                        isSelected = selectedFilter == testType,
                        onClick = { onFilterSelected(testType) }
                    )
                }

                TestType.entries.filter {
                    it.name.startsWith("GTO_")
                }.forEach { testType ->
                    FilterOption(
                        label = testType.displayName,
                        isSelected = selectedFilter == testType,
                        onClick = { onFilterSelected(testType) }
                    )
                }

                FilterOption(
                    label = TestType.IO.displayName,
                    isSelected = selectedFilter == TestType.IO,
                    onClick = { onFilterSelected(TestType.IO) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.historic_results_filter_close))
            }
        }
    )
}

@Composable
private fun FilterOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
internal fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.historic_results_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.historic_results_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.historic_results_action_retry))
            }
        }
    }
}

/**
 * Get color for score badge (SSB 1-10 scale, lower is better)
 */
private fun getScoreColor(score: Float): Color {
    return when {
        score <= 5f -> Color(0xFF4CAF50)
        score <= 7f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}
