package com.ssbmax.shared.ui.instructorgrading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.presentation.instructorgrading.InstructorGradingViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.grading_queue_back
import ssbmax.shared.generated.resources.grading_queue_filter_all
import ssbmax.shared.generated.resources.grading_queue_pending_count
import ssbmax.shared.generated.resources.grading_queue_refresh
import ssbmax.shared.generated.resources.grading_queue_title

/**
 * KMP port of the Android `app/.../ui/instructor/GradingQueueScreen.kt` --
 * lists pending submissions awaiting review, filterable by [TestType]. Private
 * composables (list/card/loading/error/empty states) extracted into
 * [GradingQueueComponents] to stay under the 300-line Quality Limit. See
 * [InstructorGradingViewModel] for the ViewModel-level porting notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradingQueueScreen(
    onSubmissionClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: InstructorGradingViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf<TestType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(Res.string.grading_queue_title))
                        Text(
                            stringResource(Res.string.grading_queue_pending_count, uiState.pendingCount),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.grading_queue_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, stringResource(Res.string.grading_queue_refresh))
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
            FilterRow(
                selectedType = selectedFilter,
                onTypeSelected = {
                    selectedFilter = it
                    viewModel.filterByType(it)
                },
                modifier = Modifier.padding(16.dp)
            )

            when {
                uiState.isLoading -> LoadingContent()
                uiState.error != null -> ErrorContent(
                    error = uiState.error!!,
                    onRetry = { viewModel.refresh() }
                )
                uiState.submissions.isEmpty() -> EmptyContent()
                else -> GradingList(
                    submissions = uiState.submissions,
                    onSubmissionClick = onSubmissionClick
                )
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
            label = { Text(stringResource(Res.string.grading_queue_filter_all)) }
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
