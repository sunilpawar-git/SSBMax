package com.ssbmax.ui.home.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssbmax.R
import com.ssbmax.core.designsystem.theme.SSBColors
import com.ssbmax.core.designsystem.theme.Spacing
import com.ssbmax.core.domain.model.TestPhase
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.ui.home.student.components.StatsCard
import com.ssbmax.ui.home.student.components.QuickActionCard
import com.ssbmax.ui.home.student.components.SectionDivider
import com.ssbmax.ui.home.student.components.SectionHeader

/**
 * Student Home Screen with Phase Progress Ribbon
 * Shows progress for Phase 1 and Phase 2 with quick access to tests
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    viewModel: StudentHomeViewModel = hiltViewModel(),
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
            
            item {
                // Show dashboard immediately (no loading spinner)
                // Dashboard updates values in-place when data arrives
                when {
                    uiState.dashboard != null -> {
                        com.ssbmax.ui.home.student.components.OLQDashboardCard(
                            processedData = uiState.dashboard!!,
                            onNavigateToResult = onNavigateToResult,
                            isRefreshing = uiState.isRefreshingDashboard,
                            onRefresh = { viewModel.refreshDashboard() },
                            modifier = Modifier.padding(horizontal = Spacing.cardPadding)
                        )
                    }
                    
                    uiState.dashboardError != null -> {
                        // Show error with retry option
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                    
                    else -> {
                        // No data yet - show empty state with call-to-action
                        com.ssbmax.ui.home.student.components.EmptyDashboardState(
                            modifier = Modifier.fillMaxWidth(),
                            onStartTestClick = onNavigateToStudy
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
