package com.ssbmax.ui.tests.ppdt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.PPDTPhase
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState
import com.ssbmax.ui.tests.ppdt.components.*
import com.ssbmax.ui.tests.ppdt.components.phases.*

/**
 * PPDT Test Screen - Image viewing + Story writing
 * Refactored to use component-based architecture
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PPDTTestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: PPDTTestViewModel = hiltViewModel(),
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

    Scaffold(
        topBar = {
            PPDTTopBar(
                currentPhase = uiState.currentPhase,
                timeRemainingSeconds = uiState.timeRemainingSeconds,
                onExitClick = { showExitDialog = true }
            )
        },
        bottomBar = {
            if (uiState.currentPhase != PPDTPhase.INSTRUCTIONS) {
                PPDTBottomBar(
                    currentPhase = uiState.currentPhase,
                    canProceed = uiState.canProceedToNextPhase,
                    onNext = { viewModel.proceedToNextPhase() },
                    onSubmit = { showSubmitDialog = true }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    TestContentLoadingState(
                        message = stringResource(R.string.ppdt_loading),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.error != null -> {
                    TestContentErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.loadTest() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    when (uiState.currentPhase) {
                        PPDTPhase.INSTRUCTIONS -> PPDTInstructionsPhase(
                            onStart = { viewModel.startTest() }
                        )
                        PPDTPhase.IMAGE_VIEWING -> PPDTImageViewingPhase(
                            imageUrl = uiState.imageUrl,
                            timeRemainingSeconds = uiState.timeRemainingSeconds
                        )
                        PPDTPhase.WRITING -> PPDTWritingPhase(
                            story = uiState.story,
                            onStoryChange = { viewModel.updateStory(it) },
                            charactersCount = uiState.charactersCount,
                            minCharacters = uiState.minCharacters,
                            maxCharacters = uiState.maxCharacters
                        )
                        PPDTPhase.REVIEW -> PPDTReviewPhase(
                            imageUrl = uiState.imageUrl,
                            story = uiState.story,
                            charactersCount = uiState.charactersCount,
                            onEdit = { viewModel.returnToWriting() }
                        )
                        PPDTPhase.SUBMITTED -> {
                            // This shouldn't show, navigation happens in LaunchedEffect
                        }
                    }
                }
            }
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        PPDTExitDialog(
            onDismiss = { showExitDialog = false },
            onExit = {
                viewModel.pauseTest()
                onNavigateBack()
            }
        )
    }

    // Submit confirmation dialog
    if (showSubmitDialog) {
        PPDTSubmitDialog(
            onDismiss = { showSubmitDialog = false },
            onSubmit = {
                showSubmitDialog = false
                viewModel.submitTest()
            }
        )
    }
}
