package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssbmax.shared.ui.premium.UpgradeScreen
import com.ssbmax.shared.ui.profile.StudentProfileScreen
import com.ssbmax.shared.ui.profile.UserProfileScreen
import com.ssbmax.shared.ui.settings.SettingsScreen
import com.ssbmax.shared.ui.settings.SubscriptionManagementScreen

fun NavGraphBuilder.profileSettingsGraph(navController: NavHostController) {
    // Profile vertical. UserProfileScreen (create/edit form)
    // replaces the earlier NotYetPortedScreen placeholder -- Splash's
    // onNavigateToProfileOnboarding still lands here (isOnboarding defaults
    // false; the onboarding query-string variant `createOnboardingRoute()`
    // is not parsed by this route registration, a named gap: Nav Compose
    // needs the query param declared in the route pattern itself to bind
    // it, and nothing in this graph currently calls that variant anyway --
    // Splash navigates to the plain `UserProfile.route`).
    composable(SSBMaxDestinations.UserProfile.route) {
        UserProfileScreen(
            onNavigateBack = { navController.navigateUp() },
            onProfileSaved = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.UserProfile.route) { inclusive = true }
                }
            }
        )
    }

    // StudentProfile (summary/stats display, distinct from UserProfile's
    // create/edit form above). Settings/achievements/history callbacks
    // route to Settings (ported) and the honest placeholder respectively --
    // there is no ported AchievementsScreen/TestHistoryScreen yet.
    composable(SSBMaxDestinations.StudentProfile.route) {
        val notYetPorted: (String) -> Unit = { screen ->
            navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute(screen))
        }
        StudentProfileScreen(
            onNavigateToSettings = { navController.navigate(SSBMaxDestinations.Settings.route) },
            onNavigateToAchievements = { notYetPorted("AchievementsScreen") },
            onNavigateToHistory = {
                navController.navigate(SSBMaxDestinations.HistoricResults.route)
            }
        )
    }

    // Settings vertical. onNavigateToFAQ routes to the honest placeholder --
    // there is no ported FAQScreen yet. onNavigateToUpgrade/
    // onNavigateToSubscriptionManagement route to the real UpgradeScreen/
    // SubscriptionManagementScreen registered below.
    composable(SSBMaxDestinations.Settings.route) {
        val notYetPorted: (String) -> Unit = { screen ->
            navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute(screen))
        }
        SettingsScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToFAQ = { notYetPorted("FAQScreen") },
            onNavigateToUpgrade = {
                navController.navigate(SSBMaxDestinations.UpgradeScreen.route)
            },
            onNavigateToSubscriptionManagement = {
                navController.navigate(SSBMaxDestinations.SubscriptionManagement.route)
            }
        )
    }

    // Subscription Management. onUpgrade routes to the real UpgradeScreen
    // below -- note this loses the `tier` argument (the Android original's
    // `com.ssbmax.ui.settings.SubscriptionManagementScreen` does the exact
    // same thing: `onUpgrade = { tier -> navController.navigate(...UpgradeScreen.route) }`
    // in `SharedNavGraph.kt`, ignoring `tier` too -- not a port regression).
    // [UpgradeScreen] itself is "visual only" (see its own class doc):
    // PlayBillingClient/StoreKitBillingClient exist as Phase 4 shims but
    // are NOT wired into SubscriptionManager -- a known, separately-tracked
    // gap that matches the Android original having no working purchase
    // flow either (its own upgrade button just shows a "Coming Soon" dialog).
    composable(SSBMaxDestinations.SubscriptionManagement.route) {
        SubscriptionManagementScreen(
            onNavigateBack = { navController.navigateUp() },
            onUpgrade = { _ ->
                navController.navigate(SSBMaxDestinations.UpgradeScreen.route)
            }
        )
    }

    // Premium Upgrade screen -- the KMP port of the Android LIVE
    // `com.ssbmax.ui.premium.UpgradeScreen` (route "premium/upgrade"). See
    // [UpgradeScreen]'s own class doc for why the sibling Android
    // `com.ssbmax.ui.upgrade`/`com.ssbmax.ui.payment` packages (dead code,
    // unreachable from any Android nav graph) were NOT ported.
    composable(SSBMaxDestinations.UpgradeScreen.route) {
        UpgradeScreen(
            onNavigateBack = { navController.navigateUp() }
        )
    }
}
