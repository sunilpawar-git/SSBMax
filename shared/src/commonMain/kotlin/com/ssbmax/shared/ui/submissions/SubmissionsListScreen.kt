package com.ssbmax.shared.ui.submissions

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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.presentation.submissions.SubmissionsListViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.submissions_list_action_refresh
import ssbmax.shared.generated.resources.submissions_list_cd_back
import ssbmax.shared.generated.resources.submissions_list_filter_all
import ssbmax.shared.generated.resources.submissions_list_filter_graded
import ssbmax.shared.generated.resources.submissions_list_filter_pending
import ssbmax.shared.generated.resources.submissions_list_filter_status
import ssbmax.shared.generated.resources.submissions_list_filter_test_type
import ssbmax.shared.generated.resources.submissions_list_subtitle
import ssbmax.shared.generated.resources.submissions_list_title

/**
 * KMP port of the Android `app/.../ui/submissions/SubmissionsListScreen.kt`.
 * Uses `koinInject()` + `collectAsState()` (no `androidx.lifecycle.compose.collectAsStateWithLifecycle()`
 * dependency, which isn't part of this migration's plain-class ViewModel contract),
 * matching every other Phase 5 screen. All string literals externalized to
 * `strings.xml` (unchanged Android original key names, reused verbatim from
 * `app/src/main/res/values/strings.xml`).
 *
 * List item card/status-badge/loading/error/empty-state composables live in
 * `SubmissionsListComponents.kt` -- a purely structural split to stay under
 * this repo's 300-line Quality Limit (see that file's own doc comment).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionsListScreen(
    onSubmissionClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToTests: () -> Unit = {},
    viewModel: SubmissionsListViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    val uiState by viewModel.uiState.collectAsState()

    var selectedFilter by remember { mutableStateOf<TestType?>(null) }
    var selectedStatus by remember { mutableStateOf<SubmissionStatus?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.submissions_list_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(Res.string.submissions_list_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.submissions_list_cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, stringResource(Res.string.submissions_list_action_refresh))
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
        Text(
            stringResource(Res.string.submissions_list_filter_test_type),
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
                label = { Text(stringResource(Res.string.submissions_list_filter_all)) }
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

        Text(
            stringResource(Res.string.submissions_list_filter_status),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusSelected(null) },
                label = { Text(stringResource(Res.string.submissions_list_filter_all)) }
            )
            FilterChip(
                selected = selectedStatus == SubmissionStatus.SUBMITTED_PENDING_REVIEW,
                onClick = { onStatusSelected(SubmissionStatus.SUBMITTED_PENDING_REVIEW) },
                label = { Text(stringResource(Res.string.submissions_list_filter_pending)) }
            )
            FilterChip(
                selected = selectedStatus == SubmissionStatus.GRADED,
                onClick = { onStatusSelected(SubmissionStatus.GRADED) },
                label = { Text(stringResource(Res.string.submissions_list_filter_graded)) }
            )
        }
    }
}
