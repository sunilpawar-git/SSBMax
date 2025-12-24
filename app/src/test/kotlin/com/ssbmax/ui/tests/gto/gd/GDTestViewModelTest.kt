package com.ssbmax.ui.tests.gto.gd

import app.cash.turbine.test
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.testing.BaseViewModelTest
import com.ssbmax.ui.tests.gto.common.GTOTestEligibilityChecker
import com.ssbmax.ui.tests.gto.common.GTOTestSubmissionHelper
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GDTestViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: GDTestViewModel
    private val mockGtoRepository = mockk<GTORepository>(relaxed = true)
    private val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockEligibilityChecker = mockk<GTOTestEligibilityChecker>(relaxed = true)
    private val mockSubmissionHelper = mockk<GTOTestSubmissionHelper>(relaxed = true)

    private val mockTopic = "Leadership in challenging situations"

    @Before
    fun setup() {
        // Setup default success behavior
        coEvery { mockEligibilityChecker.checkEligibility(any(), any()) } returns 
            GTOTestEligibilityChecker.EligibilityResult.Eligible(
                userId = "user-gd-123",
                subscriptionType = SubscriptionType.PREMIUM
            )
        coEvery { mockTestContentRepo.getRandomGDTopic() } returns Result.success(mockTopic)

        viewModel = GDTestViewModel(
            mockGtoRepository,
            mockTestContentRepo,
            mockEligibilityChecker,
            mockSubmissionHelper
        )
    }

    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("", state.testId)
        assertEquals("", state.userId)
        assertEquals("", state.topic)
        assertEquals(GDPhase.INSTRUCTIONS, state.phase)
    }

    @Test
    fun `loadTest success loads topic and updates state`() = runTest {
        val testId = "gd-test-123"

        viewModel.loadTest(testId)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(testId, state.testId)
        assertEquals("user-gd-123", state.userId)
        assertEquals(mockTopic, state.topic)
        assertEquals(SubscriptionType.PREMIUM, state.subscriptionType)
        assertEquals(GDPhase.INSTRUCTIONS, state.phase)

        coVerify {
            mockEligibilityChecker.checkEligibility(
                TestType.GTO_GD,
                GTOTestType.GROUP_DISCUSSION
            )
        }
        coVerify { mockTestContentRepo.getRandomGDTopic() }
    }

    @Test
    fun `loadTest failure shows error message`() = runTest {
        coEvery { mockTestContentRepo.getRandomGDTopic() } returns Result.failure(
            Exception("Network error")
        )

        viewModel.loadTest("gd-test-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.isNotBlank())
    }

    @Test
    fun `loadTest shows limit dialog when limit reached`() = runTest {
        coEvery { mockEligibilityChecker.checkEligibility(any(), any()) } returns
            GTOTestEligibilityChecker.EligibilityResult.LimitReached(
                message = "Monthly limit reached. Upgrade to PRO for more tests."
            )

        viewModel.loadTest("gd-test-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.showLimitDialog)
        assertNotNull(state.limitMessage)
        assertEquals("Monthly limit reached. Upgrade to PRO for more tests.", state.limitMessage)
    }

    @Test
    fun `onResponseChanged updates response and char count`() = runTest {
        val testResponse = "This is my response to the group discussion topic about leadership."

        viewModel.onResponseChanged(testResponse)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(testResponse, state.response)
        assertEquals(testResponse.trim().length, state.charCount)
    }

    @Test
    fun `proceedToReview validates minimum character count`() = runTest {
        // Set response below minimum (50 chars)
        viewModel.onResponseChanged("Too short")
        advanceUntilIdle()

        viewModel.proceedToReview()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Should stay in current phase
        assertNotNull(state.validationError)
        assertTrue(state.validationError!!.contains("at least 50 characters"))
    }

    @Test
    fun `proceedToReview validates maximum character count`() = runTest {
        // Set response above maximum (1500 chars)
        val longResponse = "A".repeat(1501)
        viewModel.onResponseChanged(longResponse)
        advanceUntilIdle()

        viewModel.proceedToReview()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertNotNull(state.validationError)
        assertTrue(state.validationError!!.contains("must not exceed 1500 characters"))
    }

    @Test
    fun `proceedToReview succeeds with valid response`() = runTest {
        // Set valid response (between 50-1500 chars)
        val validResponse = "A".repeat(100)
        viewModel.onResponseChanged(validResponse)
        advanceUntilIdle()

        viewModel.proceedToReview()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(GDPhase.REVIEW, state.phase)
        assertNull(state.validationError)
    }

    @Test
    fun `submitTest creates submission and updates state on success`() = runTest {
        // Setup state
        viewModel.loadTest("gd-test-123")
        advanceUntilIdle()

        viewModel.onResponseChanged("A".repeat(200))
        advanceUntilIdle()

        // Mock successful submission
        val submissionSlot = slot<GTOSubmission.GDSubmission>()
        coEvery { 
            mockSubmissionHelper.submitTest(
                capture(submissionSlot),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            val onSuccess = arg<(String) -> Unit>(3)
            onSuccess("submission-gd-456")
        }

        viewModel.submitTest()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isSubmitting)
        assertEquals(GDPhase.SUBMITTED, state.phase)
        assertEquals("submission-gd-456", state.submissionId)
        assertTrue(state.isCompleted)

        // Verify submission data
        val capturedSubmission = submissionSlot.captured
        assertEquals("user-gd-123", capturedSubmission.userId)
        assertEquals(mockTopic, capturedSubmission.topic)
        assertEquals(200, capturedSubmission.charCount)
    }

    @Test
    fun `submitTest shows error on failure`() = runTest {
        // Setup state
        viewModel.loadTest("gd-test-123")
        advanceUntilIdle()

        viewModel.onResponseChanged("A".repeat(200))
        advanceUntilIdle()

        // Mock failed submission
        coEvery { 
            mockSubmissionHelper.submitTest(any(), any(), any(), any(), any())
        } answers {
            val onError = arg<(String) -> Unit>(4)
            onError("Submission failed")
        }

        viewModel.submitTest()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isSubmitting)
        assertNotNull(state.submitError)
        assertEquals("Submission failed", state.submitError)
    }

    @Test
    fun `dismissError clears all error messages`() = runTest {
        // Trigger error
        coEvery { mockTestContentRepo.getRandomGDTopic() } returns Result.failure(
            Exception("Network error")
        )
        viewModel.loadTest("gd-test-123")
        advanceUntilIdle()

        // Dismiss error
        viewModel.dismissError()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertNull(state.error)
        assertNull(state.validationError)
        assertNull(state.submitError)
    }

    @Test
    fun `dismissLimitDialog hides limit dialog`() = runTest {
        coEvery { mockEligibilityChecker.checkEligibility(any(), any()) } returns
            GTOTestEligibilityChecker.EligibilityResult.LimitReached(
                message = "Limit reached"
            )
        viewModel.loadTest("gd-test-123")
        advanceUntilIdle()

        // Dismiss dialog
        viewModel.dismissLimitDialog()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.showLimitDialog)
    }
}
