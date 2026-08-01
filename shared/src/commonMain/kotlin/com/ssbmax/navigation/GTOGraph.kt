package com.ssbmax.navigation

import androidx.navigation.compose.composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
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
    composable<SSBMaxDestinations.GTOGDTest> { backStackEntry ->
        val testId = backStackEntry.toRoute<SSBMaxDestinations.GTOGDTest>().testId
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
            onNavigateToUpgrade = { navController.navigate(SSBMaxDestinations.UpgradeScreen) }
        )
    }

    composable<SSBMaxDestinations.GTOGDResult> { backStackEntry ->
        val submissionId = backStackEntry.toRoute<SSBMaxDestinations.GTOGDResult>().submissionId
        GDResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome) {
                    popUpTo<SSBMaxDestinations.StudentHome> { inclusive = true }
                }
            }
        )
    }

    // GTO - Lecturette. Same shape as GD's gap above.
    composable<SSBMaxDestinations.GTOLecturetteTest> { backStackEntry ->
        val testId = backStackEntry.toRoute<SSBMaxDestinations.GTOLecturetteTest>().testId
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
            onNavigateToUpgrade = { navController.navigate(SSBMaxDestinations.UpgradeScreen) }
        )
    }

    composable<SSBMaxDestinations.GTOLecturetteResult> { backStackEntry ->
        val submissionId = backStackEntry.toRoute<SSBMaxDestinations.GTOLecturetteResult>().submissionId
        LecturetteResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome) {
                    popUpTo<SSBMaxDestinations.StudentHome> { inclusive = true }
                }
            }
        )
    }

    // GTO - Group Planning Exercise. Same shape as GD's/Lecturette's gap above.
    composable<SSBMaxDestinations.GTOGPETest> { backStackEntry ->
        val testId = backStackEntry.toRoute<SSBMaxDestinations.GTOGPETest>().testId
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

    composable<SSBMaxDestinations.GTOGPEResult> { backStackEntry ->
        val submissionId = backStackEntry.toRoute<SSBMaxDestinations.GTOGPEResult>().submissionId
        GPEResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome) {
                    popUpTo<SSBMaxDestinations.StudentHome> { inclusive = true }
                }
            }
        )
    }
}
