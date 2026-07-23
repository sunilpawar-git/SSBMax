package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.ssbmax.shared.ui.grading.TestDetailGradingScreen
import com.ssbmax.shared.ui.instructorgrading.BatchDetailScreen
import com.ssbmax.shared.ui.instructorgrading.CreateBatchScreen
import com.ssbmax.shared.ui.instructorgrading.GradingQueueScreen
import com.ssbmax.shared.ui.instructorgrading.InstructorAnalyticsScreen
import com.ssbmax.shared.ui.instructorgrading.InstructorStudentsScreen
import com.ssbmax.shared.ui.instructorgrading.StudentDetailScreen

fun NavGraphBuilder.instructorVerticalGraph(navController: NavHostController) {
    composable(SSBMaxDestinations.InstructorStudents.route) {
        InstructorStudentsScreen(
            onNavigateBack = { navController.navigateUp() },
            onStudentClick = { studentId ->
                navController.navigate(SSBMaxDestinations.StudentDetail.createRoute(studentId))
            }
        )
    }

    composable(SSBMaxDestinations.InstructorGrading.route) {
        GradingQueueScreen(
            onSubmissionClick = { submissionId ->
                navController.navigate(SSBMaxDestinations.InstructorGradingDetail.createRoute(submissionId))
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    composable(SSBMaxDestinations.InstructorAnalytics.route) {
        InstructorAnalyticsScreen(
            onNavigateBack = { navController.navigateUp() }
        )
    }

    composable(SSBMaxDestinations.CreateBatch.route) {
        CreateBatchScreen(
            onNavigateBack = { navController.navigateUp() },
            onBatchCreated = { batchId ->
                navController.navigate(SSBMaxDestinations.BatchDetail.createRoute(batchId)) {
                    popUpTo(SSBMaxDestinations.CreateBatch.route) { inclusive = true }
                }
            }
        )
    }

    composable(
        route = SSBMaxDestinations.BatchDetail.route,
        arguments = listOf(navArgument("batchId") { type = NavType.StringType })
    ) { backStackEntry ->
        val batchId = backStackEntry.arguments?.read { getStringOrNull("batchId") } ?: ""
        BatchDetailScreen(
            batchId = batchId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToStudent = { studentId ->
                navController.navigate(SSBMaxDestinations.StudentDetail.createRoute(studentId))
            }
        )
    }

    composable(
        route = SSBMaxDestinations.StudentDetail.route,
        arguments = listOf(navArgument("studentId") { type = NavType.StringType })
    ) { backStackEntry ->
        val studentId = backStackEntry.arguments?.read { getStringOrNull("studentId") } ?: ""
        StudentDetailScreen(
            studentId = studentId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToSubmission = { submissionId ->
                navController.navigate(SSBMaxDestinations.SubmissionDetail.createRoute(submissionId))
            }
        )
    }

    // Instructor grading detail (grade a single pending submission).
    // Registered and fully functional if navigated to directly, but
    // `InstructorHomeScreen`'s own `onNavigateToGrading` targets a
    // not-yet-ported *queue/list* screen (`GradingQueueScreen`, still
    // routing to the honest placeholder above) rather than this detail
    // screen directly -- same "registered, no in-graph entry point yet"
    // shape as this migration's OIR/PPDT start-test gaps.
    composable(
        route = SSBMaxDestinations.InstructorGradingDetail.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.read { getStringOrNull("submissionId") } ?: ""
        TestDetailGradingScreen(
            submissionId = submissionId,
            onNavigateBack = { navController.navigateUp() }
        )
    }
}
