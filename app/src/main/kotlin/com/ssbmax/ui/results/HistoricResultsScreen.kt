package com.ssbmax.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.results.HistoricResult

/**
 * Historic Results Screen
 * Shows test submissions from last 6 months
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricResultsScreen(
    onNavigateBack: () -> Unit,
    onResultClick: (String, TestType) -> Unit,
    viewModel: HistoricResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historic Results") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, "Filter")
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

            uiState.error != null -> {
                ErrorState(
                    error = uiState.error ?: "",
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            uiState.results.isEmpty() -> {
                EmptyState(modifier = Modifier.padding(paddingValues))
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Filter chip if active
                    if (uiState.selectedFilter != null) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.filterByTestType(null) },
                                label = { Text("${uiState.selectedFilter?.displayName} - Clear filter") }
                            )
                        }
                    }

                    // Results list
                    items(uiState.results) { result ->
                        HistoricResultCard(
                            result = result,
                            onClick = { onResultClick(result.submissionId, result.testType) }
                        )
                    }
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            selectedFilter = uiState.selectedFilter,
            onDismiss = { showFilterDialog = false },
            onFilterSelected = {
                viewModel.filterByTestType(it)
                showFilterDialog = false
            }
        )
    }
}

@Composable
private fun HistoricResultCard(
    result: HistoricResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
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
                        text = result.testType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (result.isRecent) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "NEW",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = result.getFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Score badge
            result.overallScore?.let { score ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getScoreColor(score)
                ) {
                    Text(
                        text = "%.1f".format(score),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterDialog(
    selectedFilter: TestType?,
    onDismiss: () -> Unit,
    onFilterSelected: (TestType?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Test Type") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterOption(
                    label = "All Tests",
                    isSelected = selectedFilter == null,
                    onClick = { onFilterSelected(null) }
                )

                // Psychology Tests
                TestType.entries.filter { 
                    it in listOf(TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD) 
                }.forEach { testType ->
                    FilterOption(
                        label = testType.displayName,
                        isSelected = selectedFilter == testType,
                        onClick = { onFilterSelected(testType) }
                    )
                }

                // GTO Tests
                TestType.entries.filter {
                    it.name.startsWith("GTO_")
                }.forEach { testType ->
                    FilterOption(
                        label = testType.displayName,
                        isSelected = selectedFilter == testType,
                        onClick = { onFilterSelected(testType) }
                    )
                }

                // Interview
                FilterOption(
                    label = TestType.IO.displayName,
                    isSelected = selectedFilter == TestType.IO,
                    onClick = { onFilterSelected(TestType.IO) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun FilterOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No historic results",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Results from the last 6 months will appear here",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * Get color for score badge (SSB 1-10 scale, lower is better)
 */
private fun getScoreColor(score: Float): Color {
    return when {
        score <= 5f -> Color(0xFF4CAF50)  // Green
        score <= 7f -> Color(0xFFFFC107)  // Amber
        else -> Color(0xFFF44336)  // Red
    }
}
