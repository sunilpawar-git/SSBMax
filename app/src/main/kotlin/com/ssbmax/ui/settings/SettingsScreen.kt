package com.ssbmax.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.ui.settings.components.NotificationSettingsSection

/**
 * Settings Screen with notification preferences and app settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFAQ: () -> Unit = {},
    onNavigateToUpgrade: () -> Unit = {},
    onNavigateToSubscriptionManagement: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Subscription Section
                item {
                    SubscriptionSection(
                        currentTier = uiState.subscriptionTier,
                        onUpgradeClick = onNavigateToUpgrade,
                        onManageSubscriptionClick = onNavigateToSubscriptionManagement
                    )
                }

                // Theme Section
                item {
                    ThemeSection()
                }

                // Notifications Section
                item {
                    NotificationSettingsSection()
                }

                // Help & Support Section
                item {
                    HelpSection(
                        onFAQClick = onNavigateToFAQ,
                        onContactSupportClick = {
                            // TODO: Open email intent
                        }
                    )
                }

                // About Section
                item {
                    AppInfoSection()
                }
        }
    }
}

@Composable
private fun AppInfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.about_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            SettingsInfoItem(
                title = stringResource(R.string.about_version),
                value = stringResource(R.string.about_version_value),
                icon = Icons.Default.Info
            )

            SettingsInfoItem(
                title = stringResource(R.string.about_support),
                value = stringResource(R.string.about_support_email),
                icon = Icons.Default.Email
            )

            SettingsInfoItem(
                title = stringResource(R.string.about_privacy_policy),
                value = stringResource(R.string.about_view),
                icon = Icons.Default.Shield
            )

            SettingsInfoItem(
                title = stringResource(R.string.about_terms_of_service),
                value = stringResource(R.string.about_view),
                icon = Icons.Default.Description
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

