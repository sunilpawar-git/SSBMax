package com.ssbmax.shared.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.PerformanceOverview
import com.ssbmax.shared.domain.model.TestTypeStats
import com.ssbmax.shared.presentation.analytics.AnalyticsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.analytics_cd_back
import ssbmax.shared.generated.resources.analytics_empty_description
import ssbmax.shared.generated.resources.analytics_empty_state
import ssbmax.shared.generated.resources.analytics_error_unknown
import ssbmax.shared.generated.resources.analytics_screen_title
import ssbmax.shared.generated.resources.analytics_test_performance

/**
 * KMP port of the Android `app/.../ui/analytics/AnalyticsScreen.kt`
 * (student performance dashboard).
 *
 * `"%.1f%%".format(...)`/`"%.0f%%".format(...)` (JVM-only `String.format`,
 * no Kotlin/Native `actual`) replaced with [com.ssbmax.shared.ui.util.formatOneDecimal]
 * (this migration's established seam) composed into the `%1$s%%` string-resource
 * templates -- see `AnalyticsComponents.kt`, where those calls actually live.
 *
 * Overview/stat-item/test-stats/difficulty/progression composables live in
 * `AnalyticsComponents.kt` -- a purely structural split to stay under this
 * repo's 300-line Quality Limit (see that file's own doc comment).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = koinInject()
) {
    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAllTestStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.analytics_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.analytics_cd_back))
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.error ?: stringResource(Res.string.analytics_error_unknown))
                }
            }
            uiState.overview == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(Res.string.analytics_empty_state),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(Res.string.analytics_empty_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                AnalyticsContent(
                    overview = uiState.overview!!,
                    testStats = uiState.allTestStats,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsContent(
    overview: PerformanceOverview,
    testStats: List<TestTypeStats>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { OverviewCard(overview) }

        item {
            Text(
                stringResource(Res.string.analytics_test_performance),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(testStats) { stats -> TestStatsCard(stats) }
    }
}
