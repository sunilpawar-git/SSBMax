package com.ssbmax.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Settings Screen with notification preferences and app settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFAQ: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subscription Section
                item {
                    SubscriptionSection(
                        currentTier = uiState.subscriptionTier,
                        onUpgradeClick = {
                            // TODO: Navigate to upgrade screen
                        },
                        onManageSubscriptionClick = {
                            // TODO: Navigate to subscription management
                        }
                    )
                }

                // Theme Section
                item {
                    ThemeSection(
                        currentTheme = uiState.appTheme,
                        onThemeSelected = viewModel::updateTheme
                    )
                }

                // Notifications Section
                item {
                    NotificationSettingsSection(
                        preferences = uiState.notificationPreferences,
                        viewModel = viewModel
                    )
                }

                // Help & Support Section
                item {
                    HelpSection(
                        onFAQClick = onNavigateToFAQ,
                        onContactSupportClick = {
                            // TODO: Open email intent
                        }
                    )
                }

                // About Section
                item {
                    AppInfoSection()
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsSection(
    preferences: com.ssbmax.core.domain.model.NotificationPreferences?,
    viewModel: SettingsViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Manage your notification preferences",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Master toggle
            SettingsSwitchItem(
                title = "Push Notifications",
                description = "Enable or disable all push notifications",
                icon = Icons.Default.Notifications,
                checked = preferences?.enablePushNotifications ?: true,
                onCheckedChange = { viewModel.togglePushNotifications(it) }
            )

            if (preferences?.enablePushNotifications == true) {
                // Individual notification types
                SettingsSwitchItem(
                    title = "Grading Complete",
                    description = "When your test is graded",
                    icon = Icons.Default.CheckCircle,
                    checked = preferences.enableGradingNotifications,
                    onCheckedChange = { viewModel.toggleGradingComplete(it) }
                )

                SettingsSwitchItem(
                    title = "Feedback Available",
                    description = "When new feedback is added",
                    icon = Icons.Default.Comment,
                    checked = preferences.enableFeedbackNotifications,
                    onCheckedChange = { viewModel.toggleFeedbackAvailable(it) }
                )

                SettingsSwitchItem(
                    title = "Batch Invitations",
                    description = "When you're invited to a batch",
                    icon = Icons.Default.Group,
                    checked = preferences.enableBatchInvitations,
                    onCheckedChange = { viewModel.toggleBatchInvitation(it) }
                )

                SettingsSwitchItem(
                    title = "Announcements",
                    description = "General updates from SSBMax",
                    icon = Icons.Default.Campaign,
                    checked = preferences.enableGeneralAnnouncements,
                    onCheckedChange = { viewModel.toggleGeneralAnnouncement(it) }
                )

                SettingsSwitchItem(
                    title = "Study Reminders",
                    description = "Daily study reminders",
                    icon = Icons.Default.School,
                    checked = preferences.enableStudyReminders,
                    onCheckedChange = { viewModel.toggleStudyReminders(it) }
                )

                SettingsSwitchItem(
                    title = "Test Reminders",
                    description = "Reminders to complete tests",
                    icon = Icons.Default.Quiz,
                    checked = preferences.enableTestReminders,
                    onCheckedChange = { viewModel.toggleTestReminders(it) }
                )

                SettingsSwitchItem(
                    title = "Marketplace Updates",
                    description = "New classes and assessors",
                    icon = Icons.Default.ShoppingBag,
                    checked = preferences.enableMarketplaceUpdates,
                    onCheckedChange = { viewModel.toggleMarketplaceUpdates(it) }
                )
            }
        }
    }
}


@Composable
private fun AppInfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            SettingsInfoItem(
                title = "Version",
                value = "1.0.0 Beta",
                icon = Icons.Default.Info
            )

            SettingsInfoItem(
                title = "Support",
                value = "support@ssbmax.com",
                icon = Icons.Default.Email
            )

            SettingsInfoItem(
                title = "Privacy Policy",
                value = "View",
                icon = Icons.Default.Shield
            )

            SettingsInfoItem(
                title = "Terms of Service",
                value = "View",
                icon = Icons.Default.Description
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

