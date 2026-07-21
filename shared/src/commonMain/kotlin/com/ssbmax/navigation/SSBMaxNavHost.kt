package com.ssbmax.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.ui.auth.LoginScreen
import com.ssbmax.shared.ui.auth.RoleSelectionScreen
import com.ssbmax.shared.ui.placeholder.NotYetPortedScreen
import com.ssbmax.shared.ui.splash.SplashScreen

/**
 * commonMain NavHost — Phase 5's first real multi-screen Compose
 * Multiplatform navigation graph, using `org.jetbrains.androidx.navigation`
 * (same `androidx.navigation`/`androidx.navigation.compose` package as the
 * Android-only artifact `app` still uses for its own graph — see this
 * phase's exit report for why the two coexist rather than one replacing the
 * other yet).
 *
 * Structure is the commonMain-portable equivalent of the Android-only
 * `app/.../navigation/{NavGraph,AuthNavGraph}.kt` pair, scoped to exactly
 * the screens ported into `shared/commonMain/ui` so far: Splash -> Login ->
 * RoleSelection -> Student/Instructor home. The other 57 screens (student
 * tests/submissions/study, instructor grading/analytics, all Phase 1/2 test
 * flows, interview, GTO, etc.) are NOT reachable from here — this is not an
 * oversight, they simply haven't been ported yet (Phase 5 continues). Their
 * home-screen destinations route to [NotYetPortedScreen] rather than being
 * silently omitted or faked as working, so the graph is honestly navigable
 * end-to-end today without pretending unported work is done.
 *
 * Used directly by the iOS entry point ([com.ssbmax.shared.ui.MainViewController]),
 * which has no other nav graph. On Android, this graph is NOT yet the
 * `app` module's production entry point (`SSBMaxApp`/`MainActivity` still
 * use the old 5-file Android-only graph, which now calls into these same
 * ported `shared` composables for Splash/Login/RoleSelection — see
 * `app/.../navigation/AuthNavGraph.kt`) — swapping the whole app over to
 * this graph is gated on porting the remaining 57 screens, not this phase.
 */
@Composable
fun SSBMaxNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = SSBMaxDestinations.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(SSBMaxDestinations.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(SSBMaxDestinations.Login.route) {
                        popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = { isStudent ->
                    val destination = if (isStudent) {
                        SSBMaxDestinations.StudentHome.route
                    } else {
                        SSBMaxDestinations.InstructorHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToRoleSelection = {
                    navController.navigate(SSBMaxDestinations.RoleSelection.route) {
                        popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToProfileOnboarding = {
                    // UserProfileScreen isn't ported yet (Phase 5 continuation) --
                    // land on the honest placeholder rather than silently dropping
                    // the onboarding step.
                    navController.navigate(SSBMaxDestinations.UserProfile.route) {
                        popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(SSBMaxDestinations.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.Login.route) { inclusive = true }
                    }
                },
                onNeedsRoleSelection = {
                    navController.navigate(SSBMaxDestinations.RoleSelection.route)
                }
            )
        }

        composable(SSBMaxDestinations.RoleSelection.route) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    val destination = when {
                        role == UserRole.INSTRUCTOR -> SSBMaxDestinations.InstructorHome.route
                        else -> SSBMaxDestinations.StudentHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(SSBMaxDestinations.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }

        // Not yet ported (Phase 5 continuation) -- honest placeholders, not silent gaps.
        composable(SSBMaxDestinations.StudentHome.route) {
            NotYetPortedScreen("StudentHomeScreen")
        }
        composable(SSBMaxDestinations.InstructorHome.route) {
            NotYetPortedScreen("InstructorHomeScreen")
        }
        composable(SSBMaxDestinations.UserProfile.route) {
            NotYetPortedScreen("UserProfileScreen")
        }
    }
}
