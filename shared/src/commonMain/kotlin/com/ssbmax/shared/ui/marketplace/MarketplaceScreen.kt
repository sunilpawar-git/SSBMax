package com.ssbmax.shared.ui.marketplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.ssbmax.shared.presentation.marketplace.MarketplaceViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.marketplace_adjust_filters
import ssbmax.shared.generated.resources.marketplace_cd_back
import ssbmax.shared.generated.resources.marketplace_clear
import ssbmax.shared.generated.resources.marketplace_institutes_found
import ssbmax.shared.generated.resources.marketplace_no_institutes
import ssbmax.shared.generated.resources.marketplace_search_hint
import ssbmax.shared.generated.resources.marketplace_title
import ssbmax.shared.generated.resources.marketplace_toggle_filters

/**
 * KMP port of the Android `app/.../ui/marketplace/MarketplaceScreen.kt` --
 * coaching-institute directory with search + filters, backed by
 * [MarketplaceViewModel]'s mock data (no backend yet, on either platform).
 *
 * Split across this file (screen shell + search bar), [FiltersSection], and
 * [InstituteCard] to stay under this repo's 300-line-per-file Quality Limit
 * -- the Android original was a single 416-line file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    onNavigateBack: () -> Unit,
    onInstituteClick: (String) -> Unit = {},
    viewModel: MarketplaceViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.marketplace_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.marketplace_cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = stringResource(Res.string.marketplace_toggle_filters)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MarketplaceSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (showFilters) {
                FiltersSection(
                    filterType = uiState.filterType,
                    filterPriceRange = uiState.filterPriceRange,
                    filterCity = uiState.filterCity,
                    onTypeChange = viewModel::onFilterTypeChange,
                    onPriceRangeChange = viewModel::onFilterPriceRangeChange,
                    onCityChange = viewModel::onFilterCityChange,
                    onClearFilters = viewModel::clearFilters,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.error ?: "An error occurred",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                uiState.institutes.isEmpty() -> {
                    EmptyInstitutesState(modifier = Modifier.fillMaxSize())
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                stringResource(Res.string.marketplace_institutes_found, uiState.institutes.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(uiState.institutes) { institute ->
                            InstituteCard(
                                institute = institute,
                                onClick = { onInstituteClick(institute.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInstitutesState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(Res.string.marketplace_no_institutes),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(Res.string.marketplace_adjust_filters),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MarketplaceSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(stringResource(Res.string.marketplace_search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, stringResource(Res.string.marketplace_clear))
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}
