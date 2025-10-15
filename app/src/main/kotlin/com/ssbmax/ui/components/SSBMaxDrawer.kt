package com.ssbmax.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.TestPhase
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.UserRole

/**
 * Navigation Drawer for SSBMax app
 * Shows different menu items based on user role
 */
@Composable
fun SSBMaxDrawer(
    user: SSBMaxUser,
    onNavigateToPhase: (TestPhase) -> Unit,
    onNavigateToTest: (TestType) -> Unit,
    onNavigateToBatches: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPendingGrading: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onSwitchRole: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // User Profile Header
            item {
                DrawerHeader(user = user)
                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }
            
            // Role-specific menu items
            if (user.role.isStudent) {
                // Student Menu
                item {
                    DrawerSectionTitle(title = "SSB Tests")
                }
                
                item {
                    DrawerMenuItem(
                        icon = Icons.Default.Checklist,
                        title = "Phase 1 - Screening",
                        subtitle = "OIR & PPDT",
                        onClick = { onNavigateToPhase(TestPhase.PHASE_1) }
                    )
                }
                
                item {
                    DrawerMenuItem(
                        icon = Icons.Default.Psychology,
                        title = "Phase 2 - Assessment",
                        subtitle = "Psychology, GTO, IO",
                        onClick = { onNavigateToPhase(TestPhase.PHASE_2) }
                    )
                }
                
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                // Quick access to specific tests
                item {
                    DrawerSectionTitle(title = "Quick Access")
                }
                
                items(quickAccessTests) { test ->
                    DrawerMenuItem(
                        icon = getTestIcon(test),
                        title = test.displayName,
                        onClick = { onNavigateToTest(test) }
                    )
                }
                
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                item {
                    DrawerMenuItem(
                        icon = Icons.Default.Groups,
                        title = "My Batches",
                        onClick = onNavigateToBatches
                    )
                }
            }
            
            if (user.role.isInstructor) {
                // Instructor Menu
                item {
                    DrawerSectionTitle(title = "Instructor Tools")
                }
                
                item {
                    DrawerMenuItem(
                        icon = Icons.Default.AssignmentLate,
                        title = "Pending Grading",
                        badge = "12", // TODO: Get from state
                        onClick = onNavigateToPendingGrading
                    )
                }
                
                item {
                    DrawerMenuItem(
                        icon = Icons.Default.People,
                        title = "All Students",
                        onClick = { /* Navigate to students list */ }
                    )
                }
                
                item {
                    DrawerMenuItem(
                        icon = Icons.Default.Groups,
                        title = "Batch Management",
                        onClick = onNavigateToBatches
                    )
                }
                
                item {
                    DrawerMenuItem(
                        icon = Icons.Default.Analytics,
                        title = "Analytics Dashboard",
                        onClick = onNavigateToAnalytics
                    )
                }
                
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            
            // Common items
            item {
                DrawerSectionTitle(title = "Account")
            }
            
            item {
                DrawerMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    onClick = onNavigateToSettings
                )
            }
            
            if (user.role == UserRole.BOTH) {
                item {
                    DrawerMenuItem(
                        icon = Icons.Default.SwapHoriz,
                        title = "Switch Role",
                        subtitle = if (user.role.isStudent) "Switch to Instructor" else "Switch to Student",
                        onClick = onSwitchRole
                    )
                }
            }
            
            item {
                DrawerMenuItem(
                    icon = Icons.Default.Logout,
                    title = "Sign Out",
                    onClick = onSignOut,
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            // App Version
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(user: SSBMaxUser) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Avatar
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (user.photoUrl != null) {
                // TODO: Load image from URL
                Text(
                    user.displayName.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    user.displayName.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                user.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Role badge
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        when (user.role) {
                            UserRole.STUDENT -> "Student"
                            UserRole.INSTRUCTOR -> "Instructor"
                            UserRole.BOTH -> "Student & Instructor"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tint
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (badge != null) {
            Badge(
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Text(badge)
            }
        }
    }
}

// Quick access tests for students
private val quickAccessTests = listOf(
    TestType.OIR,
    TestType.PPDT,
    TestType.TAT,
    TestType.WAT,
    TestType.SRT
)

private fun getTestIcon(testType: TestType): ImageVector {
    return when (testType) {
        TestType.OIR -> Icons.Default.Quiz
        TestType.PPDT -> Icons.Default.Image
        TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD -> Icons.Default.EditNote
        TestType.GTO -> Icons.Default.Groups
        TestType.IO -> Icons.Default.RecordVoiceOver
    }
}

