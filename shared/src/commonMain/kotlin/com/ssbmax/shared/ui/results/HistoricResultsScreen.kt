package com.ssbmax.shared.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.presentation.results.HistoricResultsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.historic_results_cd_back
import ssbmax.shared.generated.resources.historic_results_cd_filter
import ssbmax.shared.generated.resources.historic_results_clear_filter
import ssbmax.shared.generated.resources.historic_results_title

/**
 * KMP port of the Android `app/.../ui/results/HistoricResultsScreen.kt`.
 *
 * Real fix applied while porting, not present in the Android original: every string
 * literal ("Historic Results", "Back", "Retry", etc.) is externalized to
 * `composeResources/values/strings.xml` -- the Android original hardcoded them,
 * a pre-existing violation of this repo's "no hardcoded strings" lint rule that
 * this port does not carry forward.
 *
 * Result-card/filter-dialog/empty/error/score-color composables live in
 * `HistoricResultsComponents.kt` -- a purely structural split to stay under
 * this repo's 300-line Quality Limit (see that file's own doc comment).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricResultsScreen(
    onNavigateBack: () -> Unit,
    onResultClick: (String, TestType) -> Unit,
    viewModel: HistoricResultsViewModel = koinViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.historic_results_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.historic_results_cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, stringResource(Res.string.historic_results_cd_filter))
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
                    if (uiState.selectedFilter != null) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.filterByTestType(null) },
                                label = {
                                    Text(
                                        stringResource(
                                            Res.string.historic_results_clear_filter,
                                            uiState.selectedFilter?.displayName ?: ""
                                        )
                                    )
                                }
                            )
                        }
                    }

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
