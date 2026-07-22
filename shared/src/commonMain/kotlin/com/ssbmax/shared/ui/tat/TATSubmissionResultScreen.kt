package com.ssbmax.shared.ui.tat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.shared.presentation.tatresult.TATSubmissionResultUiState
import com.ssbmax.shared.presentation.tatresult.TATSubmissionResultViewModel
import com.ssbmax.shared.ui.components.result.OLQResultContent
import com.ssbmax.shared.ui.components.result.SubmissionConfirmationCard
import com.ssbmax.shared.ui.components.result.UnifiedOLQResultTemplate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.result_tat_partial_assessment_message
import ssbmax.shared.generated.resources.result_tat_partial_assessment_title
import ssbmax.shared.generated.resources.result_tat_stories_completed
import ssbmax.shared.generated.resources.result_tat_title

/**
 * KMP port of `app/.../ui/tests/tat/TATSubmissionResultScreen.kt`. Uses
 * [UnifiedOLQResultTemplate] and [OLQResultContent] exactly the way the
 * Android original does (TAT uses the generic score-breakdown card, unlike
 * PPDT which uses its own `PPDTOLQReasoningCard`) -- both were already ported
 * into `shared` during this phase's PPDT session specifically so this TAT
 * session could reuse them unchanged, confirmed by reading
 * `shared/.../ui/components/result/OLQResultContent.kt` before writing this
 * file rather than duplicating it.
 *
 * Uses `koinInject<TATSubmissionResultViewModel>()` (not `koinViewModel()`);
 * a [DisposableEffect] calls [TATSubmissionResultViewModel.close] on leaving
 * the screen, cancelling the still-open Firestore listener.
 */
@Composable
fun TATSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: TATSubmissionResultViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(Res.string.result_tat_title),
        submissionConfirmationContent = { state -> TATSubmissionCard(state) },
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
 * Renders a subtle notice when synthesis completed using fewer than 12 valid
 * stories (`docs/architecture/TAT_Pipeline.md` §10a), so the candidate knows
 * the result may be less precise than a full assessment. Extracted as its
 * own composable, same as the Android original, so the gating logic is
 * directly testable without standing up the full screen.
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
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.result_tat_partial_assessment_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = stringResource(
                    Res.string.result_tat_partial_assessment_message,
                    olqResult.validStoriesCount,
                    olqResult.validStoriesCount + olqResult.failedStoriesCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun TATSubmissionCard(uiState: TATSubmissionResultUiState) {
    val submission = uiState.submission ?: return
    SubmissionConfirmationCard(
        subtitle = stringResource(Res.string.result_tat_stories_completed, submission.totalStories, 12)
    )
}
