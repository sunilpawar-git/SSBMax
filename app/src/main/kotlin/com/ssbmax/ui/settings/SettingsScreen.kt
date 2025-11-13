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
import com.ssbmax.core.domain.usecase.*

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

