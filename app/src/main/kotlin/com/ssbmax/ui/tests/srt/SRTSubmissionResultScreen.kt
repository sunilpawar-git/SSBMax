package com.ssbmax.ui.tests.srt

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
 * SRT Submission Result Screen - Shows AI analysis and pending instructor review
 *
 * Uses the unified result template for consistent display across all OLQ-based tests.
 */
@Composable
fun SRTSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    onViewFeedback: () -> Unit = {},
    viewModel: SRTSubmissionResultViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(R.string.result_srt_title),
        submissionConfirmationContent = { state ->
            SRTSubmissionCard(state)
        },
        submissionStatus = uiState.submission?.status,
        onNavigateHome = onNavigateHome,
        modifier = modifier
    )
}

/**
 * SRT-specific submission confirmation card content.
 */
@Composable
private fun SRTSubmissionCard(uiState: SRTSubmissionResultUiState) {
    val submission = uiState.submission
    if (submission != null) {
        SubmissionConfirmationCard(
            subtitle = stringResource(
                R.string.result_srt_situations_completed,
                submission.validResponses,
                submission.totalResponses
            )
        )
    }
}
