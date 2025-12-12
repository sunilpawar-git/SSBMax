package com.ssbmax.ui.tests.gto.lecturette

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory

/**
 * Lecturette Result Screen
 * 
 * Displays:
 * - Submission confirmation
 * - Selected topic and other choices
 * - Speech transcript
 * - 15 OLQ scores
 * - Overall performance rating
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturetteResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit,
    viewModel: LecturetteResultViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lecturette Results") },
                actions = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.Home, "Home")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading results...")
                    }
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry(submissionId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            uiState.submission != null -> {
                LecturetteResultContent(
                    submission = uiState.submission!!,
                    result = uiState.result,
                    isAnalyzing = uiState.isAnalyzing,
                    isFailed = uiState.isFailed,
                    formattedTimeSpent = uiState.formattedTimeSpent,
                    onNavigateHome = onNavigateHome,
                    modifier = modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun LecturetteResultContent(
    submission: GTOSubmission.LecturetteSubmission,
    result: com.ssbmax.core.domain.model.gto.GTOResult?,
    isAnalyzing: Boolean,
    isFailed: Boolean,
    formattedTimeSpent: String,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Submission confirmation
        item {
            LecturetteSubmissionCard(
                selectedTopic = submission.selectedTopic,
                allTopics = submission.topicChoices,
                wordCount = submission.wordCount,
                timeSpent = formattedTimeSpent
            )
        }
        
        // Analysis status
        item {
            when {
                isAnalyzing -> AnalyzingCard()
                isFailed -> AnalysisFailedCard()
            }
        }
        
        // Overall score (if completed)
        if (result != null) {
            item {
                OverallScoreCard(
                    overallScore = result.overallScore,
                    overallRating = result.overallRating,
                    aiConfidence = result.aiConfidence
                )
            }
            
            // Top OLQs
            item {
                Text(
                    text = "Top Strengths",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(result.topOLQs) { (olq, score) ->
                OLQScoreCard(olq = olq, score = score, isStrength = true)
            }
            
            // Improvement areas
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Areas for Improvement",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(result.improvementOLQs) { (olq, score) ->
                OLQScoreCard(olq = olq, score = score, isStrength = false)
            }
            
            // Speech transcript
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your Speech",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                SpeechPreviewCard(
                    selectedTopic = submission.selectedTopic,
                    speechTranscript = submission.speechTranscript,
                    wordCount = submission.wordCount
                )
            }
        }
        
        // Home button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LecturetteSubmissionCard(
    selectedTopic: String,
    allTopics: List<String>,
    wordCount: Int,
    timeSpent: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Speech Submitted!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider()
            
            Text(
                text = "Selected Topic",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = selectedTopic,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "Word Count", value = "$wordCount words")
                InfoItem(label = "Time Spent", value = timeSpent)
            }
        }
    }
}

@Composable
private fun SpeechPreviewCard(
    selectedTopic: String,
    speechTranscript: String,
    wordCount: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Topic",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = selectedTopic,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Speech Transcript",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$wordCount words",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = speechTranscript,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AnalyzingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Column {
                Text(
                    text = "Analyzing Your Speech...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AI is evaluating your speech across 15 Officer-Like Qualities. This usually takes 30-60 seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun AnalysisFailedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Column {
                Text(
                    text = "Analysis Failed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AI analysis could not be completed. Please contact support or retake the test.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun OverallScoreCard(
    overallScore: Float,
    overallRating: String,
    aiConfidence: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (overallRating) {
                "Exceptional", "Excellent" -> MaterialTheme.colorScheme.tertiaryContainer
                "Very Good", "Good" -> MaterialTheme.colorScheme.primaryContainer
                "Average" -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Overall Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "%.1f".format(overallScore),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "out of 10",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = overallRating,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "AI Confidence: $aiConfidence%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OLQScoreCard(
    olq: OLQ,
    score: com.ssbmax.core.domain.model.interview.OLQScore,
    isStrength: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isStrength) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = olq.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = score.rating,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (score.rating) {
                            "Exceptional", "Excellent" -> MaterialTheme.colorScheme.tertiary
                            "Very Good", "Good" -> MaterialTheme.colorScheme.primary
                            "Average" -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (score.score) {
                        in 1..3 -> MaterialTheme.colorScheme.tertiary
                        in 4..6 -> MaterialTheme.colorScheme.primary
                        7 -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.error
                    }
                ) {
                    Text(
                        text = "${score.score}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }
            
            if (score.reasoning.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = score.reasoning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
