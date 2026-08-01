package com.ssbmax.navigation

import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.TestType
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * KMP-convergence Phase 3b -- pins [TestResultHandler]'s subscription-tier ×
 * test-type routing decision, the exact thing `app/.../TestResultHandler.kt`
 * did for FREE/PRO users on SRT/SD (pending manual grading) vs every other
 * test type (immediate AI/no-grading result, regardless of tier). Same
 * Robolectric/androidUnitTest precedent as [Phase3aNavigationTest]:
 * `NavHostController` needs a real `Context` on the Android target.
 */
@RunWith(RobolectricTestRunner::class)
class TestResultHandlerTest {

    private fun buildController(): NavHostController {
        val navController = NavHostController(RuntimeEnvironment.getApplication())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        val graph = navController.createGraph(startDestination = SSBMaxDestinations.StudentHome.route) {
            homeGraph(navController, onOpenDrawer = {})
            psychTestsGraph(navController)
            writtenTestsGraph(navController)
            gtoGraph(navController)
            submissionsResultsGraph(navController)
        }
        navController.graph = graph
        return navController
    }

    // SRT/SD are the only two test types actually gated by subscriptionType --
    // FREE/PRO wait for manual grading, PREMIUM gets the immediate result.
    private val gatedMatrix = listOf(
        Triple(SubscriptionType.FREE, TestType.SRT, SSBMaxDestinations.SubmissionDetail.route),
        Triple(SubscriptionType.PRO, TestType.SRT, SSBMaxDestinations.SubmissionDetail.route),
        Triple(SubscriptionType.PREMIUM, TestType.SRT, SSBMaxDestinations.SRTSubmissionResult.route),
        Triple(SubscriptionType.FREE, TestType.SD, SSBMaxDestinations.SubmissionDetail.route),
        Triple(SubscriptionType.PRO, TestType.SD, SSBMaxDestinations.SubmissionDetail.route),
        Triple(SubscriptionType.PREMIUM, TestType.SD, SSBMaxDestinations.SDSubmissionResult.route)
    )

    @Test
    fun srtAndSd_routeBySubscriptionTier() {
        gatedMatrix.forEach { (subscriptionType, testType, expectedRoute) ->
            val navController = buildController()
            TestResultHandler.handleTestSubmission(
                submissionId = "sub-1",
                subscriptionType = subscriptionType,
                testType = testType,
                navController = navController
            )
            assertEquals(
                expectedRoute,
                navController.currentDestination?.route,
                "expected $subscriptionType/$testType to land on $expectedRoute"
            )
        }
    }

    // The remaining 8 test types bypass the subscription check entirely --
    // spot-checked at FREE tier (the tier where SRT/SD above diverge) to prove
    // the special case short-circuits before the subscription `when`.
    private val alwaysDirectMatrix = listOf(
        TestType.OIR to SSBMaxDestinations.OIRTestResult.route,
        TestType.PPDT to SSBMaxDestinations.PPDTSubmissionResult.route,
        TestType.TAT to SSBMaxDestinations.TATSubmissionResult.route,
        TestType.WAT to SSBMaxDestinations.WATSubmissionResult.route,
        TestType.PIQ to SSBMaxDestinations.PIQSubmissionResult.route,
        TestType.GTO_GD to SSBMaxDestinations.GTOGDResult.route,
        TestType.GTO_LECTURETTE to SSBMaxDestinations.GTOLecturetteResult.route,
        TestType.GTO_GPE to SSBMaxDestinations.GTOGPEResult.route
    )

    @Test
    fun everyOtherTestType_ignoresSubscriptionTier_evenOnFree() {
        alwaysDirectMatrix.forEach { (testType, expectedRoute) ->
            val navController = buildController()
            TestResultHandler.handleTestSubmission(
                submissionId = "sub-2",
                subscriptionType = SubscriptionType.FREE,
                testType = testType,
                navController = navController
            )
            assertEquals(
                expectedRoute,
                navController.currentDestination?.route,
                "expected FREE/$testType to bypass gating and land on $expectedRoute"
            )
        }
    }

    // A test type with no dedicated result route (e.g. before its result
    // screen is registered) must not crash -- it falls back to the generic
    // pending-review screen, same as the Android original's `else` branch.
    @Test
    fun unrecognizedTestType_fallsBackToSubmissionDetail() {
        val navController = buildController()
        TestResultHandler.handleTestSubmission(
            submissionId = "sub-3",
            subscriptionType = SubscriptionType.PREMIUM,
            testType = TestType.IO,
            navController = navController
        )
        assertEquals(SSBMaxDestinations.SubmissionDetail.route, navController.currentDestination?.route)
    }
}
