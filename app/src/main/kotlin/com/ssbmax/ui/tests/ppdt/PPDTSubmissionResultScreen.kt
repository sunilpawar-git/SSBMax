package com.ssbmax.ui.tests.ppdt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.ui.components.result.SubmissionConfirmationCard
import com.ssbmax.ui.components.result.UnifiedOLQResultTemplate

/**
 * PPDT Submission Result Screen - Shows story submission confirmation and pending review
 *
 * Uses the unified result template for consistent display across all OLQ-based tests.
 */
@Composable
fun PPDTSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    onViewFeedback: () -> Unit = {},
    viewModel: PPDTSubmissionResultViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(R.string.result_ppdt_title),
        submissionConfirmationContent = { state ->
            PPDTSubmissionCard(state)
        },
        testSpecificContent = { state ->
            PPDTStoryPreview(state)
        },
        submissionStatus = uiState.submission?.status,
        onNavigateHome = onNavigateHome,
        modifier = modifier
    )
}

/**
 * PPDT-specific submission confirmation card content.
 */
@Composable
private fun PPDTSubmissionCard(uiState: PPDTSubmissionResultUiState) {
    val submission = uiState.submission
    if (submission != null) {
        SubmissionConfirmationCard(
            title = stringResource(R.string.result_ppdt_story_submitted_title),
            subtitle = stringResource(
                R.string.result_ppdt_characters_written,
                submission.charactersCount
            )
        )
    }
}

/**
 * PPDT-specific story preview card.
 */
@Composable
private fun PPDTStoryPreview(uiState: PPDTSubmissionResultUiState) {
    val submission = uiState.submission
    if (submission != null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.result_ppdt_your_story_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                HorizontalDivider()

                Text(
                    text = submission.story,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.result_ppdt_character_count_label),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${submission.charactersCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.result_ppdt_writing_time_label),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(
                            R.string.result_ppdt_writing_time_value,
                            submission.writingTimeTakenMinutes
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
