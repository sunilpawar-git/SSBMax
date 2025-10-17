package com.ssbmax.ui.tests.ppdt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.SubmissionStatus

/**
 * PPDT Submission Result Screen - Shows pending review or graded result
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
                title = { Text("Test Result") },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home"
                        )
                    }
                }
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
                    // Status Header
                    item {
                        StatusHeaderCard(
                            status = uiState.submission!!.status,
                            aiScore = uiState.submission!!.aiPreliminaryScore?.overallScore,
                            finalScore = uiState.submission!!.instructorReview?.finalScore
                        )
                    }
                    
                    // AI Preliminary Score (if available)
                    if (uiState.submission!!.aiPreliminaryScore != null) {
                        item {
                            AIPreliminaryScoreCard(
                                aiScore = uiState.submission!!.aiPreliminaryScore!!
                            )
                        }
                    }
                    
                    // Instructor Review (if graded)
                    if (uiState.submission!!.instructorReview != null) {
                        item {
                            InstructorReviewCard(
                                review = uiState.submission!!.instructorReview!!
                            )
                        }
                    }
                    
                    // Your Story
                    item {
                        YourStoryCard(
                            story = uiState.submission!!.story,
                            charactersCount = uiState.submission!!.charactersCount
                        )
                    }
                    
                    // Action Buttons
                    item {
                        ActionButtonsCard(
                            status = uiState.submission!!.status,
                            onViewFeedback = onViewFeedback,
                            onBackToHome = onNavigateHome
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHeaderCard(
    status: SubmissionStatus,
    aiScore: Float?,
    finalScore: Float?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                SubmissionStatus.GRADED -> MaterialTheme.colorScheme.tertiaryContainer
                SubmissionStatus.SUBMITTED_PENDING_REVIEW, 
                SubmissionStatus.UNDER_REVIEW -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Icon
            Icon(
                imageVector = when (status) {
                    SubmissionStatus.GRADED -> Icons.Default.CheckCircle
                    SubmissionStatus.SUBMITTED_PENDING_REVIEW,
                    SubmissionStatus.UNDER_REVIEW -> Icons.Default.HourglassEmpty
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = when (status) {
                    SubmissionStatus.GRADED -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            
            // Status Text
            Text(
                text = status.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Score Display
            if (finalScore != null) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${finalScore.toInt()}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    text = "Final Score",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (aiScore != null) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${aiScore.toInt()}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                AssistChip(
                    onClick = { },
                    label = { Text("AI Preliminary Score") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            
            // Message based on status
            Text(
                text = when (status) {
                    SubmissionStatus.SUBMITTED_PENDING_REVIEW ->
                        "Your story has been submitted to your instructor for review."
                    SubmissionStatus.UNDER_REVIEW ->
                        "Your instructor is currently reviewing your submission."
                    SubmissionStatus.GRADED ->
                        "Your story has been reviewed and graded by your instructor."
                    else -> "Submission received"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AIPreliminaryScoreCard(aiScore: com.ssbmax.core.domain.model.PPDTAIScore) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            HorizontalDivider()
            
            // Breakdown
            ScoreItem(label = "Perception", score = aiScore.perceptionScore, maxScore = 20f)
            ScoreItem(label = "Imagination", score = aiScore.imaginationScore, maxScore = 20f)
            ScoreItem(label = "Narration", score = aiScore.narrationScore, maxScore = 20f)
            ScoreItem(label = "Character Depiction", score = aiScore.characterDepictionScore, maxScore = 20f)
            ScoreItem(label = "Positivity", score = aiScore.positivityScore, maxScore = 20f)
            
            if (aiScore.strengths.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Strengths",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                aiScore.strengths.forEach { strength ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = strength,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            if (aiScore.areasForImprovement.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Areas for Improvement",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                aiScore.areasForImprovement.forEach { area ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = area,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreItem(label: String, score: Float, maxScore: Float = 100f) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { score / maxScore },
                modifier = Modifier.width(100.dp),
                color = when {
                    score >= 75 -> MaterialTheme.colorScheme.tertiary
                    score >= 50 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                }
            )
            Text(
                text = if (maxScore == 100f) "${score.toInt()}%" else "${score.toInt()}/${maxScore.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InstructorReviewCard(review: com.ssbmax.core.domain.model.PPDTInstructorReview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Column {
                    Text(
                        text = "Instructor Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Reviewed by ${review.instructorName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            HorizontalDivider()
            
            // Detailed Scores
            val scores = review.detailedScores
            ScoreItem(label = "Perception", score = (scores.perception / 20) * 100)
            ScoreItem(label = "Imagination", score = (scores.imagination / 20) * 100)
            ScoreItem(label = "Narration", score = (scores.narration / 20) * 100)
            ScoreItem(label = "Character Depiction", score = (scores.characterDepiction / 20) * 100)
            ScoreItem(label = "Positivity", score = (scores.positivity / 20) * 100)
            
            HorizontalDivider()
            
            Text(
                text = "Feedback",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = review.feedback,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun YourStoryCard(story: String, charactersCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Your Story",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            HorizontalDivider()
            
            Text(
                text = story,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                text = "$charactersCount characters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButtonsCard(
    status: SubmissionStatus,
    onViewFeedback: () -> Unit,
    onBackToHome: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (status == SubmissionStatus.GRADED) {
                Button(
                    onClick = onViewFeedback,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Detailed Feedback")
                }
            }
            
            OutlinedButton(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Home")
            }
        }
    }
}

