package com.ssbmax.ui.tests.sdt

import app.cash.turbine.test
import com.ssbmax.core.data.repository.DifficultyProgressionManager
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.repository.TestEligibility
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitSDTTestUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SDTTestViewModelTest {

    private lateinit var viewModel: SDTTestViewModel
    private lateinit var testContentRepository: TestContentRepository
    private lateinit var submitSDTTest: SubmitSDTTestUseCase
    private lateinit var observeCurrentUser: ObserveCurrentUserUseCase
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var difficultyManager: DifficultyProgressionManager
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var securityLogger: SecurityEventLogger

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        testContentRepository = mockk(relaxed = true)
        submitSDTTest = mockk(relaxed = true)
        observeCurrentUser = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        difficultyManager = mockk(relaxed = true)
        subscriptionManager = mockk(relaxed = true)
        securityLogger = mockk(relaxed = true)

        coEvery { observeCurrentUser() } returns flowOf(createMockUser())
        coEvery { subscriptionManager.canTakeTest(any(), any()) } returns TestEligibility.Eligible(
            tier = SubscriptionTier.FREE,
            limit = 1,
            usedCount = 0,
            remainingTests = 1
        )
        coEvery { testContentRepository.createTestSession(any(), any(), any()) } returns Result.success("session123")
        coEvery { testContentRepository.getSDTQuestions(any()) } returns Result.success(createMockQuestions())
        coEvery { userProfileRepository.getUserProfile(any()) } returns flowOf(Result.success(createMockUserProfile()))

        viewModel = SDTTestViewModel(
            testContentRepository,
            submitSDTTest,
            observeCurrentUser,
            userProfileRepository,
            difficultyManager,
            subscriptionManager,
            securityLogger
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadTest success loads 4 questions and shows instructions`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)

            viewModel.loadTest("sdt_test_1")
            advanceUntilIdle()

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(4, loadedState.questions.size)
            assertEquals(SDTPhase.INSTRUCTIONS, loadedState.phase)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startTest transitions to in_progress and starts timer`() = runTest {
        viewModel.loadTest("sdt_test_1")
        advanceUntilIdle()

        viewModel.startTest()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(SDTPhase.IN_PROGRESS, state.phase)
            assertEquals(0, state.currentQuestionIndex)
            assertTrue(state.totalTimeRemaining <= 900)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `updateAnswer updates text correctly`() = runTest {
        viewModel.loadTest("sdt_test_1")
        advanceUntilIdle()
        viewModel.startTest()
        advanceUntilIdle()

        val testAnswer = "This is my test answer"
        viewModel.updateAnswer(testAnswer)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testAnswer, state.currentAnswer)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `canMoveToNext validates word count`() = runTest {
        viewModel.loadTest("sdt_test_1")
        advanceUntilIdle()
        viewModel.startTest()
        advanceUntilIdle()

        val validAnswer = "Valid answer with few words"
        viewModel.updateAnswer(validAnswer)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.canMoveToNext)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `moveToNext saves answer and moves to next question`() = runTest {
        viewModel.loadTest("sdt_test_1")
        advanceUntilIdle()
        viewModel.startTest()
        advanceUntilIdle()

        viewModel.updateAnswer("Answer to question 1")
        advanceUntilIdle()
        viewModel.moveToNext()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.currentQuestionIndex)
            assertEquals(1, state.responses.size)
            assertEquals("", state.currentAnswer)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `completing all questions transitions to review phase`() = runTest {
        viewModel.loadTest("sdt_test_1")
        advanceUntilIdle()
        viewModel.startTest()
        advanceUntilIdle()

        // Answer all 4 questions
        repeat(4) { index ->
            viewModel.updateAnswer("Answer to question ${index + 1}")
            advanceUntilIdle()
            viewModel.moveToNext()
            advanceUntilIdle()
        }

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(SDTPhase.REVIEW, state.phase)
            assertEquals(4, state.responses.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `submitTest creates submission with AI score`() = runTest {
        coEvery { submitSDTTest(any(), any()) } returns Result.success("submission123")

        viewModel.loadTest("sdt_test_1")
        advanceUntilIdle()
        viewModel.startTest()
        advanceUntilIdle()
        
        viewModel.updateAnswer("Test answer")
        advanceUntilIdle()
        viewModel.moveToNext()
        advanceUntilIdle()
        
        viewModel.submitTest()
        advanceUntilIdle()

        coVerify { submitSDTTest(any(), any()) }
        coVerify { subscriptionManager.recordTestUsage(TestType.SD, any()) }
    }

    @Test
    fun `submitTest records subscription usage`() = runTest {
        coEvery { submitSDTTest(any(), any()) } returns Result.success("submission123")

        viewModel.loadTest("sdt_test_1")
        advanceUntilIdle()
        viewModel.startTest()
        advanceUntilIdle()
        viewModel.submitTest()
        advanceUntilIdle()

        coVerify { subscriptionManager.recordTestUsage(TestType.SD, any()) }
    }

    @Test
    fun `progress is calculated correctly`() = runTest {
        viewModel.loadTest("sdt_test_1")
        advanceUntilIdle()
        viewModel.startTest()
        advanceUntilIdle()

        viewModel.updateAnswer("Answer 1")
        advanceUntilIdle()
        viewModel.moveToNext()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0.25f, state.progress)
            cancelAndConsumeRemainingEvents()
        }
    }

    private fun createMockUser() = User(
        id = "user123",
        email = "test@example.com",
        name = "Test User",
        role = UserRole.STUDENT
    )

    private fun createMockUserProfile() = UserProfile(
        userId = "user123",
        name = "Test User",
        email = "test@example.com",
        subscriptionType = SubscriptionType.FREE
    )

    private fun createMockQuestions() = createStandardSDTQuestions()
}

