package com.ssbmax.ui.phase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
 * Shows topic cards that navigate to Topic Screens, not direct test access
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Phase2DetailScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTopic: (String) -> Unit = {},
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
            
            // Topic Cards - Navigate to Topic Screens
            item {
                TopicCard(
                    topicId = "PSYCHOLOGY",
                    title = "Psychology Tests",
                    description = "TAT, WAT, SRT, and Self Description tests to assess personality traits and thought patterns.",
                    icon = Icons.Default.Psychology,
                    onClick = { onNavigateToTopic("PSYCHOLOGY") }
                )
            }
            
            item {
                TopicCard(
                    topicId = "GTO",
                    title = "Group Testing Officer Tasks",
                    description = "Group discussions, planning exercises, command tasks, and lecturettes to evaluate leadership.",
                    icon = Icons.Default.Groups,
                    onClick = { onNavigateToTopic("GTO") }
                )
            }
            
            item {
                TopicCard(
                    topicId = "INTERVIEW",
                    title = "Interview",
                    description = "Personal interview with the Interviewing Officer to assess overall personality and suitability.",
                    icon = Icons.Default.RecordVoiceOver,
                    onClick = { onNavigateToTopic("INTERVIEW") }
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

/**
 * Topic Card - Navigates to Topic Screen with tabs (Overview, Study, Tests)
 * NO direct test access - tests are only in Topic Screen's Tests tab
 */
@Composable
private fun TopicCard(
    topicId: String,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Arrow Icon
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open topic",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

// Data class kept for backward compatibility with ViewModel
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

