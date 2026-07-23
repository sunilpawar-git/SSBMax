package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.ssbmax.shared.domain.model.TestPhase
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.ui.analytics.AnalyticsScreen
import com.ssbmax.shared.ui.marketplace.MarketplaceScreen
import com.ssbmax.shared.ui.results.HistoricResultsScreen
import com.ssbmax.shared.ui.studenttests.StudentTestsScreen
import com.ssbmax.shared.ui.submissions.SubmissionDetailScreen
import com.ssbmax.shared.ui.submissions.SubmissionsListScreen

fun NavGraphBuilder.submissionsResultsGraph(navController: NavHostController) {
    // Marketplace -- coaching-institute directory, mock data only (no backend
    // on either platform). onInstituteClick routes to the honest placeholder --
    // there is no ported institute-detail screen yet (the Android original's
    // own `onInstituteClick` callback is also just a `// TODO: Navigate to
    // institute detail` no-op).
    composable(SSBMaxDestinations.Marketplace.route) {
        MarketplaceScreen(
            onNavigateBack = { navController.navigateUp() },
            onInstituteClick = { instituteId ->
                navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute("InstituteDetail($instituteId)"))
            }
        )
    }

    // Results/Submissions/Grading/Analytics vertical. HistoricResults'
    // `onResultClick` mirrors HomeGraph's `onNavigateToResult` switch: OIR/
    // PPDT/TAT/WAT/SRT/SD/IO are the test-type result screens ported into
    // commonMain/ui so far -- every other test type still routes to the
    // honest placeholder. Reachable from `StudentProfileScreen`'s "history"
    // tile (wired in ProfileSettingsGraph).
    composable(SSBMaxDestinations.HistoricResults.route) {
        val notYetPorted: (String) -> Unit = { screen ->
            navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute(screen))
        }
        HistoricResultsScreen(
            onNavigateBack = { navController.navigateUp() },
            onResultClick = { submissionId, testType ->
                when (testType) {
                    TestType.OIR -> navController.navigate(SSBMaxDestinations.OIRTestResult.createRoute(submissionId))
                    TestType.PPDT -> navController.navigate(SSBMaxDestinations.PPDTSubmissionResult.createRoute(submissionId))
                    TestType.TAT -> navController.navigate(SSBMaxDestinations.TATSubmissionResult.createRoute(submissionId))
                    TestType.WAT -> navController.navigate(SSBMaxDestinations.WATSubmissionResult.createRoute(submissionId))
                    TestType.SRT -> navController.navigate(SSBMaxDestinations.SRTSubmissionResult.createRoute(submissionId))
                    TestType.SD -> navController.navigate(SSBMaxDestinations.SDSubmissionResult.createRoute(submissionId))
                    TestType.IO -> navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(submissionId))
                    else -> notYetPorted("TestResultScreen")
                }
            }
        )
    }

    // Submissions list, reachable from `StudentHomeScreen`'s
    // `onNavigateToSubmissions` (wired in HomeGraph). `onNavigateToTests` now
    // routes to the real "All Tests" overview screen.
    composable(SSBMaxDestinations.StudentSubmissions.route) {
        SubmissionsListScreen(
            onSubmissionClick = { submissionId ->
                navController.navigate(SSBMaxDestinations.SubmissionDetail.createRoute(submissionId))
            },
            onNavigateBack = { navController.navigateUp() },
            onNavigateToTests = {
                navController.navigate(SSBMaxDestinations.StudentTests.route)
            }
        )
    }

    // Student "All Tests" overview, reachable both from
    // SubmissionsListScreen's onNavigateToTests (wired above) and from the
    // bottom nav bar's "Tests" tab
    // ([com.ssbmax.shared.ui.components.SSBMaxAppScaffold] ->
    // [com.ssbmax.shared.ui.components.SSBMaxBottomBar]). onNavigateToPhase
    // routes to the real Phase1Detail/Phase2Detail screens; onNavigateToTest
    // routes to the honest placeholder for every GTO sub-test type not yet
    // individually reachable from here (GD/Lecturette/GPE already have their
    // own routes registered in GTOGraph, but this screen doesn't yet
    // distinguish which GTO card maps to which -- same simplification the
    // Android original's own nav graph makes, since
    // `StudentTestsScreen`'s `onNavigateToTest` isn't wired to anything in
    // `SharedNavGraph.kt` either).
    composable(SSBMaxDestinations.StudentTests.route) {
        StudentTestsScreen(
            onNavigateToPhase = { phase ->
                val route = if (phase == TestPhase.PHASE_1) {
                    SSBMaxDestinations.Phase1Detail.route
                } else {
                    SSBMaxDestinations.Phase2Detail.route
                }
                navController.navigate(route)
            },
            onNavigateToTest = { testType ->
                navController.navigate(SSBMaxDestinations.NotYetPorted.createRoute("Test($testType)"))
            }
        )
    }

    // Submission detail (student's own view of a single submission),
    // reachable from `SubmissionsListScreen` above.
    composable(
        route = SSBMaxDestinations.SubmissionDetail.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
        SubmissionDetailScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }

    // Student performance analytics dashboard, reachable from
    // `StudentHomeScreen`'s `onNavigateToAnalytics` (wired in HomeGraph).
    composable(SSBMaxDestinations.Analytics.route) {
        AnalyticsScreen(
            onNavigateBack = { navController.navigateUp() }
        )
    }
}
