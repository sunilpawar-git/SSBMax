package com.ssbmax.ui.home.student.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.usecase.dashboard.ProcessedDashboardData


/**
 * OLQ Dashboard Card showing all test results with aggregated scores
 * Uses pre-computed data for performance (no calculations in UI)
 *
 * @param isLoading true while the initial Firestore fetch is in flight (dashboard == null in ViewModel).
 *                  Shows a LinearProgressIndicator so the card structure is always visible without
 *                  jumping to a different composable. Cleared to false on success or failure.
 */
@Composable
fun OLQDashboardCard(
    processedData: ProcessedDashboardData,
    onNavigateToResult: (TestType, String) -> Unit = { _, _ -> },
    isLoading: Boolean = false,
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

            // Show LinearProgressIndicator during initial fetch; card structure stays visible.
            // assertDoesNotExist in tests relies on this testTag being absent when isLoading = false.
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_loading_indicator")
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Partial-load banner: shown when one or more test types timed out or are still
            // being analyzed (see ProcessedDashboardData.unavailableTypes). The rest of the
            // dashboard still renders; this offers a one-tap retry for the missing tiles.
            if (processedData.unavailableTypes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                UnavailableResultsBanner(onRetry = onRefresh)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vertical Section Layout (Symmetrical & Balanced)
            // Pass isRefreshing to sections for subtle visual feedback
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Section 1: Screening (Phase 1)
                Phase1Section(
                    results = dashboard.phase1Results,
                    onNavigateToResult = onNavigateToResult,
                    isRefreshing = isRefreshing
                )

                // Section 2: Psychology
                PsychologySection(
                    results = dashboard.phase2Results,
                    onNavigateToResult = onNavigateToResult,
                    isRefreshing = isRefreshing
                )

                // Section 3: GTO
                GTOSection(
                    results = dashboard.phase2Results,
                    onNavigateToResult = onNavigateToResult,
                    isRefreshing = isRefreshing
                )

                // Section 4: Interview
                InterviewSection(
                    results = dashboard.phase2Results,
                    onNavigateToResult = onNavigateToResult,
                    isRefreshing = isRefreshing
                )
            }

            // Overall average score (pre-computed)
            processedData.overallAverageScore?.let { avgScore ->
                Spacer(modifier = Modifier.height(24.dp))
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

/**
 * Banner shown when part of the dashboard couldn't load (a test type timed out or its analysis is
 * still pending). Keeps the rest of the dashboard usable and offers a one-tap retry.
 */
@Composable
private fun UnavailableResultsBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_unavailable_banner"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.dashboard_some_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.dashboard_retry))
            }
        }
    }
}

// TestScoreChip extracted to DashboardTestScoreChip.kt

// ScoreBadge extracted to DashboardScoreBadge.kt

// OLQStrengthsSection extracted to DashboardOLQStrengths.kt
