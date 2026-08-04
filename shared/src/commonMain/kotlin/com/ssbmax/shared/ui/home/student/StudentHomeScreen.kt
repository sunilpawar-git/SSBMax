package com.ssbmax.shared.ui.home.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.domain.model.TestPhase
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.usecase.dashboard.ProcessedDashboardData
import com.ssbmax.shared.presentation.home.student.StudentHomeViewModel
import com.ssbmax.shared.ui.home.student.components.OLQDashboardCard
import com.ssbmax.shared.ui.home.student.components.SectionDivider
import com.ssbmax.shared.ui.home.student.components.SectionHeader
import com.ssbmax.shared.ui.permissions.LocalNotificationPermissionController
import com.ssbmax.shared.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.cd_menu
import ssbmax.shared.generated.resources.cd_notifications
import ssbmax.shared.generated.resources.dashboard_error_load_failed
import ssbmax.shared.generated.resources.dashboard_olq_dashboard
import ssbmax.shared.generated.resources.home_journey_starts
import ssbmax.shared.generated.resources.home_welcome
import ssbmax.shared.generated.resources.section_quick_actions
import ssbmax.shared.generated.resources.section_your_progress

/**
 * Student Home Screen with Phase Progress Ribbon — Phase 5 KMP port of the
 * Android original (`app/.../ui/home/student/StudentHomeScreen.kt`).
 *
 * Shows progress for Phase 1 and Phase 2 with quick access to tests. Stats
 * cards and quick-action rows are extracted into `StudentHomeSections.kt`
 * (same package) purely to keep this file under this repo's 300-line
 * Quality Limit — no behavior change.
 *
 * Deviations from the Android original, all deliberate (see
 * [StudentHomeViewModel]'s class doc for the ViewModel-side ones):
 * - [com.ssbmax.shared.ui.permissions.LocalNotificationPermissionController],
 *   a `commonMain` twin of the `app`-side CompositionLocal of the same name
 *   (see its own class doc).
 * - `onNavigateToMarketplace`/`onNavigateToAnalytics`/`onNavigateToSubmissions`/
 *   `onNavigateToNotifications` default to no-ops, same as the Android
 *   original — the destinations they target aren't ported yet (Phase 5
 *   continuation); callers wire them to `NotYetPortedScreen` at the nav-graph
 *   level, not this screen's concern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onNavigateToTopic: (topicId: String, selectedTab: Int) -> Unit,
    onNavigateToPhaseDetail: (TestPhase) -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToSubmissions: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMarketplace: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToResult: (TestType, String) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<StudentHomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationPermissionController = LocalNotificationPermissionController.current

    // Request notification permission once on home screen load. Covers TAT,
    // WAT, SRT, SDT, PPDT -- all workers deliver results via local
    // notification. Best-effort: proceeds regardless of grant/deny.
    LaunchedEffect(Unit) {
        notificationPermissionController.request()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(Res.string.home_welcome, uiState.userName),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(Res.string.home_journey_starts),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, stringResource(Res.string.cd_menu))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                if (uiState.notificationCount > 0) {
                                    Badge {
                                        Text("${uiState.notificationCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource(Res.string.cd_notifications)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sectionSpacing)
        ) {
            item { StatsCardsRow(uiState.currentStreak, uiState.testsCompleted) }

            item { SectionDivider() }

            item {
                SectionHeader(
                    icon = "📊",
                    title = stringResource(Res.string.section_your_progress),
                    modifier = Modifier.padding(top = Spacing.small)
                )
            }

            item {
                PhaseProgressRibbon(
                    phase1Progress = uiState.phase1Progress,
                    phase2Progress = uiState.phase2Progress,
                    onPhaseClick = onNavigateToPhaseDetail,
                    onTopicClick = { topicId ->
                        // Tests tab selected (tab index 2) via TopicScreen's
                        // typed `selectedTab` constructor arg, not a
                        // hand-built "topicId?selectedTab=2" route string.
                        onNavigateToTopic(topicId, 2)
                    }
                )
            }

            item { SectionDivider() }

            item {
                SectionHeader(
                    icon = "🎯",
                    title = stringResource(Res.string.dashboard_olq_dashboard),
                    modifier = Modifier.padding(top = Spacing.small)
                )
            }

            // OLQDashboardCard is always rendered — same composable tree before and after data loads.
            // Before data arrives: empty() placeholder keeps card structure visible (no layout jump).
            // After data arrives: real data replaces placeholder values in-place.
            item {
                OLQDashboardCard(
                    processedData = uiState.dashboard ?: ProcessedDashboardData.empty(),
                    isLoading = uiState.isDashboardLoading,
                    isRefreshing = uiState.isRefreshingDashboard,
                    onRefresh = { viewModel.refreshDashboard() },
                    onNavigateToResult = onNavigateToResult,
                    modifier = Modifier.padding(horizontal = Spacing.cardPadding)
                )
            }

            // Supplemental error banner — shown below the card, does not replace it.
            if (uiState.dashboardError != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.cardPadding),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.dashboardError ?: stringResource(Res.string.dashboard_error_load_failed),
                            modifier = Modifier.padding(Spacing.cardPadding),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            item { SectionDivider() }

            item {
                SectionHeader(
                    icon = "⚡",
                    title = stringResource(Res.string.section_quick_actions),
                    modifier = Modifier.padding(top = Spacing.small)
                )
            }

            item { QuickActionsRowOne(onOpenDrawer, onNavigateToMarketplace) }

            item { QuickActionsRowTwo(onNavigateToAnalytics, onNavigateToStudy) }

            item {
                Spacer(modifier = Modifier.height(Spacing.large))
            }
        }
    }
}
