package com.ssbmax.ui.tests.gpe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.SubmissionStatus

/**
 * GPE Submission Result Screen - Shows planning response submission confirmation and pending review
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GPESubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    onViewFeedback: () -> Unit = {},
    viewModel: GPESubmissionResultViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gpe_test_results)) }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.submission != null -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Submission confirmation
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        stringResource(R.string.gpe_plan_submitted_successfully),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(stringResource(
                                    R.string.character_count_simple,
                                    uiState.submission!!.charactersCount
                                ))
                            }
                        }
                    }

                    // Planning response preview
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    stringResource(R.string.gpe_your_planning_response),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                HorizontalDivider()

                                Text(
                                    uiState.submission!!.planningResponse,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        stringResource(R.string.character_count_label),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "${uiState.submission!!.charactersCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        stringResource(R.string.planning_time_label),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        stringResource(
                                            R.string.minutes_format,
                                            uiState.submission!!.planningTimeTakenMinutes
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // AI Preliminary Score (if available)
                    uiState.submission!!.aiPreliminaryScore?.let { aiScore ->
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            stringResource(R.string.ai_preliminary_analysis),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "${aiScore.overallScore.toInt()}/100",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    HorizontalDivider()

                                    // Individual scores
                                    GPEScoreRow(
                                        stringResource(R.string.gpe_score_situation_analysis),
                                        aiScore.situationAnalysisScore,
                                        20f
                                    )
                                    GPEScoreRow(
                                        stringResource(R.string.gpe_score_planning_quality),
                                        aiScore.planningQualityScore,
                                        20f
                                    )
                                    GPEScoreRow(
                                        stringResource(R.string.gpe_score_leadership),
                                        aiScore.leadershipScore,
                                        20f
                                    )
                                    GPEScoreRow(
                                        stringResource(R.string.gpe_score_resource_utilization),
                                        aiScore.resourceUtilizationScore,
                                        20f
                                    )
                                    GPEScoreRow(
                                        stringResource(R.string.gpe_score_practicality),
                                        aiScore.practicalityScore,
                                        20f
                                    )

                                    HorizontalDivider()

                                    Text(
                                        aiScore.feedback,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    // Strengths
                                    if (aiScore.strengths.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            stringResource(R.string.strengths_label),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        aiScore.strengths.forEach { strength ->
                                            Row(
                                                modifier = Modifier.padding(start = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("•", style = MaterialTheme.typography.bodySmall)
                                                Text(strength, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    // Areas for improvement
                                    if (aiScore.areasForImprovement.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            stringResource(R.string.areas_for_improvement_label),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        aiScore.areasForImprovement.forEach { area ->
                                            Row(
                                                modifier = Modifier.padding(start = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("•", style = MaterialTheme.typography.bodySmall)
                                                Text(area, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Status card
                    if (uiState.submission!!.status == SubmissionStatus.SUBMITTED_PENDING_REVIEW) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Schedule, null)
                                    Column {
                                        Text(
                                            stringResource(R.string.pending_instructor_review),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            stringResource(R.string.instructor_review_message),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Actions
                    item {
                        Button(
                            onClick = onNavigateHome,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.back_to_home))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GPEScoreRow(label: String, score: Float, maxScore: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        LinearProgressIndicator(
            progress = { score / maxScore },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            "${score.toInt()}/${maxScore.toInt()}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
