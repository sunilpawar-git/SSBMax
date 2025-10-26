package com.ssbmax.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

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
 */
fun NavGraphBuilder.instructorNavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
    // Instructor Home
    composable(SSBMaxDestinations.InstructorHome.route) {
        com.ssbmax.ui.home.instructor.InstructorHomeScreen(
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
        // TODO: Implement InstructorStudentsScreen
        InstructorPlaceholderScreen(title = "Instructor Students")
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
        // TODO: Implement InstructorAnalyticsScreen
        InstructorPlaceholderScreen(title = "Instructor Analytics")
    }
    
    // Create Batch
    composable(SSBMaxDestinations.CreateBatch.route) {
        // TODO: Implement CreateBatchScreen
        InstructorPlaceholderScreen(title = "Create Batch")
    }
    
    // Batch Detail
    composable(
        route = SSBMaxDestinations.BatchDetail.route,
        arguments = listOf(navArgument("batchId") { type = NavType.StringType })
    ) { backStackEntry ->
        val batchId = backStackEntry.arguments?.getString("batchId") ?: ""
        // TODO: Implement BatchDetailScreen
        InstructorPlaceholderScreen(title = "Batch: $batchId")
    }
    
    // Student Detail (for instructors)
    composable(
        route = SSBMaxDestinations.StudentDetail.route,
        arguments = listOf(navArgument("studentId") { type = NavType.StringType })
    ) { backStackEntry ->
        val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
        // TODO: Implement StudentDetailScreen
        InstructorPlaceholderScreen(title = "Student Details: $studentId")
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

/**
 * Temporary placeholder for instructor screens not yet implemented
 */
@Composable
private fun InstructorPlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}

