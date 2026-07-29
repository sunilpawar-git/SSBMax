package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ssbmax.shared.ui.home.instructor.InstructorHomeScreen

/**
 * Instructor navigation graph
 * Contains all instructor-specific screens and flows
 *
 * This graph handles instructor user journeys:
 * - Instructor home dashboard
 * - Student management
 * - Grading queue and grading details
 * - Batch management
 * - Analytics
 *
 * Phase 5: Instructor Home now comes from `:shared`'s `commonMain/ui`
 * (Compose Multiplatform), not the old Android-only
 * `app/.../ui/home/instructor/InstructorHomeScreen.kt` (still exists,
 * unused by this graph now — same precedent as `studentNavGraph`). Every
 * other screen below this one in the graph is unchanged.
 */
fun NavGraphBuilder.instructorNavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    // Instructor Home
    composable(SSBMaxDestinations.InstructorHome.route) {
        InstructorHomeScreen(
            onNavigateToStudent = { studentId ->
                navController.navigate(SSBMaxDestinations.StudentDetail.createRoute(studentId))
            },
            onNavigateToGrading = {
                navController.navigate(SSBMaxDestinations.InstructorGrading.route)
            },
            onNavigateToBatchDetail = { batchId ->
                navController.navigate(SSBMaxDestinations.BatchDetail.createRoute(batchId))
            },
            onNavigateToCreateBatch = {
                navController.navigate(SSBMaxDestinations.CreateBatch.route)
            },
            onOpenDrawer = onOpenDrawer
        )
    }
    
    // Instructor Students
    composable(SSBMaxDestinations.InstructorStudents.route) {
        com.ssbmax.ui.instructor.InstructorStudentsScreen(
            onNavigateBack = { navController.navigateUp() }
        )
    }
    
    // Instructor Grading Queue
    composable(SSBMaxDestinations.InstructorGrading.route) {
        com.ssbmax.ui.instructor.GradingQueueScreen(
            onSubmissionClick = { submissionId ->
                navController.navigate(SSBMaxDestinations.InstructorGradingDetail.createRoute(submissionId))
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }
    
    // Instructor Analytics
    composable(SSBMaxDestinations.InstructorAnalytics.route) {
        com.ssbmax.ui.instructor.InstructorAnalyticsScreen(
            onNavigateBack = { navController.navigateUp() }
        )
    }
    
    // Create Batch
    composable(SSBMaxDestinations.CreateBatch.route) {
        com.ssbmax.ui.instructor.CreateBatchScreen(
            onNavigateBack = { navController.navigateUp() }
        )
    }
    
    // Batch Detail
    composable(
        route = SSBMaxDestinations.BatchDetail.route,
        arguments = listOf(navArgument("batchId") { type = NavType.StringType })
    ) { backStackEntry ->
        val batchId = backStackEntry.arguments?.getString("batchId") ?: ""
        com.ssbmax.ui.instructor.BatchDetailScreen(
            batchId = batchId,
            onNavigateBack = { navController.navigateUp() }
        )
    }
    
    // Student Detail (for instructors)
    composable(
        route = SSBMaxDestinations.StudentDetail.route,
        arguments = listOf(navArgument("studentId") { type = NavType.StringType })
    ) { backStackEntry ->
        val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
        com.ssbmax.ui.instructor.StudentDetailScreen(
            studentId = studentId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToSubmission = { submissionId ->
                navController.navigate(SSBMaxDestinations.SubmissionDetail.createRoute(submissionId))
            }
        )
    }
    
    // Instructor Grading Detail (View & Grade Submission)
    composable(
        route = SSBMaxDestinations.InstructorGradingDetail.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
        com.ssbmax.ui.grading.TestDetailGradingScreen(
            submissionId = submissionId,
            onNavigateBack = { navController.navigateUp() }
        )
    }
}

