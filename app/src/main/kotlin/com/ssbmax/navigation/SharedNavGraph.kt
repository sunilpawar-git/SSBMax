package com.ssbmax.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ssbmax.core.domain.model.interview.InterviewMode

/**
 * Shared navigation graph
 * Contains all screens accessible to both students and instructors
 * 
 * This graph handles common journeys:
 * - Phase detail screens
 * - All test screens and results
 * - Study materials and topics
 * - Premium/subscription
 * - Notifications
 * - Settings
 * - Marketplace
 * - SSB Overview
 * - Submission details
 * - User profiles
 */
fun NavGraphBuilder.sharedNavGraph(
    navController: NavHostController
) {
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
            onTestComplete = { submissionId, subscriptionType ->
                // Use centralized TestResultHandler (following TAT/WAT/SRT pattern)
                com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                    submissionId = submissionId,
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
        val submissionId = backStackEntry.arguments?.getString("sessionId") ?: ""

        com.ssbmax.ui.tests.oir.OIRTestResultScreen(
            submissionId = submissionId,
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
    
    // SD Test (Self Description Test)
    composable(
        route = SSBMaxDestinations.SDTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.getString("testId") ?: ""
        com.ssbmax.ui.tests.sdt.SDTTestScreen(
            testId = testId,
            onTestComplete = { submissionId, subscriptionType ->
                com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                    submissionId = submissionId,
                    subscriptionType = subscriptionType,
                    testType = com.ssbmax.core.domain.model.TestType.SD,
                    navController = navController
                )
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }
    
    // SD Result (Self Description Test Result)
    composable(
        route = SSBMaxDestinations.SDSubmissionResult.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
        com.ssbmax.ui.tests.sdt.SDTSubmissionResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }

    // PIQ Result (Personal Information Questionnaire Result)
    composable(
        route = SSBMaxDestinations.PIQSubmissionResult.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
        com.ssbmax.ui.tests.piq.PIQSubmissionResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }

    // PIQ Test (Personal Information Questionnaire)
    composable(
        route = SSBMaxDestinations.PIQTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.getString("testId") ?: ""
        com.ssbmax.ui.tests.piq.PIQTestScreen(
            testId = testId,
            onNavigateBack = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            },
            onNavigateToResult = { submissionId ->
                navController.navigate(SSBMaxDestinations.PIQSubmissionResult.createRoute(submissionId)) {
                    popUpTo(navController.graph.startDestinationId) { saveState = false }
                }
            }
        )
    }
    
    // GTO Test
    composable(
        route = SSBMaxDestinations.GTOTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.getString("testId") ?: ""
        // TODO: Implement GTOTestScreen
        SharedPlaceholderScreen(title = "GTO Test: $testId")
    }
    
    // IO Test
    composable(
        route = SSBMaxDestinations.IOTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) {
        // IO Test (Interview Officer) - Navigate to interview feature
        com.ssbmax.ui.interview.start.StartInterviewScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToSession = { sessionId ->
                // Unified interview - always use voice-based interview (with TTS mute toggle)
                navController.navigate(SSBMaxDestinations.VoiceInterviewSession.createRoute(sessionId))
            },
            onNavigateToResult = { resultId ->
                navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(resultId))
            }
        )
    }

    // ========================
    // INTERVIEW SCREENS (Stage 4)
    // ========================

    // Start Interview - Prerequisite check and session creation
    composable(SSBMaxDestinations.StartInterview.route) {
        com.ssbmax.ui.interview.start.StartInterviewScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToSession = { sessionId ->
                // Unified interview - always use voice-based interview (with TTS mute toggle)
                navController.navigate(SSBMaxDestinations.VoiceInterviewSession.createRoute(sessionId))
            },
            onNavigateToResult = { resultId ->
                navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(resultId))
            }
        )
    }

    // Interview Session (Unified - supports TTS with mute toggle)
    composable(
        route = SSBMaxDestinations.VoiceInterviewSession.route,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
        com.ssbmax.ui.interview.session.InterviewSessionScreen(
            sessionId = sessionId,
            onNavigateBack = {
                // CRITICAL: Use explicit navigation with popUpTo to synchronously remove
                // this screen from the back stack. Using popBackStack() alone causes a race
                // condition where Compose creates a new ViewModel before the old screen is removed.
                // Pop to StudentHome (always in back stack) instead of TopicScreen which might
                // not exist if user navigated through IOTest or other routes
                // Tab indices: 0=Overview, 1=Study Material, 2=Tests
                navController.navigate(SSBMaxDestinations.TopicScreen.createRoute("INTERVIEW") + "?selectedTab=2") {
                    popUpTo(SSBMaxDestinations.StudentHome.route) {
                        inclusive = false // Keep StudentHome
                    }
                    launchSingleTop = true
                }
            },
            onNavigateToResult = { resultId ->
                navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(resultId)) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = false }
                }
            },
            onNavigateToHome = {
                // Navigate to interview topic screen after background analysis starts
                // CRITICAL: Pop to StudentHome (always in back stack) to ensure all interview
                // screens are removed, regardless of navigation path
                navController.navigate(SSBMaxDestinations.TopicScreen.createRoute("INTERVIEW") + "?selectedTab=2") {
                    popUpTo(SSBMaxDestinations.StudentHome.route) {
                        inclusive = false // Keep StudentHome
                    }
                    launchSingleTop = true
                }
            }
        )
    }

    // Interview Result
    composable(
        route = SSBMaxDestinations.InterviewResult.route,
        arguments = listOf(navArgument("resultId") { type = NavType.StringType })
    ) { backStackEntry ->
        val resultId = backStackEntry.arguments?.getString("resultId") ?: ""
        com.ssbmax.ui.interview.result.InterviewResultScreen(
            resultId = resultId,
            onNavigateBack = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }

    // ========================
    // GTO TESTS (8 Tests)
    // ========================
    
    // GTO - Group Discussion Test
    composable(
        route = SSBMaxDestinations.GTOGDTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.getString("testId") ?: ""
        com.ssbmax.ui.tests.gto.gd.GDTestScreen(
            testId = testId,
            onTestComplete = { submissionId, subscriptionType ->
                com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                    submissionId = submissionId,
                    subscriptionType = subscriptionType,
                    testType = com.ssbmax.core.domain.model.TestType.GTO_GD,
                    navController = navController
                )
            },
            onNavigateBack = { navController.navigateUp() },
            onNavigateToUpgrade = {
                navController.navigate(SSBMaxDestinations.UpgradeScreen.route)
            }
        )
    }
    
    // GTO - Group Discussion Result
    composable(
        route = SSBMaxDestinations.GTOGDResult.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
        com.ssbmax.ui.tests.gto.gd.GDResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }
    
    // GTO - Lecturette Test
    composable(
        route = SSBMaxDestinations.GTOLecturetteTest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.getString("testId") ?: ""
        com.ssbmax.ui.tests.gto.lecturette.LecturetteTestScreen(
            testId = testId,
            onTestComplete = { submissionId, subscriptionType ->
                com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                    submissionId = submissionId,
                    subscriptionType = subscriptionType,
                    testType = com.ssbmax.core.domain.model.TestType.GTO_LECTURETTE,
                    navController = navController
                )
            },
            onNavigateBack = { navController.navigateUp() },
            onNavigateToUpgrade = {
                navController.navigate(SSBMaxDestinations.UpgradeScreen.route)
            }
        )
    }
    
    // GTO - Lecturette Result
    composable(
        route = SSBMaxDestinations.GTOLecturetteResult.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
        com.ssbmax.ui.tests.gto.lecturette.LecturetteResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }

    // GTO - Group Planning Exercise Test
    composable(
        route = SSBMaxDestinations.GTOGPETest.route,
        arguments = listOf(navArgument("testId") { type = NavType.StringType })
    ) { backStackEntry ->
        val testId = backStackEntry.arguments?.getString("testId") ?: ""
        com.ssbmax.ui.tests.gpe.GPETestScreen(
            testId = testId,
            onTestComplete = { submissionId, subscriptionType ->
                com.ssbmax.ui.tests.common.TestResultHandler.handleTestSubmission(
                    submissionId = submissionId,
                    subscriptionType = subscriptionType,
                    testType = com.ssbmax.core.domain.model.TestType.GTO_GPE,
                    navController = navController
                )
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    // GTO - Group Planning Exercise Result
    composable(
        route = SSBMaxDestinations.GTOGPEResult.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
        com.ssbmax.ui.tests.gpe.GPESubmissionResultScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }

    // ========================
    // STUDY MATERIALS
    // ========================
    
    // Study Materials List
    composable(SSBMaxDestinations.StudyMaterialsList.route) {
        // TODO: Implement StudyMaterialsListScreen
        SharedPlaceholderScreen(title = "Study Materials")
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
                    testId.startsWith("piq_") ->
                        navController.navigate(SSBMaxDestinations.PIQTest.createRoute(testId))
                    // GTO Tests - Route to specific screens
                    testId.startsWith("gto_gd_") ->
                        navController.navigate(SSBMaxDestinations.GTOGDTest.createRoute(testId))
                    testId.startsWith("gto_gpe_") ->
                        navController.navigate(SSBMaxDestinations.GTOGPETest.createRoute(testId))
                    testId.startsWith("gto_lecturette_") ->
                        navController.navigate(SSBMaxDestinations.GTOLecturetteTest.createRoute(testId))
                    testId.startsWith("gto_") ->
                        navController.navigate(SSBMaxDestinations.GTOTest.createRoute(testId))
                    testId.startsWith("io_") ->
                        navController.navigate(SSBMaxDestinations.IOTest.createRoute(testId))
                    else -> {
                        android.util.Log.w("Navigation", "Unknown test ID: $testId")
                    }
                }
            },
            onNavigateToInterviewResult = { resultId ->
                navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(resultId))
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
            onNotificationClick = { notificationId ->
                // TODO: Handle notification deep linking
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }
    
    // ========================
    // SETTINGS & PROFILE
    // ========================
    
    // Settings
    composable(SSBMaxDestinations.Settings.route) {
        com.ssbmax.ui.settings.SettingsScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToFAQ = {
                // TODO: Navigate to FAQ
            },
            onNavigateToUpgrade = {
                navController.navigate(SSBMaxDestinations.UpgradeScreen.route)
            },
            onNavigateToSubscriptionManagement = {
                navController.navigate(SSBMaxDestinations.SubscriptionManagement.route)
            }
        )
    }
    
    // Subscription Management
    composable(SSBMaxDestinations.SubscriptionManagement.route) {
        com.ssbmax.ui.settings.SubscriptionManagementScreen(
            onNavigateBack = { navController.navigateUp() },
            onUpgrade = { tier ->
                navController.navigate(SSBMaxDestinations.UpgradeScreen.route)
            }
        )
    }
    
    // ========================
    // ANALYTICS
    // ========================
    
    // Analytics Dashboard (Student Performance)
    composable(SSBMaxDestinations.Analytics.route) {
        com.ssbmax.ui.analytics.AnalyticsScreen(
            onNavigateBack = { navController.navigateUp() }
        )
    }
    
    // User Profile
    composable(
        route = SSBMaxDestinations.UserProfile.route + "?onboarding={onboarding}",
        arguments = listOf(
            navArgument("onboarding") {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) { backStackEntry ->
        val isOnboarding = backStackEntry.arguments?.getBoolean("onboarding") ?: false
        com.ssbmax.ui.profile.UserProfileScreen(
            isOnboarding = isOnboarding,
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
            }
        )
    }
    
    // ========================
    // MARKETPLACE
    // ========================
    
    // Marketplace
    composable(SSBMaxDestinations.Marketplace.route) {
        com.ssbmax.ui.marketplace.MarketplaceScreen(
            onNavigateBack = { navController.navigateUp() },
            onInstituteClick = { instituteId ->
                // TODO: Navigate to institute detail
            }
        )
    }
    
    // ========================
    // SSB OVERVIEW
    // ========================
    
    // SSB Overview
    composable(SSBMaxDestinations.SSBOverview.route) {
        com.ssbmax.ui.ssboverview.SSBOverviewScreen(
            onNavigateBack = { navController.navigateUp() }
        )
    }
    
    // ========================
    // SUBMISSION DETAILS
    // ========================
    
    // Submission Detail (Shared between students and instructors)
    composable(
        route = SSBMaxDestinations.SubmissionDetail.route,
        arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val submissionId = backStackEntry.arguments?.getString("submissionId") ?: ""
        com.ssbmax.ui.submissions.SubmissionDetailScreen(
            submissionId = submissionId,
            onNavigateHome = {
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StudentHome.route) { inclusive = true }
                }
            }
        )
    }
    
}

/**
 * Temporary placeholder for shared screens not yet implemented
 */
@Composable
private fun SharedPlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}

