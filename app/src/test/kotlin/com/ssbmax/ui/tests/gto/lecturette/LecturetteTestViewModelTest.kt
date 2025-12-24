package com.ssbmax.ui.tests.gto.lecturette

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class LecturetteTestViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: LecturetteTestViewModel
    private val mockGtoRepository = mockk<GTORepository>(relaxed = true)
    private val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockEligibilityChecker = mockk<GTOTestEligibilityChecker>(relaxed = true)
    private val mockSubmissionHelper = mockk<GTOTestSubmissionHelper>(relaxed = true)

    private val mockTopics = listOf(
        "Leadership in the Indian Armed Forces",
        "Role of Technology in Modern Warfare",
        "Discipline and its Importance",
        "Teamwork in Military Operations"
    )

    @Before
    fun setup() {
        // Setup default success behavior
        coEvery { mockEligibilityChecker.checkEligibility(any(), any()) } returns 
            GTOTestEligibilityChecker.EligibilityResult.Eligible(
                userId = "user-lec-123",
                subscriptionType = SubscriptionType.PREMIUM
            )
        coEvery { mockTestContentRepo.getRandomLecturetteTopics(4) } returns Result.success(mockTopics)

        viewModel = LecturetteTestViewModel(
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
        assertEquals(emptyList<String>(), state.topicChoices)
        assertEquals(LecturettePhase.INSTRUCTIONS, state.phase)
    }

    @Test
    fun `loadTest success loads 4 topics and updates state`() = runTest {
        val testId = "lec-test-123"

        viewModel.loadTest(testId)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(testId, state.testId)
        assertEquals("user-lec-123", state.userId)
        assertEquals(4, state.topicChoices.size)
        assertEquals(mockTopics, state.topicChoices)
        assertEquals(SubscriptionType.PREMIUM, state.subscriptionType)
        assertEquals(LecturettePhase.INSTRUCTIONS, state.phase)

        coVerify {
            mockEligibilityChecker.checkEligibility(
                TestType.GTO_LECTURETTE,
                GTOTestType.LECTURETTE
            )
        }
        coVerify { mockTestContentRepo.getRandomLecturetteTopics(4) }
    }

    @Test
    fun `loadTest failure shows error message`() = runTest {
        coEvery { mockTestContentRepo.getRandomLecturetteTopics(4) } returns Result.failure(
            Exception("Network error")
        )

        viewModel.loadTest("lec-test-123")
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
                message = "Monthly limit reached for Lecturette."
            )

        viewModel.loadTest("lec-test-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.showLimitDialog)
        assertNotNull(state.limitMessage)
    }

    @Test
    fun `proceedToTopicSelection changes phase to TOPIC_SELECTION`() = runTest {
        viewModel.loadTest("lec-test-123")
        advanceUntilIdle()

        viewModel.proceedToTopicSelection()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(LecturettePhase.TOPIC_SELECTION, state.phase)
    }

    @Test
    fun `selectTopic sets topic and moves to SPEECH phase`() = runTest {
        viewModel.loadTest("lec-test-123")
        advanceUntilIdle()

        val selectedTopic = mockTopics[0]
        viewModel.selectTopic(selectedTopic)
        // Don't call advanceUntilIdle() - timer is running continuously

        val state = viewModel.uiState.value

        assertEquals(selectedTopic, state.selectedTopic)
        assertEquals(LecturettePhase.SPEECH, state.phase)
        // Timer may have decremented, check it's close to 180
        assertTrue(state.timeRemaining > 170 && state.timeRemaining <= 180)
        assertTrue(state.speechStartTime > 0)
    }

    @Test
    fun `onTranscriptChanged updates transcript and char count`() = runTest {
        val transcript = "This is my speech on leadership in the Indian Armed Forces."

        viewModel.onTranscriptChanged(transcript)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(transcript, state.speechTranscript)
        assertEquals(transcript.trim().length, state.charCount)
    }

    @Test
    fun `proceedToReview validates minimum character count`() = runTest {
        viewModel.onTranscriptChanged("Too short")
        advanceUntilIdle()

        viewModel.proceedToReview()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertNotNull(state.validationError)
        assertTrue(state.validationError!!.contains("at least 50 characters"))
    }

    @Test
    fun `proceedToReview succeeds with valid transcript`() = runTest {
        val validTranscript = "A".repeat(100)
        viewModel.onTranscriptChanged(validTranscript)
        advanceUntilIdle()

        viewModel.proceedToReview()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(LecturettePhase.REVIEW, state.phase)
        assertNull(state.validationError)
    }

    @Test
    fun `submitTest creates submission and updates state on success`() = runTest {
        // Setup state
        viewModel.loadTest("lec-test-123")
        advanceUntilIdle()

        viewModel.selectTopic(mockTopics[0])
        viewModel.onTranscriptChanged("A".repeat(200))
        advanceUntilIdle()

        // Mock successful submission
        val submissionSlot = slot<GTOSubmission.LecturetteSubmission>()
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
            onSuccess("submission-lec-456")
        }

        viewModel.submitTest()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isSubmitting)
        assertEquals(LecturettePhase.SUBMITTED, state.phase)
        assertEquals("submission-lec-456", state.submissionId)
        assertTrue(state.isCompleted)

        // Verify submission data
        val capturedSubmission = submissionSlot.captured
        assertEquals("user-lec-123", capturedSubmission.userId)
        assertEquals(mockTopics[0], capturedSubmission.selectedTopic)
        assertEquals(200, capturedSubmission.charCount)
    }

    @Test
    fun `dismissError clears all error messages`() = runTest {
        coEvery { mockTestContentRepo.getRandomLecturetteTopics(4) } returns Result.failure(
            Exception("Network error")
        )
        viewModel.loadTest("lec-test-123")
        advanceUntilIdle()

        viewModel.dismissError()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertNull(state.error)
        assertNull(state.validationError)
        assertNull(state.submitError)
    }
}
