package com.ssbmax.shared.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.shared.presentation.settings.SettingsViewModel
import com.ssbmax.shared.ui.settings.components.NotificationSettingsSection
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.settings_cd_back
import ssbmax.shared.generated.resources.settings_title

/**
 * KMP port of the Android `app/.../ui/settings/SettingsScreen.kt` — main
 * Settings screen composed of independently-state-managed sections
 * (subscription, theme, notifications, help, about).
 *
 * Each section (`SubscriptionSection`, `ThemeSection`, `NotificationSettingsSection`,
 * `HelpSection`, `AppInfoSection`) is its own file, matching the Android
 * original's file layout and this repo's 300-line Quality Limit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFAQ: () -> Unit = {},
    onNavigateToUpgrade: () -> Unit = {},
    onNavigateToSubscriptionManagement: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.settings_cd_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SubscriptionSection(
                    currentTier = uiState.subscriptionTier,
                    onUpgradeClick = onNavigateToUpgrade,
                    onManageSubscriptionClick = onNavigateToSubscriptionManagement
                )
            }

            item { ThemeSection() }

            item { NotificationSettingsSection() }

            item {
                HelpSection(
                    onFAQClick = onNavigateToFAQ,
                    onContactSupportClick = {
                        // TODO: matches Android original's unwired "open email intent" action
                    }
                )
            }

            item { AppInfoSection() }
        }
    }
}
