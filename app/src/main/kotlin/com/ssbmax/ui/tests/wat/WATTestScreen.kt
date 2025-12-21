package com.ssbmax.ui.tests.wat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.WATPhase
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState
import com.ssbmax.ui.tests.wat.components.WATInProgressView
import com.ssbmax.ui.tests.wat.components.WATInstructionsView

/**
 * WAT Test Screen - 60 words with rapid 15-second responses
 * Minimalist fullscreen design for focused responses
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WATTestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    viewModel: WATTestViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    
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
                message = uiState.loadingMessage ?: stringResource(R.string.wat_loading),
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
                WATPhase.INSTRUCTIONS -> {
                    WATInstructionsView(
                        onStart = { viewModel.startTest() },
                        onNavigateBack = onNavigateBack
                    )
                }
                WATPhase.IN_PROGRESS -> {
                    WATInProgressView(
                        word = uiState.currentWord?.word ?: "",
                        wordNumber = uiState.currentWordIndex + 1,
                        totalWords = uiState.words.size,
                        timeRemaining = uiState.timeRemaining,
                        response = uiState.currentResponse,
                        onResponseChange = { viewModel.updateResponse(it) },
                        onSubmit = { viewModel.submitResponse() },
                        onSkip = { viewModel.skipWord() },
                        showExitDialog = showExitDialog,
                        onShowExitDialog = { showExitDialog = true },
                        onDismissExitDialog = { showExitDialog = false },
                        onConfirmExit = onNavigateBack
                    )
                }
                WATPhase.COMPLETED -> {
                    // Handled by auto-submit
                }
                WATPhase.SUBMITTED -> {
                    // Handled by navigation
                }
            }
        }
    }
}

