package com.ssbmax.shared.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.about_privacy_policy
import ssbmax.shared.generated.resources.about_section_title
import ssbmax.shared.generated.resources.about_support
import ssbmax.shared.generated.resources.about_support_email
import ssbmax.shared.generated.resources.about_terms_of_service
import ssbmax.shared.generated.resources.about_version
import ssbmax.shared.generated.resources.about_version_value
import ssbmax.shared.generated.resources.about_view

/**
 * About/App-info section for [SettingsScreen], split into its own file
 * (matches the Android original's inline-but-separate-composable layout,
 * moved to a standalone file to keep [SettingsScreen] itself under this
 * repo's 300-line Quality Limit).
 */
@Composable
internal fun AppInfoSection(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.about_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            SettingsInfoItem(
                title = stringResource(Res.string.about_version),
                value = stringResource(Res.string.about_version_value),
                icon = Icons.Default.Info
            )

            SettingsInfoItem(
                title = stringResource(Res.string.about_support),
                value = stringResource(Res.string.about_support_email),
                icon = Icons.Default.Email
            )

            SettingsInfoItem(
                title = stringResource(Res.string.about_privacy_policy),
                value = stringResource(Res.string.about_view),
                icon = Icons.Default.Shield
            )

            SettingsInfoItem(
                title = stringResource(Res.string.about_terms_of_service),
                value = stringResource(Res.string.about_view),
                icon = Icons.Default.Description
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String,
    icon: ImageVector,
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
