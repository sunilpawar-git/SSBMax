package com.ssbmax.ui.tests.oir

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState
import com.ssbmax.ui.tests.oir.components.OIRQuestionView
import com.ssbmax.ui.tests.oir.components.OIRTestBottomBar
import com.ssbmax.ui.tests.oir.components.OIRTestTopBar

/**
 * OIR Test Screen — main entry point.
 *
 * Delegates rendering to component composables:
 *   - [OIRTestTopBar]      — progress counter + timer
 *   - [OIRQuestionView]    — question text, optional figure, options, feedback
 *   - [OIRTestBottomBar]   — previous / next / submit navigation
 *
 * Follows the standard pattern used by TAT/WAT/SRT: passes submissionId and
 * subscriptionType to the navigation callback; the result screen fetches data
 * from Firestore independently.
 */
@Composable
fun OIRTestScreen(
    onTestComplete: (submissionId: String, subscriptionType: com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: OIRTestViewModel = hiltViewModel(),
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
        com.ssbmax.ui.tests.common.TestLimitReachedDialog(
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
                uiState.isLoading -> TestContentLoadingState(
                    message = stringResource(R.string.oir_loading),
                    modifier = Modifier.fillMaxSize()
                )
                uiState.errorResId != null -> TestContentErrorState(
                    error = stringResource(uiState.errorResId!!),
                    onRetry = { viewModel.loadTest() },
                    modifier = Modifier.fillMaxSize()
                )
                uiState.currentQuestion != null -> OIRQuestionView(
                    question = uiState.currentQuestion!!,
                    selectedOptionId = uiState.selectedOptionIds.singleOrNull(),
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
private fun OIRExitDialog(onExit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.oir_exit_title)) },
        text = { Text(stringResource(R.string.oir_exit_message)) },
        confirmButton = {
            TextButton(onClick = onExit) { Text(stringResource(R.string.oir_exit)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.oir_continue_test)) }
        }
    )
}
