package com.ssbmax.shared.ui.gto.gd

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ssbmax.shared.presentation.gdresult.GDResultUiState
import com.ssbmax.shared.presentation.gdresult.GDResultViewModel
import com.ssbmax.shared.ui.components.result.SubmissionConfirmationCard
import com.ssbmax.shared.ui.components.result.OLQResultContent
import com.ssbmax.shared.ui.components.result.UnifiedOLQResultTemplate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.gd_result_situations
import ssbmax.shared.generated.resources.gd_result_title

/**
 * KMP port of `app/.../ui/tests/gto/gd/GDResultScreen.kt`. Reuses
 * [UnifiedOLQResultTemplate]/[OLQResultContent], same as every other
 * OLQ-scored test-type result screen (see [GDResultUiState]'s doc comment
 * for how GTO's own result shape is mapped into the shared contract).
 */
@Composable
fun GDResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: GDResultViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(submissionId) { viewModel.loadSubmission(submissionId) }

    UnifiedOLQResultTemplate(
        uiState = uiState,
        testTitle = stringResource(Res.string.gd_result_title),
        submissionConfirmationContent = { state -> GDSubmissionCard(state) },
        testSpecificContent = { state -> state.olqResult?.let { OLQResultContent(olqResult = it) } },
        submissionStatus = null,
        showInstructorReview = false,
        onNavigateHome = onNavigateHome,
        onRetry = { viewModel.retry(submissionId) },
        modifier = modifier
    )
}

@Composable
private fun GDSubmissionCard(uiState: GDResultUiState) {
    if (uiState.submission == null) return
    SubmissionConfirmationCard(subtitle = stringResource(Res.string.gd_result_situations, uiState.formattedTimeSpent))
}
