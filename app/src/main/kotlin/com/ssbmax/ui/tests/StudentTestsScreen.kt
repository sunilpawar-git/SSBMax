package com.ssbmax.ui.tests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                title = { Text("All Tests") },
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
                    text = { Text("Phase 1") },
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
                    text = { Text("Phase 2") },
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
                phaseTitle = "Phase 1 - Screening",
                phaseDescription = "The screening phase consists of OIR and PPDT tests. " +
                        "Your performance determines eligibility for Phase 2.",
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
                phaseTitle = "Phase 2 - Assessment",
                phaseDescription = "Phase 2 includes Psychology Tests, GTO Tasks, and Personal Interview. " +
                        "These comprehensively evaluate your officer-like qualities.",
                completedTests = tests.count { it.status == TestStatus.COMPLETED },
                totalTests = tests.size,
                onViewDetail = onViewPhaseDetail
            )
        }
        
        // Group tests by category
        item {
            Text(
                text = "Psychology Tests",
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
                text = "GTO Tasks",
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
                text = "Interview",
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
                        text = "Progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$completedTests of $totalTests tests completed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                TextButton(onClick = onViewDetail) {
                    Text("View Details")
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
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
                            text = "${test.durationMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${test.questionCount} ${if (test.questionCount == 1) "question" else "questions"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (test.status == TestStatus.COMPLETED && test.latestScore != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Latest: ${test.latestScore.toInt()}%",
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
                        TestStatus.NOT_ATTEMPTED -> "Start"
                        TestStatus.IN_PROGRESS -> "Resume"
                        TestStatus.SUBMITTED_PENDING_REVIEW -> "View"
                        TestStatus.GRADED -> "Results"
                        TestStatus.COMPLETED -> "Retake"
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

