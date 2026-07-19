package com.ssbmax.ui.components.result

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult

/**
 * Shared OLQ score breakdown card with expand/collapse reasoning.
 * Used by all 5 OLQ-based test result screens (TAT, WAT, SRT, SDT, PPDT).
 */
@Composable
fun OLQResultContent(
    olqResult: OLQAnalysisResult,
    modifier: Modifier = Modifier
) {
    var expandedOLQ by remember { mutableStateOf<OLQ?>(null) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.olq_result_score_breakdown_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider()
            olqResult.olqScores.entries.forEachIndexed { index, (olq, score) ->
                OLQReasoningRow(
                    olq = olq,
                    score = score,
                    isExpanded = expandedOLQ == olq,
                    onToggle = { expandedOLQ = if (expandedOLQ == olq) null else olq }
                )
                if (index < olqResult.olqScores.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun OLQReasoningRow(
    olq: OLQ,
    score: OLQScore,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = olq.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${score.score} — ${score.rating}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(R.string.olq_result_expand_hint),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = score.reasoning.ifBlank { stringResource(R.string.olq_result_reasoning_unavailable) },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OLQResultContentPreview() {
    val sampleResult = OLQAnalysisResult(
        submissionId = "preview",
        testType = TestType.TAT,
        olqScores = mapOf(
            OLQ.COURAGE to OLQScore(score = 7, confidence = 80, reasoning = "Hero showed initiative but lacked follow-through"),
            OLQ.DETERMINATION to OLQScore(score = 5, confidence = 90, reasoning = "Strong resolve demonstrated throughout the story"),
            OLQ.INITIATIVE to OLQScore(score = 6, confidence = 75, reasoning = "")
        ),
        overallScore = 6.0f,
        overallRating = "Good",
        strengths = emptyList(),
        weaknesses = emptyList(),
        recommendations = emptyList(),
        analyzedAt = 0L,
        aiConfidence = 82
    )
    OLQResultContent(olqResult = sampleResult)
}
