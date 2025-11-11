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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssbmax.core.domain.model.TestPhase
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType

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
                            "Welcome, ${uiState.userName} 🎖️",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Your SSB journey starts here",
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
                    IconButton(onClick = onNavigateToNotifications) {
                        if (uiState.notificationCount > 0) {
                            Badge {
                                Text("${uiState.notificationCount}")
                            }
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
                        subtitle = "tests",
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
                    title = "Your Progress",
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
            
            // Quick Actions
            item {
                SectionHeader(
                    icon = "⚡",
                    title = "Quick Actions",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Self Preparation",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        color = Color(0xFF7B1FA2),
                        onClick = onOpenDrawer,
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionCard(
                        title = "Join Batch",
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
                        title = "View Analytics",
                        icon = Icons.Default.Analytics,
                        color = Color(0xFF00897B),
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionCard(
                        title = "Study Materials",
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

/**
 * Stats Card - Original Design
 * Gradient background with icon and horizontal layout
 */
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
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                // Top row: Icon + Value + Unit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Text(
                        value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Bottom: Title
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
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
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}


/**
 * Section Divider - Subtle horizontal line for visual separation
 */
@Composable
private fun SectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 8.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * Section Header Component with Icon and Title
 * Creates visual separation between content sections
 */
@Composable
private fun SectionHeader(
    icon: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
        TestType.PIQ -> Icons.Default.Assignment
        TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD -> Icons.Default.EditNote
        // GTO Tasks
        TestType.GTO_GD -> Icons.Default.Forum
        TestType.GTO_GPE -> Icons.Default.Map
        TestType.GTO_PGT -> Icons.Default.TrendingUp
        TestType.GTO_GOR -> Icons.Default.DirectionsRun
        TestType.GTO_HGT -> Icons.Default.People
        TestType.GTO_LECTURETTE -> Icons.Default.Mic
        TestType.GTO_IO -> Icons.Default.Person
        TestType.GTO_CT -> Icons.Default.MilitaryTech
        TestType.IO -> Icons.Default.RecordVoiceOver
    }
}

