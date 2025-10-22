package com.ssbmax.ui.submissions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.TestType

/**
 * Submissions List Screen - Shows all user submissions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionsListScreen(
    onSubmissionClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToTests: () -> Unit = {},
    viewModel: SubmissionsListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var selectedFilter by remember { mutableStateOf<TestType?>(null) }
    var selectedStatus by remember { mutableStateOf<SubmissionStatus?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Submissions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "SSB Preparation > Test Results",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Chips
            FilterSection(
                selectedTestType = selectedFilter,
                selectedStatus = selectedStatus,
                onTestTypeSelected = {
                    selectedFilter = it
                    viewModel.filterByType(it)
                },
                onStatusSelected = {
                    selectedStatus = it
                    viewModel.filterByStatus(it)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Content
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.error != null -> {
                    ErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                }
                uiState.submissions.isEmpty() -> {
                    EmptyState(onTakeTest = onNavigateToTests)
                }
                else -> {
                    SubmissionsList(
                        submissions = uiState.submissions,
                        onSubmissionClick = onSubmissionClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    selectedTestType: TestType?,
    selectedStatus: SubmissionStatus?,
    onTestTypeSelected: (TestType?) -> Unit,
    onStatusSelected: (SubmissionStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Test Type Filters
        Text(
            "Test Type",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            FilterChip(
                selected = selectedTestType == null,
                onClick = { onTestTypeSelected(null) },
                label = { Text("All") }
            )
            FilterChip(
                selected = selectedTestType == TestType.TAT,
                onClick = { onTestTypeSelected(TestType.TAT) },
                label = { Text("TAT") }
            )
            FilterChip(
                selected = selectedTestType == TestType.WAT,
                onClick = { onTestTypeSelected(TestType.WAT) },
                label = { Text("WAT") }
            )
            FilterChip(
                selected = selectedTestType == TestType.SRT,
                onClick = { onTestTypeSelected(TestType.SRT) },
                label = { Text("SRT") }
            )
        }
        
        // Status Filters
        Text(
            "Status",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusSelected(null) },
                label = { Text("All") }
            )
            FilterChip(
                selected = selectedStatus == SubmissionStatus.SUBMITTED_PENDING_REVIEW,
                onClick = { onStatusSelected(SubmissionStatus.SUBMITTED_PENDING_REVIEW) },
                label = { Text("Pending") }
            )
            FilterChip(
                selected = selectedStatus == SubmissionStatus.GRADED,
                onClick = { onStatusSelected(SubmissionStatus.GRADED) },
                label = { Text("Graded") }
            )
        }
    }
}

@Composable
private fun SubmissionsList(
    submissions: List<SubmissionItem>,
    onSubmissionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(submissions, key = { it.id }) { submission ->
            SubmissionCard(
                submission = submission,
                onClick = { onSubmissionClick(submission.id) }
            )
        }
    }
}

@Composable
private fun SubmissionCard(
    submission: SubmissionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = submission.testName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Submitted ${submission.timeAgo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(status = submission.status)
                    
                    // Score if available
                    submission.score?.let { score ->
                        Text(
                            text = "• Score: ${score.toInt()}/100",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBadge(
    status: SubmissionStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (status) {
        SubmissionStatus.DRAFT -> Color(0xFF9E9E9E) to Icons.Default.Edit
        SubmissionStatus.SUBMITTED_PENDING_REVIEW -> Color(0xFFFF9800) to Icons.Default.Schedule
        SubmissionStatus.UNDER_REVIEW -> Color(0xFF2196F3) to Icons.Default.Visibility
        SubmissionStatus.GRADED -> Color(0xFF4CAF50) to Icons.Default.CheckCircle
        SubmissionStatus.RETURNED_FOR_REVISION -> Color(0xFFF44336) to Icons.Default.Refresh
    }
    
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = status.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                "Loading submissions...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyState(
    onTakeTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No Submissions Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Complete tests to see your submissions here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onTakeTest,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Take a Test")
            }
        }
    }
}

