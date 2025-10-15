package com.ssbmax.ui.phase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType

/**
 * Phase 1 Detail Screen - Screening Tests (OIR & PPDT)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Phase1DetailScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTest: (TestType) -> Unit = {},
    viewModel: Phase1DetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phase 1 - Screening") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Phase Overview Card
            item {
                PhaseOverviewCard(
                    phaseTitle = "Phase 1 - Screening Tests",
                    phaseDescription = "The screening tests consist of Officer Intelligence Rating (OIR) and Picture Perception & Description Test (PPDT). Candidates must clear this stage to proceed to Phase 2.",
                    totalTests = uiState.tests.size,
                    completedTests = uiState.tests.count { it.status == TestStatus.COMPLETED },
                    averageScore = uiState.averageScore
                )
            }
            
            // Test Cards
            items(uiState.tests) { test ->
                TestCard(
                    test = test,
                    onStartTest = { onNavigateToTest(test.type) },
                    onViewHistory = { /* TODO: View history */ }
                )
            }
            
            // Tips Section
            item {
                TipsCard(
                    tips = listOf(
                        "OIR tests your logical reasoning and intelligence. Practice regularly to improve speed and accuracy.",
                        "PPDT evaluates your perception and narration abilities. Focus on creating clear, positive stories.",
                        "Time management is crucial. Practice with a timer to simulate real test conditions."
                    )
                )
            }
        }
    }
}

@Composable
private fun PhaseOverviewCard(
    phaseTitle: String,
    phaseDescription: String,
    totalTests: Int,
    completedTests: Int,
    averageScore: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = phaseTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = phaseDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    label = "Tests",
                    value = "$completedTests/$totalTests"
                )
                
                StatItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "Avg Score",
                    value = if (completedTests > 0) "${averageScore.toInt()}%" else "N/A"
                )
                
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Progress",
                    value = "${(completedTests * 100 / totalTests.coerceAtLeast(1))}%"
                )
            }
        }
    }
}

@Composable
internal fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TestCard(
    test: Phase1Test,
    onStartTest: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (test.type) {
                            TestType.OIR -> Icons.Default.Quiz
                            TestType.PPDT -> Icons.Default.Image
                            else -> Icons.AutoMirrored.Filled.Assignment
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Column {
                        Text(
                            text = test.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = test.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status Badge
                TestStatusBadge(status = test.status)
            }
            
            // Description
            Text(
                text = test.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Test Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailChip(
                    icon = Icons.Default.Timer,
                    label = "${test.durationMinutes} min"
                )
                DetailChip(
                    icon = Icons.Default.QuestionMark,
                    label = "${test.questionCount} questions"
                )
                if (test.attemptsCount > 0) {
                    DetailChip(
                        icon = Icons.Default.Repeat,
                        label = "${test.attemptsCount} attempts"
                    )
                }
            }
            
            // Score Display (if completed)
            if (test.status == TestStatus.COMPLETED && test.latestScore != null) {
                LinearProgressIndicator(
                    progress = { test.latestScore / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        test.latestScore >= 75 -> MaterialTheme.colorScheme.tertiary
                        test.latestScore >= 50 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    },
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Latest Score",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${test.latestScore.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartTest,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when (test.status) {
                            TestStatus.NOT_ATTEMPTED -> Icons.Default.PlayArrow
                            TestStatus.IN_PROGRESS -> Icons.Default.PlayArrow
                            TestStatus.SUBMITTED_PENDING_REVIEW -> Icons.Default.HourglassEmpty
                            TestStatus.GRADED -> Icons.Default.Refresh
                            TestStatus.COMPLETED -> Icons.Default.Refresh
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when (test.status) {
                            TestStatus.NOT_ATTEMPTED -> "Start Test"
                            TestStatus.IN_PROGRESS -> "Resume"
                            TestStatus.SUBMITTED_PENDING_REVIEW -> "View Submission"
                            TestStatus.GRADED -> "View Results"
                            TestStatus.COMPLETED -> "Retake"
                        }
                    )
                }
                
                if (test.attemptsCount > 0) {
                    OutlinedButton(
                        onClick = onViewHistory,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("History")
                    }
                }
            }
        }
    }
}

@Composable
internal fun TestStatusBadge(status: TestStatus) {
    val (color, text, icon) = when (status) {
        TestStatus.NOT_ATTEMPTED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Not Started",
            Icons.Default.RadioButtonUnchecked
        )
        TestStatus.IN_PROGRESS -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            "In Progress",
            Icons.Default.RadioButtonChecked
        )
        TestStatus.SUBMITTED_PENDING_REVIEW -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            "Under Review",
            Icons.Default.HourglassEmpty
        )
        TestStatus.GRADED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            "Graded",
            Icons.Default.Grade
        )
        TestStatus.COMPLETED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            "Completed",
            Icons.Default.CheckCircle
        )
    }
    
    AssistChip(
        onClick = { },
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(containerColor = color)
    )
}

@Composable
internal fun DetailChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TipsCard(
    tips: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Preparation Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            tips.forEach { tip ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Data classes
data class Phase1Test(
    val type: TestType,
    val name: String,
    val subtitle: String,
    val description: String,
    val durationMinutes: Int,
    val questionCount: Int,
    val status: TestStatus,
    val latestScore: Float?,
    val attemptsCount: Int
)

