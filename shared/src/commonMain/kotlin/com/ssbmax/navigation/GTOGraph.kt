package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.ui.gto.gd.GDResultScreen
import com.ssbmax.shared.ui.gto.gd.GDTestScreen
import com.ssbmax.shared.ui.gto.gpe.GPEResultScreen
import com.ssbmax.shared.ui.gto.gpe.GPETestScreen
import com.ssbmax.shared.ui.gto.lecturette.LecturetteResultScreen
import com.ssbmax.shared.ui.gto.lecturette.LecturetteTestScreen

fun NavGraphBuilder.gtoGraph(navController: NavHostController) {
    // GTO - Group Discussion. Same reachability gap as every other test type:
    // `onNavigateToPhaseDetail` isn't ported, so there is no in-graph path to
    // *start* a new `GTOGDTest`, and the Android original's `GDResultScreen`
    // has no "retake" callback either. Both routes registered regardless, same
    // "no crash on unregistered destination" principle. (`HomeGraph`'s
    // `onNavigateToResult` switch on TestType.GTO_GD/LECTURETTE/GPE was the
    // gap noted here previously -- Phase 3a's row #1 closed it.)
    composable(
        route = SSBMaxDestinations.GTOGDTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "gto_gd_standard"
        GDTestScreen(
            testId = testId,
            onTestComplete = { submissionId, subscriptionType ->
                TestResultHandler.handleTestSubmission(
                    submissionId = submissionId,
                    subscriptionType = subscriptionType,
                    testType = TestType.GTO_GD,
                    navController = navController
                )
            },
            onNavigateBack = { navController.navigateUp() },
            onNavigateToUpgrade = { navController.navigate(SSBMaxDestinations.UpgradeScreen.route) }
        )
    }

    composable(
        route = SSBMaxDestinations.GTOGDResult.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
        GDResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }

    // GTO - Lecturette. Same shape as GD's gap above.
    composable(
        route = SSBMaxDestinations.GTOLecturetteTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "gto_lecturette_standard"
        LecturetteTestScreen(
            testId = testId,
            onTestComplete = { submissionId, subscriptionType ->
                TestResultHandler.handleTestSubmission(
                    submissionId = submissionId,
                    subscriptionType = subscriptionType,
                    testType = TestType.GTO_LECTURETTE,
                    navController = navController
                )
            },
            onNavigateBack = { navController.navigateUp() },
            onNavigateToUpgrade = { navController.navigate(SSBMaxDestinations.UpgradeScreen.route) }
        )
    }

    composable(
        route = SSBMaxDestinations.GTOLecturetteResult.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
        LecturetteResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }

    // GTO - Group Planning Exercise. Same shape as GD's/Lecturette's gap above.
    composable(
        route = SSBMaxDestinations.GTOGPETest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.read { getStringOrNull("testId") } ?: "gpe_standard"
        GPETestScreen(
            testId = testId,
            onTestComplete = { submissionId, subscriptionType ->
                TestResultHandler.handleTestSubmission(
                    submissionId = submissionId,
                    subscriptionType = subscriptionType,
                    testType = TestType.GTO_GPE,
                    navController = navController
                )
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    composable(
        route = SSBMaxDestinations.GTOGPEResult.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
        GPEResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }
}
