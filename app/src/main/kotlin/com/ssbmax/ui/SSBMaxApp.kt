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
 * Deep-link handling (KMP-convergence Phase 4) moved off this composable
 * entirely -- it now lives in `shared`'s `DeepLinkGateway`/`DeepLinkEffect`,
 * consumed by [com.ssbmax.shared.ui.SSBMaxRoot]. This graph (still
 * `MainActivity`'s production graph pre-Phase-5-cutover) does not read that
 * gateway, so a real notification tap is a no-op here until the cutover --
 * see `MainActivity.deepLinkGateway`'s doc comment for why that's an
 * accepted, not silent, gap.
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

