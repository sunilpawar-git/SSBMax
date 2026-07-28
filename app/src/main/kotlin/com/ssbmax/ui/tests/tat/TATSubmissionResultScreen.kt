package com.ssbmax.ui.tests.tat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.ui.components.result.OLQResultContent
import com.ssbmax.ui.components.result.SubmissionConfirmationCard
import com.ssbmax.ui.components.result.UnifiedOLQResultTemplate
import org.koin.compose.viewmodel.koinViewModel

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
    viewModel: TATSubmissionResultViewModel = koinViewModel(),
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
        testSpecificContent = { state ->
            Column {
                state.olqResult?.let { result ->
                    TATPartialAssessmentSection(usesPartialAssessment = state.usesPartialAssessment, olqResult = result)
                    OLQResultContent(olqResult = result)
                }
            }
        },
        submissionStatus = uiState.submission?.status,
        onNavigateHome = onNavigateHome,
        modifier = modifier
    )
}

/**
 * Renders a subtle notice when synthesis completed using fewer than 12 valid stories,
 * so the candidate knows the result may be less precise than a full assessment.
 * Extracted as its own composable (rather than an inline `if`) so the gating logic
 * is directly testable without standing up the full ViewModel/Hilt-backed screen.
 */
@Composable
internal fun TATPartialAssessmentSection(usesPartialAssessment: Boolean, olqResult: OLQAnalysisResult) {
    if (usesPartialAssessment) {
        PartialAssessmentNotice(olqResult)
    }
}

@Composable
private fun PartialAssessmentNotice(olqResult: OLQAnalysisResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.result_tat_partial_assessment_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = stringResource(
                    R.string.result_tat_partial_assessment_message,
                    olqResult.validStoriesCount,
                    olqResult.validStoriesCount + olqResult.failedStoriesCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
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
