package com.ssbmax.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssbmax.navigation.SSBMaxNavGraph
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.ui.components.SSBMaxScaffold
import org.koin.compose.viewmodel.koinViewModel

/** Auth screens that don't show scaffold */
private val AUTH_SCREENS = setOf("splash", "login", "role_selection")

/**
 * Main app composable
 * Manages global app state and navigation
 *
 * Dead code since Phase 5's cutover: `MainActivity` now renders
 * [com.ssbmax.shared.ui.SSBMaxRoot] directly, so this composable (and its
 * deep-link-blind graph) is unreached. Kept until Phase 6a deletes it and
 * the rest of `app/ui`/`app/navigation` wholesale.
 */
@Composable
fun SSBMaxApp(
    viewModel: AppViewModel = koinViewModel()
) {
    val navController = rememberNavController()

    // Get current authenticated user from ViewModel
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // Fallback to mock user if not authenticated (for preview/development)
    val user = currentUser ?: SSBMaxUser(
        id = "mock-user-id",
        email = "user@example.com",
        displayName = "SSB Aspirant",
        role = UserRole.STUDENT,
        subscriptionTier = SubscriptionTier.FREE,
        subscription = null
    )

    // Check if we're on a route that needs the scaffold
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val needsScaffold = currentRoute !in AUTH_SCREENS

    if (needsScaffold) {
        SSBMaxScaffold(
            navController = navController,
            user = user,
            onSignOut = viewModel::signOut
        ) { drawerState, onOpenDrawer ->
            SSBMaxNavGraph(
                navController = navController,
                onOpenDrawer = onOpenDrawer
            )
        }
    } else {
        SSBMaxNavGraph(
            navController = navController,
            onOpenDrawer = {}
        )
    }
}

