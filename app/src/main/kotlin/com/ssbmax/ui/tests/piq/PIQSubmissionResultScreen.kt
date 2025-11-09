package com.ssbmax.ui.tests.piq

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.ssbmax.core.domain.model.PIQAIScore

/**
 * PIQ Submission Result Screen - Shows AI quality score
 * Follows SDT/TAT pattern: fetches submission from Firestore via ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PIQSubmissionResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit = {},
    viewModel: PIQSubmissionResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PIQ Quality Assessment") }
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onNavigateHome) {
                            Text("Return to Home")
                        }
                    }
                }
            }
            uiState.submission != null -> {
                ResultContent(
                    submissionId = submissionId,
                    aiScore = uiState.submission!!.aiPreliminaryScore,
                    onNavigateHome = onNavigateHome,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun ResultContent(
    submissionId: String,
    aiScore: PIQAIScore?,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success message
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "PIQ Submitted Successfully",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Submission ID: ${submissionId.take(8)}...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        // AI Quality Score - only show if available
        aiScore?.let { score ->
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
                            Column {
                                Text(
                                    "AI Quality Assessment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Overall Score",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${score.overallScore.toInt()}/100",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        HorizontalDivider()
                        
                        // Category Breakdown
                        ScoreBreakdown("Personal Information", score.personalInfoScore, 25f)
                        ScoreBreakdown("Family Information", score.familyInfoScore, 25f)
                        ScoreBreakdown("Motivation & Goals", score.motivationScore, 25f)
                        ScoreBreakdown("Self-Assessment", score.selfAssessmentScore, 25f)
                        
                        HorizontalDivider()
                        
                        // Quality Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${score.completenessPercentage}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Completeness",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "%.1f/10".format(score.clarityScore),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    "Clarity",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "%.1f/10".format(score.consistencyScore),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    "Consistency",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        
                        HorizontalDivider()
                        
                        // Feedback
                        Text(
                            "Feedback",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            score.feedback,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Strengths
            if (score.strengths.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Strengths",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            score.strengths.forEach { strength ->
                                Row(
                                    modifier = Modifier.padding(start = 32.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        "• ",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        strength,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Areas for Improvement
            if (score.areasForImprovement.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.TipsAndUpdates,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Areas for Improvement",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            score.areasForImprovement.forEach { area ->
                                Row(
                                    modifier = Modifier.padding(start = 32.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        "• ",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        area,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Your PIQ will be reviewed by assessors during the SSB interview. A well-filled PIQ helps them prepare better questions for you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Return Home Button
        item {
            Button(
                onClick = onNavigateHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Return to Home")
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${score.toInt()}/${maxScore.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        LinearProgressIndicator(
            progress = { score / maxScore },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
