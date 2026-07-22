package com.ssbmax.shared.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.presentation.settings.notifications.NotificationSettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.notification_settings_description
import ssbmax.shared.generated.resources.notification_settings_title

/**
 * Notification Settings Section — KMP port of the Android
 * `app/.../ui/settings/components/NotificationSettingsSection.kt`. Each
 * section owns its own [NotificationSettingsViewModel] via `koinInject()`.
 *
 * Deliberately NOT ported: the Android original's parallel `@Deprecated`
 * parameterized overload (a Phase 3-era migration shim explicitly marked
 * for "Removal Target: Phase 5" in its own doc comment) — this port only
 * carries forward the current, self-managed-ViewModel API its own doc
 * comment recommends migrating callers to.
 *
 * Toggle rows (push/grading/feedback/batch/announcements/study/test/marketplace)
 * moved to [NotificationSwitchItems] to keep this file under this repo's
 * 300-line Quality Limit.
 */
@Composable
fun NotificationSettingsSection(modifier: Modifier = Modifier) {
    val viewModel: NotificationSettingsViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val preferences = uiState.notificationPreferences

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
                text = stringResource(Res.string.notification_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(Res.string.notification_settings_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            NotificationSwitchItems(
                preferences = preferences,
                onTogglePushNotifications = viewModel::togglePushNotifications,
                onToggleGradingComplete = viewModel::toggleGradingComplete,
                onToggleFeedbackAvailable = viewModel::toggleFeedbackAvailable,
                onToggleBatchInvitation = viewModel::toggleBatchInvitation,
                onToggleGeneralAnnouncement = viewModel::toggleGeneralAnnouncement,
                onToggleStudyReminders = viewModel::toggleStudyReminders,
                onToggleTestReminders = viewModel::toggleTestReminders,
                onToggleMarketplaceUpdates = viewModel::toggleMarketplaceUpdates
            )
        }
    }
}
