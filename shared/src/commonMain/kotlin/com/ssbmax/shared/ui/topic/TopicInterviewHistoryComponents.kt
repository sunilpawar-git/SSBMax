package com.ssbmax.shared.ui.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.interview.InterviewResult
import com.ssbmax.shared.ui.util.formatFullDateTime24h
import com.ssbmax.shared.ui.util.formatOneDecimal
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.interview_history_title
import ssbmax.shared.generated.resources.interview_no_past_results

/**
 * Past-interview-results composables for [TopicScreen]'s Interview topic,
 * extracted purely to keep [TopicComponents] under the repo's 300-line
 * Quality Limit -- no behavior change from having them inline.
 *
 * `DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").withZone(ZoneId.systemDefault())`
 * (JVM-only) replaced with [formatFullDateTime24h]; `String.format("%.1f", ...)`
 * (JVM-only) replaced with [formatOneDecimal] -- both established KMP-safe
 * seams from this phase's `ui/util` package.
 */
@Composable
internal fun InterviewHistorySection(
    results: List<InterviewResult>,
    isLoading: Boolean,
    onResultClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(Res.string.interview_history_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (results.isEmpty()) {
            Text(
                text = stringResource(Res.string.interview_no_past_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val displayResults = results.take(5)
            displayResults.forEach { result ->
                InterviewResultCard(result = result, onClick = { onResultClick(result.id) })
            }

            if (results.size > 5) {
                Text(
                    text = "+ ${results.size - 5} more results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InterviewResultCard(result: InterviewResult, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatFullDateTime24h(result.completedAt.toEpochMilliseconds()),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Score: ${formatOneDecimal(result.getAverageOLQScore())} • ${result.getDurationMinutes()} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(shape = MaterialTheme.shapes.small, color = getInterviewScoreColor(result.overallRating).copy(alpha = 0.2f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(
                        text = "${result.overallRating}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = getInterviewScoreColor(result.overallRating)
                    )
                    Text(text = "/10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** SSB scale: lower score is better (1-5 = excellent, 6 = good, 7 = average, 8-10 = poor). */
private fun getInterviewScoreColor(rating: Int): Color {
    return when (rating) {
        in 1..5 -> Color(0xFF4CAF50)
        6 -> Color(0xFF8BC34A)
        7 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}
