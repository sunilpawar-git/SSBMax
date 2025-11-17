package com.ssbmax.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.domain.model.NotificationPreferences
import com.ssbmax.ui.settings.SettingsViewModel

@Composable
fun NotificationSettingsSection(
    preferences: NotificationPreferences?,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.notification_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.notification_settings_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Master toggle
            SettingsSwitchItem(
                title = stringResource(R.string.notification_push_title),
                description = stringResource(R.string.notification_push_description),
                icon = Icons.Default.Notifications,
                checked = preferences?.enablePushNotifications ?: true,
                onCheckedChange = { viewModel.togglePushNotifications(it) }
            )

            if (preferences?.enablePushNotifications == true) {
                // Individual notification types
                SettingsSwitchItem(
                    title = stringResource(R.string.notification_grading_complete_title),
                    description = stringResource(R.string.notification_grading_complete_description),
                    icon = Icons.Default.CheckCircle,
                    checked = preferences.enableGradingNotifications,
                    onCheckedChange = { viewModel.toggleGradingComplete(it) }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_feedback_title),
                    description = stringResource(R.string.notification_feedback_description),
                    icon = Icons.Default.Comment,
                    checked = preferences.enableFeedbackNotifications,
                    onCheckedChange = { viewModel.toggleFeedbackAvailable(it) }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_batch_invitations_title),
                    description = stringResource(R.string.notification_batch_invitations_description),
                    icon = Icons.Default.Group,
                    checked = preferences.enableBatchInvitations,
                    onCheckedChange = { viewModel.toggleBatchInvitation(it) }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_announcements_title),
                    description = stringResource(R.string.notification_announcements_description),
                    icon = Icons.Default.Campaign,
                    checked = preferences.enableGeneralAnnouncements,
                    onCheckedChange = { viewModel.toggleGeneralAnnouncement(it) }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_study_reminders_title),
                    description = stringResource(R.string.notification_study_reminders_description),
                    icon = Icons.Default.School,
                    checked = preferences.enableStudyReminders,
                    onCheckedChange = { viewModel.toggleStudyReminders(it) }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_test_reminders_title),
                    description = stringResource(R.string.notification_test_reminders_description),
                    icon = Icons.Default.Quiz,
                    checked = preferences.enableTestReminders,
                    onCheckedChange = { viewModel.toggleTestReminders(it) }
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_marketplace_updates_title),
                    description = stringResource(R.string.notification_marketplace_updates_description),
                    icon = Icons.Default.ShoppingBag,
                    checked = preferences.enableMarketplaceUpdates,
                    onCheckedChange = { viewModel.toggleMarketplaceUpdates(it) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    icon: ImageVector,
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
