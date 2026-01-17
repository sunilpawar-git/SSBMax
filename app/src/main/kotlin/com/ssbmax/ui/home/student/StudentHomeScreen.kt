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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatsCard(
                        title = stringResource(R.string.stats_study_streak),
                        value = "${uiState.currentStreak}",
                        subtitle = stringResource(R.string.stats_days),
                        icon = Icons.Default.LocalFireDepartment,
                        gradient = Brush.linearGradient(
                            colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    StatsCard(
                        title = stringResource(R.string.stats_tests_done),
                        value = "${uiState.testsCompleted}",
                        subtitle = stringResource(R.string.stats_tests),
                        icon = Icons.Default.CheckCircle,
                        gradient = Brush.linearGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                        ),
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
                    modifier = Modifier.padding(top = 8.dp)
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
                        onNavigateToTopic("$topicId?selectedTab=2")
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
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                when {
                    uiState.isLoadingDashboard -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    uiState.dashboard != null -> {
                        com.ssbmax.ui.home.student.components.OLQDashboardCard(
                            processedData = uiState.dashboard!!, // Now using ProcessedDashboardData
                            onNavigateToResult = onNavigateToResult,
                            isRefreshing = uiState.isRefreshingDashboard,
                            onRefresh = { viewModel.refreshDashboard() },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    
                    uiState.dashboard == null && !uiState.isLoadingDashboard -> {
                        com.ssbmax.ui.home.student.components.EmptyDashboardState(
                            modifier = Modifier.fillMaxWidth(),
                            onStartTestClick = onNavigateToStudy
                        )
                    }
                    
                    uiState.dashboardError != null -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = uiState.dashboardError ?: stringResource(R.string.dashboard_error_load_failed),
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
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
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = stringResource(R.string.action_self_preparation),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        color = Color(0xFF7B1FA2),
                        onClick = onOpenDrawer,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionCard(
                        title = stringResource(R.string.action_join_batch),
                        icon = Icons.Default.GroupAdd,
                        color = Color(0xFF1976D2),
                        onClick = onNavigateToMarketplace,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = stringResource(R.string.action_view_analytics),
                        icon = Icons.Default.Analytics,
                        color = Color(0xFF00897B),
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionCard(
                        title = stringResource(R.string.action_study_materials),
                        icon = Icons.Default.Book,
                        color = Color(0xFFD84315),
                        onClick = onNavigateToStudy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
