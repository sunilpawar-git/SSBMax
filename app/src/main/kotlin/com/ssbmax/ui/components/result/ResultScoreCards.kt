package com.ssbmax.ui.components.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore

/**
 * Card displaying the overall score and rating from OLQ analysis.
 *
 * @param overallScore The overall score on 1-10 SSB scale (lower is better)
 * @param overallRating Text rating (e.g., "Excellent", "Good", "Average")
 * @param aiConfidence AI confidence percentage (0-100)
 */
@Composable
fun OverallScoreCard(
    overallScore: Float,
    overallRating: String,
    aiConfidence: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getScoreContainerColor(overallRating)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.result_overall_performance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "%.1f".format(overallScore),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.result_score_out_of_10),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = overallRating,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.result_ai_confidence, aiConfidence),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.result_ssb_scale_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Card displaying a single OLQ score with name, rating, and reasoning.
 *
 * @param olq The Officer-Like Quality being displayed
 * @param score The score details including numeric score, rating, and reasoning
 * @param isStrength True if this OLQ is a strength, false if weakness, null if neither
 */
@Composable
fun OLQScoreCard(
    olq: OLQ,
    score: OLQScore,
    isStrength: Boolean?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (isStrength) {
                true -> MaterialTheme.colorScheme.tertiaryContainer
                false -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = olq.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = score.rating,
                        style = MaterialTheme.typography.bodySmall,
                        color = getRatingColor(score.rating)
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = getScoreBadgeColor(score.score)
                ) {
                    Text(
                        text = "${score.score}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }

            if (score.reasoning.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = score.reasoning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Simple info item with label and value, typically used in submission confirmation cards.
 *
 * @param label The label text (e.g., "Stories Completed")
 * @param value The value text (e.g., "12/12")
 */
@Composable
fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Helper function to get container color based on rating.
 */
@Composable
private fun getScoreContainerColor(rating: String) = when (rating) {
    "Exceptional", "Excellent" -> MaterialTheme.colorScheme.tertiaryContainer
    "Very Good", "Good" -> MaterialTheme.colorScheme.primaryContainer
    "Average" -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

/**
 * Helper function to get text color based on rating.
 */
@Composable
private fun getRatingColor(rating: String) = when (rating) {
    "Exceptional", "Excellent" -> MaterialTheme.colorScheme.tertiary
    "Very Good", "Good" -> MaterialTheme.colorScheme.primary
    "Average" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.error
}

/**
 * Helper function to get badge color based on numeric score.
 * SSB scale: 1-3 is excellent, 4-6 is good, 7 is average, 8-10 needs improvement
 */
@Composable
private fun getScoreBadgeColor(score: Int) = when (score) {
    in 1..3 -> MaterialTheme.colorScheme.tertiary
    in 4..6 -> MaterialTheme.colorScheme.primary
    7 -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.error
}
