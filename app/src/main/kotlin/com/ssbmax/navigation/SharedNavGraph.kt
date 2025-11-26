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
                navController.navigate(
                    SSBMaxDestinations.TextInterviewSession.createRoute(sessionId)
                )
            },
            onNavigateToResult = { resultId ->
                navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(resultId))
            }
        )
    }

    // ========================
    // INTERVIEW SCREENS (Stage 4)
    // ========================

    // Start Interview - Mode selection and prerequisite check
    composable(SSBMaxDestinations.StartInterview.route) {
        com.ssbmax.ui.interview.start.StartInterviewScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToSession = { sessionId ->
                // Check interview mode from session to route appropriately
                // For now, route based on mode stored in session
                // TODO: Fetch session mode to determine routing
                navController.navigate(SSBMaxDestinations.TextInterviewSession.createRoute(sessionId))
            },
            onNavigateToResult = { resultId ->
                navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(resultId))
            }
        )
    }

    // Text Interview Session
    composable(
        route = SSBMaxDestinations.TextInterviewSession.route,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
        com.ssbmax.ui.interview.session.InterviewSessionScreen(
            sessionId = sessionId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToResult = { resultId ->
                navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(resultId)) {
                    popUpTo(SSBMaxDestinations.StartInterview.route) { inclusive = true }
                }
            },
            onNavigateToHome = {
                // Navigate to home, clearing interview backstack
                navController.navigate(SSBMaxDestinations.StudentHome.route) {
                    popUpTo(SSBMaxDestinations.StartInterview.route) { inclusive = true }
                }
            }
        )
    }

    // Voice Interview Session
    composable(
        route = SSBMaxDestinations.VoiceInterviewSession.route,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
    ) { backStackEntry ->
        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
        com.ssbmax.ui.interview.voice.VoiceInterviewSessionScreen(
            sessionId = sessionId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToResult = { resultId ->
                navController.navigate(SSBMaxDestinations.InterviewResult.createRoute(resultId)) {
                    popUpTo(SSBMaxDestinations.StartInterview.route) { inclusive = true }
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
                    testId.startsWith("gto_") -> 
                        navController.navigate(SSBMaxDestinations.GTOTest.createRoute(testId))
                    testId.startsWith("io_") -> 
                        navController.navigate(SSBMaxDestinations.IOTest.createRoute(testId))
                    else -> {
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

