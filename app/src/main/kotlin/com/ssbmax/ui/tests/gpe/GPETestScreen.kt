package com.ssbmax.ui.tests.gpe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.shared.domain.model.GPEPhase
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState
import com.ssbmax.ui.tests.gpe.components.GPEBottomBar
import com.ssbmax.ui.tests.gpe.components.GPEExitDialog
import com.ssbmax.ui.tests.gpe.components.GPESubmitDialog
import com.ssbmax.ui.tests.gpe.components.GPETopBar
import com.ssbmax.ui.tests.gpe.components.phases.GPEInstructionsPhase
import com.ssbmax.ui.tests.gpe.components.phases.GPEPlanningPhase
import com.ssbmax.ui.tests.gpe.components.phases.GPEReviewPhase
import org.koin.compose.viewmodel.koinViewModel

/**
 * GPE Test Screen - Image viewing + Planning response
 * Follows component-based architecture similar to PPDT
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GPETestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.shared.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: GPETestViewModel = koinViewModel(),
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
                            scenario = uiState.scenario,
                            resources = uiState.resources,
                            imageUrl = uiState.imageUrl
                        )
                        GPEPhase.REVIEW -> GPEReviewPhase(
                            scenario = uiState.scenario,
                            resources = uiState.resources,
                            planningResponse = uiState.planningResponse,
                            charactersCount = uiState.charactersCount,
                            onEdit = { viewModel.returnToPlanning() }
                        )
                            GPEPhase.SUBMITTED -> {
                            // Show loading/success state briefly before navigation kicks in
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    // Navigate to result screen when submission is complete
    LaunchedEffect(uiState.isSubmitted, uiState.submissionId) {
        if (uiState.isSubmitted) {
            val submissionId = uiState.submissionId
            val subscriptionType = uiState.subscriptionType
            
            if (!submissionId.isNullOrBlank() && subscriptionType != null) {
                android.util.Log.d("GPETestScreen", "✅ Test submitted, navigating to result screen")
                onTestComplete(submissionId, subscriptionType)
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
