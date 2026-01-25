package com.ssbmax.ui.tests.ppdt

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
import com.ssbmax.ui.components.SSBRecommendationBanner
import com.ssbmax.ui.tests.gto.common.AnalyzingCard
import com.ssbmax.ui.tests.gto.common.AnalysisFailedCard
import com.ssbmax.ui.tests.gto.common.OverallScoreCard
import com.ssbmax.ui.tests.gto.common.OLQScoreCard

/**
 * PPDT Submission Result Screen - Shows story submission confirmation and pending review
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PPDTSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    onViewFeedback: () -> Unit = {},
    viewModel: PPDTSubmissionResultViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PPDT Test Results") }
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
                                        "Story Submitted Successfully!",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("${uiState.submission!!.charactersCount} characters written")
                            }
                        }
                    }
                    
                    // Story preview
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Your Story",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                HorizontalDivider()
                                
                                Text(
                                    uiState.submission!!.story,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                
                                HorizontalDivider()
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Character Count:",
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
                                        "Writing Time:",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "${uiState.submission!!.writingTimeTakenMinutes} minutes",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    // OLQ Analysis Results (using TAT pattern)
                    when (uiState.submission!!.analysisStatus) {
                        AnalysisStatus.ANALYZING -> {
                            item { AnalyzingCard(message = "Analyzing Your PPDT Story...") }
                        }
                        AnalysisStatus.FAILED -> {
                            item { AnalysisFailedCard() }
                        }
                        AnalysisStatus.COMPLETED -> {
                            uiState.submission!!.olqResult?.let { result ->
                                // SSB Recommendation Banner (above Overall Score)
                                uiState.ssbRecommendation?.let { recommendation ->
                                    item {
                                        SSBRecommendationBanner(
                                            model = recommendation
                                        )
                                    }
                                }
                                
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
                        else -> {
                            // PENDING_ANALYSIS or other states - show pending card
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
                                                "Your instructor will review your story and provide detailed feedback",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
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
