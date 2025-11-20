package com.ssbmax.ui.tests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.TestPhase
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType

/**
 * Student Tests Screen - Overview of all tests
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTestsScreen(
    onNavigateToPhase: (TestPhase) -> Unit = {},
    onNavigateToTest: (TestType) -> Unit = {},
    viewModel: StudentTestsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.student_tests_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.student_tests_tab_phase1)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.student_tests_tab_phase2)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null
                        )
                    }
                )
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> Phase1TestsList(
                    tests = uiState.phase1Tests,
                    onNavigateToTest = onNavigateToTest,
                    onViewPhaseDetail = { onNavigateToPhase(TestPhase.PHASE_1) }
                )
                1 -> Phase2TestsList(
                    tests = uiState.phase2Tests,
                    onNavigateToTest = onNavigateToTest,
                    onViewPhaseDetail = { onNavigateToPhase(TestPhase.PHASE_2) }
                )
            }
        }
    }
}

@Composable
private fun Phase1TestsList(
    tests: List<TestOverviewItem>,
    onNavigateToTest: (TestType) -> Unit,
    onViewPhaseDetail: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PhaseOverviewBanner(
                phaseTitle = stringResource(R.string.student_tests_phase1_title),
                phaseDescription = stringResource(R.string.student_tests_phase1_description),
                completedTests = tests.count { it.status == TestStatus.COMPLETED },
                totalTests = tests.size,
                onViewDetail = onViewPhaseDetail
            )
        }
        
        items(tests) { test ->
            TestOverviewCard(
                test = test,
                onStartTest = { onNavigateToTest(test.type) }
            )
        }
    }
}

@Composable
private fun Phase2TestsList(
    tests: List<TestOverviewItem>,
    onNavigateToTest: (TestType) -> Unit,
    onViewPhaseDetail: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PhaseOverviewBanner(
                phaseTitle = stringResource(R.string.student_tests_phase2_title),
                phaseDescription = stringResource(R.string.student_tests_phase2_description),
                completedTests = tests.count { it.status == TestStatus.COMPLETED },
                totalTests = tests.size,
                onViewDetail = onViewPhaseDetail
            )
        }
        
        // Group tests by category
        item {
            Text(
                text = stringResource(R.string.student_tests_category_psychology),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(tests.filter { it.category == "Psychology" }) { test ->
            TestOverviewCard(
                test = test,
                onStartTest = { onNavigateToTest(test.type) }
            )
        }

        item {
            Text(
                text = stringResource(R.string.student_tests_category_gto),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(tests.filter { it.category == "GTO" }) { test ->
            TestOverviewCard(
                test = test,
                onStartTest = { onNavigateToTest(test.type) }
            )
        }

        item {
            Text(
                text = stringResource(R.string.student_tests_category_interview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(tests.filter { it.category == "Interview" }) { test ->
            TestOverviewCard(
                test = test,
                onStartTest = { onNavigateToTest(test.type) }
            )
        }
    }
}

@Composable
private fun PhaseOverviewBanner(
    phaseTitle: String,
    phaseDescription: String,
    completedTests: Int,
    totalTests: Int,
    onViewDetail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            
            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.student_tests_progress_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.student_tests_progress_format, completedTests, totalTests),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                TextButton(onClick = onViewDetail) {
                    Text(stringResource(R.string.student_tests_view_details))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TestOverviewCard(
    test: TestOverviewItem,
    onStartTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = test.icon,
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
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.student_tests_duration_format, test.durationMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${test.questionCount} ${if (test.questionCount == 1) stringResource(R.string.student_tests_question_single) else stringResource(R.string.student_tests_question_plural)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (test.status == TestStatus.COMPLETED && test.latestScore != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.student_tests_latest_score, test.latestScore.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            FilledTonalButton(
                onClick = onStartTest,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    when (test.status) {
                        TestStatus.NOT_ATTEMPTED -> stringResource(R.string.student_tests_action_start)
                        TestStatus.IN_PROGRESS -> stringResource(R.string.student_tests_action_resume)
                        TestStatus.SUBMITTED_PENDING_REVIEW -> stringResource(R.string.student_tests_action_view)
                        TestStatus.GRADED -> stringResource(R.string.student_tests_action_results)
                        TestStatus.COMPLETED -> stringResource(R.string.student_tests_action_retake)
                    }
                )
            }
        }
    }
}

// Data class
data class TestOverviewItem(
    val type: TestType,
    val name: String,
    val icon: ImageVector,
    val category: String,
    val durationMinutes: Int,
    val questionCount: Int,
    val status: TestStatus,
    val latestScore: Float?
)

