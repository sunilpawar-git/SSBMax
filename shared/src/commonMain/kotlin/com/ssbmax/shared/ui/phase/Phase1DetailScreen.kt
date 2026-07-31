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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Quiz
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.domain.model.TestStatus
import com.ssbmax.shared.presentation.phase1detail.Phase1DetailViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.phase1_overview_description
import ssbmax.shared.generated.resources.phase1_overview_title
import ssbmax.shared.generated.resources.phase1_screening_title

/**
 * KMP port of the Android `app/.../ui/phase/Phase1DetailScreen.kt` -- Phase 1
 * (Screening: OIR & PPDT) overview. Shows topic cards that navigate to Topic
 * Screens, not direct test access. Shared `StatItem`/`TopicCard`/`TipsCard`
 * composables live in [PhaseDetailComponents] (see its own doc for why they're
 * consolidated with [Phase2DetailScreen]'s equivalents rather than duplicated).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Phase1DetailScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTopic: (String) -> Unit = {},
    viewModel: Phase1DetailViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.phase1_screening_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            modifier = modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Phase1OverviewCard(
                    totalTests = uiState.tests.size,
                    completedTests = uiState.tests.count { it.status == TestStatus.COMPLETED },
                    averageScore = uiState.averageScore
                )
            }

            item {
                PhaseTopicCard(
                    title = "Officer Intelligence Rating (OIR)",
                    description = "Evaluates cognitive abilities, logical reasoning, and problem-solving skills under time pressure.",
                    icon = Icons.Default.Quiz,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { onNavigateToTopic("OIR") }
                )
            }

            item {
                PhaseTopicCard(
                    title = "Picture Perception & Description Test (PPDT)",
                    description = "Assesses perception, imagination, and story construction from ambiguous pictures.",
                    icon = Icons.Default.Image,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { onNavigateToTopic("PPDT") }
                )
            }

            item {
                PhaseTipsCard(
                    title = "Preparation Tips",
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
private fun Phase1OverviewCard(
    totalTests: Int,
    completedTests: Int,
    averageScore: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(Res.string.phase1_overview_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(Res.string.phase1_overview_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
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
