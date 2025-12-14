package com.ssbmax.ui.components.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R

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
            title = stringResource(R.string.nav_home),
            onClick = onNavigateToHome,
            isSelected = currentRoute.contains("home"),
            isHomeButton = true
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // SSB Tests Section
        DrawerSectionHeader(stringResource(R.string.drawer_ssb_tests))

        // Phase 1 - Screening Tests
        DrawerExpandableSection(
            icon = Icons.Default.CheckCircle,
            title = stringResource(R.string.drawer_phase1_title),
            expanded = phase1Expanded,
            onToggle = onTogglePhase1
        ) {
            DrawerSubMenuItem(
                title = stringResource(R.string.drawer_oir_test),
                icon = "🎯",
                onClick = { onNavigateToTopic("oir") }
            )
            DrawerSubMenuItem(
                title = stringResource(R.string.drawer_ppdt),
                icon = "🖼️",
                onClick = { onNavigateToTopic("ppdt") }
            )
        }

        // Phase 2 - Assessments
        DrawerExpandableSection(
            icon = Icons.Default.Psychology,
            title = stringResource(R.string.drawer_phase2_title),
            expanded = phase2Expanded,
            onToggle = onTogglePhase2
        ) {
            DrawerSubMenuItem(
                title = stringResource(R.string.drawer_piq_form),
                icon = "📝",
                onClick = { onNavigateToTopic("piq_form") }
            )
            DrawerSubMenuItem(
                title = stringResource(R.string.drawer_psychology_tests),
                icon = "🧪",
                onClick = { onNavigateToTopic("psychology") }
            )
            DrawerSubMenuItem(
                title = stringResource(R.string.drawer_gto_tests),
                icon = "👥",
                onClick = { onNavigateToTopic("gto") }
            )
            DrawerSubMenuItem(
                title = stringResource(R.string.drawer_interview),
                icon = "🎤",
                onClick = { onNavigateToTopic("interview") }
            )
            DrawerSubMenuItem(
                title = stringResource(R.string.drawer_conference),
                icon = "🤝",
                onClick = { onNavigateToTopic("conference") }
            )
        }

        // Medicals
        DrawerMenuItem(
            icon = Icons.Default.MedicalServices,
            title = stringResource(R.string.drawer_medicals),
            onClick = { onNavigateToTopic("medicals") }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Quick Access Section
        DrawerSectionHeader(stringResource(R.string.drawer_quick_access))

        DrawerMenuItem(
            icon = Icons.Default.Info,
            title = stringResource(R.string.drawer_ssb_overview),
            onClick = onNavigateToSSBOverview
        )

        DrawerMenuItem(
            icon = Icons.Default.Group,
            title = stringResource(R.string.drawer_my_batches),
            onClick = onNavigateToMyBatches
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Account Section
        DrawerSectionHeader(stringResource(R.string.drawer_account))

        DrawerMenuItem(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.drawer_settings),
            onClick = onNavigateToSettings,
            isSelected = currentRoute.contains("settings")
        )

        DrawerMenuItem(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = stringResource(R.string.drawer_sign_out),
            onClick = onSignOut
        )

        // Version Footer
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.version_footer),
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
        isHomeButton -> MaterialTheme.colorScheme.primary // Bright primary color for home button
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isHomeButton -> MaterialTheme.colorScheme.onPrimary // High contrast for home button
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val iconSize = if (isHomeButton) 28.dp else 24.dp // Bigger icon for home button
    val textStyle = if (isHomeButton) {
        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.bodyLarge
    }
    val verticalPadding = if (isHomeButton) 16.dp else 12.dp // More padding for home button

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = textStyle,
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
                val context = androidx.compose.ui.platform.LocalContext.current
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = if (expanded) {
                        context.getString(R.string.cd_collapse)
                    } else {
                        context.getString(R.string.cd_expand)
                    }
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

