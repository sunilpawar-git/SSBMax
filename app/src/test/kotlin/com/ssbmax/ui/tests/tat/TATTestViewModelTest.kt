package com.ssbmax.ui.tests.tat

import androidx.work.WorkManager
import app.cash.turbine.test
import com.ssbmax.shared.domain.model.EntryType
import com.ssbmax.shared.domain.model.Gender
import com.ssbmax.shared.domain.model.SSBMaxUser
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.TATPhase
import com.ssbmax.shared.domain.model.TATQuestion
import com.ssbmax.shared.domain.model.TATSubmission
import com.ssbmax.shared.domain.model.UserProfile
import com.ssbmax.shared.domain.model.UserRole
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitTATTestUseCase
import com.ssbmax.shared.domain.usecase.tat.LoadTATTestUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class TATTestViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: TATTestViewModel
    private val mockLoadTATTest = mockk<LoadTATTestUseCase>(relaxed = true)
    private val mockSubmitTATTest = mockk<SubmitTATTestUseCase>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)
    private val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)
    private val mockSubscriptionManager = mockk<com.ssbmax.core.data.repository.SubscriptionManager>(relaxed = true)
    private val mockDifficultyManager = mockk<com.ssbmax.core.data.repository.DifficultyProgressionManager>(relaxed = true)
    private val mockSecurityLogger = mockk<com.ssbmax.core.data.security.SecurityEventLogger>(relaxed = true)
    private val mockWorkManager = mockk<WorkManager>(relaxed = true)
    private val mockOrchestrator = mockk<TATAnalysisPipelineOrchestrator>(relaxed = true)

    private val mockQuestions = createMockTATQuestions()
    private val mockUser = SSBMaxUser(
        id = "test-user-123", email = "test@example.com", displayName = "Test User",
        photoUrl = null, role = UserRole.STUDENT,
        createdAt = System.currentTimeMillis(), lastLoginAt = System.currentTimeMillis()
    )
    private val mockUserProfile = UserProfile(
        userId = "test-user-123", fullName = "Test User", age = 22,
        gender = Gender.MALE, entryType = EntryType.GRADUATE,
        subscriptionType = SubscriptionType.FREE, createdAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        every { mockObserveCurrentUser() } returns flowOf(mockUser)
        coEvery { mockLoadTATTest(any(), any()) } returns Result.success(mockQuestions)
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(mockUserProfile))
        coEvery { mockSubmitTATTest(any(), any()) } returns Result.success("submission-tat-123")
        coEvery { mockOrchestrator.startPipeline(any(), any(), any()) } returns Result.success(Unit)
    }

    @Test
    fun `loadTest success loads 12 questions and shows instructions`() = runTest {
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should not have error", state.error)
            assertEquals("Should have 12 questions", 12, state.questions.size)
            assertEquals("Should be in instructions phase", TATPhase.INSTRUCTIONS, state.phase)
            assertNotNull("Should have config", state.config)
        }
        coVerify { mockLoadTATTest("test-user-123", "tat_standard") }
    }

    @Test
    fun `loadTest failure shows error message`() = runTest {
        coEvery { mockLoadTATTest(any(), any()) } returns Result.failure(Exception("Network error"))

        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not be loading", state.isLoading)
            assertNotNull("Should have error message", state.error)
            assertTrue("Error should be non-empty", !state.error.isNullOrBlank())
        }
    }

    @Test
    fun `updateStory updates current image story text`() = runTest {
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()

        viewModel.updateStory("This is my TAT story about the image.")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Story should be updated",
                "This is my TAT story about the image.", state.currentStory)
        }
    }

    @Test
    fun `updateStory handles long stories correctly`() = runTest {
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()

        val longStory = "A".repeat(1000)
        viewModel.updateStory(longStory)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Story should be stored", longStory, state.currentStory)
            assertTrue("Story length should be tracked", state.currentStory.length == 1000)
        }
    }

    @Test
    fun `currentQuestionIndex starts at 0`() = runTest {
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Should start at question 0", 0, state.currentQuestionIndex)
        }
    }

    @Test
    fun `responses list stores completed stories`() = runTest {
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("Responses should start empty", state.responses.isEmpty())
        }
    }

    @Test
    fun `submitTest submits all stories successfully`() = runTest {
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.updateStory("Story for image 1")
        advanceUntilIdle()

        viewModel.submitTest()
        advanceUntilIdle()

        coVerify { mockSubmitTATTest(any(), any()) }
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("Should be submitted", state.isSubmitted)
            assertNotNull("Should have submission ID", state.submissionId)
            assertEquals("Phase should be SUBMITTED", TATPhase.SUBMITTED, state.phase)
        }
    }

    @Test
    fun `submitTest failure shows error`() = runTest {
        coEvery { mockSubmitTATTest(any(), any()) } returns Result.failure(Exception("Submission failed"))

        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()

        viewModel.submitTest()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not be submitted", state.isSubmitted)
            assertNotNull("Should have error", state.error)
        }
    }

    @Test
    fun `submitTest saves current story before creating submission`() = runTest {
        val submissionSlot = slot<TATSubmission>()
        coEvery { mockSubmitTATTest(capture(submissionSlot), any()) } returns Result.success("submission-tat-123")

        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.updateStory("A".repeat(200))
        advanceUntilIdle()

        viewModel.submitTest()
        advanceUntilIdle()

        assertEquals("Current story should be included", 1, submissionSlot.captured.stories.size)
        assertEquals("A".repeat(200), submissionSlot.captured.stories.first().story)
    }

    @Test
    fun `loadTest populates questions with non-empty imageUrls`() = runTest {
        // WHY: imageUrl is the source bundled into each work request by the orchestrator.
        // If imageUrl is blank at load time, workers receive empty bytes and fall back to text-only.
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            val pictureCards = state.questions.filter { it.cardPosition < 12 }
            assertTrue("Must have loaded picture cards", pictureCards.isNotEmpty())
            pictureCards.forEach { q ->
                assertFalse("Card ${q.id} must have non-empty imageUrl", q.imageUrl.isBlank())
            }
        }
    }

    @Test
    fun `loadTest questions have unique IDs matching what worker KEY_QUESTION_ID receives`() = runTest {
        // WHY: orchestrator looks up each question by story.questionId.
        // Non-stable IDs cause KEY_IMAGE_URL to be passed as empty string.
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            val ids = state.questions.map { it.id }
            assertEquals("All question IDs must be unique", ids.distinct().size, ids.size)
            assertTrue("IDs must be non-empty strings", ids.all { it.isNotBlank() })
        }
    }

    private fun createViewModel() = TATTestViewModel(
        mockLoadTATTest, mockSubmitTATTest, mockObserveCurrentUser, mockUserProfileRepo,
        mockSubscriptionManager, mockDifficultyManager, mockSecurityLogger,
        mockWorkManager, mockOrchestrator
    )

    private fun createMockTATQuestions(): List<TATQuestion> =
        (1..11).map { index ->
            TATQuestion(id = "tat_q_$index", imageUrl = "https://example.com/tat_$index.jpg",
                cardPosition = index, viewingTimeSeconds = 30, writingTimeMinutes = 4,
                minCharacters = 150, maxCharacters = 1500)
        } + TATQuestion(id = "blank_card", imageUrl = "", cardPosition = 12,
            viewingTimeSeconds = 30, writingTimeMinutes = 4, minCharacters = 150, maxCharacters = 1500)
}
