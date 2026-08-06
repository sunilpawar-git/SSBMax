package com.ssbmax.shared.ui.oir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.presentation.oir.OIRErrorType
import com.ssbmax.shared.presentation.oir.OIRTestViewModel
import com.ssbmax.shared.ui.common.TestLimitReachedDialog
import com.ssbmax.shared.ui.oir.components.OIRQuestionView
import com.ssbmax.shared.ui.oir.components.OIRTestBottomBar
import com.ssbmax.shared.ui.oir.components.OIRTestTopBar
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.oir_continue_test
import ssbmax.shared.generated.resources.oir_error_auth_required
import ssbmax.shared.generated.resources.oir_error_invalid_question
import ssbmax.shared.generated.resources.oir_error_questions_unavailable
import ssbmax.shared.generated.resources.oir_error_submit_failed
import ssbmax.shared.generated.resources.oir_exit
import ssbmax.shared.generated.resources.oir_exit_message
import ssbmax.shared.generated.resources.oir_exit_title
import ssbmax.shared.generated.resources.oir_loading
import ssbmax.shared.generated.resources.oir_retry

/**
 * KMP port of `app/.../ui/tests/oir/OIRTestScreen.kt`. Delegates rendering
 * to [OIRTestTopBar]/[OIRQuestionView]/[OIRTestBottomBar], same as the
 * Android original.
 *
 * Uses `koinViewModel<OIRTestViewModel>()` — the countdown-timer coroutine
 * runs on `viewModelScope` and is cancelled automatically in
 * [OIRTestViewModel.onCleared], same as the Android original's
 * `androidx.lifecycle.ViewModel.onCleared()`; no manual `DisposableEffect`
 * teardown needed (Phase 1 of the KMP-convergence plan).
 *
 * `TestContentErrorState`/`TestContentLoadingState` (Android-only,
 * `app/.../ui/components/`) are not ported — this screen inlines the
 * loading/error states directly (small enough that a dedicated shared
 * component isn't warranted yet; revisit if a second ported test-type
 * vertical needs the same shapes).
 */
@Composable
fun OIRTestScreen(
    onTestComplete: (submissionId: String, subscriptionType: SubscriptionTier) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: OIRTestViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted && uiState.sessionId != null && uiState.subscriptionType != null) {
            onTestComplete(uiState.sessionId!!, uiState.subscriptionType!!)
        }
    }

    if (uiState.isLimitReached) {
        TestLimitReachedDialog(
            tier = uiState.subscriptionTier,
            testsLimit = uiState.testsLimit,
            testsUsed = uiState.testsUsed,
            resetsAt = uiState.resetsAt,
            onUpgrade = { onNavigateBack() },
            onDismiss = onNavigateBack
        )
        return
    }

    if (uiState.isLoading) {
        OIRTestMessage(message = stringResource(Res.string.oir_loading))
        return
    }
    uiState.errorType?.let { errorType ->
        OIRTestMessage(
            message = stringResource(errorMessageFor(errorType)),
            actionLabel = stringResource(Res.string.oir_retry),
            onAction = { viewModel.loadTest() }
        )
        return
    }

    Scaffold(
        topBar = {
            OIRTestTopBar(
                questionNumber = uiState.currentQuestionIndex + 1,
                totalQuestions = uiState.totalQuestions,
                timeRemainingSeconds = uiState.timeRemainingSeconds,
                onExitClick = { showExitDialog = true }
            )
        },
        bottomBar = {
            OIRTestBottomBar(
                currentIndex = uiState.currentQuestionIndex,
                totalQuestions = uiState.totalQuestions,
                isAnswered = uiState.currentQuestionAnswered,
                onPrevious = { viewModel.previousQuestion() },
                onNext = { viewModel.nextQuestion() },
                onSubmit = { viewModel.submitTest() }
            )
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.currentQuestion != null -> OIRQuestionView(
                    question = uiState.currentQuestion!!,
                    selectedOptionIds = uiState.selectedOptionIds,
                    onOptionSelected = { viewModel.selectOption(it) },
                    showFeedback = uiState.showFeedback,
                    isCorrect = uiState.isCurrentAnswerCorrect,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showExitDialog) {
        OIRExitDialog(
            onExit = {
                viewModel.pauseTest()
                onNavigateBack()
            },
            onDismiss = { showExitDialog = false }
        )
    }
}

@Composable
private fun OIRTestMessage(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(message)
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

private fun errorMessageFor(errorType: OIRErrorType) = when (errorType) {
    OIRErrorType.AUTH_REQUIRED -> Res.string.oir_error_auth_required
    OIRErrorType.QUESTIONS_UNAVAILABLE -> Res.string.oir_error_questions_unavailable
    OIRErrorType.SUBMIT_FAILED -> Res.string.oir_error_submit_failed
    OIRErrorType.INVALID_QUESTION -> Res.string.oir_error_invalid_question
}

@Composable
private fun OIRExitDialog(onExit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.oir_exit_title)) },
        text = { Text(stringResource(Res.string.oir_exit_message)) },
        confirmButton = {
            TextButton(onClick = onExit) { Text(stringResource(Res.string.oir_exit)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.oir_continue_test)) }
        }
    )
}
