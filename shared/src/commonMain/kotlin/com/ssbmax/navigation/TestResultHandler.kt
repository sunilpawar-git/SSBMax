package com.ssbmax.navigation

import androidx.navigation.NavController
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.TestType

/**
 * KMP port of `app/.../ui/tests/common/TestResultHandler.kt` -- SSOT for
 * post-submission routing (KMP-convergence plan, Phase 3b). OIR/PPDT/TAT/WAT/
 * PIQ/GTO_GD/GTO_LECTURETTE/GTO_GPE always land on their result screen
 * immediately (AI-graded, or -- for PIQ -- not graded at all). Every other
 * type (SRT/SD today) is subscription-gated: PREMIUM gets the immediate
 * result, FREE/PRO wait for manual assessor review on [SubmissionDetail].
 * Debug `Log.d` tracing from the Android original dropped -- it was
 * development scaffolding, not production logging.
 */
object TestResultHandler {

    private val DIRECT_RESULT_TEST_TYPES = setOf(
        TestType.OIR,
        TestType.PPDT,
        TestType.TAT,
        TestType.WAT,
        TestType.PIQ,
        TestType.GTO_GD,
        TestType.GTO_LECTURETTE,
        TestType.GTO_GPE
    )

    /**
     * Handle test submission navigation based on subscription type.
     * @param submissionId The ID of the submitted test (or sessionId for OIR)
     * @param subscriptionType User's subscription type (FREE, PRO, PREMIUM)
     * @param testType The type of test that was submitted
     * @param navController Navigation controller for routing
     */
    fun handleTestSubmission(
        submissionId: String,
        subscriptionType: SubscriptionType,
        testType: TestType,
        navController: NavController
    ) {
        if (testType in DIRECT_RESULT_TEST_TYPES) {
            navigateToResult(submissionId, testType, navController)
            return
        }

        when (subscriptionType) {
            SubscriptionType.PREMIUM -> navigateToResult(submissionId, testType, navController)
            SubscriptionType.PRO,
            SubscriptionType.FREE -> navigateToPendingReview(submissionId, navController)
        }
    }

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
            TestType.GTO_GD -> SSBMaxDestinations.GTOGDResult.createRoute(submissionId)
            TestType.GTO_LECTURETTE -> SSBMaxDestinations.GTOLecturetteResult.createRoute(submissionId)
            TestType.GTO_GPE -> SSBMaxDestinations.GTOGPEResult.createRoute(submissionId)
            else -> SSBMaxDestinations.SubmissionDetail.createRoute(submissionId)
        }

        navController.navigate(route) {
            popUpTo(SSBMaxDestinations.StudentHome.route) { saveState = false }
        }
    }

    private fun navigateToPendingReview(
        submissionId: String,
        navController: NavController
    ) {
        navController.navigate(SSBMaxDestinations.SubmissionDetail.createRoute(submissionId)) {
            popUpTo(SSBMaxDestinations.StudentHome.route) { saveState = false }
        }
    }
}
