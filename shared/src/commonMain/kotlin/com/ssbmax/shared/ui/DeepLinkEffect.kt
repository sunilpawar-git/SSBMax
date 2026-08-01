package com.ssbmax.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavUri
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navOptions
import com.ssbmax.navigation.DeepLinkParser
import com.ssbmax.navigation.isAuthScreen
import com.ssbmax.shared.domain.util.DomainLogger

private const val TAG = "DeepLinkEffect"

/**
 * commonMain port of `app/ui/SSBMaxApp.kt`'s deep-link `LaunchedEffect`
 * (KMP-convergence Phase 4) -- the seam [SSBMaxRoot] wires both platforms'
 * entry points into via `DeepLinkGateway`. Preserves the original's three
 * hard-won behaviours, adapted to type-safe routing (`NavDestination.route`
 * no longer holds a filled-in-args string on either the old or new graph --
 * see [com.ssbmax.navigation.SSBMaxDestinations]'s own class doc):
 *
 * - **Auth-screen deferral** -- a cold-start tap arrives before the graph
 *   clears Splash/Login/RoleSelection; wait rather than navigate into chrome
 *   that isn't showing yet.
 * - **Already-on-target de-dupe** -- [androidx.navigation.NavDestination.hasDeepLink]
 *   against the *current* destination: if it's already the one this deep
 *   link resolves to, skip navigating a second time (which would push a
 *   duplicate entry and cancel the first screen's ViewModel). Destinations
 *   that never registered a deep link (i.e. everything except
 *   [com.ssbmax.navigation.SSBMaxDestinations.InterviewResult] today) never
 *   match here, so this is a no-op for them, exactly as before.
 * - **Loud, non-fatal failure** -- a route with no registered deep link (or
 *   any other navigation failure) logs via [DomainLogger] instead of
 *   crashing; [com.ssbmax.navigation.DeepLinkParser.ROUTE_INTERVIEW_HISTORY]
 *   is one such route today (built by `NotificationHelper`, never
 *   registered on either platform's graph -- a pre-existing gap, not
 *   introduced by this seam).
 *
 * Takes its Koin-resolved dependencies as plain parameters rather than
 * calling `koinInject()` itself, so this composable -- the one piece of
 * real navigation logic in this phase -- is exercisable in a plain
 * `runComposeUiTest` without a Koin graph.
 */
@Composable
internal fun DeepLinkEffect(
    navController: NavHostController,
    pendingRoute: String?,
    isAuthenticated: Boolean,
    onConsume: () -> Unit,
    logger: DomainLogger
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    LaunchedEffect(pendingRoute, currentDestination, isAuthenticated) {
        val route = pendingRoute ?: return@LaunchedEffect
        if (!isAuthenticated || currentDestination == null) return@LaunchedEffect

        if (currentDestination.isAuthScreen()) {
            logger.d(TAG, "Waiting to pass auth screen before deep-linking to $route")
            return@LaunchedEffect
        }

        val deepLinkUri = NavUri(DeepLinkParser.SCHEME + route)

        if (currentDestination.hasDeepLink(deepLinkUri)) {
            logger.d(TAG, "Already on target route ($route), skipping duplicate navigation")
            onConsume()
            return@LaunchedEffect
        }

        try {
            navController.navigate(deepLinkUri, navOptions { launchSingleTop = true })
            logger.d(TAG, "Navigated to deep link: $route")
        } catch (e: IllegalArgumentException) {
            // Thrown by NavController.navigate when no destination has a
            // matching NavDeepLink (e.g. ROUTE_INTERVIEW_HISTORY today, never
            // registered on either platform's graph) -- log, don't crash.
            logger.w(TAG, "No destination registered for deep link: $route")
        }
        onConsume()
    }
}
