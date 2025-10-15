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
 * Phase 2 Detail Screen - Assessment Tests (Psychology, GTO, IO)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Phase2DetailScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTest: (TestType) -> Unit = {},
    viewModel: Phase2DetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phase 2 - Assessment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    titleContentColor = MaterialTheme.colorScheme.onTertiary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onTertiary
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
                Phase2OverviewCard(
                    phaseTitle = "Phase 2 - Assessment Tests",
                    phaseDescription = "Phase 2 consists of Psychology Tests, Group Testing Officer (GTO) Tasks, " +
                            "and Interview. This comprehensive assessment evaluates your personality, leadership qualities, " +
                            "and suitability for officer roles.",
                    totalTests = uiState.tests.size,
                    completedTests = uiState.tests.count { it.status == TestStatus.COMPLETED },
                    averageScore = uiState.averageScore
                )
            }
            
            // Test Category Sections
            item {
                Text(
                    text = "Psychology Tests",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(uiState.psychologyTests) { test ->
                Phase2TestCard(
                    test = test,
                    onStartTest = { onNavigateToTest(test.type) },
                    onViewHistory = { /* TODO: View history */ }
                )
            }
            
            item {
                Text(
                    text = "GTO Tasks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(uiState.gtoTests) { test ->
                Phase2TestCard(
                    test = test,
                    onStartTest = { onNavigateToTest(test.type) },
                    onViewHistory = { /* TODO: View history */ }
                )
            }
            
            item {
                Text(
                    text = "Interview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(uiState.ioTests) { test ->
                Phase2TestCard(
                    test = test,
                    onStartTest = { onNavigateToTest(test.type) },
                    onViewHistory = { /* TODO: View history */ }
                )
            }
            
            // Tips Section
            item {
                Phase2TipsCard(
                    tips = listOf(
                        "Psychology tests reveal your personality traits. Be honest and spontaneous in your responses.",
                        "GTO tasks assess teamwork, leadership, and problem-solving. Stay calm and collaborative.",
                        "The interview evaluates your overall personality. Be confident, truthful, and well-prepared.",
                        "Maintain consistency across all tests. Contradictory responses raise red flags."
                    )
                )
            }
        }
    }
}

@Composable
private fun Phase2OverviewCard(
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
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Text(
                text = phaseDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
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
private fun Phase2TestCard(
    test: Phase2Test,
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
                        imageVector = getTestIcon(test.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
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
                
                TestStatusBadge(status = test.status)
            }
            
            Text(
                text = test.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Test Details Row
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
                    label = "${test.questionCount} ${if (test.questionCount == 1) "task" else "tasks"}"
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
private fun Phase2TipsCard(
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
                    text = "Phase 2 Tips",
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

private fun getTestIcon(testType: TestType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (testType) {
        TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD -> Icons.Default.EditNote
        TestType.GTO -> Icons.Default.Groups
        TestType.IO -> Icons.Default.RecordVoiceOver
        else -> Icons.AutoMirrored.Filled.Assignment
    }
}

// Data class
data class Phase2Test(
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

