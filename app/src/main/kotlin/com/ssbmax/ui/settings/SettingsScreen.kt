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
    onNavigateToUpgrade: () -> Unit = {},
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
                        onUpgradeClick = onNavigateToUpgrade,
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
                
                // Developer Options Section
                item {
                    DeveloperOptionsSection(
                        onRunHealthCheck = viewModel::runHealthCheck,
                        isCheckingHealth = uiState.isCheckingHealth,
                        onMigrateOIR = viewModel::migrateOIR,
                        onMigratePPDT = viewModel::migratePPDT,
                        isMigrating = uiState.isMigrating
                    )
                }
            }
        }
    }
    
    // Health Check Result Dialog
    uiState.healthCheckResult?.let { healthStatus ->
        HealthCheckDialog(
            healthStatus = healthStatus,
            onDismiss = viewModel::clearHealthCheckResult
        )
    }
    
    // Migration Result Dialog (OIR)
    uiState.migrationResult?.let { migrationResult ->
        MigrationResultDialog(
            migrationResult = migrationResult,
            onDismiss = viewModel::clearMigrationResult
        )
    }
    
    // Migration Result Dialog (PPDT)
    uiState.ppdtMigrationResult?.let { migrationResult ->
        PPDTMigrationResultDialog(
            migrationResult = migrationResult,
            onDismiss = viewModel::clearPPDTMigrationResult
        )
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

@Composable
private fun DeveloperOptionsSection(
    onRunHealthCheck: () -> Unit,
    isCheckingHealth: Boolean,
    onMigrateOIR: () -> Unit,
    onMigratePPDT: () -> Unit,
    isMigrating: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Developer Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Text(
                text = "Testing and debugging tools",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Firebase Health Check Button
            Button(
                onClick = onRunHealthCheck,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCheckingHealth,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                if (isCheckingHealth) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Firebase Health Check")
                }
            }
            
            Text(
                text = "Tests connectivity to Firestore and Cloud Storage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Migrate OIR Button
            Button(
                onClick = onMigrateOIR,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isMigrating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                if (isMigrating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Migrating...")
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Migrate OIR to Firestore")
                }
            }
            
            Text(
                text = "Uploads OIR topic + 7 study materials to Firestore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Migrate PPDT Button
            Button(
                onClick = onMigratePPDT,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isMigrating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                if (isMigrating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Migrating...")
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Migrate PPDT to Firestore")
                }
            }
            
            Text(
                text = "Uploads PPDT topic + 6 study materials to Firestore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cloud Content Status
            Text(
                text = "Cloud Content Configuration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            // Enable Cloud Content Button
            var cloudEnabled by remember { 
                mutableStateOf(com.ssbmax.core.domain.config.ContentFeatureFlags.useCloudContent) 
            }
            
            var oirCloudEnabled by remember { 
                mutableStateOf(com.ssbmax.core.domain.config.ContentFeatureFlags.isTopicCloudEnabled("OIR")) 
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Cloud Content",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Switch(
                    checked = cloudEnabled,
                    onCheckedChange = { enabled ->
                        cloudEnabled = enabled
                        com.ssbmax.core.domain.config.ContentFeatureFlags.useCloudContent = enabled
                        android.util.Log.d("SettingsScreen", "Master cloud flag set to: $enabled")
                        android.util.Log.d("SettingsScreen", "Flags after toggle:\n${com.ssbmax.core.domain.config.ContentFeatureFlags.getStatus()}")
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable OIR from Firestore",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Switch(
                    checked = oirCloudEnabled,
                    onCheckedChange = { enabled ->
                        oirCloudEnabled = enabled
                        if (enabled) {
                            com.ssbmax.core.domain.config.ContentFeatureFlags.enableTopicCloud("OIR")
                            android.util.Log.d("SettingsScreen", "OIR cloud flag ENABLED")
                        } else {
                            com.ssbmax.core.domain.config.ContentFeatureFlags.disableTopicCloud("OIR")
                            android.util.Log.d("SettingsScreen", "OIR cloud flag DISABLED")
                        }
                        android.util.Log.d("SettingsScreen", "Flags after OIR toggle:\n${com.ssbmax.core.domain.config.ContentFeatureFlags.getStatus()}")
                    },
                    enabled = cloudEnabled
                )
            }
            
            Text(
                text = if (cloudEnabled && oirCloudEnabled) 
                    "✓ OIR will load from Firestore\n⚠ Restart app to apply changes" 
                else 
                    "OIR loads from local hardcoded data",
                style = MaterialTheme.typography.bodySmall,
                color = if (cloudEnabled && oirCloudEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun HealthCheckDialog(
    healthStatus: com.ssbmax.core.data.health.FirebaseHealthCheck.HealthStatus,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (healthStatus.isFullyHealthy) 
                        Icons.Default.CheckCircle 
                    else 
                        Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (healthStatus.isFullyHealthy)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                Text("Firebase Health Check")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Firestore Status
                HealthCheckItem(
                    title = "Firestore",
                    isHealthy = healthStatus.isFirestoreHealthy,
                    error = healthStatus.firestoreError
                )
                
                HorizontalDivider()
                
                // Cloud Storage Status
                HealthCheckItem(
                    title = "Cloud Storage",
                    isHealthy = healthStatus.isStorageHealthy,
                    error = healthStatus.storageError
                )
                
                HorizontalDivider()
                
                // Overall Status
                Text(
                    text = when {
                        healthStatus.isFullyHealthy -> "✓ All systems operational"
                        healthStatus.isPartiallyHealthy -> "⚠ Partial connectivity"
                        else -> "✗ All systems down - using local fallback"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        healthStatus.isFullyHealthy -> MaterialTheme.colorScheme.primary
                        healthStatus.isPartiallyHealthy -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun HealthCheckItem(
    title: String,
    isHealthy: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isHealthy) "✓ Healthy" else "✗ Failed",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        
        error?.let {
            Text(
                text = "Error: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 28.dp)
            )
        }
    }
}

@Composable
private fun MigrationResultDialog(
    migrationResult: MigrateOIRUseCase.MigrationResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        migrationResult.success -> Icons.Default.CheckCircle
                        migrationResult.materialsMigrated > 0 -> Icons.Default.Warning
                        else -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when {
                        migrationResult.success -> MaterialTheme.colorScheme.primary
                        migrationResult.materialsMigrated > 0 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Text("OIR Migration Result")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary
                Text(
                    text = migrationResult.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        migrationResult.success -> MaterialTheme.colorScheme.primary
                        migrationResult.materialsMigrated > 0 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                
                HorizontalDivider()
                
                // Details
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Topic:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (migrationResult.topicMigrated) "✓ Migrated" else "✗ Failed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (migrationResult.topicMigrated) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Materials:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${migrationResult.materialsMigrated}/${migrationResult.totalMaterials}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (migrationResult.materialsMigrated == migrationResult.totalMaterials)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.tertiary
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Duration:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${migrationResult.durationMs}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Errors (if any)
                if (migrationResult.errors.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Errors:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        migrationResult.errors.take(3).forEach { error ->
                            Text(
                                text = "• $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (migrationResult.errors.size > 3) {
                            Text(
                                text = "... and ${migrationResult.errors.size - 3} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
                
                // Success message
                if (migrationResult.success) {
                    HorizontalDivider()
                    Text(
                        text = "✓ OIR content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Running health check again",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PPDTMigrationResultDialog(
    migrationResult: MigratePPDTUseCase.MigrationResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        migrationResult.success -> Icons.Default.CheckCircle
                        migrationResult.materialsMigrated > 0 -> Icons.Default.Warning
                        else -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when {
                        migrationResult.success -> MaterialTheme.colorScheme.primary
                        migrationResult.materialsMigrated > 0 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Text("PPDT Migration Result")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary
                Text(
                    text = migrationResult.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        migrationResult.success -> MaterialTheme.colorScheme.primary
                        migrationResult.materialsMigrated > 0 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                
                HorizontalDivider()
                
                // Details
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Topic:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (migrationResult.topicMigrated) "✓ Migrated" else "✗ Failed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (migrationResult.topicMigrated) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Materials:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${migrationResult.materialsMigrated}/${migrationResult.totalMaterials}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (migrationResult.materialsMigrated == migrationResult.totalMaterials)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.tertiary
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Duration:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${migrationResult.durationMs}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Errors (if any)
                if (migrationResult.errors.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Errors:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    migrationResult.errors.forEach { error ->
                        Text(
                            text = "• $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                // Success message
                if (migrationResult.success) {
                    HorizontalDivider()
                    Text(
                        text = "✓ PPDT content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Navigating to PPDT topic (should show 12+ materials)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

