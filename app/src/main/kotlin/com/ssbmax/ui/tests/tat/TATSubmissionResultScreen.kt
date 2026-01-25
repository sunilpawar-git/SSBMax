package com.ssbmax.ui.tests.tat

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
 * TAT Submission Result Screen - Shows AI score and pending instructor review
 *
 * Uses the unified result template for consistent display across all OLQ-based tests.
 */
@Composable
fun TATSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    onViewFeedback: () -> Unit = {},
    viewModel: TATSubmissionResultViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(R.string.result_tat_title),
        submissionConfirmationContent = { state ->
            TATSubmissionCard(state)
        },
        submissionStatus = uiState.submission?.status,
        onNavigateHome = onNavigateHome,
        modifier = modifier
    )
}

/**
 * TAT-specific submission confirmation card content.
 */
@Composable
private fun TATSubmissionCard(uiState: TATSubmissionResultUiState) {
    val submission = uiState.submission
    if (submission != null) {
        SubmissionConfirmationCard(
            subtitle = stringResource(
                R.string.result_tat_stories_completed,
                submission.totalStories,
                12
            )
        )
    }
}
