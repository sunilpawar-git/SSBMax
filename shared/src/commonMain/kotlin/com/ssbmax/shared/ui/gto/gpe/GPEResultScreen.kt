package com.ssbmax.shared.ui.gto.gpe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ssbmax.shared.presentation.gperesult.GPEResultUiState
import com.ssbmax.shared.presentation.gperesult.GPEResultViewModel
import com.ssbmax.shared.ui.components.result.OLQResultContent
import com.ssbmax.shared.ui.components.result.SubmissionConfirmationCard
import com.ssbmax.shared.ui.components.result.UnifiedOLQResultTemplate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.gpe_result_time_spent
import ssbmax.shared.generated.resources.gpe_result_title

/**
 * KMP port of `app/.../ui/tests/gpe/GPESubmissionResultScreen.kt`. Same
 * [UnifiedOLQResultTemplate] reuse as
 * [com.ssbmax.shared.ui.gto.lecturette.LecturetteResultScreen].
 */
@Composable
fun GPEResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: GPEResultViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(submissionId) { viewModel.loadSubmission(submissionId) }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(Res.string.gpe_result_title),
        submissionConfirmationContent = { state -> GPESubmissionCard(state) },
        testSpecificContent = { state -> state.olqResult?.let { OLQResultContent(olqResult = it) } },
        submissionStatus = null,
        showInstructorReview = false,
        onNavigateHome = onNavigateHome,
        onRetry = { viewModel.retry(submissionId) },
        modifier = modifier
    )
}

@Composable
private fun GPESubmissionCard(uiState: GPEResultUiState) {
    if (uiState.submission == null) return
    SubmissionConfirmationCard(subtitle = stringResource(Res.string.gpe_result_time_spent, uiState.formattedTimeSpent))
}
