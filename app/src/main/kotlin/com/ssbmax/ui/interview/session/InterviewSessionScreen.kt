package com.ssbmax.ui.interview.session

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R

/**
 * Interview Session Screen
 *
 * Manages the interview flow:
 * - Displays questions one by one
 * - Captures text responses
 * - Submits to AI for analysis
 * - Progresses through all questions
 * - Completes interview and navigates to results
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewSessionScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (String) -> Unit,
    viewModel: InterviewSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate to result when completed
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted && uiState.resultId != null) {
            onNavigateToResult(uiState.resultId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(
                        R.string.interview_question_number,
                        uiState.currentQuestionIndex + 1,
                        uiState.totalQuestions
                    ))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error ?: stringResource(R.string.interview_error_generic),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                uiState.currentQuestion != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Progress indicator
                        LinearProgressIndicator(
                            progress = { uiState.getProgressPercentage() / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Question card
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Question",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = uiState.currentQuestion!!.questionText,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        // Response input
                        OutlinedTextField(
                            value = uiState.responseText,
                            onValueChange = { viewModel.updateResponse(it) },
                            label = { Text(stringResource(R.string.interview_response_hint)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            minLines = 6
                        )

                        // Submit button
                        Button(
                            onClick = { viewModel.submitResponse() },
                            enabled = uiState.canSubmitResponse(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSubmittingResponse) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    if (uiState.hasMoreQuestions()) {
                                        stringResource(R.string.interview_button_submit)
                                    } else {
                                        stringResource(R.string.interview_button_complete)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
