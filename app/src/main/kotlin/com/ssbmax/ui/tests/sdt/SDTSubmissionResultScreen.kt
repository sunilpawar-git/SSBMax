package com.ssbmax.ui.tests.sdt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.ui.tests.gto.common.AnalyzingCard
import com.ssbmax.ui.tests.gto.common.AnalysisFailedCard
import com.ssbmax.ui.tests.gto.common.OverallScoreCard
import com.ssbmax.ui.tests.gto.common.OLQScoreCard
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SDTSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: SDTSubmissionResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sdt_result_title)) }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.submission != null -> {
                ResultContent(
                    submission = uiState.submission!!, 
                    onNavigateHome = onNavigateHome,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun ResultContent(
    submission: com.ssbmax.core.domain.model.SDTSubmission, 
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.sdt_result_summary_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.sdt_result_completed, submission.totalResponses))
                    Text(stringResource(R.string.sdt_result_time_taken, submission.totalTimeTakenMinutes))
                    Text(stringResource(R.string.sdt_result_valid_responses, submission.validResponses))
                }
            }
        }

        // OLQ Analysis Status and Results (Phase 3)
        when (submission.analysisStatus) {
            AnalysisStatus.PENDING_ANALYSIS -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(32.dp))
                            Column {
                                Text(
                                    "OLQ Analysis Pending",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Your submission will be analyzed shortly.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            AnalysisStatus.ANALYZING -> {
                item { AnalyzingCard(message = "Analyzing Your SDT Responses...") }
            }
            AnalysisStatus.FAILED -> {
                item { AnalysisFailedCard() }
            }
            AnalysisStatus.COMPLETED -> {
                submission.olqResult?.let { result ->
                    // Overall Score Card
                    item {
                        OverallScoreCard(
                            overallScore = result.overallScore,
                            overallRating = result.overallRating,
                            aiConfidence = result.aiConfidence
                        )
                    }

                    // Strengths
                    if (result.strengths.isNotEmpty()) {
                        item {
                            Text(
                                "Top Strengths",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(result.strengths) { strength ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
                                    Text(strength, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }

                    // All OLQ Scores by Category
                    item {
                        Text(
                            "Officer-Like Qualities Assessment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OLQCategory.entries.forEach { category ->
                        val olqsInCategory = result.olqScores.filter { it.key.category == category }
                        if (olqsInCategory.isNotEmpty()) {
                            item {
                                Text(
                                    category.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(olqsInCategory.entries.toList()) { (olq, score) ->
                                OLQScoreCard(olq = olq, score = score, isStrength = null)
                            }
                        }
                    }

                    // Weaknesses / Areas for Improvement
                    if (result.weaknesses.isNotEmpty()) {
                        item {
                            Text(
                                "Areas for Improvement",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(result.weaknesses) { weakness ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.error)
                                    Text(weakness, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }

                    // Recommendations
                    if (result.recommendations.isNotEmpty()) {
                        item {
                            Text(
                                "Recommendations",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(result.recommendations) { recommendation ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                                    Text(recommendation, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }

        items(submission.responses) { response ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(response.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!response.isSkipped) {
                        Text(response.answer, style = MaterialTheme.typography.bodyMedium)
                        Text("Characters: ${response.charCount}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(stringResource(R.string.sdt_result_skipped), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        // Actions - Back to Home button
        item {
            Button(
                onClick = onNavigateHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sdt_result_action_home))
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(stringResource(R.string.sdt_result_score_format, score.toInt()), fontWeight = FontWeight.Bold)
    }
}

