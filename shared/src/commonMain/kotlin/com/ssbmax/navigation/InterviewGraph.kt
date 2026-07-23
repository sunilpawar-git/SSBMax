package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.ssbmax.shared.ui.interviewresult.InterviewResultScreen
import com.ssbmax.shared.ui.interviewsession.InterviewSessionScreen
import com.ssbmax.shared.ui.interviewstart.StartInterviewScreen

fun NavGraphBuilder.interviewGraph(navController: NavHostController) {
    // Interview vertical (start/session/result). Reachability gap, named
    // explicitly (same shape as every other vertical): `StudentHomeScreen`
    // has no "start new interview" callback at all (the Android original's own
    // `StartInterviewScreen` is reached via a route not exposed through any ported
    // home-screen callback yet), so `StartInterview` is registered but not reachable
    // from Student Home -- only via direct navigation/deep link. `InterviewResult` IS
    // reachable via `StudentHomeScreen`'s "view past interview result" tile
    // (`onNavigateToResult` with `TestType.IO`, wired in HomeGraph). See
    // [com.ssbmax.shared.presentation.interviewsession.InterviewCompleter]'s doc
    // comment for this vertical's own async-analysis finding: a completed interview
    // session persists correctly but is not yet AI-analyzed through this path, same
    // as every other Phase 5 vertical. This vertical's mic/audio-recording
    // investigation found no audio recording anywhere in the Android original (plain
    // text input + platform keyboard voice-to-text), so no new platform shim was
    // needed here at all.
    composable(SSBMaxDestinations.StartInterview.route) {
        StartInterviewScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToSession = { sessionId ->
                navController.navigate(SSBMaxDestinations.VoiceInterviewSession.createRoute(sessionId)) {
                    popUpTo(SSBMaxDestinations.StartInterview.route) { inclusive = true }
                }
            }
        )
    }

    composable(
        route = SSBMaxDestinations.VoiceInterviewSession.route,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val sessionId = backStackEntry.arguments?.read { getStringOrNull("sessionId") } ?: ""
        InterviewSessionScreen(
            sessionId = sessionId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToResult = { resultId ->
                navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(resultId)) {
                    popUpTo(SSBMaxDestinations.VoiceInterviewSession.createRoute(sessionId)) { inclusive = true }
                }
            },
            onNavigateToHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }

    composable(
        route = SSBMaxDestinations.InterviewResult.route,
        arguments = listOf(navArgument("resultId") { type = NavType.StringType })
    ) { backStackEntry ->
        val resultId = backStackEntry.arguments?.read { getStringOrNull("resultId") } ?: ""
        InterviewResultScreen(
            resultId = resultId,
            onNavigateBack = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }
}
