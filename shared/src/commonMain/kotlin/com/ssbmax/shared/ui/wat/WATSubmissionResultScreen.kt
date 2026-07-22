package com.ssbmax.shared.ui.wat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ssbmax.shared.presentation.watresult.WATSubmissionResultUiState
import com.ssbmax.shared.presentation.watresult.WATSubmissionResultViewModel
import com.ssbmax.shared.ui.components.result.OLQResultContent
import com.ssbmax.shared.ui.components.result.SubmissionConfirmationCard
import com.ssbmax.shared.ui.components.result.UnifiedOLQResultTemplate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.result_wat_responses_completed
import ssbmax.shared.generated.resources.result_wat_title

/**
 * KMP port of `app/.../ui/tests/wat/WATSubmissionResultScreen.kt`. Uses
 * [UnifiedOLQResultTemplate] and [OLQResultContent] exactly the way the
 * Android original does (WAT uses the generic score-breakdown card, same as
 * TAT) -- both already ported into `shared` during this phase's PPDT/TAT
 * sessions, reused unchanged here.
 *
 * Uses `koinInject<WATSubmissionResultViewModel>()` (not `koinViewModel()`);
 * a [DisposableEffect] calls [WATSubmissionResultViewModel.close] on leaving
 * the screen, cancelling the still-open Firestore listener.
 *
 * No partial-assessment section here (unlike TAT's `TATPartialAssessmentSection`)
 * -- WAT scores 60 independent one-line responses rather than batching
 * multiple long-form stories, so the "some entries could not be analyzed"
 * degradation TAT documents doesn't apply the same way; the Android
 * original's `WATSubmissionResultUiState` has no `usesPartialAssessment`
 * field to port.
 */
@Composable
fun WATSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: WATSubmissionResultViewModel = koinInject(),
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
        testTitle = stringResource(Res.string.result_wat_title),
        submissionConfirmationContent = { state -> WATSubmissionCard(state) },
        testSpecificContent = { state -> state.olqResult?.let { OLQResultContent(olqResult = it) } },
        submissionStatus = uiState.submission?.status,
        onNavigateHome = onNavigateHome,
        modifier = modifier
    )
}

@Composable
private fun WATSubmissionCard(uiState: WATSubmissionResultUiState) {
    val submission = uiState.submission ?: return
    SubmissionConfirmationCard(
        subtitle = stringResource(Res.string.result_wat_responses_completed, submission.validResponses, submission.totalResponses)
    )
}
