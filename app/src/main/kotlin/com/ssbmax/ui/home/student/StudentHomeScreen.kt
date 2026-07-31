package com.ssbmax.ui.home.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.R
import com.ssbmax.shared.ui.theme.SSBColors
import com.ssbmax.shared.ui.theme.Spacing
import com.ssbmax.shared.domain.model.TestPhase
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.usecase.dashboard.ProcessedDashboardData
import com.ssbmax.ui.home.student.components.OLQDashboardCard
import com.ssbmax.ui.home.student.components.QuickActionCard
import com.ssbmax.ui.home.student.components.SectionDivider
import com.ssbmax.ui.home.student.components.SectionHeader
import com.ssbmax.ui.home.student.components.StatsCard
import com.ssbmax.ui.permissions.LocalNotificationPermissionController
import org.koin.compose.viewmodel.koinViewModel

/**
 * Student Home Screen with Phase Progress Ribbon
 * Shows progress for Phase 1 and Phase 2 with quick access to tests
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    viewModel: StudentHomeViewModel = koinViewModel(),
    onNavigateToTopic: (String) -> Unit,
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
    val uiState by viewModel.uiState.collectAsState()
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
                            stringResource(R.string.home_welcome, uiState.userName),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.home_journey_starts),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, stringResource(R.string.cd_menu))
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
                                contentDescription = stringResource(R.string.cd_notifications)
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
            // Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    StatsCard(
                        title = stringResource(R.string.stats_study_streak),
                        value = "${uiState.currentStreak}",
                        subtitle = stringResource(R.string.stats_days),
                        icon = Icons.Default.LocalFireDepartment,
                        gradient = Brush.linearGradient(
                            colors = listOf(SSBColors.Warning, SSBColors.Warning.copy(alpha = 0.8f))
                        ),
                        iconContentDescription = stringResource(R.string.cd_stats_streak_icon),
                        modifier = Modifier.weight(1f)
                    )

                    StatsCard(
                        title = stringResource(R.string.stats_tests_done),
                        value = "${uiState.testsCompleted}",
                        subtitle = stringResource(R.string.stats_tests),
                        icon = Icons.Default.CheckCircle,
                        gradient = Brush.linearGradient(
                            colors = listOf(SSBColors.Success, SSBColors.Success.copy(alpha = 0.8f))
                        ),
                        iconContentDescription = stringResource(R.string.cd_stats_tests_icon),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Section Divider
            item {
                SectionDivider()
            }

            // Progress Ribbon Header
            item {
                SectionHeader(
                    icon = "📊",
                    title = stringResource(R.string.section_your_progress),
                    modifier = Modifier.padding(top = Spacing.small)
                )
            }
            
            // Phase Progress Ribbon
            item {
                PhaseProgressRibbon(
                    phase1Progress = uiState.phase1Progress,
                    phase2Progress = uiState.phase2Progress,
                    onPhaseClick = onNavigateToPhaseDetail,
                    onTopicClick = { topicId ->
                        // Navigate to topic with Tests tab selected (tab index 2)
                        val route = buildTopicRoute(topicId, selectedTab = 2)
                        onNavigateToTopic(route)
                    }
                )
            }
            
            // Section Divider
            item {
                SectionDivider()
            }
            
            // OLQ Dashboard
            item {
                SectionHeader(
                    icon = "🎯",
                    title = stringResource(R.string.dashboard_olq_dashboard),
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
                            text = uiState.dashboardError ?: stringResource(R.string.dashboard_error_load_failed),
                            modifier = Modifier.padding(Spacing.cardPadding),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Section Divider
            item {
                SectionDivider()
            }
            
            // Quick Actions
            item {
                SectionHeader(
                    icon = "⚡",
                    title = stringResource(R.string.section_quick_actions),
                    modifier = Modifier.padding(top = Spacing.small)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    QuickActionCard(
                        title = stringResource(R.string.action_self_preparation),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        color = SSBColors.NavyBlue,
                        onClick = onOpenDrawer,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionCard(
                        title = stringResource(R.string.action_join_batch),
                        icon = Icons.Default.GroupAdd,
                        color = SSBColors.Info,
                        onClick = onNavigateToMarketplace,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    QuickActionCard(
                        title = stringResource(R.string.action_view_analytics),
                        icon = Icons.Default.Analytics,
                        color = SSBColors.OliveGreen,
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionCard(
                        title = stringResource(R.string.action_study_materials),
                        icon = Icons.Default.Book,
                        color = SSBColors.Error,
                        onClick = onNavigateToStudy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(Spacing.large))
            }
        }
    }
}

/**
 * Builds topic route with optional query parameters
 * @param topicId The topic ID
 * @param selectedTab Optional tab index (defaults to Overview tab)
 * @return Route string for navigation
 */
private fun buildTopicRoute(topicId: String, selectedTab: Int? = null): String {
    return if (selectedTab != null) {
        "$topicId?selectedTab=$selectedTab"
    } else {
        topicId
    }
}
