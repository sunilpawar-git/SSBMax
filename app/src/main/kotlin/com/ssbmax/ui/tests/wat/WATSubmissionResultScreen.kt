package com.ssbmax.ui.tests.wat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.ui.components.result.SubmissionConfirmationCard
import com.ssbmax.ui.components.result.UnifiedOLQResultTemplate

/**
 * WAT Submission Result Screen - Shows AI analysis and pending instructor review
 *
 * Uses the unified result template for consistent display across all OLQ-based tests.
 */
@Composable
fun WATSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    onViewFeedback: () -> Unit = {},
    viewModel: WATSubmissionResultViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(R.string.result_wat_title),
        submissionConfirmationContent = { state ->
            WATSubmissionCard(state)
        },
        submissionStatus = uiState.submission?.status,
        onNavigateHome = onNavigateHome,
        modifier = modifier
    )
}

/**
 * WAT-specific submission confirmation card content.
 */
@Composable
private fun WATSubmissionCard(uiState: WATSubmissionResultUiState) {
    val submission = uiState.submission
    if (submission != null) {
        SubmissionConfirmationCard(
            subtitle = stringResource(
                R.string.result_wat_responses_completed,
                submission.validResponses,
                submission.totalResponses
            )
        )
    }
}
