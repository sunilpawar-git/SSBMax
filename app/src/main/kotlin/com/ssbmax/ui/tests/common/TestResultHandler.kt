package com.ssbmax.ui.tests.common

import androidx.navigation.NavController
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.navigation.SSBMaxDestinations

/**
 * Centralized handler for test result navigation based on subscription type
 * Routes users to appropriate result screens (AI immediate vs Assessor pending review)
 */
object TestResultHandler {
    
    /**
     * Handle test submission navigation based on subscription type
     * @param submissionId The ID of the submitted test (or sessionId for OIR)
     * @param subscriptionType User's subscription type (FREE, PREMIUM_ASSESSOR, PREMIUM_AI)
     * @param testType The type of test that was submitted
     * @param navController Navigation controller for routing
     */
    fun handleTestSubmission(
        submissionId: String,
        subscriptionType: SubscriptionType,
        testType: TestType,
        navController: NavController
    ) {
        // Special handling for OIR, PPDT, TAT, WAT, PIQ: Show results directly 
        // (bypasses Firestore to avoid permission issues during development)
        if (testType == TestType.OIR || testType == TestType.PPDT || 
            testType == TestType.TAT || testType == TestType.WAT || testType == TestType.PIQ) {
            navigateToResult(submissionId, testType, navController)
            return
        }
        
        when (subscriptionType) {
            SubscriptionType.PREMIUM -> {
                // Premium users get immediate AI-graded results
                navigateToResult(submissionId, testType, navController)
            }
            SubscriptionType.PRO,
            SubscriptionType.FREE -> {
                // Pro and free users wait for manual grading
                navigateToPendingReview(submissionId, navController)
            }
        }
    }
    
    /**
     * Navigate to immediate test result screen (AI grading)
     */
    private fun navigateToResult(
        submissionId: String,
        testType: TestType,
        navController: NavController
    ) {
        val route = when (testType) {
            TestType.OIR -> SSBMaxDestinations.OIRTestResult.createRoute(submissionId)
            TestType.TAT -> SSBMaxDestinations.TATSubmissionResult.createRoute(submissionId)
            TestType.WAT -> SSBMaxDestinations.WATSubmissionResult.createRoute(submissionId)
            TestType.SRT -> SSBMaxDestinations.SRTSubmissionResult.createRoute(submissionId)
            TestType.PPDT -> SSBMaxDestinations.PPDTSubmissionResult.createRoute(submissionId)
            TestType.PIQ -> SSBMaxDestinations.PIQSubmissionResult.createRoute(submissionId)
            TestType.SD -> SSBMaxDestinations.SDSubmissionResult.createRoute(submissionId)
            // GTO Tests - Route to specific GTO result screens
            TestType.GTO_GD -> SSBMaxDestinations.GTOGDResult.createRoute(submissionId)
            TestType.GTO_LECTURETTE -> SSBMaxDestinations.GTOLecturetteResult.createRoute(submissionId)
            TestType.GTO_GPE -> SSBMaxDestinations.GTOGPEResult.createRoute(submissionId)
            else -> SSBMaxDestinations.SubmissionDetail.createRoute(submissionId)
        }
        navController.navigate(route) {
            // Clear test screen from back stack
            popUpTo(navController.graph.startDestinationId) {
                saveState = false
            }
        }
    }
    
    /**
     * Navigate to pending review screen (Assessor grading)
     */
    private fun navigateToPendingReview(
        submissionId: String,
        navController: NavController
    ) {
        navController.navigate(
            SSBMaxDestinations.SubmissionDetail.createRoute(submissionId)
        ) {
            // Clear test screen from back stack
            popUpTo(navController.graph.startDestinationId) {
                saveState = false
            }
        }
    }
}

