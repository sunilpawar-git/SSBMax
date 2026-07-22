package com.ssbmax.shared.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.NotificationPreferences
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.notification_announcements_description
import ssbmax.shared.generated.resources.notification_announcements_title
import ssbmax.shared.generated.resources.notification_batch_invitations_description
import ssbmax.shared.generated.resources.notification_batch_invitations_title
import ssbmax.shared.generated.resources.notification_feedback_description
import ssbmax.shared.generated.resources.notification_feedback_title
import ssbmax.shared.generated.resources.notification_grading_complete_description
import ssbmax.shared.generated.resources.notification_grading_complete_title
import ssbmax.shared.generated.resources.notification_marketplace_updates_description
import ssbmax.shared.generated.resources.notification_marketplace_updates_title
import ssbmax.shared.generated.resources.notification_push_description
import ssbmax.shared.generated.resources.notification_push_title
import ssbmax.shared.generated.resources.notification_study_reminders_description
import ssbmax.shared.generated.resources.notification_study_reminders_title
import ssbmax.shared.generated.resources.notification_test_reminders_description
import ssbmax.shared.generated.resources.notification_test_reminders_title

/**
 * The master toggle + per-category switches for
 * [NotificationSettingsSection], split into their own file to keep the
 * parent file under this repo's 300-line Quality Limit.
 */
@Composable
internal fun NotificationSwitchItems(
    preferences: NotificationPreferences?,
    onTogglePushNotifications: (Boolean) -> Unit,
    onToggleGradingComplete: (Boolean) -> Unit,
    onToggleFeedbackAvailable: (Boolean) -> Unit,
    onToggleBatchInvitation: (Boolean) -> Unit,
    onToggleGeneralAnnouncement: (Boolean) -> Unit,
    onToggleStudyReminders: (Boolean) -> Unit,
    onToggleTestReminders: (Boolean) -> Unit,
    onToggleMarketplaceUpdates: (Boolean) -> Unit
) {
    SettingsSwitchItem(
        title = stringResource(Res.string.notification_push_title),
        description = stringResource(Res.string.notification_push_description),
        icon = Icons.Default.Notifications,
        checked = preferences?.enablePushNotifications ?: true,
        onCheckedChange = onTogglePushNotifications
    )

    if (preferences?.enablePushNotifications == true) {
        SettingsSwitchItem(
            title = stringResource(Res.string.notification_grading_complete_title),
            description = stringResource(Res.string.notification_grading_complete_description),
            icon = Icons.Default.CheckCircle,
            checked = preferences.enableGradingNotifications,
            onCheckedChange = onToggleGradingComplete
        )

        SettingsSwitchItem(
            title = stringResource(Res.string.notification_feedback_title),
            description = stringResource(Res.string.notification_feedback_description),
            icon = Icons.AutoMirrored.Filled.Comment,
            checked = preferences.enableFeedbackNotifications,
            onCheckedChange = onToggleFeedbackAvailable
        )

        SettingsSwitchItem(
            title = stringResource(Res.string.notification_batch_invitations_title),
            description = stringResource(Res.string.notification_batch_invitations_description),
            icon = Icons.Default.Group,
            checked = preferences.enableBatchInvitations,
            onCheckedChange = onToggleBatchInvitation
        )

        SettingsSwitchItem(
            title = stringResource(Res.string.notification_announcements_title),
            description = stringResource(Res.string.notification_announcements_description),
            icon = Icons.Default.Campaign,
            checked = preferences.enableGeneralAnnouncements,
            onCheckedChange = onToggleGeneralAnnouncement
        )

        SettingsSwitchItem(
            title = stringResource(Res.string.notification_study_reminders_title),
            description = stringResource(Res.string.notification_study_reminders_description),
            icon = Icons.Default.School,
            checked = preferences.enableStudyReminders,
            onCheckedChange = onToggleStudyReminders
        )

        SettingsSwitchItem(
            title = stringResource(Res.string.notification_test_reminders_title),
            description = stringResource(Res.string.notification_test_reminders_description),
            icon = Icons.Default.Quiz,
            checked = preferences.enableTestReminders,
            onCheckedChange = onToggleTestReminders
        )

        SettingsSwitchItem(
            title = stringResource(Res.string.notification_marketplace_updates_title),
            description = stringResource(Res.string.notification_marketplace_updates_description),
            icon = Icons.Default.ShoppingBag,
            checked = preferences.enableMarketplaceUpdates,
            onCheckedChange = onToggleMarketplaceUpdates
        )
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
