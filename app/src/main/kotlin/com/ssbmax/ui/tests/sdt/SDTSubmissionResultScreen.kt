package com.ssbmax.ui.tests.sdt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
 * SDT Submission Result Screen - Shows submission summary and OLQ analysis
 *
 * Uses the unified result template for consistent display across all OLQ-based tests.
 */
@Composable
fun SDTSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: SDTSubmissionResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(R.string.result_sdt_title),
        submissionConfirmationContent = { state ->
            SDTSubmissionCard(state)
        },
        testSpecificContent = { state ->
            SDTResponsesList(state)
        },
        submissionStatus = uiState.submission?.status,
        onNavigateHome = onNavigateHome
    )
}

/**
 * SDT-specific submission confirmation card content.
 */
@Composable
private fun SDTSubmissionCard(uiState: SDTSubmissionResultUiState) {
    val submission = uiState.submission
    if (submission != null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.sdt_result_summary_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(stringResource(R.string.sdt_result_completed, submission.totalResponses))
                Text(stringResource(R.string.sdt_result_time_taken, submission.totalTimeTakenMinutes))
                Text(stringResource(R.string.sdt_result_valid_responses, submission.validResponses))
            }
        }
    }
}

/**
 * SDT-specific responses list showing each question and answer.
 */
@Composable
private fun SDTResponsesList(uiState: SDTSubmissionResultUiState) {
    val submission = uiState.submission
    if (submission != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            submission.responses.forEach { response ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = response.question,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!response.isSkipped) {
                            Text(
                                text = response.answer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.result_sdt_characters_label, response.charCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.sdt_result_skipped),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
