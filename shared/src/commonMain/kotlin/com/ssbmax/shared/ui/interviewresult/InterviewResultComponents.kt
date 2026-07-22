package com.ssbmax.shared.ui.interviewresult

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.presentation.interviewresult.InterviewResultUiState
import com.ssbmax.shared.ui.util.formatOneDecimal
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.interview_result_average_score
import ssbmax.shared.generated.resources.interview_result_completion
import ssbmax.shared.generated.resources.interview_result_duration
import ssbmax.shared.generated.resources.interview_result_feedback_title
import ssbmax.shared.generated.resources.interview_result_overall_rating

/**
 * Sub-components for [InterviewResultScreen], split out to keep both files
 * under this plan's 300-line Quality Limit.
 */
@Composable
internal fun OverallSummaryCard(uiState: InterviewResultUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(Res.string.interview_result_overall_rating), style = MaterialTheme.typography.labelMedium)
            Text(text = uiState.performanceLevel.displayName, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(Res.string.interview_result_average_score, formatOneDecimal(uiState.averageScore)),
                style = MaterialTheme.typography.bodyLarge
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(Res.string.interview_result_duration, uiState.durationMinutes))
                Text(text = stringResource(Res.string.interview_result_completion, uiState.completionRate.toInt()))
            }
        }
    }
}

@Composable
internal fun OLQListCard(titleRes: StringResource, olqs: List<OLQ>, isPositive: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            olqs.forEach { olq ->
                Text(
                    text = "• ${olq.displayName}",
                    color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
internal fun OLQScoreRow(olqName: String, categoryName: String, score: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = olqName, style = MaterialTheme.typography.bodyMedium)
                Text(text = categoryName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Text(text = "$score/10", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
internal fun FeedbackCard(feedback: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(Res.string.interview_result_feedback_title), style = MaterialTheme.typography.titleMedium)
            Text(text = feedback, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
