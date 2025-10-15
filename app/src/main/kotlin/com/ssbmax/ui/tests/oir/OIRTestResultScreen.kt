package com.ssbmax.ui.tests.oir

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.*

/**
 * OIR Test Result Screen - Shows detailed breakdown after test completion
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OIRTestResultScreen(
    sessionId: String,
    onNavigateHome: () -> Unit = {},
    onRetakeTest: () -> Unit = {},
    onReviewAnswers: () -> Unit = {},
    viewModel: OIRTestResultViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(sessionId) {
        viewModel.loadResult(sessionId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Results") },
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
            uiState.result != null -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Score Header
                    item {
                        ScoreHeaderCard(result = uiState.result!!)
                    }
                    
                    // Quick Stats
                    item {
                        QuickStatsCard(result = uiState.result!!)
                    }
                    
                    // Category Performance
                    item {
                        Text(
                            text = "Performance by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(uiState.result!!.categoryScores.values.toList()) { categoryScore ->
                        CategoryPerformanceCard(categoryScore = categoryScore)
                    }
                    
                    // Difficulty Breakdown
                    item {
                        Text(
                            text = "Difficulty Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    item {
                        DifficultyBreakdownCard(
                            difficultyScores = uiState.result!!.difficultyBreakdown
                        )
                    }
                    
                    // Action Buttons
                    item {
                        ActionButtonsCard(
                            onRetakeTest = onRetakeTest,
                            onReviewAnswers = onReviewAnswers,
                            onBackToHome = onNavigateHome
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreHeaderCard(result: OIRTestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (result.grade) {
                TestGrade.EXCELLENT, TestGrade.VERY_GOOD -> MaterialTheme.colorScheme.tertiaryContainer
                TestGrade.GOOD -> MaterialTheme.colorScheme.primaryContainer
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
            // Grade Emoji
            Text(
                text = result.grade.emoji,
                style = MaterialTheme.typography.displayLarge
            )
            
            // Grade Text
            Text(
                text = result.grade.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Score
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${result.percentageScore.toInt()}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Passed/Failed Badge
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        if (result.passed) "PASSED" else "NEEDS IMPROVEMENT",
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (result.passed) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (result.passed) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    labelColor = if (result.passed) {
                        MaterialTheme.colorScheme.onTertiary
                    } else {
                        MaterialTheme.colorScheme.onError
                    }
                )
            )
        }
    }
}

@Composable
private fun QuickStatsCard(result: OIRTestResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Quick Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Correct",
                    value = result.correctAnswers.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatItem(
                    icon = Icons.Default.Cancel,
                    label = "Incorrect",
                    value = result.incorrectAnswers.toString(),
                    color = MaterialTheme.colorScheme.error
                )
                StatItem(
                    icon = Icons.Default.RadioButtonUnchecked,
                    label = "Skipped",
                    value = result.skippedQuestions.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Timer,
                    label = "Time Taken",
                    value = "${result.timeTakenSeconds / 60}m",
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    icon = Icons.Default.Score,
                    label = "Raw Score",
                    value = result.rawScore.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryPerformanceCard(categoryScore: CategoryScore) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryScore.category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${categoryScore.percentage.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            LinearProgressIndicator(
                progress = { categoryScore.percentage / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    categoryScore.percentage >= 75 -> MaterialTheme.colorScheme.tertiary
                    categoryScore.percentage >= 50 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${categoryScore.correctAnswers}/${categoryScore.totalQuestions} correct",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Avg: ${categoryScore.averageTimeSeconds}s/q",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DifficultyBreakdownCard(
    difficultyScores: Map<QuestionDifficulty, DifficultyScore>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            difficultyScores.values.forEach { score ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = when (score.difficulty) {
                                QuestionDifficulty.EASY -> Icons.Default.Circle
                                QuestionDifficulty.MEDIUM -> Icons.Default.TripOrigin
                                QuestionDifficulty.HARD -> Icons.Default.RadioButtonChecked
                            },
                            contentDescription = null,
                            tint = when (score.difficulty) {
                                QuestionDifficulty.EASY -> Color.Green
                                QuestionDifficulty.MEDIUM -> Color.Yellow
                                QuestionDifficulty.HARD -> Color.Red
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = score.difficulty.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Text(
                        text = "${score.correctAnswers}/${score.totalQuestions}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "${score.percentage.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(60.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsCard(
    onRetakeTest: () -> Unit,
    onReviewAnswers: () -> Unit,
    onBackToHome: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onReviewAnswers,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Review Answers")
            }
            
            OutlinedButton(
                onClick = onRetakeTest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retake Test")
            }
            
            TextButton(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Home")
            }
        }
    }
}

