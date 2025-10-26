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
 * Phase 1 Detail Screen - Screening Tests (OIR & PPDT)
 * Shows topic cards that navigate to Topic Screens, not direct test access
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Phase1DetailScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTopic: (String) -> Unit = {},
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
            
            // Topic Cards - Navigate to Topic Screen
            item {
                TopicCard(
                    topicId = "OIR",
                    title = "Officer Intelligence Rating (OIR)",
                    description = "Evaluates cognitive abilities, logical reasoning, and problem-solving skills under time pressure.",
                    icon = Icons.Default.Quiz,
                    onClick = { onNavigateToTopic("OIR") }
                )
            }
            
            item {
                TopicCard(
                    topicId = "PPDT",
                    title = "Picture Perception & Description Test (PPDT)",
                    description = "Assesses perception, imagination, and story construction from ambiguous pictures.",
                    icon = Icons.Default.Image,
                    onClick = { onNavigateToTopic("PPDT") }
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
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
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

