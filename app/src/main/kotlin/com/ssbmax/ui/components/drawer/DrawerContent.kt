package com.ssbmax.ui.components.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Main content section of the navigation drawer.
 */
@Composable
fun DrawerContent(
    currentRoute: String,
    phase1Expanded: Boolean,
    phase2Expanded: Boolean,
    onNavigateToHome: () -> Unit,
    onNavigateToTopic: (topicId: String) -> Unit,
    onNavigateToSSBOverview: () -> Unit,
    onNavigateToMyBatches: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onTogglePhase1: () -> Unit,
    onTogglePhase2: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Home Link - Always brightly colored for easy navigation
        DrawerMenuItem(
            icon = Icons.Default.Home,
            title = "Home",
            onClick = onNavigateToHome,
            isSelected = currentRoute.contains("home"),
            isHomeButton = true
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // SSB Tests Section
        DrawerSectionHeader("SSB Tests")

        // Phase 1 - Screening Tests
        DrawerExpandableSection(
            icon = Icons.Default.CheckCircle,
            title = "Phase 1 - Screening Tests (Day 1)",
            expanded = phase1Expanded,
            onToggle = onTogglePhase1
        ) {
            DrawerSubMenuItem(
                title = "OIR Test",
                icon = "🎯",
                onClick = { onNavigateToTopic("oir") }
            )
            DrawerSubMenuItem(
                title = "PPDT",
                icon = "🖼️",
                onClick = { onNavigateToTopic("ppdt") }
            )
        }

        // Phase 2 - Assessments
        DrawerExpandableSection(
            icon = Icons.Default.Psychology,
            title = "Phase 2 - Assessments (Day 2-5)",
            expanded = phase2Expanded,
            onToggle = onTogglePhase2
        ) {
            DrawerSubMenuItem(
                title = "Filling PIQ Form",
                icon = "📝",
                onClick = { onNavigateToTopic("piq_form") }
            )
            DrawerSubMenuItem(
                title = "Psychology Tests",
                icon = "🧪",
                onClick = { onNavigateToTopic("psychology") }
            )
            DrawerSubMenuItem(
                title = "GTO Tests",
                icon = "👥",
                onClick = { onNavigateToTopic("gto") }
            )
            DrawerSubMenuItem(
                title = "Interview",
                icon = "🎤",
                onClick = { onNavigateToTopic("interview") }
            )
            DrawerSubMenuItem(
                title = "Conference",
                icon = "🤝",
                onClick = { onNavigateToTopic("conference") }
            )
        }

        // Medicals
        DrawerMenuItem(
            icon = Icons.Default.MedicalServices,
            title = "Medicals",
            onClick = { onNavigateToTopic("medicals") }
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Quick Access Section
        DrawerSectionHeader("Quick Access")

        DrawerMenuItem(
            icon = Icons.Default.Info,
            title = "Overview of SSB",
            onClick = onNavigateToSSBOverview
        )

        DrawerMenuItem(
            icon = Icons.Default.Group,
            title = "My Batches",
            onClick = onNavigateToMyBatches
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Account Section
        DrawerSectionHeader("Account")

        DrawerMenuItem(
            icon = Icons.Default.Settings,
            title = "Settings",
            onClick = onNavigateToSettings,
            isSelected = currentRoute.contains("settings")
        )

        DrawerMenuItem(
            icon = Icons.Default.Logout,
            title = "Sign Out",
            onClick = onSignOut
        )

        // Version Footer
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    isHomeButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isHomeButton -> MaterialTheme.colorScheme.primaryContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isHomeButton -> MaterialTheme.colorScheme.onPrimaryContainer
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
        }
    }
}

@Composable
private fun DrawerExpandableSection(
    icon: ImageVector,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 32.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DrawerSubMenuItem(
    title: String,
    icon: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

