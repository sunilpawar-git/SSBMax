package com.ssbmax.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.ssbmax.shared.domain.model.TestPhase
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.ui.auth.LoginScreen
import com.ssbmax.shared.ui.auth.RoleSelectionScreen
import com.ssbmax.shared.ui.home.instructor.InstructorHomeScreen
import com.ssbmax.shared.ui.home.student.StudentHomeScreen
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
 * `app/.../navigation/{NavGraph,AuthNavGraph,Student/InstructorNavGraph}.kt`
 * set, scoped to exactly the screens ported into `shared/commonMain/ui` so
 * far: Splash -> Login -> RoleSelection -> Student/Instructor home. The
 * other 59 screens (student tests/submissions/study, instructor
 * grading/analytics/batches, all Phase 1/2 test flows, interview, GTO,
 * etc.) are NOT reachable from here — this is not an oversight, they simply
 * haven't been ported yet (Phase 5 continues). Every sub-navigation
 * callback the two ported home screens expose (topic detail, phase detail,
 * result screens, notifications, marketplace, analytics, grading, batches,
 * student/batch detail) routes to the single [SSBMaxDestinations.NotYetPorted]
 * destination with the intended screen's display name, rather than
 * navigating to a route this graph never registered (which would crash Nav
 * Compose's destination lookup) or being silently dropped — the graph is
 * honestly navigable end-to-end today without pretending unported work is
 * done.
 *
 * Used directly by the iOS entry point ([com.ssbmax.shared.ui.MainViewController]),
 * which has no other nav graph. On Android, this graph is NOT yet the
 * `app` module's production entry point (`SSBMaxApp`/`MainActivity` still
 * use the old Android-only graph, which now calls into these same ported
 * `shared` composables for Splash/Login/RoleSelection/Student+InstructorHome
 * — see `app/.../navigation/{AuthNavGraph,Student/InstructorNavGraph}.kt`)
 * — swapping the whole app over to this graph is gated on porting the
 * remaining screens, not this phase.
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

        composable(SSBMaxDestinations.StudentHome.route) {
            val notYetPorted: (String) -> Unit = { screen ->
                navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute(screen))
            }
            StudentHomeScreen(
                onNavigateToTopic = { notYetPorted("TopicScreen") },
                onNavigateToPhaseDetail = { phase ->
                    notYetPorted(if (phase == TestPhase.PHASE_1) "Phase1DetailScreen" else "Phase2DetailScreen")
                },
                onNavigateToStudy = { notYetPorted("StudyMaterialsScreen") },
                onNavigateToSubmissions = { notYetPorted("SubmissionsListScreen") },
                onNavigateToNotifications = { notYetPorted("NotificationCenterScreen") },
                onNavigateToMarketplace = { notYetPorted("MarketplaceScreen") },
                onNavigateToAnalytics = { notYetPorted("AnalyticsScreen") },
                onNavigateToResult = { _: TestType, _: String -> notYetPorted("TestResultScreen") },
                onOpenDrawer = { notYetPorted("NavigationDrawer") }
            )
        }

        composable(SSBMaxDestinations.InstructorHome.route) {
            val notYetPorted: (String) -> Unit = { screen ->
                navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute(screen))
            }
            InstructorHomeScreen(
                onNavigateToStudent = { notYetPorted("StudentDetailScreen") },
                onNavigateToGrading = { notYetPorted("GradingQueueScreen") },
                onNavigateToBatchDetail = { notYetPorted("BatchDetailScreen") },
                onNavigateToCreateBatch = { notYetPorted("CreateBatchScreen") },
                onOpenDrawer = { notYetPorted("NavigationDrawer") }
            )
        }

        composable(SSBMaxDestinations.UserProfile.route) {
            NotYetPortedScreen("UserProfileScreen")
        }

        composable(
            route = SSBMaxDestinations.NotYetPorted.route,
            arguments = listOf(
                navArgument("screen") {
                    type = NavType.StringType
                    defaultValue = "Screen"
                }
            )
        ) { backStackEntry ->
            // `NavBackStackEntry.arguments` is a multiplatform `SavedState` (androidx.savedstate),
            // not the Android-only `Bundle.getString(...)` API -- read via the `SavedStateReader`
            // extension, which is the actual common-target-safe accessor (verified against
            // androidx.savedstate 1.3.0-beta01 sources; `Bundle.getString` doesn't compile for iOS).
            val screen = backStackEntry.arguments?.read { getStringOrNull("screen") } ?: "Screen"
            NotYetPortedScreen(screen)
        }
    }
}
