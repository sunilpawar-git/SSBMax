package com.ssbmax.ui.tests.gto.gd

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
 * Group Discussion (GD) Test Screen
 * 
 * A 20-minute test where candidates write their thoughts on a given topic
 * with white noise simulation to match real SSB conditions.
 * 
 * Phases:
 * 1. Instructions: Test format, rules, white noise warning
 * 2. Discussion: Topic display + text input + timer + white noise
 * 3. Review: Show response, word count, allow editing
 * 4. Submitted: Navigate to result screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GDTestScreen(
    testId: String,
    onTestComplete: (submissionId: String, subscriptionType: com.ssbmax.core.domain.model.SubscriptionType) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit = {},
    viewModel: GDTestViewModel = hiltViewModel(),
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
    
    // Enable white noise during discussion phase
    LaunchedEffect(uiState.phase) {
        when (uiState.phase) {
            GDPhase.DISCUSSION -> whiteNoiseState.enable()
            else -> whiteNoiseState.disable()
        }
    }
    
    // Navigate to result screen when submission is complete
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            val submissionId = uiState.submissionId
            if (!submissionId.isNullOrBlank()) {
                android.util.Log.d("GDTestScreen", "✅ Test submitted, navigating to result screen")
                android.util.Log.d("GDTestScreen", "   - Submission ID: $submissionId")
                android.util.Log.d("GDTestScreen", "   - Subscription type: ${uiState.subscriptionType}")
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
    
    GDTestDialogs(
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

    GDTestContent(
        uiState = uiState,
        viewModel = viewModel,
        whiteNoiseState = whiteNoiseState,
        onNavigateBack = onNavigateBack,
        onShowExitDialog = { showExitDialog = true },
        modifier = modifier
    )
}

@Composable
private fun GDTestDialogs(
    showExitDialog: Boolean,
    onDismissExitDialog: () -> Unit,
    onConfirmExit: () -> Unit,
    uiState: GDTestUiState,
    viewModel: GDTestViewModel,
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
            text = { Text(uiState.upgradeMessage ?: "Upgrade to access GTO tests") },
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
private fun GDTestContent(
    uiState: GDTestUiState,
    viewModel: GDTestViewModel,
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
                GDPhase.INSTRUCTIONS -> InstructionsPhase(
                    onStart = { viewModel.startDiscussion() },
                    onNavigateBack = onNavigateBack
                )
                GDPhase.DISCUSSION -> DiscussionPhase(
                    topic = uiState.topic,
                    response = uiState.response,
                    charCount = uiState.charCount,
                    timeRemaining = uiState.formattedTime,
                    isTimeLow = uiState.isTimeLow,
                    onResponseChanged = viewModel::onResponseChanged,
                    onProceedToReview = { viewModel.proceedToReview() },
                    onNavigateBack = onShowExitDialog
                )
                GDPhase.REVIEW -> ReviewPhase(
                    topic = uiState.topic,
                    response = uiState.response,
                    charCount = uiState.charCount,
                    isSubmitting = uiState.isSubmitting,
                    onBackToDiscussion = { viewModel.backToDiscussion() },
                    onSubmit = { viewModel.submitTest() }
                )
                GDPhase.SUBMITTED -> {
                    GTOSubmissionSuccessScreen(
                        testName = "Group Discussion",
                        onNavigateHome = onNavigateBack
                    )
                }
            }
        }

        if (uiState.phase == GDPhase.DISCUSSION) {
            AnimatedWhiteNoiseOverlay(
                baseAlpha = 0.08f,
                intensityRange = 0.03f,
                isEnabled = whiteNoiseState.isEnabled.value
            )
        }
    }
}
