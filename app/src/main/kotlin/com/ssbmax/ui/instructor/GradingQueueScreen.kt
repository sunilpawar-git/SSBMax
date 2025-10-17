package com.ssbmax.ui.instructor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.TestType

/**
 * Grading Queue Screen for Instructors
 * Shows pending submissions awaiting review
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradingQueueScreen(
    onSubmissionClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: InstructorGradingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf<TestType?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Grading Queue")
                        Text(
                            "${uiState.pendingCount} pending",
                            style = MaterialTheme.typography.bodySmall
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Row
            FilterRow(
                selectedType = selectedFilter,
                onTypeSelected = {
                    selectedFilter = it
                    viewModel.filterByType(it)
                },
                modifier = Modifier.padding(16.dp)
            )
            
            // Content
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                }
                uiState.submissions.isEmpty() -> {
                    EmptyContent()
                }
                else -> {
                    GradingList(
                        submissions = uiState.submissions,
                        onSubmissionClick = onSubmissionClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selectedType: TestType?,
    onTypeSelected: (TestType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = { Text("All") }
        )
        FilterChip(
            selected = selectedType == TestType.TAT,
            onClick = { onTypeSelected(TestType.TAT) },
            label = { Text("TAT") }
        )
        FilterChip(
            selected = selectedType == TestType.WAT,
            onClick = { onTypeSelected(TestType.WAT) },
            label = { Text("WAT") }
        )
        FilterChip(
            selected = selectedType == TestType.SRT,
            onClick = { onTypeSelected(TestType.SRT) },
            label = { Text("SRT") }
        )
    }
}

@Composable
private fun GradingList(
    submissions: List<GradingQueueItem>,
    onSubmissionClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(submissions, key = { it.id }) { item ->
            GradingCard(
                item = item,
                onClick = { onSubmissionClick(item.id) }
            )
        }
    }
}

@Composable
private fun GradingCard(
    item: GradingQueueItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.testName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    PriorityBadge(priority = item.priority)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = item.timeWaiting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                item.aiScore?.let { score ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AI Score: ${score.toInt()}/100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Grade",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PriorityBadge(priority: GradingPriority) {
    val color = when (priority) {
        GradingPriority.URGENT -> Color.Red
        GradingPriority.HIGH -> Color(0xFFFF9800)
        GradingPriority.NORMAL -> Color(0xFF2196F3)
        GradingPriority.LOW -> Color.Gray
    }
    
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = priority.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(text = error)
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "No Pending Submissions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Great job! All submissions are graded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

