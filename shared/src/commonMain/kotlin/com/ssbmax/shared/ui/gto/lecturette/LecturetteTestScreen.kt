package com.ssbmax.shared.ui.gto.lecturette

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.platform.audio.WhiteNoisePlayer
import com.ssbmax.shared.presentation.lecturette.LecturettePhase
import com.ssbmax.shared.presentation.lecturette.LecturetteTestUiState
import com.ssbmax.shared.presentation.lecturette.LecturetteTestViewModel
import com.ssbmax.shared.ui.gto.common.AnimatedWhiteNoiseOverlay
import com.ssbmax.shared.ui.gto.common.GTOErrorDialog
import com.ssbmax.shared.ui.gto.common.GTOExitDialog
import com.ssbmax.shared.ui.gto.common.GTOLimitDialog
import com.ssbmax.shared.ui.gto.common.GTOSubmissionSuccessScreen
import com.ssbmax.shared.ui.gto.common.rememberWhiteNoiseState
import com.ssbmax.shared.ui.gto.lecturette.components.LecturetteInstructionsPhase
import com.ssbmax.shared.ui.gto.lecturette.components.LecturetteReviewPhase
import com.ssbmax.shared.ui.gto.lecturette.components.LecturetteSpeechPhase
import com.ssbmax.shared.ui.gto.lecturette.components.LecturetteTopicSelectionPhase
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.lecturette_title

/**
 * KMP port of `app/.../ui/tests/gto/lecturette/LecturetteTestScreen.kt`.
 * Same `koinViewModel`/`collectAsStateWithLifecycle` pattern as
 * [com.ssbmax.shared.ui.gto.gd.GDTestScreen] (Phase 1 of the KMP-convergence
 * plan).
 */
@Composable
fun LecturetteTestScreen(
    testId: String,
    onTestComplete: (submissionId: String, subscriptionType: SubscriptionTier) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    onNavigateToUpgrade: () -> Unit = {},
    viewModel: LecturetteTestViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val whiteNoisePlayer = koinInject<WhiteNoisePlayer>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val whiteNoiseState = rememberWhiteNoiseState(whiteNoisePlayer)
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            whiteNoiseState.disable()
        }
    }

    LaunchedEffect(testId) { viewModel.loadTest(testId) }

    LaunchedEffect(uiState.phase) {
        when (uiState.phase) {
            LecturettePhase.SPEECH -> whiteNoiseState.enable()
            else -> whiteNoiseState.disable()
        }
    }

    LaunchedEffect(uiState.isCompleted) {
        val submissionId = uiState.submissionId
        if (uiState.isCompleted && !submissionId.isNullOrBlank()) {
            onTestComplete(submissionId, uiState.subscriptionType)
        }
    }

    LecturetteDialogs(
        uiState = uiState,
        showExitDialog = showExitDialog,
        onDismissExit = { showExitDialog = false },
        onExit = { whiteNoiseState.disable(); onNavigateBack() },
        onUpgrade = { viewModel.dismissLimitDialog(); onNavigateToUpgrade() },
        onDismissLimit = { viewModel.dismissLimitDialog(); onNavigateBack() },
        onDismissError = { viewModel.dismissError() }
    )

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> when (uiState.phase) {
                LecturettePhase.INSTRUCTIONS -> LecturetteInstructionsPhase(
                    onStart = { viewModel.proceedToTopicSelection() },
                    onNavigateBack = onNavigateBack
                )
                LecturettePhase.TOPIC_SELECTION -> LecturetteTopicSelectionPhase(
                    topics = uiState.topicChoices,
                    onTopicSelected = { viewModel.selectTopic(it) },
                    onNavigateBack = onNavigateBack
                )
                LecturettePhase.SPEECH -> LecturetteSpeechPhase(
                    selectedTopic = uiState.selectedTopic,
                    speechTranscript = uiState.speechTranscript,
                    charCount = uiState.charCount,
                    timeRemaining = uiState.formattedTime,
                    isTimeLow = uiState.isTimeLow,
                    onTranscriptChanged = viewModel::onTranscriptChanged,
                    onProceedToReview = { viewModel.proceedToReview() },
                    onNavigateBack = { showExitDialog = true }
                )
                LecturettePhase.REVIEW -> LecturetteReviewPhase(
                    selectedTopic = uiState.selectedTopic,
                    speechTranscript = uiState.speechTranscript,
                    charCount = uiState.charCount,
                    isSubmitting = uiState.isSubmitting,
                    onBackToSpeech = { viewModel.backToSpeech() },
                    onSubmit = { viewModel.submitTest() }
                )
                LecturettePhase.SUBMITTED -> GTOSubmissionSuccessScreen(
                    testName = stringResource(Res.string.lecturette_title),
                    onNavigateHome = onNavigateBack
                )
            }
        }

        if (uiState.phase == LecturettePhase.SPEECH) {
            AnimatedWhiteNoiseOverlay(baseAlpha = 0.08f, intensityRange = 0.03f, isEnabled = whiteNoiseState.isEnabled.value)
        }
    }
}

@Composable
private fun LecturetteDialogs(
    uiState: LecturetteTestUiState,
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
    if (uiState.showLimitDialog) {
        GTOLimitDialog(message = uiState.limitMessage, onUpgrade = onUpgrade, onDismiss = onDismissLimit)
    }
    if (uiState.error != null) {
        GTOErrorDialog(message = uiState.error, onDismiss = onDismissError)
    }
}
