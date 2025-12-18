package com.ssbmax.ui.home.student.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.usecase.dashboard.ProcessedDashboardData
import com.ssbmax.ui.theme.SSBScoreColors

/**
 * OLQ Dashboard Card showing all test results with aggregated scores
 * Uses pre-computed data for performance (no calculations in UI)
 */
@Composable
fun OLQDashboardCard(
    processedData: ProcessedDashboardData,
    onNavigateToResult: (TestType, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val dashboard = processedData.dashboard // Extract for convenience

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your SSB Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Progress badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${dashboard.completedTestsCount}/${dashboard.totalTests} Tests",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two-column layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1: Phase 1 Tests (OIR, PPDT)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PHASE 1",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    TestScoreChip(
                        testName = "OIR",
                        score = dashboard.phase1Results.oirResult?.percentageScore,
                        isOLQBased = false,
                        onClick = { 
                            dashboard.phase1Results.oirResult?.let { 
                                onNavigateToResult(TestType.OIR, it.sessionId) 
                            }
                        }
                    )

                    TestScoreChip(
                        testName = "PPDT",
                        score = dashboard.phase1Results.ppdtResult?.finalScore,
                        isOLQBased = false,
                        onClick = {
                            dashboard.phase1Results.ppdtResult?.let {
                                onNavigateToResult(TestType.PPDT, it.submissionId)
                            }
                        }
                    )
                }

                // Column 2: Phase 2 Tests (Psychology + GTO + Interview)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PHASE 2",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Psychology Tests
                    Text(
                        text = "Psychology",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TestScoreChip(
                        testName = "TAT", 
                        score = dashboard.phase2Results.tatResult?.overallScore,
                        onClick = {
                            dashboard.phase2Results.tatResult?.let {
                                onNavigateToResult(TestType.TAT, it.submissionId)
                            }
                        }
                    )
                    TestScoreChip(
                        testName = "WAT", 
                        score = dashboard.phase2Results.watResult?.overallScore,
                        onClick = {
                            dashboard.phase2Results.watResult?.let {
                                onNavigateToResult(TestType.WAT, it.submissionId)
                            }
                        }
                    )
                    TestScoreChip(
                        testName = "SRT", 
                        score = dashboard.phase2Results.srtResult?.overallScore,
                        onClick = {
                            dashboard.phase2Results.srtResult?.let {
                                onNavigateToResult(TestType.SRT, it.submissionId)
                            }
                        }
                    )
                    TestScoreChip(
                        testName = "Self Desc", 
                        score = dashboard.phase2Results.sdResult?.overallScore,
                        onClick = {
                            dashboard.phase2Results.sdResult?.let {
                                onNavigateToResult(TestType.SD, it.submissionId)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // GTO Tests
                    Text(
                        text = "GTO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    dashboard.phase2Results.gtoResults.forEach { (testType, result) ->
                        TestScoreChip(
                            testName = testType.displayName,
                            score = result.overallScore,
                            onClick = { onNavigateToResult(result.testType, result.submissionId) }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Interview
                    Text(
                        text = "Interview",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TestScoreChip(
                        testName = "Interview",
                        score = dashboard.phase2Results.interviewResult?.getAverageOLQScore(),
                        onClick = {
                            dashboard.phase2Results.interviewResult?.let {
                                onNavigateToResult(TestType.IO, it.id)
                            }
                        }
                    )
                }
            }

            // Overall average score (pre-computed)
            processedData.overallAverageScore?.let { avgScore ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Overall Average",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    ScoreBadge(avgScore)
                }
            }

            // Top 3 Strengths (pre-computed)
            if (processedData.topOLQs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                OLQStrengthsSection(
                    title = "🌟 Your Strengths",
                    olqs = processedData.topOLQs,
                    color = Color(0xFF4CAF50)  // Green
                )
            }

            // Areas for Improvement (pre-computed)
            if (processedData.improvementOLQs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                OLQStrengthsSection(
                    title = "📈 Focus Areas",
                    olqs = processedData.improvementOLQs,
                    color = Color(0xFFFFC107)  // Amber
                )
            }
        }
    }
}

/**
 * Individual test score chip
 * Color-coded: Green (≤5), Amber (6-7), Red (≥8)
 */
@Composable
private fun TestScoreChip(
    testName: String,
    score: Float?,
    isOLQBased: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && score != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        shape = RoundedCornerShape(8.dp),
        color = when {
            score == null -> MaterialTheme.colorScheme.surfaceVariant
            score <= 5f -> Color(0xFF4CAF50).copy(alpha = 0.2f)  // Green
            score <= 7f -> Color(0xFFFFC107).copy(alpha = 0.2f)  // Amber
            else -> Color(0xFFF44336).copy(alpha = 0.2f)  // Red
        },
        border = if (score != null) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                when {
                    score <= 5f -> Color(0xFF4CAF50)
                    score <= 7f -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }
            )
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = testName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (score != null) {
                Text(
                    text = "%.1f".format(score),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        score <= 5f -> Color(0xFF4CAF50)
                        score <= 7f -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Large score badge for overall average
 */
@Composable
private fun ScoreBadge(score: Float) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            score <= 5f -> Color(0xFF4CAF50)
            score <= 7f -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        }
    ) {
        Text(
            text = "%.1f".format(score),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

/**
 * Section displaying OLQ strengths or improvement areas
 */
@Composable
private fun OLQStrengthsSection(
    title: String,
    olqs: List<Pair<OLQ, Float>>,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        olqs.forEach { (olq, score) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = olq.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "%.1f".format(score),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty state when no tests completed
 */
@Composable
fun EmptyDashboardState(
    modifier: Modifier = Modifier,
    onStartTestClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📊",
                style = MaterialTheme.typography.displayLarge
            )
            
            Text(
                text = "Start Your SSB Journey",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Complete tests to see your OLQ profile and track progress",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(onClick = onStartTestClick) {
                Text("Start First Test")
            }
        }
    }
}
