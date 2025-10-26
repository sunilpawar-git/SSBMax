package com.ssbmax.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

/**
 * Student navigation graph
 * Contains all student-specific screens and flows
 * 
 * This graph handles student user journeys:
 * - Student home dashboard
 * - Tests overview
 * - Submissions list
 * - Study materials
 * - Student profile
 */
fun NavGraphBuilder.studentNavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
) {
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
}

