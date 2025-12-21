package com.ssbmax.ui.tests.srt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.SRTPhase
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState
import com.ssbmax.ui.tests.srt.components.SRTInProgressView
import com.ssbmax.ui.tests.srt.components.SRTInstructionsView
import com.ssbmax.ui.tests.srt.components.SRTReviewView

/**
 * SRT Test Screen - 60 practical situations with response input
 * Students describe how they would react in each situation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SRTTestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: SRTTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showSubmitDialog by rememberSaveable { mutableStateOf(false) }
    
    // Initialize test
    LaunchedEffect(testId) {
        viewModel.loadTest(testId)
    }
    
    // Handle navigation events (one-time events, consumed on collection)
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is com.ssbmax.ui.tests.common.TestNavigationEvent.NavigateToResult -> {
                    onTestComplete(event.submissionId, event.subscriptionType)
                }
            }
        }
    }
    
    // Show limit reached dialog if needed
    if (uiState.isLimitReached) {
        com.ssbmax.ui.tests.common.TestLimitReachedDialog(
            tier = uiState.subscriptionTier,
            testsLimit = uiState.testsLimit,
            testsUsed = uiState.testsUsed,
            resetsAt = uiState.resetsAt,
            onUpgrade = {
                // TODO: Navigate to upgrade screen
                onNavigateBack()
            },
            onDismiss = onNavigateBack
        )
        return
    }
    
    when {
        uiState.isLoading -> {
            TestContentLoadingState(
                message = uiState.loadingMessage ?: stringResource(R.string.srt_loading),
                modifier = Modifier.fillMaxSize()
            )
        }
        uiState.error != null -> {
            TestContentErrorState(
                error = uiState.error!!,
                onRetry = { viewModel.loadTest(testId) },
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
            when (uiState.phase) {
                SRTPhase.INSTRUCTIONS -> {
                    SRTInstructionsView(
                        onStart = { viewModel.startTest() },
                        onNavigateBack = onNavigateBack
                    )
                }
                SRTPhase.IN_PROGRESS -> {
                    SRTInProgressView(
                        situation = uiState.currentSituation?.situation ?: "",
                        situationNumber = uiState.currentSituationIndex + 1,
                        totalSituations = uiState.situations.size,
                        response = uiState.currentResponse,
                        onResponseChange = { viewModel.updateResponse(it) },
                        minChars = uiState.config?.minResponseLength ?: 20,
                        maxChars = uiState.config?.maxResponseLength ?: 200,
                        canMoveNext = uiState.canMoveToNext,
                        onNext = { viewModel.moveToNext() },
                        onSkip = { viewModel.skipSituation() },
                        showExitDialog = showExitDialog,
                        onShowExitDialog = { showExitDialog = true },
                        onDismissExitDialog = { showExitDialog = false },
                        onConfirmExit = onNavigateBack
                    )
                }
                SRTPhase.REVIEW -> {
                    SRTReviewView(
                        responses = uiState.responses,
                        totalSituations = uiState.situations.size,
                        onEdit = { index -> viewModel.editResponse(index) },
                        onSubmit = { showSubmitDialog = true }
                    )
                }
                SRTPhase.COMPLETED, SRTPhase.SUBMITTED -> {
                    // Handled by navigation
                }
            }
        }
    }
    
    // Submit confirmation dialog
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text(stringResource(R.string.srt_submit_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.srt_submit_message, uiState.validResponseCount, uiState.situations.size))
                    if (uiState.validResponseCount < 60) {
                        Text(
                            stringResource(R.string.srt_submit_incomplete),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSubmitDialog = false
                    viewModel.submitTest()
                }) {
                    Text(stringResource(R.string.srt_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text(stringResource(R.string.srt_cancel))
                }
            }
        )
    }
}

