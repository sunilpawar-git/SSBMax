package com.ssbmax.ui.tests.common

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.navigation.SSBMaxDestinations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestResultHandlerTest {

    private val navController: NavController = mockk(relaxed = true) {
        every { graph.startDestinationId } returns 1
    }

    @Test
    fun premium_srt_navigates_to_result() {
        TestResultHandler.handleTestSubmission(
            submissionId = "sid",
            subscriptionType = SubscriptionType.PREMIUM,
            testType = TestType.SRT,
            navController = navController
        )

        verify {
            navController.navigate(eq(SSBMaxDestinations.SRTSubmissionResult.createRoute("sid")), any<NavOptionsBuilder.() -> Unit>())
        }
    }

    @Test
    fun free_srt_navigates_to_pending_review() {
        TestResultHandler.handleTestSubmission(
            submissionId = "sid",
            subscriptionType = SubscriptionType.FREE,
            testType = TestType.SRT,
            navController = navController
        )

        verify {
            navController.navigate(eq(SSBMaxDestinations.SubmissionDetail.createRoute("sid")), any<NavOptionsBuilder.() -> Unit>())
        }
    }

    @Test
    fun piq_always_navigates_to_result() {
        TestResultHandler.handleTestSubmission(
            submissionId = "sid",
            subscriptionType = SubscriptionType.FREE,
            testType = TestType.PIQ,
            navController = navController
        )

        verify {
            navController.navigate(eq(SSBMaxDestinations.PIQSubmissionResult.createRoute("sid")), any<NavOptionsBuilder.() -> Unit>())
        }
    }
}













