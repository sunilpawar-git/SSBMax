package com.ssbmax.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.TestType
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * KMP-convergence Phase 3b -- pins [TestResultHandler]'s subscription-tier ×
 * test-type routing decision, the exact thing `app/.../TestResultHandler.kt`
 * did for FREE/PRO users on SRT/SD (pending manual grading) vs every other
 * test type (immediate AI/no-grading result, regardless of tier). Same
 * Robolectric/androidUnitTest precedent as [Phase3aNavigationTest].
 *
 * Phase 3d: assertions moved from comparing `currentDestination?.route`
 * strings to `hasRoute<T>()` -- type-safe routes generate that string from
 * the destination's KClass now, not from `SSBMaxDestinations`'s legacy
 * pattern constants. Each matrix row pairs a description with an inline
 * `hasRoute<T>()` check (reified, so it can't be looked up dynamically by a
 * shared KClass value the way the old string comparison could).
 */
@RunWith(RobolectricTestRunner::class)
class TestResultHandlerTest {

    private fun buildController(): NavHostController {
        val navController = NavHostController(RuntimeEnvironment.getApplication())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        val graph = navController.createGraph(startDestination = SSBMaxDestinations.StudentHome) {
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
    private val gatedMatrix: List<Triple<SubscriptionTier, TestType, (NavDestination?) -> Boolean>> = listOf(
        Triple(SubscriptionTier.FREE, TestType.SRT) { it?.hasRoute<SSBMaxDestinations.SubmissionDetail>() == true },
        Triple(SubscriptionTier.PRO, TestType.SRT) { it?.hasRoute<SSBMaxDestinations.SubmissionDetail>() == true },
        Triple(SubscriptionTier.PREMIUM, TestType.SRT) { it?.hasRoute<SSBMaxDestinations.SRTSubmissionResult>() == true },
        Triple(SubscriptionTier.FREE, TestType.SD) { it?.hasRoute<SSBMaxDestinations.SubmissionDetail>() == true },
        Triple(SubscriptionTier.PRO, TestType.SD) { it?.hasRoute<SSBMaxDestinations.SubmissionDetail>() == true },
        Triple(SubscriptionTier.PREMIUM, TestType.SD) { it?.hasRoute<SSBMaxDestinations.SDSubmissionResult>() == true }
    )

    @Test
    fun srtAndSd_routeBySubscriptionTier() {
        gatedMatrix.forEach { (subscriptionType, testType, landedOnExpected) ->
            val navController = buildController()
            TestResultHandler.handleTestSubmission(
                submissionId = "sub-1",
                subscriptionTier = subscriptionType,
                testType = testType,
                navController = navController
            )
            assertTrue(
                landedOnExpected(navController.currentDestination),
                "expected $subscriptionType/$testType to land on the gated destination, was on ${navController.currentDestination?.route}"
            )
        }
    }

    // The remaining 8 test types bypass the subscription check entirely --
    // spot-checked at FREE tier (the tier where SRT/SD above diverge) to prove
    // the special case short-circuits before the subscription `when`.
    private val alwaysDirectMatrix: List<Pair<TestType, (NavDestination?) -> Boolean>> = listOf(
        TestType.OIR to { d: NavDestination? -> d?.hasRoute<SSBMaxDestinations.OIRTestResult>() == true },
        TestType.PPDT to { d: NavDestination? -> d?.hasRoute<SSBMaxDestinations.PPDTSubmissionResult>() == true },
        TestType.TAT to { d: NavDestination? -> d?.hasRoute<SSBMaxDestinations.TATSubmissionResult>() == true },
        TestType.WAT to { d: NavDestination? -> d?.hasRoute<SSBMaxDestinations.WATSubmissionResult>() == true },
        TestType.PIQ to { d: NavDestination? -> d?.hasRoute<SSBMaxDestinations.PIQSubmissionResult>() == true },
        TestType.GTO_GD to { d: NavDestination? -> d?.hasRoute<SSBMaxDestinations.GTOGDResult>() == true },
        TestType.GTO_LECTURETTE to { d: NavDestination? -> d?.hasRoute<SSBMaxDestinations.GTOLecturetteResult>() == true },
        TestType.GTO_GPE to { d: NavDestination? -> d?.hasRoute<SSBMaxDestinations.GTOGPEResult>() == true }
    )

    @Test
    fun everyOtherTestType_ignoresSubscriptionTier_evenOnFree() {
        alwaysDirectMatrix.forEach { (testType, landedOnExpected) ->
            val navController = buildController()
            TestResultHandler.handleTestSubmission(
                submissionId = "sub-2",
                subscriptionTier = SubscriptionTier.FREE,
                testType = testType,
                navController = navController
            )
            assertTrue(
                landedOnExpected(navController.currentDestination),
                "expected FREE/$testType to bypass gating, was on ${navController.currentDestination?.route}"
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
            subscriptionTier = SubscriptionTier.PREMIUM,
            testType = TestType.IO,
            navController = navController
        )
        assertTrue(navController.currentDestination?.hasRoute<SSBMaxDestinations.SubmissionDetail>() == true)
    }
}
