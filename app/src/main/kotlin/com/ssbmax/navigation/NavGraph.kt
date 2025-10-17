package com.ssbmax.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.ui.auth.LoginScreen
import com.ssbmax.ui.auth.RoleSelectionScreen
import com.ssbmax.ui.splash.SplashScreen

/**
 * Main navigation graph for SSBMax app
 * Handles authentication flow and role-based navigation
 */
@Composable
fun SSBMaxNavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
    startDestination: String = SSBMaxDestinations.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Splash Screen
        composable(SSBMaxDestinations.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(SSBMaxDestinations.Login.route) {
                        popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = { isStudent ->
                    val destination = if (isStudent) {
                        SSBMaxDestinations.StudentHome.route
                    } else {
                        SSBMaxDestinations.InstructorHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToRoleSelection = {
                    navController.navigate(SSBMaxDestinations.RoleSelection.route) {
                        popUpTo(SSBMaxDestinations.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Login Screen
        composable(SSBMaxDestinations.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // TODO: Navigate based on user role from AuthViewModel
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.Login.route) { inclusive = true }
                    }
                },
                onNeedsRoleSelection = {
                    navController.navigate(SSBMaxDestinations.RoleSelection.route)
                }
            )
        }
        
        // Role Selection Screen
        composable(SSBMaxDestinations.RoleSelection.route) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    val destination = when {
                        role.isStudent -> SSBMaxDestinations.StudentHome.route
                        role.isInstructor -> SSBMaxDestinations.InstructorHome.route
                        else -> SSBMaxDestinations.StudentHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(SSBMaxDestinations.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }
        
        // ========================
        // STUDENT FLOW
        // ========================
        
        // Student Home
        composable(SSBMaxDestinations.StudentHome.route) {
            com.ssbmax.ui.home.student.StudentHomeScreen(
                onNavigateToTest = { testType ->
                    // TODO: Navigate to specific test based on type
                },
                onNavigateToPhaseDetail = { phase ->
                    when (phase) {
                        com.ssbmax.core.domain.model.TestPhase.PHASE_1 -> 
                            navController.navigate(SSBMaxDestinations.Phase1Detail.route)
                        com.ssbmax.core.domain.model.TestPhase.PHASE_2 -> 
                            navController.navigate(SSBMaxDestinations.Phase2Detail.route)
                    }
                },
                onNavigateToStudy = {
                    navController.navigate(SSBMaxDestinations.StudyMaterialsList.route)
                },
                onOpenDrawer = onOpenDrawer
            )
        }
        
        // Student Tests
        composable(SSBMaxDestinations.StudentTests.route) {
            com.ssbmax.ui.tests.StudentTestsScreen(
                onNavigateToPhase = { phase ->
                    when (phase) {
                        com.ssbmax.core.domain.model.TestPhase.PHASE_1 ->
                            navController.navigate(SSBMaxDestinations.Phase1Detail.route)
                        com.ssbmax.core.domain.model.TestPhase.PHASE_2 ->
                            navController.navigate(SSBMaxDestinations.Phase2Detail.route)
                    }
                },
                onNavigateToTest = { testType ->
                    // TODO: Navigate to specific test based on type
                }
            )
        }
        
        // Student Study
        composable(SSBMaxDestinations.StudentStudy.route) {
            com.ssbmax.ui.study.StudyMaterialsScreen(
                onNavigateToCategory = { category ->
                    // TODO: Navigate to category detail
                },
                onNavigateToSearch = {
                    // TODO: Navigate to search
                }
            )
        }
        
        // Student Profile
        composable(SSBMaxDestinations.StudentProfile.route) {
            com.ssbmax.ui.profile.StudentProfileScreen(
                onNavigateToSettings = {
                    // TODO: Navigate to settings
                },
                onNavigateToAchievements = {
                    // TODO: Navigate to achievements
                },
                onNavigateToHistory = {
                    // TODO: Navigate to test history
                }
            )
        }
        
        // ========================
        // INSTRUCTOR FLOW
        // ========================
        
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
            PlaceholderScreen(title = "Instructor Students")
        }
        
        // Instructor Grading Queue
        composable(SSBMaxDestinations.InstructorGrading.route) {
            com.ssbmax.ui.grading.InstructorGradingScreen(
                onNavigateToGrading = { submissionId ->
                    navController.navigate(SSBMaxDestinations.InstructorGradingDetail.createRoute(submissionId))
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Instructor Analytics
        composable(SSBMaxDestinations.InstructorAnalytics.route) {
            // TODO: Implement InstructorAnalyticsScreen
            PlaceholderScreen(title = "Instructor Analytics")
        }
        
        // ========================
        // PHASE SCREENS
        // ========================
        
        // Phase 1 Detail
        composable(SSBMaxDestinations.Phase1Detail.route) {
            com.ssbmax.ui.phase.Phase1DetailScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToTest = { testType ->
                    when (testType) {
                        com.ssbmax.core.domain.model.TestType.OIR ->
                            navController.navigate(SSBMaxDestinations.OIRTest.createRoute("oir_standard"))
                        com.ssbmax.core.domain.model.TestType.PPDT ->
                            navController.navigate(SSBMaxDestinations.PPDTTest.createRoute("ppdt_standard"))
                        else -> { /* TODO: Other tests */ }
                    }
                }
            )
        }
        
        // Phase 2 Detail
        composable(SSBMaxDestinations.Phase2Detail.route) {
            com.ssbmax.ui.phase.Phase2DetailScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToTest = { testType ->
                    // TODO: Navigate to specific test based on type
                }
            )
        }
        
        // ========================
        // TEST SCREENS
        // ========================
        
        // OIR Test
        composable(
            route = SSBMaxDestinations.OIRTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            com.ssbmax.ui.tests.oir.OIRTestScreen(
                onTestComplete = { sessionId ->
                    navController.navigate(SSBMaxDestinations.OIRTestResult.createRoute(sessionId)) {
                        popUpTo(SSBMaxDestinations.OIRTest.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // OIR Test Result
        composable(
            route = SSBMaxDestinations.OIRTestResult.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            com.ssbmax.ui.tests.oir.OIRTestResultScreen(
                sessionId = sessionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                },
                onRetakeTest = {
                    navController.navigate(SSBMaxDestinations.OIRTest.createRoute("oir_standard")) {
                        popUpTo(SSBMaxDestinations.OIRTestResult.route) { inclusive = true }
                    }
                },
                onReviewAnswers = {
                    // TODO: Navigate to review screen
                }
            )
        }
        
        // PPDT Test
        composable(
            route = SSBMaxDestinations.PPDTTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            com.ssbmax.ui.tests.ppdt.PPDTTestScreen(
                testId = testId,
                onTestComplete = { submissionId ->
                    navController.navigate(SSBMaxDestinations.PPDTSubmissionResult.createRoute(submissionId)) {
                        popUpTo(SSBMaxDestinations.PPDTTest.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // PPDT Submission Result
        composable(
            route = SSBMaxDestinations.PPDTSubmissionResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
            com.ssbmax.ui.tests.ppdt.PPDTSubmissionResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                },
                onViewFeedback = {
                    // TODO: Navigate to detailed feedback screen
                }
            )
        }
        
        // Psychology Test
        composable(
            route = SSBMaxDestinations.PsychologyTest.route,
            arguments = listOf(
                navArgument("testId") { type = NavType.StringType },
                navArgument("subTest") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            val subTest = backStackEntry.arguments?.getString("subTest") ?: ""
            // TODO: Implement PsychologyTestScreen
            PlaceholderScreen(title = "Psychology Test: $subTest")
        }
        
        // GTO Test
        composable(
            route = SSBMaxDestinations.GTOTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            // TODO: Implement GTOTestScreen
            PlaceholderScreen(title = "GTO Test: $testId")
        }
        
        // IO Test
        composable(
            route = SSBMaxDestinations.IOTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            // TODO: Implement IOTestScreen
            PlaceholderScreen(title = "IO Test: $testId")
        }
        
        // ========================
        // STUDY MATERIALS
        // ========================
        
        // Study Materials List
        composable(SSBMaxDestinations.StudyMaterialsList.route) {
            // TODO: Implement StudyMaterialsListScreen
            PlaceholderScreen(title = "Study Materials")
        }
        
        // Study Material Detail
        composable(
            route = SSBMaxDestinations.StudyMaterialDetail.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            // TODO: Implement StudyMaterialDetailScreen
            PlaceholderScreen(title = "Study Material: $categoryId")
        }
        
        // ========================
        // BATCH MANAGEMENT
        // ========================
        
        // Join Batch
        composable(SSBMaxDestinations.JoinBatch.route) {
            // TODO: Implement JoinBatchScreen
            PlaceholderScreen(title = "Join Batch")
        }
        
        // Create Batch
        composable(SSBMaxDestinations.CreateBatch.route) {
            // TODO: Implement CreateBatchScreen
            PlaceholderScreen(title = "Create Batch")
        }
        
        // Batch Detail
        composable(
            route = SSBMaxDestinations.BatchDetail.route,
            arguments = listOf(navArgument("batchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getString("batchId") ?: ""
            // TODO: Implement BatchDetailScreen
            PlaceholderScreen(title = "Batch: $batchId")
        }
        
        // ========================
        // INSTRUCTOR SPECIFIC
        // ========================
        
        // Student Detail (for instructors)
        composable(
            route = SSBMaxDestinations.StudentDetail.route,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            // TODO: Implement StudentDetailScreen
            PlaceholderScreen(title = "Student Details: $studentId")
        }
        
        // Instructor Grading Detail (View & Grade Submission)
        composable(
            route = SSBMaxDestinations.InstructorGradingDetail.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
            com.ssbmax.ui.grading.TestDetailGradingScreen(
                submissionId = submissionId,
                onNavigateBack = { navController.navigateUp() },
                onGradingComplete = {
                    navController.navigate(SSBMaxDestinations.InstructorGrading.route) {
                        popUpTo(SSBMaxDestinations.InstructorGrading.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

// Temporary placeholder for screens not yet implemented
@Composable
private fun PlaceholderScreen(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "$title\n(Coming Soon)",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

