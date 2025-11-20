package com.ssbmax.ui.tests.tat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.TATPhase
import com.ssbmax.ui.components.TestContentErrorState
import com.ssbmax.ui.components.TestContentLoadingState
import com.ssbmax.ui.tests.tat.components.*
import com.ssbmax.ui.tests.tat.components.phases.*

/**
 * TAT Test Screen - 12 pictures with story writing
 * Each picture: 30s viewing + 4min writing
 * Refactored to use component-based architecture
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TATTestScreen(
    testId: String,
    onTestComplete: (String, com.ssbmax.core.domain.model.SubscriptionType) -> Unit = { _, _ -> }, // submissionId, subscriptionType
    onNavigateBack: () -> Unit = {},
    viewModel: TATTestViewModel = hiltViewModel(),
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

    // Show subscription limit dialog if test limit reached
    if (uiState.isLimitReached) {
        com.ssbmax.ui.tests.common.TestLimitReachedDialog(
            tier = uiState.subscriptionTier,
            testsLimit = uiState.testsLimit,
            testsUsed = uiState.testsUsed,
            resetsAt = uiState.resetsAt,
            onDismiss = onNavigateBack,
            onUpgrade = onNavigateBack // Navigate back, user will handle upgrade from dashboard
        )
        return // Don't show the test UI
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.tat_test))
                        if (uiState.currentQuestion != null) {
                            Text(
                                stringResource(R.string.tat_picture_number, uiState.currentQuestionIndex + 1),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (uiState.phase != TATPhase.INSTRUCTIONS) {
                        // Progress indicator
                        Text(
                            stringResource(R.string.tat_completed_stories, uiState.completedStories),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.phase != TATPhase.INSTRUCTIONS && uiState.phase != TATPhase.SUBMITTED) {
                TATBottomBar(
                    phase = uiState.phase,
                    canMoveNext = uiState.canMoveToNextQuestion,
                    canMovePrevious = uiState.canMoveToPreviousQuestion,
                    canSubmit = uiState.canSubmitTest,
                    onPrevious = { viewModel.moveToPreviousQuestion() },
                    onNext = { viewModel.moveToNextQuestion() },
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
            when (uiState.phase) {
                TATPhase.INSTRUCTIONS -> {
                    TATInstructionsPhase(
                        onStart = { viewModel.startTest() }
                    )
                }
                TATPhase.IMAGE_VIEWING -> {
                    TATImageViewingPhase(
                        imageUrl = uiState.currentQuestion?.imageUrl ?: "",
                        timeRemaining = uiState.viewingTimeRemaining,
                        sequenceNumber = uiState.currentQuestionIndex + 1
                    )
                }
                TATPhase.WRITING -> {
                    TATWritingPhase(
                        story = uiState.currentStory,
                        onStoryChange = { viewModel.updateStory(it) },
                        timeRemaining = uiState.writingTimeRemaining,
                        minCharacters = uiState.currentQuestion?.minCharacters ?: 150,
                        maxCharacters = uiState.currentQuestion?.maxCharacters ?: 800,
                        charactersCount = uiState.currentStory.length,
                        sequenceNumber = uiState.currentQuestionIndex + 1
                    )
                }
                TATPhase.REVIEW_CURRENT -> {
                    TATReviewPhase(
                        story = uiState.currentStory,
                        charactersCount = uiState.currentStory.length,
                        sequenceNumber = uiState.currentQuestionIndex + 1,
                        onEdit = { viewModel.editCurrentStory() },
                        onConfirm = { viewModel.confirmCurrentStory() }
                    )
                }
                TATPhase.SUBMITTED -> {
                    // Handled by navigation
                }
            }

            // Loading overlay
            if (uiState.isLoading) {
                TestContentLoadingState(
                    message = stringResource(R.string.tat_loading),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Error overlay
            if (uiState.error != null) {
                TestContentErrorState(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadTest(testId) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        TATExitDialog(
            onDismiss = { showExitDialog = false },
            onExit = {
                showExitDialog = false
                onNavigateBack()
            }
        )
    }

    // Submit confirmation dialog
    if (showSubmitDialog) {
        TATSubmitDialog(
            completedStories = uiState.completedStories,
            onDismiss = { showSubmitDialog = false },
            onSubmit = {
                showSubmitDialog = false
                viewModel.submitTest()
            }
        )
    }
}
