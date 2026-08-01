package com.ssbmax.navigation

import androidx.navigation.compose.composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.ui.piq.PIQSubmissionResultScreen
import com.ssbmax.shared.ui.piq.PIQTestScreen
import com.ssbmax.shared.ui.sdt.SDTSubmissionResultScreen
import com.ssbmax.shared.ui.sdt.SDTTestScreen
import com.ssbmax.shared.ui.srt.SRTSubmissionResultScreen
import com.ssbmax.shared.ui.srt.SRTTestScreen

fun NavGraphBuilder.writtenTestsGraph(navController: NavHostController) {
    composable<SSBMaxDestinations.SRTTest> { backStackEntry ->
        val testId = backStackEntry.toRoute<SSBMaxDestinations.SRTTest>().testId
        SRTTestScreen(
            testId = testId,
            onTestComplete = { submissionId, subscriptionType ->
                // SRT is one of two test types (with SD) actually gated by
                // subscriptionType -- FREE/PRO wait for manual grading.
                TestResultHandler.handleTestSubmission(
                    submissionId = submissionId,
                    subscriptionType = subscriptionType,
                    testType = TestType.SRT,
                    navController = navController
                )
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    // SRT reachability gap, named explicitly (same shape as the WAT gap
    // documented in PsychTestsGraph): the Android original's `SRTSubmissionResultScreen`
    // has no "retake test" callback either (only `onNavigateHome`), so there is
    // genuinely no in-graph path back to `SRTTest` today -- it's reachable only
    // via `StudentHomeScreen`'s "view past SRT result" tile landing on
    // `SRTSubmissionResult`, which itself has no forward link to `SRTTest`. The
    // route is still registered here (not omitted) so a future direct-navigation
    // caller or deep link has somewhere real to land, consistent with this
    // graph's own "no crash on unregistered destination" principle.
    composable<SSBMaxDestinations.SRTSubmissionResult> { backStackEntry ->
        val submissionId = backStackEntry.toRoute<SSBMaxDestinations.SRTSubmissionResult>().submissionId
        SRTSubmissionResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome) {
                    popUpTo<SSBMaxDestinations.StudentHome> { inclusive = true }
                }
            }
        )
    }

    composable<SSBMaxDestinations.SDTest> { backStackEntry ->
        val testId = backStackEntry.toRoute<SSBMaxDestinations.SDTest>().testId
        SDTTestScreen(
            testId = testId,
            onTestComplete = { submissionId, subscriptionType ->
                // SD is the other test type gated by subscriptionType.
                TestResultHandler.handleTestSubmission(
                    submissionId = submissionId,
                    subscriptionType = subscriptionType,
                    testType = TestType.SD,
                    navController = navController
                )
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    // SDT reachability gap, named explicitly (same shape as the SRT gap
    // documented above): the Android original's `SDTSubmissionResultScreen`
    // has no "retake test" callback either (only `onNavigateHome`), so there is
    // genuinely no in-graph path back to `SDTest` today -- it's reachable only
    // via `StudentHomeScreen`'s "view past SDT result" tile landing on
    // `SDSubmissionResult`, which itself has no forward link to `SDTest`. The
    // route is still registered here (not omitted) so a future direct-navigation
    // caller or deep link has somewhere real to land, consistent with this
    // graph's own "no crash on unregistered destination" principle.
    composable<SSBMaxDestinations.SDSubmissionResult> { backStackEntry ->
        val submissionId = backStackEntry.toRoute<SSBMaxDestinations.SDSubmissionResult>().submissionId
        SDTSubmissionResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome) {
                    popUpTo<SSBMaxDestinations.StudentHome> { inclusive = true }
                }
            }
        )
    }

    // PIQ (Personal Information Questionnaire) -- untimed ~90-field form,
    // not a scored test; unlike every other test type in this graph, its
    // own screen calls `onNavigateToResult(submissionId)` directly (no
    // `onTestComplete(submissionId, subscriptionType)` -- PIQ has no
    // downstream use for subscriptionType, see PIQTestViewModel.kt's doc
    // comment). Same reachability gap as SRT/SDT above: no in-graph path
    // back to `PIQTest` from `PIQSubmissionResult` (no retake callback in
    // the Android original either) -- both routes registered regardless.
    composable<SSBMaxDestinations.PIQTest> { backStackEntry ->
        val testId = backStackEntry.toRoute<SSBMaxDestinations.PIQTest>().testId
        PIQTestScreen(
            testId = testId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToResult = { submissionId ->
                navController.navigate(SSBMaxDestinations.PIQSubmissionResult(submissionId)) {
                    popUpTo<SSBMaxDestinations.PIQTest> { inclusive = true }
                }
            }
        )
    }

    composable<SSBMaxDestinations.PIQSubmissionResult> { backStackEntry ->
        val submissionId = backStackEntry.toRoute<SSBMaxDestinations.PIQSubmissionResult>().submissionId
        PIQSubmissionResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome) {
                    popUpTo<SSBMaxDestinations.StudentHome> { inclusive = true }
                }
            }
        )
    }
}
