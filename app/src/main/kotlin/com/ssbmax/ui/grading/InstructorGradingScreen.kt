package com.ssbmax.ui.grading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.GradingPriority
import com.ssbmax.core.domain.model.GradingQueueItem
import com.ssbmax.core.domain.model.TestType

/**
 * Instructor Grading Screen - Shows pending submissions queue
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorGradingScreen(
    onNavigateToGrading: (String) -> Unit = {}, // submissionId
    onNavigateBack: () -> Unit = {},
    viewModel: InstructorGradingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf<TestType?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grading Queue") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Filter button
                    IconButton(onClick = { showFilterMenu = true }) {
                        Badge(
                            containerColor = if (selectedFilter != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter"
                            )
                        }
                    }
                    
                    // Refresh button
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                val filteredQueue = if (selectedFilter != null) {
                    uiState.queueItems.filter { it.testType == selectedFilter }
                } else {
                    uiState.queueItems
                }
                
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Stats Header
                    GradingStatsCard(
                        totalPending = uiState.stats.totalPending,
                        todayGraded = uiState.stats.todayGraded,
                        averageTime = uiState.stats.averageGradingTimeMinutes,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    // Queue List
                    if (filteredQueue.isEmpty()) {
                        EmptyQueueView(
                            hasFilter = selectedFilter != null,
                            onClearFilter = { selectedFilter = null },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredQueue) { item ->
                                GradingQueueItemCard(
                                    item = item,
                                    onClick = { onNavigateToGrading(item.submissionId) }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Filter menu
        if (showFilterMenu) {
            DropdownMenu(
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All Tests") },
                    onClick = {
                        selectedFilter = null
                        showFilterMenu = false
                    },
                    leadingIcon = {
                        if (selectedFilter == null) {
                            Icon(Icons.Default.Check, null)
                        }
                    }
                )
                
                HorizontalDivider()
                
                TestType.values().forEach { testType ->
                    DropdownMenuItem(
                        text = { Text(testType.displayName) },
                        onClick = {
                            selectedFilter = testType
                            showFilterMenu = false
                        },
                        leadingIcon = {
                            if (selectedFilter == testType) {
                                Icon(Icons.Default.Check, null)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GradingStatsCard(
    totalPending: Int,
    todayGraded: Int,
    averageTime: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.PendingActions,
                value = totalPending.toString(),
                label = "Pending"
            )
            
            VerticalDivider(modifier = Modifier.height(40.dp))
            
            StatItem(
                icon = Icons.Default.CheckCircle,
                value = todayGraded.toString(),
                label = "Today"
            )
            
            VerticalDivider(modifier = Modifier.height(40.dp))
            
            StatItem(
                icon = Icons.Default.Timer,
                value = "${averageTime}m",
                label = "Avg Time"
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
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
private fun GradingQueueItemCard(
    item: GradingQueueItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                PriorityBadge(priority = item.priority)
            }
            
            // Test Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(item.testName) },
                    leadingIcon = {
                        Icon(
                            imageVector = getTestIcon(item.testType),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                
                item.batchName?.let { batchName ->
                    AssistChip(
                        onClick = { },
                        label = { Text(batchName) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
            
            // AI Score & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.aiScore?.let { aiScore ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "AI: ${aiScore.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.timeAgo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: GradingPriority) {
    val (color, icon) = when (priority) {
        GradingPriority.URGENT -> MaterialTheme.colorScheme.error to Icons.Default.PriorityHigh
        GradingPriority.HIGH -> MaterialTheme.colorScheme.errorContainer to Icons.Default.ArrowUpward
        GradingPriority.NORMAL -> MaterialTheme.colorScheme.surfaceVariant to Icons.Default.Remove
        GradingPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant to Icons.Default.ArrowDownward
    }
    
    AssistChip(
        onClick = { },
        label = {
            Text(
                priority.displayName,
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color
        )
    )
}

@Composable
private fun EmptyQueueView(
    hasFilter: Boolean,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        
        Text(
            text = if (hasFilter) "No submissions match filter" else "All caught up!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = if (hasFilter) {
                "Try changing your filter to see more submissions"
            } else {
                "No pending submissions to grade"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (hasFilter) {
            Button(onClick = onClearFilter) {
                Text("Clear Filter")
            }
        }
    }
}

private fun getTestIcon(testType: TestType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (testType) {
        TestType.OIR -> Icons.Default.Quiz
        TestType.PPDT -> Icons.Default.Image
        TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD -> Icons.Default.EditNote
        TestType.GTO -> Icons.Default.Groups
        TestType.IO -> Icons.Default.RecordVoiceOver
    }
}

