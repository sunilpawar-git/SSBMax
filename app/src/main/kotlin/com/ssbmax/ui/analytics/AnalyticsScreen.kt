package com.ssbmax.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.DifficultyStats
import com.ssbmax.core.domain.model.PerformanceOverview
import com.ssbmax.core.domain.model.TestTypeStats

/**
 * Analytics Dashboard Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadAllTestStats()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.error ?: "Unknown error")
                }
            }
            uiState.overview == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No data yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Complete some tests to see your analytics",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                AnalyticsContent(
                    overview = uiState.overview!!,
                    testStats = uiState.allTestStats,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsContent(
    overview: PerformanceOverview,
    testStats: List<TestTypeStats>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview Card
        item {
            OverviewCard(overview)
        }
        
        // Test Stats
        item {
            Text(
                "Test Performance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(testStats) { stats ->
            TestStatsCard(stats)
        }
    }
}

@Composable
private fun OverviewCard(overview: PerformanceOverview) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Overall Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    label = "Tests",
                    value = overview.totalTests.toString(),
                    icon = Icons.Default.CheckCircle
                )
                StatItem(
                    label = "Avg Score",
                    value = "%.1f%%".format(overview.averageScore),
                    icon = Icons.Default.Star
                )
                StatItem(
                    label = "Study Time",
                    value = "${overview.totalStudyTimeMinutes}m",
                    icon = Icons.Default.DateRange
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TestStatsCard(stats: TestTypeStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stats.testType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                DifficultyChip(stats.currentDifficulty)
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Attempts", style = MaterialTheme.typography.bodySmall)
                    Text(stats.totalAttempts.toString(), fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Avg Score", style = MaterialTheme.typography.bodySmall)
                    Text("%.1f%%".format(stats.averageScore), fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Best", style = MaterialTheme.typography.bodySmall)
                    Text("%.1f%%".format(stats.bestScore), fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Difficulty breakdown
            DifficultyBreakdown(stats.easyStats, stats.mediumStats, stats.hardStats)
            
            // Progression
            if (stats.progressionStatus.nextLevel != null) {
                Spacer(Modifier.height(12.dp))
                ProgressionIndicator(stats.progressionStatus)
            }
        }
    }
}

@Composable
private fun DifficultyChip(difficulty: String) {
    val color = when (difficulty) {
        "EASY" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        "MEDIUM" -> androidx.compose.ui.graphics.Color(0xFFFFA726)
        "HARD" -> androidx.compose.ui.graphics.Color(0xFFEF5350)
        else -> MaterialTheme.colorScheme.secondary
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            difficulty,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun DifficultyBreakdown(
    easy: DifficultyStats,
    medium: DifficultyStats,
    hard: DifficultyStats
) {
    Column {
        Text("Difficulty Breakdown", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        
        if (easy.attempts > 0) {
            DifficultyRow("Easy", easy.attempts, easy.accuracy)
        }
        if (medium.attempts > 0) {
            DifficultyRow("Medium", medium.attempts, medium.accuracy)
        }
        if (hard.attempts > 0) {
            DifficultyRow("Hard", hard.attempts, hard.accuracy)
        }
    }
}

@Composable
private fun DifficultyRow(label: String, attempts: Int, accuracy: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$label ($attempts)", style = MaterialTheme.typography.bodySmall)
        Text("%.1f%% accuracy".format(accuracy), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProgressionIndicator(status: com.ssbmax.core.domain.model.ProgressionStatus) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Progress to ${status.nextLevel}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "%.0f%%".format(status.progressPercentage),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { status.progressPercentage / 100f },
            modifier = Modifier.fillMaxWidth()
        )
        if (!status.canProgress) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Need ${status.attemptsNeeded} more attempts & %.0f%% accuracy".format(status.accuracyNeeded),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

