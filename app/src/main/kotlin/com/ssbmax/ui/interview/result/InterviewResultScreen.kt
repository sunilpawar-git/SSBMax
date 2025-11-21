package com.ssbmax.ui.interview.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
 * Interview Result Screen
 *
 * Displays:
 * - Overall rating and performance level
 * - OLQ scores breakdown
 * - Strengths and weaknesses
 * - AI-generated feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewResultScreen(
    resultId: String,
    onNavigateBack: () -> Unit,
    viewModel: InterviewResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.interview_result_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, stringResource(R.string.cd_back))
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.result != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Overall summary card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.interview_result_overall_rating),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = uiState.performanceLevel.displayName,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.interview_result_average_score,
                                            uiState.averageScore
                                        ),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.interview_result_duration,
                                                uiState.durationMinutes
                                            )
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.interview_result_completion,
                                                uiState.completionRate
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Strengths
                        if (uiState.strengths.isNotEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.interview_result_strengths_title),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        uiState.strengths.forEach { olq ->
                                            Text(
                                                text = "• ${olq.displayName}",
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Weaknesses
                        if (uiState.weaknesses.isNotEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.interview_result_weaknesses_title),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        uiState.weaknesses.forEach { olq ->
                                            Text(
                                                text = "• ${olq.displayName}",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // OLQ Scores
                        item {
                            Text(
                                text = stringResource(R.string.interview_result_olq_scores_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        items(uiState.getSortedOLQScores()) { (olq, score) ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = olq.displayName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = olq.category.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Text(
                                        text = "${score.score}/5",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }

                        // Feedback
                        if (uiState.result?.feedback?.isNotBlank() == true) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.interview_result_feedback_title),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = uiState.result!!.feedback,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        // Done button
                        item {
                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.interview_result_button_done))
                            }
                        }
                    }
                }
            }
        }
    }
}
