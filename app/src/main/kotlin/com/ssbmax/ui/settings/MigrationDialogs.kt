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

/**
 * Shared migration result dialogs to keep SettingsScreen.kt under 300 lines per section
 */

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

