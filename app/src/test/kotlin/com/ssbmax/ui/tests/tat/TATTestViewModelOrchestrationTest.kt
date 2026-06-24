package com.ssbmax.ui.tests.tat

import androidx.work.WorkManager
import app.cash.turbine.test
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.TATPhase
import com.ssbmax.core.domain.model.TATQuestion
import com.ssbmax.core.domain.model.TATStoryResponse
import com.ssbmax.core.domain.model.UserProfile
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitTATTestUseCase
import com.ssbmax.core.domain.usecase.tat.LoadTATTestUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for TATTestViewModel orchestration delegation (Phase 3).
 * Verifies that the ViewModel delegates pipeline start to TATAnalysisPipelineOrchestrator
 * instead of directly building the WorkManager chain.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TATTestViewModelOrchestrationTest : BaseViewModelTest() {

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

    private val mockUser = SSBMaxUser(
        id = "test-user-123", email = "test@example.com", displayName = "Test User",
        photoUrl = null, role = UserRole.STUDENT,
        createdAt = 1_700_000_000L, lastLoginAt = 1_700_000_000L
    )
    private val mockUserProfile = UserProfile(
        userId = "test-user-123", fullName = "Test User", age = 22,
        gender = Gender.MALE, entryType = EntryType.GRADUATE,
        subscriptionType = SubscriptionType.FREE, createdAt = 1_700_000_000L
    )

    @Before
    fun setup() {
        every { mockObserveCurrentUser() } returns flowOf(mockUser)
        coEvery { mockLoadTATTest(any(), any()) } returns Result.success(createMockTATQuestions())
        coEvery { mockUserProfileRepo.getUserProfile(any()) } returns flowOf(Result.success(mockUserProfile))
        coEvery { mockSubmitTATTest(any(), any()) } returns Result.success("submission-tat-123")
        coEvery { mockOrchestrator.startPipeline(any(), any(), any()) } returns Result.success(Unit)
    }

    @Test
    fun `delegates pipeline start to orchestrator after submit success`() = runTest {
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.updateStory("My TAT story for image 1")
        advanceUntilIdle()
        viewModel.submitTest()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockOrchestrator.startPipeline(eq("submission-tat-123"), any(), any())
        }
    }

    @Test
    fun `does not directly build work chain anymore`() = runTest {
        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.submitTest()
        advanceUntilIdle()

        // ViewModel must not touch WorkManager directly — orchestrator owns that
        verify(exactly = 0) { mockWorkManager.beginWith(any<androidx.work.OneTimeWorkRequest>()) }
        coVerify(exactly = 1) { mockOrchestrator.startPipeline(any(), any(), any()) }
    }

    @Test
    fun `handles orchestrator failure gracefully while still completing submission`() = runTest {
        coEvery { mockOrchestrator.startPipeline(any(), any(), any()) } returns
            Result.failure(Exception("WorkManager unavailable"))

        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.submitTest()
        advanceUntilIdle()

        // Submission is saved even if analysis pipeline fails to start
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("Submission must still be saved", state.isSubmitted)
            assertNotNull("SubmissionId must be set", state.submissionId)
            assertEquals("Phase must be SUBMITTED", TATPhase.SUBMITTED, state.phase)
        }
    }

    @Test
    fun `submission still contains all stories before pipeline begins`() = runTest {
        val storiesSlot = slot<List<TATStoryResponse>>()
        coEvery {
            mockOrchestrator.startPipeline(any(), capture(storiesSlot), any())
        } returns Result.success(Unit)

        viewModel = createViewModel()
        viewModel.loadTest("tat_standard")
        advanceUntilIdle()
        viewModel.updateStory("A well-developed story about courage and determination.")
        advanceUntilIdle()
        viewModel.submitTest()
        advanceUntilIdle()

        assertTrue("Pipeline must receive the captured stories", storiesSlot.isCaptured)
        assertTrue("Stories must include the written story",
            storiesSlot.captured.any { it.story.contains("courage") })
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
