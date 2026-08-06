package com.ssbmax.shared.ui.gto.gpe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.domain.model.GPEPhase
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.presentation.gpe.GPETestUiState
import com.ssbmax.shared.presentation.gpe.GPETestViewModel
import com.ssbmax.shared.ui.common.TestLimitReachedDialog
import com.ssbmax.shared.ui.gto.common.GTOErrorDialog
import com.ssbmax.shared.ui.gto.common.GTOExitDialog
import com.ssbmax.shared.ui.gto.common.GTOSubmissionSuccessScreen
import com.ssbmax.shared.ui.gto.gpe.components.GPEInstructionsPhase
import com.ssbmax.shared.ui.gto.gpe.components.GPEPlanningPhase
import com.ssbmax.shared.ui.gto.gpe.components.GPEReviewPhase
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.gpe_title

/**
 * KMP port of `app/.../ui/tests/gpe/GPETestScreen.kt`. Same
 * `koinInject`/`DisposableEffect` pattern as
 * [com.ssbmax.shared.ui.gto.gd.GDTestScreen] /
 * [com.ssbmax.shared.ui.gto.lecturette.LecturetteTestScreen].
 *
 * No white-noise overlay -- GPE is a solo written-response test (like GD's
 * discussion phase superficially resembles, but the Android original never
 * enables `WhiteNoisePlayer` anywhere in `GPETestViewModel`/`GPETestScreen`;
 * white noise is GD/Lecturette-specific, simulating a live group/speech
 * environment, not applicable to GPE's silent planning phase).
 *
 * `GPEPhase.IMAGE_VIEWING` is intentionally NOT handled here -- see
 * [GPETestViewModel]'s doc comment: it is dead, unreachable code in the
 * Android original (the real flow is INSTRUCTIONS -> PLANNING -> REVIEW ->
 * SUBMITTED), so this `when` is exhaustive over the phases actually reached,
 * with a no-op branch for the unreachable one (matching the Android
 * original's own empty `{}` block for that case).
 *
 * Uses `koinViewModel<GPETestViewModel>()` (Phase 1 of the KMP-convergence
 * plan) -- the timer coroutine runs on `viewModelScope` and is cancelled
 * automatically in `onCleared()`, no manual `DisposableEffect` teardown
 * needed.
 */
@Composable
fun GPETestScreen(
    testId: String,
    onTestComplete: (submissionId: String, subscriptionType: SubscriptionTier) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    onNavigateToUpgrade: () -> Unit = {},
    viewModel: GPETestViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(testId) { viewModel.loadTest(testId) }

    LaunchedEffect(uiState.isCompleted) {
        val submissionId = uiState.submissionId
        if (uiState.isCompleted && !submissionId.isNullOrBlank()) {
            onTestComplete(submissionId, uiState.subscriptionType)
        }
    }

    GPEDialogs(
        uiState = uiState,
        showExitDialog = showExitDialog,
        onDismissExit = { showExitDialog = false },
        onExit = onNavigateBack,
        onUpgrade = { viewModel.dismissLimitDialog(); onNavigateToUpgrade() },
        onDismissLimit = { viewModel.dismissLimitDialog(); onNavigateBack() },
        onDismissError = { viewModel.dismissError() }
    )

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading || uiState.question == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> when (uiState.phase) {
                GPEPhase.INSTRUCTIONS -> GPEInstructionsPhase(
                    onStart = { viewModel.startTest() },
                    onNavigateBack = onNavigateBack
                )
                GPEPhase.IMAGE_VIEWING -> Unit // unreachable in the real flow; see doc comment above
                GPEPhase.PLANNING -> GPEPlanningPhase(
                    question = uiState.question!!,
                    planningResponse = uiState.planningResponse,
                    charactersCount = uiState.charactersCount,
                    timeRemaining = uiState.formattedTime,
                    isTimeLow = uiState.timeRemaining in 1..29,
                    onResponseChanged = viewModel::onPlanningResponseChanged,
                    onProceedToReview = { viewModel.proceedToReview() },
                    onNavigateBack = { showExitDialog = true }
                )
                GPEPhase.REVIEW -> GPEReviewPhase(
                    question = uiState.question!!,
                    planningResponse = uiState.planningResponse,
                    charactersCount = uiState.charactersCount,
                    isSubmitting = uiState.isSubmitting,
                    onEdit = { viewModel.returnToPlanning() },
                    onSubmit = { viewModel.submitTest() }
                )
                GPEPhase.SUBMITTED -> GTOSubmissionSuccessScreen(testName = stringResource(Res.string.gpe_title), onNavigateHome = onNavigateBack)
            }
        }
    }
}

@Composable
private fun GPEDialogs(
    uiState: GPETestUiState,
    showExitDialog: Boolean,
    onDismissExit: () -> Unit,
    onExit: () -> Unit,
    onUpgrade: () -> Unit,
    onDismissLimit: () -> Unit,
    onDismissError: () -> Unit
) {
    if (showExitDialog) {
        GTOExitDialog(onDismiss = onDismissExit, onExit = onExit)
    }
    uiState.limitReached?.let { limitReached ->
        TestLimitReachedDialog(
            tier = limitReached.tier,
            testsLimit = limitReached.limit,
            testsUsed = limitReached.usedCount,
            resetsAt = limitReached.resetsAt,
            onUpgrade = onUpgrade,
            onDismiss = onDismissLimit
        )
    }
    if (uiState.error != null) {
        GTOErrorDialog(message = uiState.error, onDismiss = onDismissError)
    }
    if (uiState.submitError != null) {
        GTOErrorDialog(message = uiState.submitError, onDismiss = onDismissError)
    }
}
