package com.ssbmax.ui.home.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssbmax.core.domain.model.TestPhase
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.ui.components.SubscriptionRibbon

/**
 * Student Home Screen with Phase Progress Ribbon
 * Shows progress for Phase 1 and Phase 2 with quick access to tests
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    viewModel: StudentHomeViewModel = hiltViewModel(),
    onNavigateToTest: (TestType) -> Unit,
    onNavigateToPhaseDetail: (TestPhase) -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToSubmissions: () -> Unit = {},
    onNavigateToUpgrade: () -> Unit = {},
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
                            "Welcome, ${uiState.userName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Keep up the great work!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Notifications */ }) {
                        Badge {
                            Text("${uiState.notificationCount}")
                        }
                        Icon(Icons.Default.Notifications, "Notifications")
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
                        title = "Study Streak",
                        value = "${uiState.currentStreak}",
                        subtitle = "days",
                        icon = Icons.Default.LocalFireDepartment,
                        gradient = Brush.linearGradient(
                            colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatsCard(
                        title = "Tests Done",
                        value = "${uiState.testsCompleted}",
                        subtitle = "completed",
                        icon = Icons.Default.CheckCircle,
                        gradient = Brush.linearGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Subscription Ribbon
            item {
                SubscriptionRibbon(
                    currentTier = uiState.subscriptionTier,
                    onUpgradeClick = onNavigateToUpgrade
                )
            }
            
            // Progress Ribbon Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your Progress",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TextButton(onClick = { /* View all */ }) {
                        Text("View All")
                        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            // Phase Progress Ribbon
            item {
                PhaseProgressRibbon(
                    phase1Progress = uiState.phase1Progress,
                    phase2Progress = uiState.phase2Progress,
                    onPhaseClick = onNavigateToPhaseDetail,
                    onTestClick = onNavigateToTest
                )
            }
            
            // Recent Submissions Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Test Results",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TextButton(onClick = onNavigateToSubmissions) {
                        Text("View All")
                        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            // TODO: Add recent submissions cards here when ViewModel is updated
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToSubmissions
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "View Your Test Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Check scores, feedback, and progress",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    }
                }
            }
            
            // Quick Actions
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Study Materials",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        color = Color(0xFF7B1FA2),
                        onClick = onNavigateToStudy,
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionCard(
                        title = "Join Batch",
                        icon = Icons.Default.GroupAdd,
                        color = Color(0xFF1976D2),
                        onClick = { /* Join batch */ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Recommended Tests
            if (uiState.recommendedTests.isNotEmpty()) {
                item {
                    Text(
                        "Recommended for You",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(uiState.recommendedTests) { test ->
                    RecommendedTestCard(
                        testType = test,
                        onClick = { onNavigateToTest(test) }
                    )
                }
            }
            
            // Daily Tip
            item {
                DailyTipCard(tip = uiState.dailyTip)
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(16.dp)
        ) {
            Column {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    "$title • $subtitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RecommendedTestCard(
    testType: TestType,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(getTestColor(testType).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getTestIcon(testType),
                    contentDescription = null,
                    tint = getTestColor(testType),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    testType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    testType.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyTipCard(tip: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    "Daily Tip",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    tip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

// Helper functions
private fun getTestColor(testType: TestType): Color {
    return when (testType.phase) {
        TestPhase.PHASE_1 -> Color(0xFF1976D2)
        TestPhase.PHASE_2 -> Color(0xFF388E3C)
    }
}

private fun getTestIcon(testType: TestType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (testType) {
        TestType.OIR -> Icons.Default.Psychology
        TestType.PPDT -> Icons.Default.Image
        TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD -> Icons.Default.EditNote
        TestType.GTO -> Icons.Default.Groups
        TestType.IO -> Icons.Default.RecordVoiceOver
    }
}

