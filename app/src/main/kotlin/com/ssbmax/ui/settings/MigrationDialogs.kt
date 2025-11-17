package com.ssbmax.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.core.data.health.FirebaseHealthCheck
import com.ssbmax.core.domain.usecase.migration.MigrateConferenceUseCase
import com.ssbmax.core.domain.usecase.migration.MigrateGTOUseCase
import com.ssbmax.core.domain.usecase.migration.MigrateInterviewUseCase
import com.ssbmax.core.domain.usecase.migration.MigrateMedicalsUseCase
import com.ssbmax.core.domain.usecase.migration.MigrateOIRUseCase
import com.ssbmax.core.domain.usecase.migration.MigratePIQFormUseCase
import com.ssbmax.core.domain.usecase.migration.MigratePPDTUseCase
import com.ssbmax.core.domain.usecase.migration.MigratePsychologyUseCase
import com.ssbmax.core.domain.usecase.migration.MigrateSSBOverviewUseCase

/**
 * Shared migration result dialogs to keep SettingsScreen.kt under 300 lines per section
 */

@Composable
fun HealthCheckDialog(
    healthStatus: FirebaseHealthCheck.HealthStatus,
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
fun MigrationResultDialog(
    migrationResult: MigrateOIRUseCase.MigrationResult,
    onDismiss: () -> Unit
) {
    MigrationResultDialogTemplate(
        title = "OIR Migration Result",
        message = migrationResult.message,
        success = migrationResult.success,
        topicMigrated = migrationResult.topicMigrated,
        materialsMigrated = migrationResult.materialsMigrated,
        totalMaterials = migrationResult.totalMaterials,
        durationMs = migrationResult.durationMs,
        errors = migrationResult.errors,
        successMessage = "✓ OIR content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Running health check again",
        onDismiss = onDismiss
    )
}

@Composable
fun PPDTMigrationResultDialog(
    migrationResult: MigratePPDTUseCase.MigrationResult,
    onDismiss: () -> Unit
) {
    MigrationResultDialogTemplate(
        title = "PPDT Migration Result",
        message = migrationResult.message,
        success = migrationResult.success,
        topicMigrated = migrationResult.topicMigrated,
        materialsMigrated = migrationResult.materialsMigrated,
        totalMaterials = migrationResult.totalMaterials,
        durationMs = migrationResult.durationMs,
        errors = migrationResult.errors,
        successMessage = "✓ PPDT content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Navigating to PPDT topic (should show 12+ materials)",
        onDismiss = onDismiss
    )
}

@Composable
fun PsychologyMigrationResultDialog(
    migrationResult: MigratePsychologyUseCase.MigrationResult,
    onDismiss: () -> Unit
) {
    MigrationResultDialogTemplate(
        title = "Psychology Migration Result",
        message = migrationResult.message,
        success = migrationResult.success,
        topicMigrated = migrationResult.topicMigrated,
        materialsMigrated = migrationResult.materialsMigrated,
        totalMaterials = migrationResult.totalMaterials,
        durationMs = migrationResult.durationMs,
        errors = migrationResult.errors,
        successMessage = "✓ Psychology content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Navigating to Psychology topic (should show 8 materials)",
        onDismiss = onDismiss
    )
}

@Composable
fun PIQFormMigrationResultDialog(
    migrationResult: MigratePIQFormUseCase.MigrationResult,
    onDismiss: () -> Unit
) {
    MigrationResultDialogTemplate(
        title = "PIQ Form Migration Result",
        message = migrationResult.message,
        success = migrationResult.success,
        topicMigrated = migrationResult.topicMigrated,
        materialsMigrated = migrationResult.materialsMigrated,
        totalMaterials = migrationResult.totalMaterials,
        durationMs = migrationResult.durationMs,
        errors = migrationResult.errors,
        successMessage = "✓ PIQ Form content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Navigating to PIQ Form topic (should show 3 materials)",
        onDismiss = onDismiss
    )
}

@Composable
fun GTOMigrationResultDialog(
    migrationResult: MigrateGTOUseCase.MigrationResult,
    onDismiss: () -> Unit
) {
    MigrationResultDialogTemplate(
        title = "GTO Migration Result",
        message = migrationResult.message,
        success = migrationResult.success,
        topicMigrated = migrationResult.topicMigrated,
        materialsMigrated = migrationResult.materialsMigrated,
        totalMaterials = migrationResult.totalMaterials,
        durationMs = migrationResult.durationMs,
        errors = migrationResult.errors,
        successMessage = "✓ GTO content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Navigating to GTO topic (should show 7 materials)",
        onDismiss = onDismiss
    )
}

@Composable
fun InterviewMigrationResultDialog(
    migrationResult: MigrateInterviewUseCase.MigrationResult,
    onDismiss: () -> Unit
) {
    MigrationResultDialogTemplate(
        title = "Interview Migration Result",
        message = migrationResult.message,
        success = migrationResult.success,
        topicMigrated = migrationResult.topicMigrated,
        materialsMigrated = migrationResult.materialsMigrated,
        totalMaterials = migrationResult.totalMaterials,
        durationMs = migrationResult.durationMs,
        errors = migrationResult.errors,
        successMessage = "✓ Interview content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Navigating to Interview topic (should show 7 materials)",
        onDismiss = onDismiss
    )
}

@Composable
fun SSBOverviewMigrationResultDialog(
    migrationResult: MigrateSSBOverviewUseCase.MigrationResult,
    onDismiss: () -> Unit
) {
    MigrationResultDialogTemplate(
        title = "SSB Overview Migration Result",
        message = migrationResult.message,
        success = migrationResult.success,
        topicMigrated = migrationResult.topicMigrated,
        materialsMigrated = migrationResult.materialsMigrated,
        totalMaterials = migrationResult.totalMaterials,
        durationMs = migrationResult.durationMs,
        errors = migrationResult.errors,
        successMessage = "✓ SSB Overview content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Navigating to SSB Overview topic (should show 4 materials)",
        onDismiss = onDismiss
    )
}

@Composable
fun MedicalsMigrationResultDialog(
    migrationResult: MigrateMedicalsUseCase.MigrationResult,
    onDismiss: () -> Unit
) {
    MigrationResultDialogTemplate(
        title = "Medicals Migration Result",
        message = migrationResult.message,
        success = migrationResult.success,
        topicMigrated = migrationResult.topicMigrated,
        materialsMigrated = migrationResult.materialsMigrated,
        totalMaterials = migrationResult.totalMaterials,
        durationMs = migrationResult.durationMs,
        errors = migrationResult.errors,
        successMessage = "✓ Medicals content is now available in Firestore!\n\nYou can verify by:\n• Checking Firebase Console\n• Navigating to Medicals topic (should show 5 materials)",
        onDismiss = onDismiss
    )
}

@Composable
fun ConferenceMigrationResultDialog(
    migrationResult: MigrateConferenceUseCase.MigrationResult,
    onDismiss: () -> Unit
) {
    MigrationResultDialogTemplate(
        title = "Conference Migration Result 🎉",
        message = migrationResult.message,
        success = migrationResult.success,
        topicMigrated = migrationResult.topicMigrated,
        materialsMigrated = migrationResult.materialsMigrated,
        totalMaterials = migrationResult.totalMaterials,
        durationMs = migrationResult.durationMs,
        errors = migrationResult.errors,
        successMessage = "🎉 Conference content is now available in Firestore!\n\n✅ ALL 9 TOPICS MIGRATED - 100% COMPLETE!\n\nYou can verify by:\n• Checking Firebase Console\n• Navigating to Conference topic (should show 4 materials)\n\n🚀 SSBMax Firestore Migration Complete!",
        onDismiss = onDismiss
    )
}

@Composable
private fun MigrationResultDialogTemplate(
    title: String,
    message: String,
    success: Boolean,
    topicMigrated: Boolean,
    materialsMigrated: Int,
    totalMaterials: Int,
    durationMs: Long,
    errors: List<String>,
    successMessage: String,
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
                        success -> Icons.Default.CheckCircle
                        materialsMigrated > 0 -> Icons.Default.Warning
                        else -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when {
                        success -> MaterialTheme.colorScheme.primary
                        materialsMigrated > 0 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Text(title)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        success -> MaterialTheme.colorScheme.primary
                        materialsMigrated > 0 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                
                HorizontalDivider()
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Topic:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (topicMigrated) "✓ Migrated" else "✗ Failed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (topicMigrated) 
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
                        text = "$materialsMigrated/$totalMaterials",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (materialsMigrated == totalMaterials)
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
                        text = "${durationMs}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (errors.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Errors:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    errors.forEach { error ->
                        Text(
                            text = "• $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                if (success) {
                    HorizontalDivider()
                    Text(
                        text = successMessage,
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

