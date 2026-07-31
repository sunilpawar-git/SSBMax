package com.ssbmax.shared.ui.srt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.presentation.srtresult.SRTSubmissionResultUiState
import com.ssbmax.shared.presentation.srtresult.SRTSubmissionResultViewModel
import com.ssbmax.shared.ui.components.result.OLQResultContent
import com.ssbmax.shared.ui.components.result.SubmissionConfirmationCard
import com.ssbmax.shared.ui.components.result.UnifiedOLQResultTemplate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.result_srt_situations_completed
import ssbmax.shared.generated.resources.result_srt_title

/**
 * KMP port of `app/.../ui/tests/srt/SRTSubmissionResultScreen.kt`. Uses
 * [UnifiedOLQResultTemplate] and [OLQResultContent] exactly the way the
 * Android original does (SRT uses the generic score-breakdown card, same as
 * TAT/WAT), both already ported into `shared` during this phase's earlier
 * sessions, reused unchanged here.
 *
 * Uses `koinViewModel<SRTSubmissionResultViewModel>()`; `viewModelScope` is
 * cancelled automatically on leaving the screen, cancelling the still-open
 * Firestore listener.
 */
@Composable
fun SRTSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: SRTSubmissionResultViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(Res.string.result_srt_title),
        submissionConfirmationContent = { state -> SRTSubmissionCard(state) },
        testSpecificContent = { state -> state.olqResult?.let { OLQResultContent(olqResult = it) } },
        submissionStatus = uiState.submission?.status,
        onNavigateHome = onNavigateHome,
        modifier = modifier
    )
}

@Composable
private fun SRTSubmissionCard(uiState: SRTSubmissionResultUiState) {
    val submission = uiState.submission ?: return
    SubmissionConfirmationCard(
        subtitle = stringResource(Res.string.result_srt_situations_completed, submission.validResponses, submission.totalResponses)
    )
}
