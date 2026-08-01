package com.ssbmax.navigation

import androidx.navigation.compose.composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.ssbmax.shared.ui.grading.TestDetailGradingScreen
import com.ssbmax.shared.ui.instructorgrading.BatchDetailScreen
import com.ssbmax.shared.ui.instructorgrading.CreateBatchScreen
import com.ssbmax.shared.ui.instructorgrading.GradingQueueScreen
import com.ssbmax.shared.ui.instructorgrading.InstructorAnalyticsScreen
import com.ssbmax.shared.ui.instructorgrading.InstructorStudentsScreen
import com.ssbmax.shared.ui.instructorgrading.StudentDetailScreen

fun NavGraphBuilder.instructorVerticalGraph(navController: NavHostController) {
    composable<SSBMaxDestinations.InstructorStudents> {
        InstructorStudentsScreen(
            onNavigateBack = { navController.navigateUp() },
            onStudentClick = { studentId ->
                navController.navigate(SSBMaxDestinations.StudentDetail(studentId))
            }
        )
    }

    composable<SSBMaxDestinations.InstructorGrading> {
        GradingQueueScreen(
            onSubmissionClick = { submissionId ->
                navController.navigate(SSBMaxDestinations.InstructorGradingDetail(submissionId))
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    composable<SSBMaxDestinations.InstructorAnalytics> {
        InstructorAnalyticsScreen(
            onNavigateBack = { navController.navigateUp() }
        )
    }

    composable<SSBMaxDestinations.CreateBatch> {
        CreateBatchScreen(
            onNavigateBack = { navController.navigateUp() },
            onBatchCreated = { batchId ->
                navController.navigate(SSBMaxDestinations.BatchDetail(batchId)) {
                    popUpTo<SSBMaxDestinations.CreateBatch> { inclusive = true }
                }
            }
        )
    }

    composable<SSBMaxDestinations.BatchDetail> { backStackEntry ->
        val batchId = backStackEntry.toRoute<SSBMaxDestinations.BatchDetail>().batchId
        BatchDetailScreen(
            batchId = batchId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToStudent = { studentId ->
                navController.navigate(SSBMaxDestinations.StudentDetail(studentId))
            }
        )
    }

    composable<SSBMaxDestinations.StudentDetail> { backStackEntry ->
        val studentId = backStackEntry.toRoute<SSBMaxDestinations.StudentDetail>().studentId
        StudentDetailScreen(
            studentId = studentId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToSubmission = { submissionId ->
                navController.navigate(SSBMaxDestinations.SubmissionDetail(submissionId))
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
    composable<SSBMaxDestinations.InstructorGradingDetail> { backStackEntry ->
        val submissionId = backStackEntry.toRoute<SSBMaxDestinations.InstructorGradingDetail>().submissionId
        TestDetailGradingScreen(
            submissionId = submissionId,
            onNavigateBack = { navController.navigateUp() }
        )
    }
}
