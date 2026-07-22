package com.ssbmax.shared.ui.phase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.TestStatus
import com.ssbmax.shared.presentation.phase2detail.Phase2DetailViewModel
import org.koin.compose.koinInject

/**
 * KMP port of the Android `app/.../ui/phase/Phase2DetailScreen.kt` -- Phase 2
 * (Psychology/GTO/Interview) overview. Shows topic cards that navigate to
 * Topic Screens, not direct test access. Shared `StatItem`/`TopicCard`/
 * `TipsCard` composables live in [PhaseDetailComponents] (see its own doc for
 * why they're consolidated with [Phase1DetailScreen]'s equivalents rather
 * than duplicated).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Phase2DetailScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTopic: (String) -> Unit = {},
    viewModel: Phase2DetailViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phase 2 - Assessment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            modifier = modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Phase2OverviewCard(
                    totalTests = uiState.tests.size,
                    completedTests = uiState.tests.count { it.status == TestStatus.COMPLETED },
                    averageScore = uiState.averageScore
                )
            }

            item {
                PhaseTopicCard(
                    title = "Psychology Tests",
                    description = "TAT, WAT, SRT, and Self Description tests to assess personality traits and thought patterns.",
                    icon = Icons.Default.Psychology,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = { onNavigateToTopic("PSYCHOLOGY") }
                )
            }

            item {
                PhaseTopicCard(
                    title = "Group Testing Officer Tasks",
                    description = "Group discussions, planning exercises, command tasks, and lecturettes to evaluate leadership.",
                    icon = Icons.Default.Groups,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = { onNavigateToTopic("GTO") }
                )
            }

            item {
                PhaseTopicCard(
                    title = "Interview",
                    description = "Personal interview with the Interviewing Officer to assess overall personality and suitability.",
                    icon = Icons.Default.RecordVoiceOver,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = { onNavigateToTopic("INTERVIEW") }
                )
            }

            item {
                PhaseTipsCard(
                    title = "Phase 2 Tips",
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
    totalTests: Int,
    completedTests: Int,
    averageScore: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Phase 2 - Assessment Tests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = "Phase 2 consists of Psychology Tests, Group Testing Officer (GTO) Tasks, " +
                    "and Interview. This comprehensive assessment evaluates your personality, leadership qualities, " +
                    "and suitability for officer roles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PhaseStatItem(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    label = "Tests",
                    value = "$completedTests/$totalTests"
                )
                PhaseStatItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "Avg Score",
                    value = if (completedTests > 0) "${averageScore.toInt()}%" else "N/A"
                )
                PhaseStatItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Progress",
                    value = "${(completedTests * 100 / totalTests.coerceAtLeast(1))}%"
                )
            }
        }
    }
}
