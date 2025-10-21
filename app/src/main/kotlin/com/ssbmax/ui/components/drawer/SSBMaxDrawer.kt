package com.ssbmax.ui.components.drawer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssbmax.core.domain.model.UserProfile

/**
 * Main drawer component that combines header and content.
 * Provides a complete navigation drawer for the SSBMax app.
 */
@Composable
fun SSBMaxDrawer(
    userProfile: UserProfile?,
    currentRoute: String,
    phase1Expanded: Boolean,
    phase2Expanded: Boolean,
    onNavigateToHome: () -> Unit,
    onNavigateToTopic: (topicId: String) -> Unit,
    onNavigateToSSBOverview: () -> Unit,
    onNavigateToMyBatches: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onTogglePhase1: () -> Unit,
    onTogglePhase2: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // User Profile Header
        DrawerHeader(
            userProfile = userProfile,
            onEditProfile = onEditProfile
        )

        // Drawer Navigation Content
        DrawerContent(
            currentRoute = currentRoute,
            phase1Expanded = phase1Expanded,
            phase2Expanded = phase2Expanded,
            onNavigateToHome = onNavigateToHome,
            onNavigateToTopic = onNavigateToTopic,
            onNavigateToSSBOverview = onNavigateToSSBOverview,
            onNavigateToMyBatches = onNavigateToMyBatches,
            onNavigateToSettings = onNavigateToSettings,
            onTogglePhase1 = onTogglePhase1,
            onTogglePhase2 = onTogglePhase2,
            onSignOut = onSignOut
        )
    }
}

