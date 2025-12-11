package com.ssbmax.ui.tests.common

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.navigation.SSBMaxDestinations
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class TestResultHandlerTest {

    private val navController: NavController = mockk(relaxed = true)

    @Test
    fun premium_srt_navigates_to_result() {
        val routeSlot = slot<String>()

        TestResultHandler.handleTestSubmission(
            submissionId = "sid",
            subscriptionType = SubscriptionType.PREMIUM,
            testType = TestType.SRT,
            navController = navController
        )

        verify {
            navController.navigate(capture(routeSlot), any<NavOptionsBuilder.() -> Unit>())
        }
        assert(routeSlot.captured == SSBMaxDestinations.SRTSubmissionResult.createRoute("sid"))
    }

    @Test
    fun free_srt_navigates_to_pending_review() {
        val routeSlot = slot<String>()

        TestResultHandler.handleTestSubmission(
            submissionId = "sid",
            subscriptionType = SubscriptionType.FREE,
            testType = TestType.SRT,
            navController = navController
        )

        verify {
            navController.navigate(capture(routeSlot), any<NavOptionsBuilder.() -> Unit>())
        }
        assert(routeSlot.captured == SSBMaxDestinations.SubmissionDetail.createRoute("sid"))
    }

    @Test
    fun piq_always_navigates_to_result() {
        val routeSlot = slot<String>()

        TestResultHandler.handleTestSubmission(
            submissionId = "sid",
            subscriptionType = SubscriptionType.FREE,
            testType = TestType.PIQ,
            navController = navController
        )

        verify {
            navController.navigate(capture(routeSlot), any<NavOptionsBuilder.() -> Unit>())
        }
        assert(routeSlot.captured == SSBMaxDestinations.PIQSubmissionResult.createRoute("sid"))
    }
}
