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
import com.ssbmax.ui.settings.components.DeveloperOptionsSection
import com.ssbmax.ui.settings.components.NotificationSettingsSection

/**
 * Settings Screen with notification preferences and app settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFAQ: () -> Unit = {},
    onNavigateToUpgrade: () -> Unit = {},
    onNavigateToSubscriptionManagement: () -> Unit = {},
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
                        onManageSubscriptionClick = onNavigateToSubscriptionManagement
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
                        onMigratePsychology = viewModel::migratePsychology,
                        onMigratePIQForm = viewModel::migratePIQForm,
                        onMigrateGTO = viewModel::migrateGTO,
                        onMigrateInterview = viewModel::migrateInterview,
                        onMigrateSSBOverview = viewModel::migrateSSBOverview,
                        onMigrateMedicals = viewModel::migrateMedicals,
                        onMigrateConference = viewModel::migrateConference,
                        onClearCache = viewModel::clearFirestoreCache,
                        isMigrating = uiState.isMigrating,
                        isClearingCache = uiState.isClearingCache
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
    
    // Migration Result Dialog (Psychology)
    uiState.psychologyMigrationResult?.let { migrationResult ->
        PsychologyMigrationResultDialog(
            migrationResult = migrationResult,
            onDismiss = viewModel::clearPsychologyMigrationResult
        )
    }
    
    // Migration Result Dialog (PIQ Form)
    uiState.piqFormMigrationResult?.let { migrationResult ->
        PIQFormMigrationResultDialog(
            migrationResult = migrationResult,
            onDismiss = viewModel::clearPIQFormMigrationResult
        )
    }
    
    // Migration Result Dialog (GTO)
    uiState.gtoMigrationResult?.let { migrationResult ->
        GTOMigrationResultDialog(
            migrationResult = migrationResult,
            onDismiss = viewModel::clearGTOMigrationResult
        )
    }
    
    // Migration Result Dialog (Interview)
    uiState.interviewMigrationResult?.let { migrationResult ->
        InterviewMigrationResultDialog(
            migrationResult = migrationResult,
            onDismiss = viewModel::clearInterviewMigrationResult
        )
    }
    
    // Migration Result Dialog (SSB Overview)
    uiState.ssbOverviewMigrationResult?.let { migrationResult ->
        SSBOverviewMigrationResultDialog(
            migrationResult = migrationResult,
            onDismiss = viewModel::clearSSBOverviewMigrationResult
        )
    }
    
    // Migration Result Dialog (Medicals)
    uiState.medicalsMigrationResult?.let { migrationResult ->
        MedicalsMigrationResultDialog(
            migrationResult = migrationResult,
            onDismiss = viewModel::clearMedicalsMigrationResult
        )
    }
    
    // Migration Result Dialog (Conference) - THE FINAL ONE! 🎉
    uiState.conferenceMigrationResult?.let { migrationResult ->
        ConferenceMigrationResultDialog(
            migrationResult = migrationResult,
            onDismiss = viewModel::clearConferenceMigrationResult
        )
    }
    
    // Cache Cleared Dialog
    uiState.refreshResult?.let { result ->
        if (uiState.cacheCleared) {
            AlertDialog(
            onDismissRequest = viewModel::clearCacheResult,
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Content Refreshed!")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "✓ Content has been refreshed from Firestore server!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    HorizontalDivider()
                    
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Topics:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${result.topicsRefreshed}/9",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
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
                            text = "${result.materialsRefreshed}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (result.errors.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "⚠️ Errors:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        result.errors.take(3).forEach { error ->
                            Text(
                                text = "• $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    Text(
                        text = "💡 Navigate to the content you edited - it should show your changes now!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::clearCacheResult) {
                    Text("Got it!")
                }
            }
            )
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

@Composable
private fun PsychologyMigrationResultDialog(
    migrationResult: MigratePsychologyUseCase.MigrationResult,
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
                Text("Psychology Migration Result")
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
                        text = "✓ Psychology content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Navigating to Psychology topic (should show 8 materials)",
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
private fun PIQFormMigrationResultDialog(
    migrationResult: MigratePIQFormUseCase.MigrationResult,
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
                Text("PIQ Form Migration Result")
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
                        text = "✓ PIQ Form content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Navigating to PIQ Form topic (should show 3 materials)",
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

