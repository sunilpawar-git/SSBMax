package com.ssbmax.ui.tests.srt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.ui.tests.gto.common.AnalyzingCard
import com.ssbmax.ui.tests.gto.common.AnalysisFailedCard
import com.ssbmax.ui.tests.gto.common.OverallScoreCard
import com.ssbmax.ui.tests.gto.common.OLQScoreCard

/**
 * SRT Submission Result Screen - Shows AI analysis and pending instructor review
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SRTSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    onViewFeedback: () -> Unit = {},
    viewModel: SRTSubmissionResultViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SRT Test Results") }
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
                                        "Test Submitted Successfully!",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("${uiState.submission!!.validResponses}/${uiState.submission!!.totalResponses} situations answered")
                            }
                        }
                    }

                    // OLQ Analysis Status and Results (Phase 3)
                    val submission = uiState.submission!!
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
                            item { AnalyzingCard(message = "Analyzing Your SRT Responses...") }
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
                                            "Pending Instructor Review",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Your instructor will review and provide final feedback",
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
                            Text("Back to Home")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBreakdown(
    label: String,
    score: Float,
    maxScore: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { score / maxScore },
                modifier = Modifier.width(100.dp)
            )
            Text(
                "${score.toInt()}/${maxScore.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

