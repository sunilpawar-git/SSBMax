package com.ssbmax.shared.ui.components.result

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.ui.util.formatOneDecimal
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.result_ai_confidence
import ssbmax.shared.generated.resources.result_overall_performance
import ssbmax.shared.generated.resources.result_score_out_of_10
import ssbmax.shared.generated.resources.result_ssb_scale_note

/**
 * KMP port of `app/.../ui/components/result/ResultScoreCards.kt`.
 *
 * One real change: the Android original's `"%.1f".format(overallScore)`
 * (`String.format` has no common `actual` -- a recurring Kotlin/Native
 * compile gotcha documented throughout this migration) is replaced with
 * [formatOneDecimal], already built in this phase's `ui/util` package for
 * exactly this kind of score formatting.
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
        colors = CardDefaults.cardColors(containerColor = getScoreContainerColor(overallRating))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.result_overall_performance),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = formatOneDecimal(overallScore),
                style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(Res.string.result_score_out_of_10),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = overallRating, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.result_ai_confidence, aiConfidence),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.result_ssb_scale_note),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = olq.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = score.rating, style = MaterialTheme.typography.bodySmall, color = getRatingColor(score.rating))
                }
                Surface(shape = MaterialTheme.shapes.small, color = getScoreBadgeColor(score.score)) {
                    Text(
                        text = "${score.score}", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }
            if (score.reasoning.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = score.reasoning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun getScoreContainerColor(rating: String) = when (rating) {
    "Exceptional", "Excellent" -> MaterialTheme.colorScheme.tertiaryContainer
    "Very Good", "Good" -> MaterialTheme.colorScheme.primaryContainer
    "Average" -> MaterialTheme.colorScheme.secondaryContainer
    "Below Average" -> MaterialTheme.colorScheme.surfaceVariant
    else -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun getRatingColor(rating: String) = when (rating) {
    "Exceptional", "Excellent" -> MaterialTheme.colorScheme.tertiary
    "Very Good", "Good" -> MaterialTheme.colorScheme.primary
    "Average" -> MaterialTheme.colorScheme.onSurfaceVariant
    "Below Average" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onErrorContainer
}

@Composable
private fun getScoreBadgeColor(score: Int) = when (score) {
    in 1..3 -> MaterialTheme.colorScheme.tertiary
    in 4..6 -> MaterialTheme.colorScheme.primary
    7 -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.error
}
