package com.ssbmax.ui.tests.gpe

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.GPEPhase
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState
import com.ssbmax.ui.tests.gpe.components.*
import com.ssbmax.ui.tests.gpe.components.phases.*
import com.ssbmax.ui.tests.gto.common.GTOSubmissionSuccessScreen

/**
 * GPE Test Screen - Image viewing + Planning response
 * Follows component-based architecture similar to PPDT
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GPETestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: GPETestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showSubmitDialog by rememberSaveable { mutableStateOf(false) }

    // Initialize test
    LaunchedEffect(testId) {
        viewModel.loadTest(testId)
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
            GPETopBar(
                currentPhase = uiState.currentPhase,
                timeRemainingSeconds = uiState.timeRemainingSeconds,
                onExitClick = { showExitDialog = true }
            )
        },
        bottomBar = {
            if (uiState.currentPhase != GPEPhase.INSTRUCTIONS) {
                GPEBottomBar(
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
                        message = stringResource(R.string.gpe_loading),
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
                        GPEPhase.INSTRUCTIONS -> GPEInstructionsPhase(
                            onStart = { viewModel.startTest() }
                        )
                        // Should not be reached with new flow, but required for exhaustive when
                        GPEPhase.IMAGE_VIEWING -> {}
                        
                        GPEPhase.PLANNING -> GPEPlanningPhase(
                            planningResponse = uiState.planningResponse,
                            onPlanningResponseChange = { viewModel.updatePlanningResponse(it) },
                            charactersCount = uiState.charactersCount,
                            minCharacters = uiState.minCharacters,
                            maxCharacters = uiState.maxCharacters,
                            timeRemainingSeconds = uiState.timeRemainingSeconds,
                            scenario = uiState.scenario,
                            resources = uiState.resources,
                            imageUrl = uiState.imageUrl
                        )
                        GPEPhase.REVIEW -> GPEReviewPhase(
                            imageUrl = uiState.imageUrl,
                            scenario = uiState.scenario,
                            resources = uiState.resources,
                            planningResponse = uiState.planningResponse,
                            charactersCount = uiState.charactersCount,
                            onEdit = { viewModel.returnToPlanning() }
                        )
                        GPEPhase.SUBMITTED -> {
                            GTOSubmissionSuccessScreen(
                                testName = "Group Planning Exercise",
                                onNavigateHome = onNavigateBack
                            )
                        }
                    }
                }
            }
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        GPEExitDialog(
            onDismiss = { showExitDialog = false },
            onExit = {
                viewModel.pauseTest()
                onNavigateBack()
            }
        )
    }

    // Submit confirmation dialog
    if (showSubmitDialog) {
        GPESubmitDialog(
            onDismiss = { showSubmitDialog = false },
            onSubmit = {
                showSubmitDialog = false
                viewModel.submitTest()
            }
        )
    }
}
