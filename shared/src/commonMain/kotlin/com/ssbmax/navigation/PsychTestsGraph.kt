package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.ssbmax.shared.ui.oir.OIRTestResultScreen
import com.ssbmax.shared.ui.oir.OIRTestScreen
import com.ssbmax.shared.ui.ppdt.PPDTSubmissionResultScreen
import com.ssbmax.shared.ui.ppdt.PPDTTestScreen
import com.ssbmax.shared.ui.tat.TATSubmissionResultScreen
import com.ssbmax.shared.ui.tat.TATTestScreen
import com.ssbmax.shared.ui.wat.WATSubmissionResultScreen
import com.ssbmax.shared.ui.wat.WATTestScreen

fun NavGraphBuilder.psychTestsGraph(navController: NavHostController) {
    composable(
        route = SSBMaxDestinations.OIRTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "oir_standard"
        OIRTestScreen(
            onTestComplete = { submissionId, _ ->
                navController.navigate(SSBMaxDestinations.OIRTestResult.createRoute(submissionId)) {
                    popUpTo(SSBMaxDestinations.OIRTest.createRoute(testId)) { inclusive = true }
                }
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    composable(
        route = SSBMaxDestinations.OIRTestResult.route,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.read { getStringOrNull("sessionId") } ?: ""
        OIRTestResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            },
            onRetakeTest = {
                navController.navigate(SSBMaxDestinations.OIRTest.createRoute("oir_standard")) {
                    popUpTo(SSBMaxDestinations.OIRTestResult.createRoute(submissionId)) { inclusive = true }
                }
            },
            onReviewAnswers = {
                // Review-answers screen isn't ported yet (same gap as the Android
                // original, which also just has a `// TODO` here) -- not a new gap
                // introduced by this port.
            }
        )
    }

    composable(
        route = SSBMaxDestinations.PPDTTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "ppdt_standard"
        PPDTTestScreen(
            testId = testId,
            onTestComplete = { submissionId, _ ->
                navController.navigate(SSBMaxDestinations.PPDTSubmissionResult.createRoute(submissionId)) {
                    popUpTo(SSBMaxDestinations.PPDTTest.createRoute(testId)) { inclusive = true }
                }
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    // PPDT reachability gap, named explicitly (same shape as the OIR gap
    // documented above): the Android original's `PPDTSubmissionResultScreen`
    // has no "retake test" callback either (only `onNavigateHome`), so unlike
    // OIR's result screen, there is genuinely no in-graph path back to
    // `PPDTTest` today -- it's reachable only via `StudentHomeScreen`'s
    // "view past PPDT result" tile landing on `PPDTSubmissionResult`, which
    // itself has no forward link to `PPDTTest`. The route is still registered
    // here (not omitted) so a future direct-navigation caller or deep link has
    // somewhere real to land, consistent with this graph's own "no crash on
    // unregistered destination" principle.
    composable(
        route = SSBMaxDestinations.PPDTSubmissionResult.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
        PPDTSubmissionResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }

    composable(
        route = SSBMaxDestinations.TATTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "tat_standard"
        TATTestScreen(
            testId = testId,
            onTestComplete = { submissionId, _ ->
                navController.navigate(SSBMaxDestinations.TATSubmissionResult.createRoute(submissionId)) {
                    popUpTo(SSBMaxDestinations.TATTest.createRoute(testId)) { inclusive = true }
                }
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    // TAT reachability gap, named explicitly (same shape as the PPDT gap
    // documented above): the Android original's `TATSubmissionResultScreen`
    // has no "retake test" callback either (only `onNavigateHome`), so there is
    // genuinely no in-graph path back to `TATTest` today -- it's reachable only
    // via `StudentHomeScreen`'s "view past TAT result" tile landing on
    // `TATSubmissionResult`, which itself has no forward link to `TATTest`. The
    // route is still registered here (not omitted) so a future direct-navigation
    // caller or deep link has somewhere real to land, consistent with this
    // graph's own "no crash on unregistered destination" principle.
    composable(
        route = SSBMaxDestinations.TATSubmissionResult.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
        TATSubmissionResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }

    composable(
        route = SSBMaxDestinations.WATTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "wat_standard"
        WATTestScreen(
            testId = testId,
            onTestComplete = { submissionId, _ ->
                navController.navigate(SSBMaxDestinations.WATSubmissionResult.createRoute(submissionId)) {
                    popUpTo(SSBMaxDestinations.WATTest.createRoute(testId)) { inclusive = true }
                }
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    // WAT reachability gap, named explicitly (same shape as the TAT gap
    // documented above): the Android original's `WATSubmissionResultScreen`
    // has no "retake test" callback either (only `onNavigateHome`), so there is
    // genuinely no in-graph path back to `WATTest` today -- it's reachable only
    // via `StudentHomeScreen`'s "view past WAT result" tile landing on
    // `WATSubmissionResult`, which itself has no forward link to `WATTest`. The
    // route is still registered here (not omitted) so a future direct-navigation
    // caller or deep link has somewhere real to land, consistent with this
    // graph's own "no crash on unregistered destination" principle.
    composable(
        route = SSBMaxDestinations.WATSubmissionResult.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
        WATSubmissionResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }
}
