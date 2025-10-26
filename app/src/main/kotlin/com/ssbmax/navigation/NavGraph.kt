package com.ssbmax.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
                },
                onNavigateToProfileOnboarding = {
                    navController.navigate(SSBMaxDestinations.UserProfile.createOnboardingRoute()) {
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
                onNavigateToTopic = { topicRoute ->
                    // topicRoute includes query param: "oir?selectedTab=2"
                    navController.navigate("topic/$topicRoute")
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
                onNavigateToSubmissions = {
                    navController.navigate(SSBMaxDestinations.StudentSubmissions.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(SSBMaxDestinations.NotificationCenter.route)
                },
                onNavigateToMarketplace = {
                    navController.navigate(SSBMaxDestinations.Marketplace.route)
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
        
        // Student Submissions List
        composable(SSBMaxDestinations.StudentSubmissions.route) {
            com.ssbmax.ui.submissions.SubmissionsListScreen(
                onSubmissionClick = { submissionId ->
                    navController.navigate(SSBMaxDestinations.SubmissionDetail.createRoute(submissionId))
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Student Study
        composable(SSBMaxDestinations.StudentStudy.route) {
            com.ssbmax.ui.study.StudyMaterialsScreen(
                onNavigateToTopic = { topicId ->
                    navController.navigate(SSBMaxDestinations.TopicScreen.createRoute(topicId))
                },
                onNavigateToSearch = {
                    // TODO: Navigate to search (future enhancement)
                },
                onNavigateToBookmarks = {
                    // Already handled internally by StudyMaterialsScreen
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
            PlaceholderScreen(title = "Instructor Analytics")
        }
        
        // ========================
        // PHASE SCREENS
        // ========================
        
        // Phase 1 Detail - Shows topic cards, navigates to Topic Screens
        composable(SSBMaxDestinations.Phase1Detail.route) {
            com.ssbmax.ui.phase.Phase1DetailScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToTopic = { topicId ->
                    navController.navigate(SSBMaxDestinations.TopicScreen.createRoute(topicId))
                }
            )
        }
        
        // Phase 2 Detail - Shows topic cards, navigates to Topic Screens
        composable(SSBMaxDestinations.Phase2Detail.route) {
            com.ssbmax.ui.phase.Phase2DetailScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToTopic = { topicId ->
                    navController.navigate(SSBMaxDestinations.TopicScreen.createRoute(topicId))
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
                onTestComplete = { sessionId, subscriptionType ->
                    com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                        submissionId = sessionId,
                        subscriptionType = subscriptionType,
                        testType = com.ssbmax.core.domain.model.TestType.OIR,
                        navController = navController
                    )
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
                onTestComplete = { submissionId, subscriptionType ->
                    com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                        submissionId = submissionId,
                        subscriptionType = subscriptionType,
                        testType = com.ssbmax.core.domain.model.TestType.PPDT,
                        navController = navController
                    )
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
        
        // TAT Test
        composable(
            route = SSBMaxDestinations.TATTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            com.ssbmax.ui.tests.tat.TATTestScreen(
                testId = testId,
                onTestComplete = { submissionId, subscriptionType ->
                    // Use TestResultHandler to navigate based on subscription type
                    com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                        submissionId = submissionId,
                        subscriptionType = subscriptionType,
                        testType = com.ssbmax.core.domain.model.TestType.TAT,
                        navController = navController
                    )
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // TAT Result
        composable(
            route = SSBMaxDestinations.TATSubmissionResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
            com.ssbmax.ui.tests.tat.TATSubmissionResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                },
                onViewFeedback = {
                    navController.navigate(SSBMaxDestinations.SubmissionDetail.createRoute(submissionId))
                }
            )
        }
        
        // WAT Test
        composable(
            route = SSBMaxDestinations.WATTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            com.ssbmax.ui.tests.wat.WATTestScreen(
                testId = testId,
                onTestComplete = { submissionId, subscriptionType ->
                    com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                        submissionId = submissionId,
                        subscriptionType = subscriptionType,
                        testType = com.ssbmax.core.domain.model.TestType.WAT,
                        navController = navController
                    )
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // WAT Result
        composable(
            route = SSBMaxDestinations.WATSubmissionResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
            com.ssbmax.ui.tests.wat.WATSubmissionResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                },
                onViewFeedback = {
                    navController.navigate(SSBMaxDestinations.SubmissionDetail.createRoute(submissionId))
                }
            )
        }
        
        // SRT Test
        composable(
            route = SSBMaxDestinations.SRTTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            com.ssbmax.ui.tests.srt.SRTTestScreen(
                testId = testId,
                onTestComplete = { submissionId, subscriptionType ->
                    com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                        submissionId = submissionId,
                        subscriptionType = subscriptionType,
                        testType = com.ssbmax.core.domain.model.TestType.SRT,
                        navController = navController
                    )
                },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // SRT Result
        composable(
            route = SSBMaxDestinations.SRTSubmissionResult.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
            com.ssbmax.ui.tests.srt.SRTSubmissionResultScreen(
                submissionId = submissionId,
                onNavigateHome = {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                },
                onViewFeedback = {
                    navController.navigate(SSBMaxDestinations.SubmissionDetail.createRoute(submissionId))
                }
            )
        }
        
        // SD (Self Description) Test
        composable(
            route = SSBMaxDestinations.SDTest.route,
            arguments = listOf(navArgument("testId") { type = NavType.StringType })
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            // TODO: Implement SDTestScreen
            PlaceholderScreen(title = "Self Description Test: $testId")
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
            com.ssbmax.ui.study.StudyMaterialDetailScreen(
                categoryId = categoryId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Topic Screen (with Study Material/Tests tabs)
        composable(
            route = SSBMaxDestinations.TopicScreen.route + "?selectedTab={selectedTab}",
            arguments = listOf(
                navArgument("topicId") { type = NavType.StringType },
                navArgument("selectedTab") { 
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
            val selectedTab = backStackEntry.arguments?.getInt("selectedTab") ?: 0
            com.ssbmax.ui.topic.TopicScreen(
                topicId = topicId,
                initialTab = selectedTab,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToStudyMaterial = { materialId ->
                    navController.navigate(SSBMaxDestinations.StudyMaterialDetail.createRoute(materialId))
                },
                onNavigateToTest = { testId ->
                    // Navigate to appropriate test based on topic
                    when {
                        testId.startsWith("oir_") -> 
                            navController.navigate(SSBMaxDestinations.OIRTest.createRoute(testId))
                        testId.startsWith("ppdt_") -> 
                            navController.navigate(SSBMaxDestinations.PPDTTest.createRoute(testId))
                        testId.startsWith("tat_") -> 
                            navController.navigate(SSBMaxDestinations.TATTest.createRoute(testId))
                        testId.startsWith("wat_") -> 
                            navController.navigate(SSBMaxDestinations.WATTest.createRoute(testId))
                        testId.startsWith("srt_") -> 
                            navController.navigate(SSBMaxDestinations.SRTTest.createRoute(testId))
                        testId.startsWith("sd_") -> 
                            navController.navigate(SSBMaxDestinations.SDTest.createRoute(testId))
                        testId.startsWith("gto_") -> 
                            navController.navigate(SSBMaxDestinations.GTOTest.createRoute(testId))
                        testId.startsWith("io_") -> 
                            navController.navigate(SSBMaxDestinations.IOTest.createRoute(testId))
                        else -> {
                            // Log unknown test type or show error
                            android.util.Log.w("Navigation", "Unknown test ID: $testId")
                        }
                    }
                }
            )
        }
        
        // ========================
        // PREMIUM/SUBSCRIPTION
        // ========================
        
        // Upgrade Screen
        composable(SSBMaxDestinations.UpgradeScreen.route) {
            com.ssbmax.ui.premium.UpgradeScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // ========================
        // NOTIFICATIONS
        // ========================
        
        // Notification Center
        composable(SSBMaxDestinations.NotificationCenter.route) {
            com.ssbmax.ui.notifications.NotificationCenterScreen(
                onNavigateBack = { navController.navigateUp() },
                onNotificationClick = { notification ->
                    // TODO: Handle deep linking based on notification actionUrl
                    // For now, just dismiss the notification center
                    navController.navigateUp()
                }
            )
        }
        
        // ========================
        // SETTINGS
        // ========================
        
        // Settings Screen
        composable(SSBMaxDestinations.Settings.route) {
            com.ssbmax.ui.settings.SettingsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToFAQ = { navController.navigate(SSBMaxDestinations.FAQ.route) },
                onNavigateToUpgrade = { navController.navigate(SSBMaxDestinations.Upgrade.route) }
            )
        }
        
        // FAQ Screen
        composable(SSBMaxDestinations.FAQ.route) {
            com.ssbmax.ui.faq.FAQScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // ========================
        // UPGRADE & PAYMENT FLOW
        // ========================
        
        // Upgrade Screen
        composable(SSBMaxDestinations.Upgrade.route) {
            com.ssbmax.ui.upgrade.UpgradeScreen(
                onNavigateBack = { navController.navigateUp() },
                onPlanSelected = { plan ->
                    // Store plan in SavedStateHandle and navigate
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedPlan", plan)
                    navController.navigate(SSBMaxDestinations.Payment.route)
                }
            )
        }
        
        // Payment Screen
        composable(SSBMaxDestinations.Payment.route) {
            val previousBackStackEntry = navController.previousBackStackEntry
            val plan = previousBackStackEntry
                ?.savedStateHandle
                ?.get<com.ssbmax.core.domain.model.SubscriptionPlan>("selectedPlan")
            
            if (plan != null) {
                com.ssbmax.ui.payment.MockPaymentScreen(
                    plan = plan,
                    onPaymentSuccess = {
                        // Store plan for success screen
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("paidPlan", plan)
                        navController.navigate(SSBMaxDestinations.PaymentSuccess.route) {
                            popUpTo(SSBMaxDestinations.Upgrade.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.navigateUp() }
                )
            } else {
                // No plan selected, navigate back
                LaunchedEffect(Unit) {
                    navController.navigateUp()
                }
            }
        }
        
        // Payment Success Screen
        composable(SSBMaxDestinations.PaymentSuccess.route) {
            val previousBackStackEntry = navController.previousBackStackEntry
            val plan = previousBackStackEntry
                ?.savedStateHandle
                ?.get<com.ssbmax.core.domain.model.SubscriptionPlan>("paidPlan")
            
            if (plan != null) {
                com.ssbmax.ui.payment.PaymentSuccessScreen(
                    plan = plan,
                    onNavigateToHome = {
                        navController.navigate(SSBMaxDestinations.StudentHome.route) {
                            popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                        }
                    }
                )
            } else {
                // No plan data, navigate to home
                LaunchedEffect(Unit) {
                    navController.navigate(SSBMaxDestinations.StudentHome.route) {
                        popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                    }
                }
            }
        }
        
        // User Profile Screen
        composable(
            route = SSBMaxDestinations.UserProfile.route + "?isOnboarding={isOnboarding}",
            arguments = listOf(
                navArgument("isOnboarding") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val isOnboarding = backStackEntry.arguments?.getBoolean("isOnboarding") ?: false
            com.ssbmax.ui.profile.UserProfileScreen(
                onNavigateBack = { navController.navigateUp() },
                onProfileSaved = {
                    if (isOnboarding) {
                        // After onboarding, navigate to home
                        navController.navigate(SSBMaxDestinations.StudentHome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigateUp()
                    }
                },
                isOnboarding = isOnboarding
            )
        }
        
        // ========================
        // INFORMATIONAL SCREENS
        // ========================
        
        // SSB Overview
        composable(SSBMaxDestinations.SSBOverview.route) {
            com.ssbmax.ui.ssboverview.SSBOverviewScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // ========================
        // MARKETPLACE & BATCH MANAGEMENT
        // ========================
        
        // Marketplace
        composable(SSBMaxDestinations.Marketplace.route) {
            com.ssbmax.ui.marketplace.MarketplaceScreen(
                onNavigateBack = { navController.navigateUp() },
                onInstituteClick = { instituteId ->
                    // TODO: Navigate to institute detail screen
                }
            )
        }
        
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
        // SUBMISSION DETAIL (STUDENT VIEW)
        // ========================
        
        // Submission Detail (for students to view their own submission)
        composable(
            route = SSBMaxDestinations.SubmissionDetail.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
            com.ssbmax.ui.submissions.SubmissionDetailScreen(
                submissionId = submissionId,
                onNavigateBack = { navController.navigateUp() }
            )
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
                onNavigateBack = { navController.navigateUp() }
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

