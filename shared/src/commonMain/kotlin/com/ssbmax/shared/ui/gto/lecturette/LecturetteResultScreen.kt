package com.ssbmax.shared.ui.gto.lecturette

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ssbmax.shared.presentation.lecturetteresult.LecturetteResultUiState
import com.ssbmax.shared.presentation.lecturetteresult.LecturetteResultViewModel
import com.ssbmax.shared.ui.components.result.OLQResultContent
import com.ssbmax.shared.ui.components.result.SubmissionConfirmationCard
import com.ssbmax.shared.ui.components.result.UnifiedOLQResultTemplate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.lecturette_result_situations
import ssbmax.shared.generated.resources.lecturette_result_title

/**
 * KMP port of `app/.../ui/tests/gto/lecturette/LecturetteResultScreen.kt`.
 * Same [UnifiedOLQResultTemplate] reuse as [com.ssbmax.shared.ui.gto.gd.GDResultScreen].
 */
@Composable
fun LecturetteResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: LecturetteResultViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(submissionId) { viewModel.loadSubmission(submissionId) }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(Res.string.lecturette_result_title),
        submissionConfirmationContent = { state -> LecturetteSubmissionCard(state) },
        testSpecificContent = { state -> state.olqResult?.let { OLQResultContent(olqResult = it) } },
        submissionStatus = null,
        showInstructorReview = false,
        onNavigateHome = onNavigateHome,
        onRetry = { viewModel.retry(submissionId) },
        modifier = modifier
    )
}

@Composable
private fun LecturetteSubmissionCard(uiState: LecturetteResultUiState) {
    if (uiState.submission == null) return
    SubmissionConfirmationCard(subtitle = stringResource(Res.string.lecturette_result_situations, uiState.formattedTimeSpent))
}
