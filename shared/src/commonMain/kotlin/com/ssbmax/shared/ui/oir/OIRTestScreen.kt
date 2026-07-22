package com.ssbmax.shared.ui.oir

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.presentation.oir.OIRErrorType
import com.ssbmax.shared.presentation.oir.OIRTestViewModel
import com.ssbmax.shared.ui.common.TestLimitReachedDialog
import com.ssbmax.shared.ui.oir.components.OIRQuestionView
import com.ssbmax.shared.ui.oir.components.OIRTestBottomBar
import com.ssbmax.shared.ui.oir.components.OIRTestTopBar
import org.koin.compose.koinInject
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

/**
 * KMP port of `app/.../ui/tests/oir/OIRTestScreen.kt`. Delegates rendering
 * to [OIRTestTopBar]/[OIRQuestionView]/[OIRTestBottomBar], same as the
 * Android original.
 *
 * Uses `koinInject<OIRTestViewModel>()` (not `koinViewModel()`) — matches
 * this phase's established plain-class ViewModel pattern
 * ([com.ssbmax.shared.presentation.home.student.StudentHomeViewModel] etc.);
 * a [DisposableEffect] calls [OIRTestViewModel.close] on leaving the screen
 * so the countdown-timer coroutine and its `CoroutineScope` are cancelled —
 * the Android original relied on `androidx.lifecycle.ViewModel.onCleared()`
 * for this, which doesn't exist for a plain-class ViewModel.
 *
 * `TestContentErrorState`/`TestContentLoadingState` (Android-only,
 * `app/.../ui/components/`) are not ported — this screen inlines the
 * loading/error states directly (small enough that a dedicated shared
 * component isn't warranted yet; revisit if a second ported test-type
 * vertical needs the same shapes).
 */
@Composable
fun OIRTestScreen(
    onTestComplete: (submissionId: String, subscriptionType: SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: OIRTestViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

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
                uiState.isLoading -> Text(stringResource(Res.string.oir_loading))
                uiState.errorType != null -> Text(stringResource(errorMessageFor(uiState.errorType!!)))
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
