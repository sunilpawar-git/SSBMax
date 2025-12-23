package com.ssbmax.ui.home.student.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.usecase.dashboard.ProcessedDashboardData
import com.ssbmax.ui.theme.SSBScoreColors

/**
 * Formats relative time string ("Updated just now", "5m ago", etc.)
 */
private fun formatRelativeTime(refreshTime: Long, justNowText: String): String {
    val diffMillis = System.currentTimeMillis() - refreshTime
    val diffSeconds = diffMillis / 1000
    val diffMinutes = diffSeconds / 60
    return when {
        diffSeconds < 60 -> justNowText
        diffMinutes < 60 -> "Updated ${diffMinutes}m ago"
        else -> "Updated ${diffMinutes / 60}h ago"
    }
}

/**
 * OLQ Dashboard Card showing all test results with aggregated scores
 * Uses pre-computed data for performance (no calculations in UI)
 */
@Composable
fun OLQDashboardCard(
    processedData: ProcessedDashboardData,
    onNavigateToResult: (TestType, String) -> Unit = { _, _ -> },
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dashboard = processedData.dashboard // Extract for convenience
    
    // Animate refresh icon rotation
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = tween(
            durationMillis = if (isRefreshing) 1000 else 0,
            easing = androidx.compose.animation.core.LinearEasing
        ),
        label = "refresh_rotation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dashboard_your_progress),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Right side: Refresh button + Progress badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Refresh button
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh_dashboard),
                            modifier = Modifier.rotate(rotationAngle),
                            tint = if (isRefreshing) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Progress badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${dashboard.completedTestsCount}/${dashboard.totalTests} ${stringResource(R.string.dashboard_tests)}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
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
                        text = stringResource(R.string.dashboard_phase_1),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    TestScoreChip(
                        testName = stringResource(R.string.dashboard_test_oir),
                        score = dashboard.phase1Results.oirResult?.percentageScore,
                        isOLQBased = false,
                        onClick = { 
                            dashboard.phase1Results.oirResult?.let { 
                                onNavigateToResult(TestType.OIR, it.sessionId) 
                            }
                        }
                    )

                    TestScoreChip(
                        testName = stringResource(R.string.dashboard_test_ppdt),
                        score = dashboard.phase1Results.ppdtOLQResult?.overallScore 
                            ?: dashboard.phase1Results.ppdtResult?.finalScore,
                        isOLQBased = true,  // ✅ Now OLQ-based
                        onClick = if (dashboard.phase1Results.ppdtResult != null) {
                            // Clickable when submission exists (even during analysis)
                            {
                                dashboard.phase1Results.ppdtResult?.let {
                                    onNavigateToResult(TestType.PPDT, it.submissionId)
                                }
                            }
                        } else null  // No submission = not clickable
                    )
                }

                // Column 2: Phase 2 Tests (Psychology + GTO + Interview)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_phase_2),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Psychology Tests
                    Text(
                        text = stringResource(R.string.dashboard_psychology),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TestScoreChip(
                        testName = stringResource(R.string.dashboard_test_tat), 
                        score = dashboard.phase2Results.tatResult?.overallScore,
                        onClick = {
                            dashboard.phase2Results.tatResult?.let {
                                onNavigateToResult(TestType.TAT, it.submissionId)
                            }
                        }
                    )
                    TestScoreChip(
                        testName = stringResource(R.string.dashboard_test_wat), 
                        score = dashboard.phase2Results.watResult?.overallScore,
                        onClick = {
                            dashboard.phase2Results.watResult?.let {
                                onNavigateToResult(TestType.WAT, it.submissionId)
                            }
                        }
                    )
                    TestScoreChip(
                        testName = stringResource(R.string.dashboard_test_srt), 
                        score = dashboard.phase2Results.srtResult?.overallScore,
                        onClick = {
                            dashboard.phase2Results.srtResult?.let {
                                onNavigateToResult(TestType.SRT, it.submissionId)
                            }
                        }
                    )
                    TestScoreChip(
                        testName = stringResource(R.string.dashboard_test_self_desc), 
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
                        text = stringResource(R.string.dashboard_gto),
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
                        text = stringResource(R.string.dashboard_interview),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TestScoreChip(
                        testName = stringResource(R.string.dashboard_test_interview),
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
                        text = stringResource(R.string.dashboard_overall_average),
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
                    title = stringResource(R.string.dashboard_your_strengths),
                    olqs = processedData.topOLQs,
                    color = Color(0xFF4CAF50)  // Green
                )
            }

            // Areas for Improvement (pre-computed)
            if (processedData.improvementOLQs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                OLQStrengthsSection(
                    title = stringResource(R.string.dashboard_focus_areas),
                    olqs = processedData.improvementOLQs,
                    color = Color(0xFFFFC107)  // Amber
                )
            }

            // Last updated timestamp
            processedData.cacheMetadata.let { metadata ->
                if (!metadata.cacheHit) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.dashboard_last_updated_now),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// TestScoreChip extracted to DashboardTestScoreChip.kt

// ScoreBadge extracted to DashboardScoreBadge.kt

// OLQStrengthsSection extracted to DashboardOLQStrengths.kt
// EmptyDashboardState extracted to DashboardEmptyState.kt
