package com.ssbmax.shared.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.domain.model.AppTheme
import com.ssbmax.shared.presentation.settings.theme.ThemeSettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.theme_dark_description
import ssbmax.shared.generated.resources.theme_light_description
import ssbmax.shared.generated.resources.theme_section_description
import ssbmax.shared.generated.resources.theme_section_title
import ssbmax.shared.generated.resources.theme_system_description

/**
 * Theme selector section for [SettingsScreen] — KMP port of the Android
 * `app/.../ui/settings/ThemeSection.kt`.
 *
 * Note on wiring: this composable owns [ThemeSettingsViewModel] via
 * `koinViewModel()` (Phase 1 of the KMP-convergence plan) -- teardown is
 * automatic via `onCleared()`/`viewModelScope`, same as
 * `NotificationSettingsSection` below; [SettingsScreen]'s own top-level
 * `SettingsViewModel` is a separate instance.
 *
 * Deviation from the Android original: the Android `ThemeSection` also
 * pushed the selected theme into `com.ssbmax.ui.theme.LocalThemeState` (an
 * Android-only `CompositionLocal` the app's root `Activity`-level theme
 * wrapper reads to recompose immediately). No `shared/commonMain` equivalent
 * exists yet -- there is no commonMain root theme composable subscribing to
 * [com.ssbmax.shared.platform.settings.AppThemeSettings] at all today, on
 * either platform. The preference IS persisted correctly via
 * `appThemeSettings.setTheme(...)` (this screen's own state updates
 * immediately), but nothing in `shared`/iOS/this Android-KMP path yet
 * re-themes the actual running app in response -- a real, named gap, not
 * silently dropped, and out of this session's scope (wiring a shared root
 * theme composable is a separate, larger change).
 */
@Composable
fun ThemeSection(modifier: Modifier = Modifier) {
    val viewModel: ThemeSettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(Res.string.theme_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = stringResource(Res.string.theme_section_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK).forEach { theme ->
                    ThemeOption(
                        theme = theme,
                        isSelected = uiState.appTheme == theme,
                        onSelected = { viewModel.updateTheme(theme) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: AppTheme,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = theme.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
                Text(
                    text = theme.getDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        RadioButton(
            selected = isSelected,
            onClick = onSelected
        )
    }
}

/**
 * UI-specific icon for [AppTheme].
 */
private val AppTheme.icon: ImageVector
    get() = when (this) {
        AppTheme.LIGHT -> Icons.Default.LightMode
        AppTheme.DARK -> Icons.Default.DarkMode
        AppTheme.SYSTEM -> Icons.Default.BrightnessAuto
    }

@Composable
private fun AppTheme.getDescription(): String = when (this) {
    AppTheme.LIGHT -> stringResource(Res.string.theme_light_description)
    AppTheme.DARK -> stringResource(Res.string.theme_dark_description)
    AppTheme.SYSTEM -> stringResource(Res.string.theme_system_description)
}
