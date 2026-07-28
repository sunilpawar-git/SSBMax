package com.ssbmax.ui.tests.gto.lecturette

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.di.GTOWhiteNoisePlayerEntryPoint
import com.ssbmax.ui.components.TestContentLoadingState
import com.ssbmax.ui.tests.gto.common.AnimatedWhiteNoiseOverlay
import com.ssbmax.ui.tests.gto.common.GTOSubmissionSuccessScreen
import com.ssbmax.ui.tests.gto.common.rememberWhiteNoiseState
import dagger.hilt.android.EntryPointAccessors

/**
 * Lecturette Test Screen
 * 
 * A 3-minute speech test where candidates:
 * 1. Choose 1 topic from 4 options
 * 2. Immediately deliver a speech (no preparation time)
 * 3. White noise plays during speech
 * 4. Submit for AI analysis
 * 
 * Phases:
 * 1. Instructions: Test format, rules, white noise warning
 * 2. Topic Selection: Choose 1 from 4 topics
 * 3. Speech: 3-minute timer + text input + white noise
 * 4. Review: Preview speech before submission
 * 5. Submitted: Navigate to result
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturetteTestScreen(
    testId: String,
    onTestComplete: (submissionId: String, subscriptionType: com.ssbmax.core.domain.model.SubscriptionType) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit = {},
    viewModel: LecturetteTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Inject GTOWhiteNoisePlayer via Hilt entry point (not a ViewModel)
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        GTOWhiteNoisePlayerEntryPoint::class.java
    )
    val whiteNoisePlayer = entryPoint.whiteNoisePlayer()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val whiteNoiseState = rememberWhiteNoiseState(whiteNoisePlayer)
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    
    // Load test on mount
    LaunchedEffect(testId) {
        viewModel.loadTest(testId)
    }
    
    // Enable white noise during speech phase
    LaunchedEffect(uiState.phase) {
        when (uiState.phase) {
            LecturettePhase.SPEECH -> whiteNoiseState.enable()
            else -> whiteNoiseState.disable()
        }
    }
    
    // Navigate to result screen when submission is complete
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            val submissionId = uiState.submissionId
            if (!submissionId.isNullOrBlank()) {
                android.util.Log.d("LecturetteTestScreen", "✅ Test submitted, navigating to result screen")
                android.util.Log.d("LecturetteTestScreen", "   - Submission ID: $submissionId")
                android.util.Log.d("LecturetteTestScreen", "   - Subscription type: ${uiState.subscriptionType}")
                onTestComplete(submissionId, uiState.subscriptionType)
            }
        }
    }
    
    
    // Cleanup on exit
    DisposableEffect(Unit) {
        onDispose {
            whiteNoiseState.disable()
        }
    }
    
    LecturetteTestDialogs(
        showExitDialog = showExitDialog,
        onDismissExitDialog = { showExitDialog = false },
        onConfirmExit = {
            whiteNoiseState.disable()
            onNavigateBack()
        },
        uiState = uiState,
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        onNavigateToUpgrade = onNavigateToUpgrade
    )

    LecturetteTestContent(
        uiState = uiState,
        viewModel = viewModel,
        whiteNoiseState = whiteNoiseState,
        onNavigateBack = onNavigateBack,
        onShowExitDialog = { showExitDialog = true },
        modifier = modifier
    )
}

@Composable
private fun LecturetteTestDialogs(
    showExitDialog: Boolean,
    onDismissExitDialog: () -> Unit,
    onConfirmExit: () -> Unit,
    uiState: LecturetteTestUiState,
    viewModel: LecturetteTestViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit
) {
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = onDismissExitDialog,
            title = { Text("Exit Test?") },
            text = { Text("Your progress will be lost. Are you sure you want to exit?") },
            confirmButton = {
                TextButton(onClick = onConfirmExit) { Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = onDismissExitDialog) { Text("Cancel") }
            }
        )
    }

    if (uiState.showLimitDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissLimitDialog()
                onNavigateBack()
            },
            title = { Text("Test Limit Reached") },
            text = { Text(uiState.limitMessage ?: "You've reached your monthly test limit.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissLimitDialog()
                    onNavigateToUpgrade()
                }) {
                    Text("Upgrade")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissLimitDialog()
                    onNavigateBack()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpgradeDialog() },
            title = { Text("Upgrade Required") },
            text = { Text(uiState.upgradeMessage ?: "Upgrade to access Lecturette test") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissUpgradeDialog()
                    onNavigateToUpgrade()
                }) {
                    Text("Upgrade")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissUpgradeDialog()
                    onNavigateBack()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun LecturetteTestContent(
    uiState: LecturetteTestUiState,
    viewModel: LecturetteTestViewModel,
    whiteNoiseState: com.ssbmax.ui.tests.gto.common.WhiteNoiseState,
    onNavigateBack: () -> Unit,
    onShowExitDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            TestContentLoadingState(
                message = uiState.loadingMessage ?: "Loading test...",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            when (uiState.phase) {
                LecturettePhase.INSTRUCTIONS -> InstructionsPhase(
                    onStart = { viewModel.proceedToTopicSelection() },
                    onNavigateBack = onNavigateBack
                )
                LecturettePhase.TOPIC_SELECTION -> TopicSelectionPhase(
                    topics = uiState.topicChoices,
                    onTopicSelected = { viewModel.selectTopic(it) },
                    onNavigateBack = onNavigateBack
                )
                LecturettePhase.SPEECH -> SpeechPhase(
                    selectedTopic = uiState.selectedTopic,
                    speechTranscript = uiState.speechTranscript,
                    charCount = uiState.charCount,
                    timeRemaining = uiState.formattedTime,
                    isTimeLow = uiState.isTimeLow,
                    onTranscriptChanged = viewModel::onTranscriptChanged,
                    onProceedToReview = { viewModel.proceedToReview() },
                    onNavigateBack = onShowExitDialog
                )
                LecturettePhase.REVIEW -> ReviewPhase(
                    selectedTopic = uiState.selectedTopic,
                    speechTranscript = uiState.speechTranscript,
                    charCount = uiState.charCount,
                    isSubmitting = uiState.isSubmitting,
                    onBackToSpeech = { viewModel.backToSpeech() },
                    onSubmit = { viewModel.submitTest() }
                )
                LecturettePhase.SUBMITTED -> {
                    GTOSubmissionSuccessScreen(
                        testName = "Lecturette",
                        onNavigateHome = onNavigateBack
                    )
                }
            }
        }

        if (uiState.phase == LecturettePhase.SPEECH) {
            AnimatedWhiteNoiseOverlay(
                baseAlpha = 0.08f,
                intensityRange = 0.03f,
                isEnabled = whiteNoiseState.isEnabled.value
            )
        }
    }
}
