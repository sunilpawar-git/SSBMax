
package com.ssbmax.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.NotificationPreferences
import com.ssbmax.ui.settings.notifications.NotificationSettingsViewModel

/**
 * Notification Settings Section with independent ViewModel
 * This is the new pattern where each section manages its own state
 */
@Composable
fun NotificationSettingsSection(
    modifier: Modifier = Modifier,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationSettingsSectionContent(
        preferences = uiState.notificationPreferences,
        onTogglePushNotifications = viewModel::togglePushNotifications,
        onToggleGradingComplete = viewModel::toggleGradingComplete,
        onToggleFeedbackAvailable = viewModel::toggleFeedbackAvailable,
        onToggleBatchInvitation = viewModel::toggleBatchInvitation,
        onToggleGeneralAnnouncement = viewModel::toggleGeneralAnnouncement,
        onToggleStudyReminders = viewModel::toggleStudyReminders,
        onToggleTestReminders = viewModel::toggleTestReminders,
        onToggleMarketplaceUpdates = viewModel::toggleMarketplaceUpdates,
        modifier = modifier
    )
}

/**
 * Legacy version for backward compatibility during transition.
 *
 * @deprecated Use NotificationSettingsSection() without parameters - it now manages its own ViewModel
 *
 * **Migration Timeline:**
 * - Deprecated: Phase 3 (2024-Q3)
 * - Removal Target: Phase 5 (2025-Q1)
 *
 * **Migration Guide:**
 * ```kotlin
 * // OLD (deprecated - manual state passing)
 * NotificationSettingsSection(
 *     preferences = viewModel.preferences,
 *     onTogglePushNotifications = { enabled -> viewModel.toggle(enabled) },
 *     onToggleGradingComplete = { ... },
 *     // ... 8 more parameters
 * )
 *
 * // NEW (recommended - self-managed state)
 * NotificationSettingsSection(modifier = Modifier.fillMaxWidth())
 * // ViewModel injected automatically via Hilt
 * ```
 *
 * **Breaking Changes:**
 * - All state management parameters removed
 * - ViewModel now injected via `hiltViewModel()` internally
 * - Simpler API with single modifier parameter
 *
 * **Why This Change:**
 * - Follows Compose best practices (components manage own state)
 * - Reduces boilerplate (8 callback parameters eliminated)
 * - Better testability with dedicated ViewModel
 *
 * @see NotificationSettingsSection
 */
@Deprecated(
    message = "Use NotificationSettingsSection() without parameters - it now manages its own ViewModel",
    replaceWith = ReplaceWith("NotificationSettingsSection(modifier)")
)
@Composable
fun NotificationSettingsSection(
    preferences: NotificationPreferences?,
    onTogglePushNotifications: (Boolean) -> Unit,
    onToggleGradingComplete: (Boolean) -> Unit,
    onToggleFeedbackAvailable: (Boolean) -> Unit,
    onToggleBatchInvitation: (Boolean) -> Unit,
    onToggleGeneralAnnouncement: (Boolean) -> Unit,
    onToggleStudyReminders: (Boolean) -> Unit,
    onToggleTestReminders: (Boolean) -> Unit,
    onToggleMarketplaceUpdates: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    NotificationSettingsSectionContent(
        preferences = preferences,
        onTogglePushNotifications = onTogglePushNotifications,
        onToggleGradingComplete = onToggleGradingComplete,
        onToggleFeedbackAvailable = onToggleFeedbackAvailable,
        onToggleBatchInvitation = onToggleBatchInvitation,
        onToggleGeneralAnnouncement = onToggleGeneralAnnouncement,
        onToggleStudyReminders = onToggleStudyReminders,
        onToggleTestReminders = onToggleTestReminders,
        onToggleMarketplaceUpdates = onToggleMarketplaceUpdates,
        modifier = modifier
    )
}

/**
 * Internal content composable - separates presentation from state management
 */
@Composable
private fun NotificationSettingsSectionContent(
    preferences: NotificationPreferences?,
    onTogglePushNotifications: (Boolean) -> Unit,
    onToggleGradingComplete: (Boolean) -> Unit,
    onToggleFeedbackAvailable: (Boolean) -> Unit,
    onToggleBatchInvitation: (Boolean) -> Unit,
    onToggleGeneralAnnouncement: (Boolean) -> Unit,
    onToggleStudyReminders: (Boolean) -> Unit,
    onToggleTestReminders: (Boolean) -> Unit,
    onToggleMarketplaceUpdates: (Boolean) -> Unit,
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
                onCheckedChange = onTogglePushNotifications
            )

            if (preferences?.enablePushNotifications == true) {
                // Individual notification types
                SettingsSwitchItem(
                    title = stringResource(R.string.notification_grading_complete_title),
                    description = stringResource(R.string.notification_grading_complete_description),
                    icon = Icons.Default.CheckCircle,
                    checked = preferences.enableGradingNotifications,
                    onCheckedChange = onToggleGradingComplete
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_feedback_title),
                    description = stringResource(R.string.notification_feedback_description),
                    icon = Icons.AutoMirrored.Filled.Comment,
                    checked = preferences.enableFeedbackNotifications,
                    onCheckedChange = onToggleFeedbackAvailable
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_batch_invitations_title),
                    description = stringResource(R.string.notification_batch_invitations_description),
                    icon = Icons.Default.Group,
                    checked = preferences.enableBatchInvitations,
                    onCheckedChange = onToggleBatchInvitation
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_announcements_title),
                    description = stringResource(R.string.notification_announcements_description),
                    icon = Icons.Default.Campaign,
                    checked = preferences.enableGeneralAnnouncements,
                    onCheckedChange = onToggleGeneralAnnouncement
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_study_reminders_title),
                    description = stringResource(R.string.notification_study_reminders_description),
                    icon = Icons.Default.School,
                    checked = preferences.enableStudyReminders,
                    onCheckedChange = onToggleStudyReminders
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_test_reminders_title),
                    description = stringResource(R.string.notification_test_reminders_description),
                    icon = Icons.Default.Quiz,
                    checked = preferences.enableTestReminders,
                    onCheckedChange = onToggleTestReminders
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.notification_marketplace_updates_title),
                    description = stringResource(R.string.notification_marketplace_updates_description),
                    icon = Icons.Default.ShoppingBag,
                    checked = preferences.enableMarketplaceUpdates,
                    onCheckedChange = onToggleMarketplaceUpdates
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
