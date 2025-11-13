package com.ssbmax.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DeveloperOptionsSection(
    onRunHealthCheck: () -> Unit,
    isCheckingHealth: Boolean,
    onMigrateOIR: () -> Unit,
    onMigratePPDT: () -> Unit,
    onMigratePsychology: () -> Unit,
    onMigratePIQForm: () -> Unit,
    onMigrateGTO: () -> Unit,
    onMigrateInterview: () -> Unit,
    onMigrateSSBOverview: () -> Unit,
    onMigrateMedicals: () -> Unit,
    onMigrateConference: () -> Unit,
    onClearCache: () -> Unit,
    isMigrating: Boolean,
    isClearingCache: Boolean,
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
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Migrate Psychology Button
            Button(
                onClick = onMigratePsychology,
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
                    Text("Migrate Psychology to Firestore")
                }
            }
            
            Text(
                text = "Uploads Psychology topic + 8 study materials to Firestore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Migrate PIQ Form Button
            Button(
                onClick = onMigratePIQForm,
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
                    Text("Migrate PIQ Form to Firestore")
                }
            }
            
            Text(
                text = "Uploads PIQ Form topic + 3 study materials to Firestore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Migrate GTO Button
            Button(
                onClick = onMigrateGTO,
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
                    Text("Migrate GTO to Firestore")
                }
            }
            
            Text(
                text = "Uploads GTO topic + 7 study materials to Firestore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Migrate Interview Button
            Button(
                onClick = onMigrateInterview,
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
                    Text("Migrate Interview to Firestore")
                }
            }
            
            Text(
                text = "Uploads Interview topic + 7 study materials to Firestore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Migrate SSB Overview Button
            Button(
                onClick = onMigrateSSBOverview,
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
                    Text("Migrate SSB Overview to Firestore")
                }
            }
            
            Text(
                text = "Uploads SSB Overview topic + 4 study materials to Firestore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Migrate Medicals Button
            Button(
                onClick = onMigrateMedicals,
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
                    Text("Migrate Medicals to Firestore")
                }
            }
            
            Text(
                text = "Uploads Medicals topic + 5 study materials to Firestore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Migrate Conference Button (THE FINAL ONE!)
            Button(
                onClick = onMigrateConference,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isMigrating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isMigrating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
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
                    Text("Migrate Conference to Firestore 🎉")
                }
            }
            
            Text(
                text = "Uploads Conference topic + 4 study materials to Firestore (FINAL TOPIC - 100%!)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Clear Cache Button
            Button(
                onClick = onClearCache,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isClearingCache,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isClearingCache) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clearing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Cache & Refresh Content")
                }
            }
            
            Text(
                text = "⚠️ Clears cached Firestore data. Next load fetches fresh content from server. Use after editing content in Firebase Console.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
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
