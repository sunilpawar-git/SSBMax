package com.ssbmax.shared.ui.gto.gd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.presentation.gd.GDPhase
import com.ssbmax.shared.presentation.gd.GDTestViewModel
import com.ssbmax.shared.ui.gto.common.AnimatedWhiteNoiseOverlay
import com.ssbmax.shared.ui.gto.common.GTOErrorDialog
import com.ssbmax.shared.ui.gto.common.GTOExitDialog
import com.ssbmax.shared.ui.gto.common.GTOLimitDialog
import com.ssbmax.shared.ui.gto.common.GTOSubmissionSuccessScreen
import com.ssbmax.shared.ui.gto.common.rememberWhiteNoiseState
import com.ssbmax.shared.ui.gto.gd.components.GDDiscussionPhase
import com.ssbmax.shared.ui.gto.gd.components.GDInstructionsPhase
import com.ssbmax.shared.ui.gto.gd.components.GDReviewPhase
import com.ssbmax.shared.platform.audio.WhiteNoisePlayer
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.gd_title

/**
 * KMP port of `app/.../ui/tests/gto/gd/GDTestScreen.kt`. Uses
 * `koinInject<GDTestViewModel>()` (plain-class ViewModel pattern, not
 * `koinViewModel()`), matching every other Phase 5 test-taking screen -- a
 * [DisposableEffect] calls [GDTestViewModel.close] on leaving the screen.
 */
@Composable
fun GDTestScreen(
    testId: String,
    onTestComplete: (submissionId: String, subscriptionType: SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: GDTestViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val whiteNoisePlayer = koinInject<WhiteNoisePlayer>()
    val uiState by viewModel.uiState.collectAsState()
    val whiteNoiseState = rememberWhiteNoiseState(whiteNoisePlayer)
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            whiteNoiseState.disable()
            viewModel.close()
        }
    }

    LaunchedEffect(testId) { viewModel.loadTest(testId) }

    LaunchedEffect(uiState.phase) {
        when (uiState.phase) {
            GDPhase.DISCUSSION -> whiteNoiseState.enable()
            else -> whiteNoiseState.disable()
        }
    }

    LaunchedEffect(uiState.isCompleted) {
        val submissionId = uiState.submissionId
        if (uiState.isCompleted && !submissionId.isNullOrBlank()) {
            onTestComplete(submissionId, uiState.subscriptionType)
        }
    }

    if (showExitDialog) {
        GTOExitDialog(
            onDismiss = { showExitDialog = false },
            onExit = { whiteNoiseState.disable(); onNavigateBack() }
        )
    }

    if (uiState.showLimitDialog) {
        GTOLimitDialog(
            message = uiState.limitMessage,
            onUpgrade = { viewModel.dismissLimitDialog(); onNavigateBack() },
            onDismiss = { viewModel.dismissLimitDialog(); onNavigateBack() }
        )
    }

    if (uiState.error != null) {
        GTOErrorDialog(message = uiState.error!!, onDismiss = { viewModel.dismissError() })
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> when (uiState.phase) {
                GDPhase.INSTRUCTIONS -> GDInstructionsPhase(
                    onStart = { viewModel.startDiscussion() },
                    onNavigateBack = onNavigateBack
                )
                GDPhase.DISCUSSION -> GDDiscussionPhase(
                    topic = uiState.topic,
                    response = uiState.response,
                    charCount = uiState.charCount,
                    timeRemaining = uiState.formattedTime,
                    isTimeLow = uiState.isTimeLow,
                    onResponseChanged = viewModel::onResponseChanged,
                    onProceedToReview = { viewModel.proceedToReview() },
                    onNavigateBack = { showExitDialog = true }
                )
                GDPhase.REVIEW -> GDReviewPhase(
                    topic = uiState.topic,
                    response = uiState.response,
                    charCount = uiState.charCount,
                    isSubmitting = uiState.isSubmitting,
                    onBackToDiscussion = { viewModel.backToDiscussion() },
                    onSubmit = { viewModel.submitTest() }
                )
                GDPhase.SUBMITTED -> GTOSubmissionSuccessScreen(testName = stringResource(Res.string.gd_title), onNavigateHome = onNavigateBack)
            }
        }

        if (uiState.phase == GDPhase.DISCUSSION) {
            AnimatedWhiteNoiseOverlay(baseAlpha = 0.08f, intensityRange = 0.03f, isEnabled = whiteNoiseState.isEnabled.value)
        }
    }
}
