package com.ssbmax.ui.tests.gto.gd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory

/**
 * Group Discussion Result Screen
 * 
 * Displays:
 * - Submission confirmation
 * - Analysis status (real-time updates via Flow)
 * - Topic and response preview
 * - 15 OLQ scores grouped by category
 * - Overall rating and score
 * - Top strengths and improvement areas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GDResultScreen(
    submissionId: String,
    onNavigateHome: () -> Unit,
    viewModel: GDResultViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Load submission on mount
    LaunchedEffect(submissionId) {
        viewModel.loadSubmission(submissionId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Discussion Results") },
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
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Submission confirmation
                    item {
                        SubmissionConfirmationCard(
                            topic = uiState.submission!!.topic,
                            wordCount = uiState.submission!!.wordCount,
                            timeSpent = uiState.formattedTimeSpent,
                            status = uiState.submission!!.status
                        )
                    }
                    
                    // Analysis status
                    item {
                        when {
                            uiState.isAnalyzing -> AnalyzingCard()
                            uiState.isFailed -> AnalysisFailedCard()
                            uiState.isCompleted && uiState.result != null -> {
                                // Show results - handled by items below
                            }
                        }
                    }
                    
                    // Overall score (if completed)
                    if (uiState.isCompleted && uiState.result != null) {
                        item {
                            OverallScoreCard(
                                overallScore = uiState.result!!.overallScore,
                                overallRating = uiState.result!!.overallRating,
                                aiConfidence = uiState.result!!.aiConfidence
                            )
                        }
                        
                        // Top OLQs (strengths)
                        item {
                            Text(
                                text = "Top Strengths",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        items(uiState.result!!.topOLQs) { (olq, score) ->
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
                        
                        items(uiState.result!!.improvementOLQs) { (olq, score) ->
                            OLQScoreCard(olq = olq, score = score, isStrength = false)
                        }
                        
                        // All OLQ scores grouped by category
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Detailed OLQ Analysis",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Group OLQs by category
                        OLQCategory.entries.forEach { category ->
                            item {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            
                            val categoryOLQs = OLQ.getByCategory(category)
                            items(categoryOLQs) { olq ->
                                val score = uiState.result!!.olqScores[olq]
                                if (score != null) {
                                    OLQScoreCard(olq = olq, score = score, isStrength = null)
                                }
                            }
                        }
                        
                        // Response preview
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your Response",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        item {
                            ResponsePreviewCard(
                                topic = uiState.submission!!.topic,
                                response = uiState.submission!!.response,
                                wordCount = uiState.submission!!.wordCount
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
        }
    }
}

@Composable
private fun SubmissionConfirmationCard(
    topic: String,
    wordCount: Int,
    timeSpent: String,
    status: GTOSubmissionStatus
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
                    text = "Test Submitted Successfully!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider()
            
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
                    text = "Analyzing Your Response...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AI is evaluating your response across 15 Officer-Like Qualities. This usually takes 30-60 seconds.",
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
            
            // Score circle
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Note: SSB uses 1-10 scale where LOWER scores indicate BETTER performance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OLQScoreCard(
    olq: OLQ,
    score: com.ssbmax.core.domain.model.interview.OLQScore,
    isStrength: Boolean?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (isStrength) {
                true -> MaterialTheme.colorScheme.tertiaryContainer
                false -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                null -> MaterialTheme.colorScheme.surfaceVariant
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
                
                // Score badge
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
private fun ResponsePreviewCard(
    topic: String,
    response: String,
    wordCount: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Topic",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = topic,
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
                    text = "Your Response",
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
                text = response,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
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
