package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.ssbmax.shared.ui.notifications.NotificationCenterScreen
import com.ssbmax.shared.ui.phase.Phase1DetailScreen
import com.ssbmax.shared.ui.phase.Phase2DetailScreen
import com.ssbmax.shared.ui.placeholder.NotYetPortedScreen
import com.ssbmax.shared.ui.ssboverview.SSBOverviewScreen
import com.ssbmax.shared.ui.study.StudyMaterialDetailScreen
import com.ssbmax.shared.ui.study.StudyMaterialsScreen
import com.ssbmax.shared.ui.topic.TopicScreen

fun NavGraphBuilder.studyContentGraph(navController: NavHostController) {
    // Study Materials vertical, reachable from StudentHomeScreen's
    // onNavigateToStudy (wired in HomeGraph). onNavigateToTopic routes to the
    // real Topic screen. onNavigateToSearch/onNavigateToBookmarks have no
    // ported destination -- both are bare no-op default parameters in the
    // Android original too.
    composable(SSBMaxDestinations.StudyMaterialsList.route) {
        StudyMaterialsScreen(
            onNavigateToTopic = { topicName ->
                navController.navigate(SSBMaxDestinations.TopicScreen.createRoute(topicName))
            }
        )
    }

    // Student Study (KMP-convergence Phase 3a, row #9) -- a declared
    // `BottomNavItem` (`student/study`) that, before this fix, was
    // Android-only: `StudentNavGraph.kt`'s own registration renders the same
    // `StudyMaterialsScreen` as `StudyMaterialsList` above, just reached from
    // the bottom nav bar rather than Student Home's "Study" quick action.
    // Same no-op rationale for onNavigateToSearch/onNavigateToBookmarks.
    composable(SSBMaxDestinations.StudentStudy.route) {
        StudyMaterialsScreen(
            onNavigateToTopic = { topicId ->
                navController.navigate(SSBMaxDestinations.TopicScreen.createRoute(topicId))
            }
        )
    }

    composable(
        route = SSBMaxDestinations.StudyMaterialDetail.route,
        arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
    ) { backStackEntry ->
        val categoryId = backStackEntry.arguments?.read { getStringOrNull("categoryId") } ?: ""
        StudyMaterialDetailScreen(
            categoryId = categoryId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToRelatedMaterial = { relatedId ->
                navController.navigate(SSBMaxDestinations.StudyMaterialDetail.createRoute(relatedId))
            }
        )
    }

    // Topic -- 3-tab detail screen (Overview/Study Material/Tests) reachable
    // from StudentHomeScreen/Phase1Detail/Phase2Detail/StudyMaterialsScreen's
    // onNavigateToTopic (all wired). onNavigateToStudyMaterial routes to the
    // real StudyMaterialDetail screen. onNavigateToTest maps the testId
    // string (e.g. "oir_standard") back to the matching registered test route
    // where one exists; GTO's 5 not-yet-ported sub-tests (PGT/HGT/GOR/IO/CT)
    // and PPDT (no testId-keyed route registered under that exact prefix)
    // fall through to the honest placeholder. onNavigateToInterviewResult
    // routes to the real InterviewResult screen.
    //
    // `?selectedTab={selectedTab}` (KMP-convergence Phase 3a, row #2): mirrors
    // the Android original's own route registration (`SharedNavGraph.kt:576-590`).
    // `StudentHomeScreen`'s onTopicClick emits e.g. "OIR?selectedTab=2" (Tests
    // tab preselected) -- without this arg declared, that string had no
    // matching destination and silently failed to navigate.
    composable(
        route = SSBMaxDestinations.TopicScreen.route + "?selectedTab={selectedTab}",
        arguments = listOf(
            navArgument("topicId") { type = NavType.StringType },
            navArgument("selectedTab") {
                type = NavType.IntType
                defaultValue = 0
            }
        )
    ) { backStackEntry ->
        val topicId = backStackEntry.arguments?.read { getStringOrNull("topicId") } ?: "OIR"
        val selectedTab = backStackEntry.arguments?.read { getIntOrNull("selectedTab") } ?: 0
        TopicScreen(
            topicId = topicId,
            initialTab = selectedTab,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToStudyMaterial = { materialId ->
                navController.navigate(SSBMaxDestinations.StudyMaterialDetail.createRoute(materialId))
            },
            onNavigateToTest = { testId ->
                val route = when {
                    testId.startsWith("oir") -> SSBMaxDestinations.OIRTest.createRoute(testId)
                    testId.startsWith("ppdt") -> SSBMaxDestinations.PPDTTest.createRoute(testId)
                    testId.startsWith("tat") -> SSBMaxDestinations.TATTest.createRoute(testId)
                    testId.startsWith("wat") -> SSBMaxDestinations.WATTest.createRoute(testId)
                    testId.startsWith("srt") -> SSBMaxDestinations.SRTTest.createRoute(testId)
                    testId.startsWith("sd") -> SSBMaxDestinations.SDTest.createRoute(testId)
                    testId.startsWith("piq") -> SSBMaxDestinations.PIQTest.createRoute(testId)
                    testId.startsWith("gto_gd") -> SSBMaxDestinations.GTOGDTest.createRoute(testId)
                    testId.startsWith("gto_lecturette") -> SSBMaxDestinations.GTOLecturetteTest.createRoute(testId)
                    testId.startsWith("gto_gpe") -> SSBMaxDestinations.GTOGPETest.createRoute(testId)
                    testId.startsWith("io") -> SSBMaxDestinations.StartInterview.route
                    else -> SSBMaxDestinations.NotYetPorted.createRoute("Test($testId)")
                }
                navController.navigate(route)
            },
            onNavigateToInterviewResult = { resultId ->
                navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(resultId))
            }
        )
    }

    // Notification Center, reachable from StudentHomeScreen's
    // onNavigateToNotifications (wired in HomeGraph). onNotificationClick has
    // no ported per-notification deep-link destination yet -- routes to the
    // honest placeholder, same as the Android original's own bare
    // `onNotificationClick = {}` default parameter (no real caller wires it
    // to anything either).
    composable(SSBMaxDestinations.NotificationCenter.route) {
        NotificationCenterScreen(
            onNavigateBack = { navController.navigateUp() },
            onNotificationClick = { notification ->
                navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute("NotificationDeepLink(${notification.actionUrl})"))
            }
        )
    }

    // Phase 1/2 Detail -- reachable from StudentHomeScreen's
    // onNavigateToPhaseDetail (wired in HomeGraph). onNavigateToTopic routes
    // to the real Topic screen -- a student can finally reach an actual
    // test-taking screen from Student Home via Phase1/2Detail -> Topic ->
    // Tests tab -> onNavigateToTest, closing the OIR/PPDT/TAT/etc. "no path
    // to start a new test" gap documented in HomeGraph (Topic's own
    // onNavigateToTest still routes to the honest placeholder above, since
    // none of the individual test screens are wired from here by testId
    // string yet -- see Topic's own registration above for exactly which
    // test types ARE reachable that way).
    composable(SSBMaxDestinations.Phase1Detail.route) {
        Phase1DetailScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToTopic = { topicId ->
                navController.navigate(SSBMaxDestinations.TopicScreen.createRoute(topicId))
            }
        )
    }

    composable(SSBMaxDestinations.Phase2Detail.route) {
        Phase2DetailScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToTopic = { topicId ->
                navController.navigate(SSBMaxDestinations.TopicScreen.createRoute(topicId))
            }
        )
    }

    // SSB Overview -- static/educational content about the SSB selection
    // process. Reachable via the navigation drawer's "Overview of SSB"
    // item ([com.ssbmax.shared.ui.components.drawer.DrawerContent]'s
    // `onNavigateToSSBOverview`, wired in
    // [com.ssbmax.shared.ui.components.SSBMaxAppScaffold]) -- previously
    // registered but reachable only by direct navigation/deep link, since the
    // drawer wasn't ported yet.
    composable(SSBMaxDestinations.SSBOverview.route) {
        SSBOverviewScreen(
            onNavigateBack = { navController.navigateUp() }
        )
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
