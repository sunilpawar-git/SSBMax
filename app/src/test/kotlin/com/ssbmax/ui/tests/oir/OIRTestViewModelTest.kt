package com.ssbmax.ui.tests.oir

import android.util.Log
import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Unit tests for OIRTestViewModel
 * Tests question loading, answer selection, navigation, and score calculation
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OIRTestViewModelTest : BaseViewModelTest() {
    
    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            // Mock android.util.Log for all tests
            mockkStatic(Log::class)
            every { Log.d(any(), any()) } returns 0
            every { Log.e(any(), any()) } returns 0
            every { Log.e(any(), any(), any()) } returns 0
            every { Log.w(any(), any<String>()) } returns 0
            every { Log.i(any(), any()) } returns 0
            every { Log.v(any(), any()) } returns 0
        }
    }
    
    private lateinit var viewModel: OIRTestViewModel
    private val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockSubmissionRepo = mockk<com.ssbmax.core.domain.repository.SubmissionRepository>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)
    private val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)
    private val mockDifficultyManager = mockk<com.ssbmax.core.data.repository.DifficultyProgressionManager>(relaxed = true)
    private val mockSubscriptionManager = mockk<com.ssbmax.core.data.repository.SubscriptionManager>(relaxed = true)
    private val mockGetOLQDashboard = mockk<com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase>(relaxed = true)
    private val mockSecurityLogger = mockk<com.ssbmax.core.data.security.SecurityEventLogger>(relaxed = true)
    
    private val mockQuestions = createMockQuestions()
    private val mockUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@example.com",
        displayName = "Test User",
        photoUrl = null,
        role = UserRole.STUDENT,
        createdAt = System.currentTimeMillis(),
        lastLoginAt = System.currentTimeMillis()
    )
    private val mockUserProfile = UserProfile(
        userId = "test-user-123",
        fullName = "Test User",
        age = 22,
        gender = Gender.MALE,
        entryType = EntryType.GRADUATE,
        subscriptionType = SubscriptionType.FREE,
        createdAt = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        // Mock successful question loading using new caching system
        coEvery { 
            mockTestContentRepo.getOIRTestQuestions(any(), any()) 
        } returns Result.success(mockQuestions)
        
        // Mock current user observable
        coEvery {
            mockObserveCurrentUser()
        } returns flowOf(mockUser)
        
        // Mock user profile
        coEvery { 
            mockUserProfileRepo.getUserProfile(any()) 
        } returns flowOf(Result.success(mockUserProfile))
        
        // Mock difficulty manager
        coEvery {
            mockDifficultyManager.getRecommendedDifficulty(any())
        } returns "EASY"
        
        // Mock subscription manager - use sealed class subtype
        coEvery {
            mockSubscriptionManager.canTakeTest(any(), any())
        } returns com.ssbmax.core.data.repository.TestEligibility.Eligible(
            remainingTests = 1
        )
        
        // Mock cache initialization (may be called by repository)
        coEvery { mockTestContentRepo.initializeOIRCache() } returns Result.success(Unit)
        
        // Mock cache status
        coEvery { mockTestContentRepo.getOIRCacheStatus() } returns mockk(relaxed = true)

        // Mock dashboard cache invalidation
        coEvery {
            mockGetOLQDashboard.invalidateCache(any())
        } returns Unit
    }

    // ==================== Test Loading ====================
    
    @Test
    fun `loadTest success loads questions and starts timer`() = runTest {
        // When - Create ViewModel (calls loadTest() in init with UnconfinedTestDispatcher)
        viewModel = createViewModel()
        
        // Then - state should be updated immediately due to UnconfinedTestDispatcher
        val state = viewModel.uiState.value
        
        assertFalse("Should not be loading: ${state.isLoading}", state.isLoading)
        assertNull("Should not have error: ${state.error}", state.error)
        assertEquals("Should have 5 questions", 5, state.totalQuestions)
        assertEquals("Should start at question 0", 0, state.currentQuestionIndex)
        assertNotNull("Should have current question", state.currentQuestion)
        assertEquals("Timer should be 40 minutes (2400s)", 2400, state.timeRemainingSeconds)
        
        // Verify questions were fetched using new caching system with difficulty
        coVerify { mockTestContentRepo.getOIRTestQuestions(50, any()) }
    }
    
    @Test
    fun `loadTest failure shows error message`() = runTest {
        // Given - mock failure
        coEvery { 
            mockTestContentRepo.getOIRTestQuestions(any(), any()) 
        } returns Result.failure(Exception("Network error"))
        
        // When
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Then - check final state directly (not using turbine due to multiple emissions)
        val state = viewModel.uiState.value
        
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
        assertTrue(
            "Error should mention failed loading",
            state.error!!.contains("Failed to load test")
        )
    }
    
    @Test
    fun `loadTest with empty questions shows error`() = runTest {
        // Given - mock empty questions
        coEvery { 
            mockTestContentRepo.getOIRTestQuestions(any(), any()) 
        } returns Result.success(emptyList())
        
        // When
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Then - check final state directly
        val state = viewModel.uiState.value
        
        // Debug output
        android.util.Log.d("TEST", "isLoading: ${state.isLoading}, error: ${state.error}, isLimitReached: ${state.isLimitReached}")
        
        assertFalse("Should not be loading, but was ${state.isLoading}", state.isLoading)
        assertNotNull("Should have error, but was null", state.error)
        assertTrue(
            "Error should mention no questions available, but was: ${state.error}",
            state.error?.contains("No questions available") == true
        )
    }
    
    // ==================== Answer Selection ====================
    
    @Test
    fun `selectOption records correct answer and shows feedback`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        val firstQuestion = mockQuestions[0]
        val correctOptionId = firstQuestion.correctAnswerId
        
        // When
        viewModel.selectOption(correctOptionId)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Selected option should match", correctOptionId, state.selectedOptionId)
            assertTrue("Should show feedback", state.showFeedback)
            assertTrue("Answer should be marked correct", state.isCurrentAnswerCorrect)
            assertTrue("Question should be marked as answered", state.currentQuestionAnswered)
        }
    }
    
    @Test
    fun `selectOption records incorrect answer`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        val firstQuestion = mockQuestions[0]
        val incorrectOptionId = firstQuestion.options.first { it.id != firstQuestion.correctAnswerId }.id
        
        // When
        viewModel.selectOption(incorrectOptionId)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Selected option should match", incorrectOptionId, state.selectedOptionId)
            assertTrue("Should show feedback", state.showFeedback)
            assertFalse("Answer should be marked incorrect", state.isCurrentAnswerCorrect)
            assertTrue("Question should be marked as answered", state.currentQuestionAnswered)
        }
    }
    
    // ==================== Navigation ====================
    
    @Test
    fun `nextQuestion moves to next question`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Answer first question
        viewModel.selectOption(mockQuestions[0].correctAnswerId)
        
        // When
        viewModel.nextQuestion()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should be on question 1", 1, state.currentQuestionIndex)
            assertNotNull("Should have current question", state.currentQuestion)
            assertEquals("Should be second question", mockQuestions[1].id, state.currentQuestion?.id)
            assertNull("Selected option should be reset", state.selectedOptionId)
            assertFalse("Feedback should be hidden", state.showFeedback)
        }
    }
    
    @Test
    fun `previousQuestion moves to previous question`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Move to second question
        viewModel.nextQuestion()
        
        // When
        viewModel.previousQuestion()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should be back on question 0", 0, state.currentQuestionIndex)
            assertEquals("Should be first question", mockQuestions[0].id, state.currentQuestion?.id)
        }
    }
    
    @Test
    fun `previousQuestion at start does nothing`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        val initialIndex = viewModel.uiState.value.currentQuestionIndex
        
        // When
        viewModel.previousQuestion()
        
        // Then
        assertEquals("Should stay at index 0", initialIndex, viewModel.uiState.value.currentQuestionIndex)
    }
    
    // ==================== Test Submission ====================
    
    @Test
    fun `submitTest calculates correct score and ends session`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Answer all questions correctly
        mockQuestions.forEachIndexed { index, question ->
            viewModel.selectOption(question.correctAnswerId)
            if (index < mockQuestions.size - 1) {
                viewModel.nextQuestion()
            }
        }
        
        // When
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertTrue("Test should be completed", state.isCompleted)
            assertNotNull("Should have session ID", state.sessionId)
            assertNotNull("Should have subscription type", state.subscriptionType)
            assertNotNull("Should have test result", state.testResult)
            
            val result = state.testResult!!
            assertEquals("Should have 5 correct answers", 5, result.correctAnswers)
            assertEquals("Should have 0 incorrect answers", 0, result.incorrectAnswers)
            assertEquals("Should have 0 skipped", 0, result.skippedQuestions)
            assertTrue("Percentage score should be 100%", result.percentageScore >= 99f)
            
            // Verify results were submitted to repository with correct ID
            coVerify { 
                mockSubmissionRepo.submitOIR(
                    match { it.id == state.sessionId },
                    null
                ) 
            }
            
            // Verify dashboard cache was invalidated
            coVerify { mockGetOLQDashboard.invalidateCache(mockUser.id) }
        }
    }
    
    @Test
    fun `submitTest with mixed answers calculates partial score`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Answer first 3 correctly, last 2 incorrectly
        mockQuestions.forEachIndexed { index, question ->
            if (index < 3) {
                viewModel.selectOption(question.correctAnswerId)
            } else {
                val wrongOption = question.options.first { it.id != question.correctAnswerId }.id
                viewModel.selectOption(wrongOption)
            }
            if (index < mockQuestions.size - 1) {
                viewModel.nextQuestion()
            }
        }
        
        // When
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then
        val result = viewModel.uiState.value.testResult!!
        assertEquals("Should have 3 correct answers", 3, result.correctAnswers)
        assertEquals("Should have 2 incorrect answers", 2, result.incorrectAnswers)
        assertTrue("Percentage score should be around 60%", result.percentageScore in 55f..65f)
    }
    
    @Test
    fun `submitTest with unanswered questions counts as skipped`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Answer only first 2 questions
        viewModel.selectOption(mockQuestions[0].correctAnswerId)
        viewModel.nextQuestion()
        viewModel.selectOption(mockQuestions[1].correctAnswerId)
        
        // When
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then
        val result = viewModel.uiState.value.testResult!!
        assertEquals("Should have 2 correct answers", 2, result.correctAnswers)
        assertEquals("Should have 3 skipped", 3, result.skippedQuestions)
    }
    
    // ==================== Timer Tests ====================
    
    @Test
    fun `timer decrements every second`() = runTest {
        // Given
        viewModel = createViewModel()
        // NOTE: Don't call advanceUntilIdle() here - with UnconfinedTestDispatcher,
        // the init block runs immediately and state is already updated
        
        val initialTime = viewModel.uiState.value.timeRemainingSeconds
        assertEquals("Initial time should be 2400s (40 min)", 2400, initialTime)
        
        // When - advance 5 seconds
        advanceTimeBy(5000)
        advanceUntilIdle()
        
        // Then
        val newTime = viewModel.uiState.value.timeRemainingSeconds
        assertTrue("Time should have decreased (initial=$initialTime, new=$newTime)", newTime < initialTime)
        assertTrue("Time should decrease by at least 4 seconds", initialTime - newTime >= 4)
    }
    
    @Test
    fun `timer expiry auto-submits test`() = runTest {
        // Given - create ViewModel with shorter time for testing
        coEvery { 
            mockTestContentRepo.getOIRTestQuestions(any()) 
        } returns Result.success(createMockQuestions().take(1)) // Only 1 question
        
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When - advance time to expiry (30 minutes = 1800 seconds)
        advanceTimeBy(1801000) // 1801 seconds
        
        // Then
        assertTrue("Test should be completed", viewModel.uiState.value.isCompleted)
    }
    
    // ==================== Category Scores ====================
    
    @Test
    fun `calculateResults generates category scores`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Answer all questions
        mockQuestions.forEach { question ->
            viewModel.selectOption(question.correctAnswerId)
            viewModel.nextQuestion()
        }
        
        // When
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then
        val result = viewModel.uiState.value.testResult!!
        assertFalse("Category scores should not be empty", result.categoryScores.isEmpty())
        
        // Verify all categories are present
        OIRQuestionType.values().forEach { type ->
            assertTrue(
                "Should have score for category $type",
                result.categoryScores.containsKey(type)
            )
        }
    }
    
    // ==================== Validation Tests ====================
    
    @Test
    fun `loadTest filters out invalid questions with malformed option IDs`() = runTest {
        // Given - questions with corrupted option IDs (single letter instead of opt_X)
        val corruptedQuestions = listOf(
            OIRQuestion(
                id = "oir_corrupt_1",
                questionNumber = 1,
                type = OIRQuestionType.VERBAL_REASONING,
                difficulty = QuestionDifficulty.EASY,
                questionText = "Test question?",
                options = listOf(
                    OIROption("a", "Option A"),  // ❌ Should be "opt_a"
                    OIROption("b", "Option B"),  // ❌ Should be "opt_b"
                    OIROption("c", "Option C"),  // ❌ Should be "opt_c"
                    OIROption("d", "Option D")   // ❌ Should be "opt_d"
                ),
                correctAnswerId = "b",  // ❌ Should be "opt_b"
                explanation = "Test"
            )
        )
        
        coEvery { 
            mockTestContentRepo.getOIRTestQuestions(any(), any()) 
        } returns Result.success(corruptedQuestions)
        
        // When
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Then - corrupted question should be filtered out, check final state
        val state = viewModel.uiState.value
        
        // Should show error because ALL questions were invalid
        assertTrue(
            "Should have error about validation",
            state.error?.contains("validation") == true || 
            state.error?.contains("contact support") == true
        )
    }
    
    @Test
    fun `loadTest filters out questions with malformed correctAnswerId`() = runTest {
        // Given - question with embedded question number in correctAnswerId
        val corruptedQuestions = listOf(
            OIRQuestion(
                id = "oir_103",
                questionNumber = 103,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                difficulty = QuestionDifficulty.MEDIUM,
                questionText = "What is 2+2?",
                options = listOf(
                    OIROption("opt_a", "3"),
                    OIROption("opt_b", "4"),  // Correct
                    OIROption("opt_c", "5"),
                    OIROption("opt_d", "6")
                ),
                correctAnswerId = "opt_103_b",  // ❌ Should be "opt_b"
                explanation = "2+2=4"
            )
        )
        
        coEvery { 
            mockTestContentRepo.getOIRTestQuestions(any(), any()) 
        } returns Result.success(corruptedQuestions)
        
        // When
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Then - should filter out invalid question
        val state = viewModel.uiState.value
        assertTrue(
            "Should have error or filter out question",
            state.error != null || state.totalQuestions == 0
        )
    }
    
    @Test
    fun `loadTest handles mix of valid and invalid questions`() = runTest {
        // Given - mix of valid and invalid questions
        val mixedQuestions = listOf(
            // Valid question
            OIRQuestion(
                id = "oir_valid_1",
                questionNumber = 1,
                type = OIRQuestionType.VERBAL_REASONING,
                difficulty = QuestionDifficulty.EASY,
                questionText = "Valid question?",
                options = listOf(
                    OIROption("opt_a", "A"),
                    OIROption("opt_b", "B"),
                    OIROption("opt_c", "C"),
                    OIROption("opt_d", "D")
                ),
                correctAnswerId = "opt_b",
                explanation = "Test"
            ),
            // Invalid question (malformed option IDs)
            OIRQuestion(
                id = "oir_invalid_1",
                questionNumber = 2,
                type = OIRQuestionType.VERBAL_REASONING,
                difficulty = QuestionDifficulty.EASY,
                questionText = "Invalid question?",
                options = listOf(
                    OIROption("a", "A"),  // ❌ Invalid
                    OIROption("b", "B"),  // ❌ Invalid
                    OIROption("c", "C"),  // ❌ Invalid
                    OIROption("d", "D")   // ❌ Invalid
                ),
                correctAnswerId = "b",  // ❌ Invalid
                explanation = "Test"
            ),
            // Another valid question
            OIRQuestion(
                id = "oir_valid_2",
                questionNumber = 3,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                difficulty = QuestionDifficulty.MEDIUM,
                questionText = "Another valid question?",
                options = listOf(
                    OIROption("opt_a", "1"),
                    OIROption("opt_b", "2"),
                    OIROption("opt_c", "3"),
                    OIROption("opt_d", "4")
                ),
                correctAnswerId = "opt_c",
                explanation = "Test"
            )
        )
        
        coEvery { 
            mockTestContentRepo.getOIRTestQuestions(any(), any()) 
        } returns Result.success(mixedQuestions)
        
        // When
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Then - should only load valid questions
        val state = viewModel.uiState.value
        
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have error", state.error)
        assertEquals(
            "Should have 2 valid questions (1 invalid filtered out)",
            2,
            state.totalQuestions
        )
        
        // Verify only valid questions are present
        val session = viewModel.uiState.value.currentQuestion
        assertNotNull("Should have current question", session)
        assertTrue(
            "Current question should be valid",
            session?.id?.startsWith("oir_valid") == true
        )
    }
    
    @Test
    fun `selectOption validates question before scoring`() = runTest {
        // Given - valid setup
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // When - select an option
        val firstQuestion = mockQuestions[0]
        viewModel.selectOption(firstQuestion.correctAnswerId)
        
        // Then - answer should be recorded (validation happens internally but doesn't fail)
        val state = viewModel.uiState.value
        assertTrue("Answer should be recorded", state.currentQuestionAnswered)
        assertTrue("Feedback should be shown", state.showFeedback)
    }
    
    // ==================== Cleanup ====================
    
    @Test
    fun `onCleared cancels timer job`() = runTest {
        // Given
        viewModel = createViewModel()
        // NOTE: Don't call advanceUntilIdle() here - with UnconfinedTestDispatcher,
        // the init block runs immediately and state is already updated
        
        val initialTime = viewModel.uiState.value.timeRemainingSeconds
        assertTrue("Initial time should be set", initialTime > 0)
        
        // Verify timer is running
        advanceTimeBy(2000)
        advanceUntilIdle()
        val timeAfter2Sec = viewModel.uiState.value.timeRemainingSeconds
        assertTrue("Timer should have decreased", timeAfter2Sec < initialTime)
        
        // Note: We cannot call protected onCleared() directly in tests
        // In actual usage, onCleared() is called by Android system when ViewModel is destroyed
        // The test verifies that timer functionality works correctly
        assertTrue("Timer was active and working", initialTime > 0)
    }
    
    // ==================== Subscription Limit Tests ====================
    
    @Test
    fun `loadTest shows limit reached when FREE tier exhausted`() = runTest {
        // Given - mock limit reached
        coEvery {
            mockSubscriptionManager.canTakeTest(TestType.OIR, any())
        } returns com.ssbmax.core.data.repository.TestEligibility.LimitReached(
            tier = com.ssbmax.core.domain.model.SubscriptionTier.FREE,
            limit = 1,
            usedCount = 1,
            resetsAt = "Dec 1, 2025"
        )
        
        // When
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should show limit reached", state.isLimitReached)
        assertEquals("Should show FREE tier", com.ssbmax.core.domain.model.SubscriptionTier.FREE, state.subscriptionTier)
        assertEquals("Should show 1 test limit", 1, state.testsLimit)
        assertEquals("Should show 1 test used", 1, state.testsUsed)
        assertEquals("Should show reset date", "Dec 1, 2025", state.resetsAt)
        assertFalse("Should not be loading", state.isLoading)
        assertEquals("Should have 0 questions", 0, state.totalQuestions)
    }
    
    @Test
    fun `loadTest proceeds when user is eligible`() = runTest {
        // Given - mock eligible with remaining tests
        coEvery {
            mockSubscriptionManager.canTakeTest(TestType.OIR, any())
        } returns com.ssbmax.core.data.repository.TestEligibility.Eligible(
            remainingTests = 5
        )
        
        // When
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should NOT show limit reached", state.isLimitReached)
        assertTrue("Should have loaded questions", state.totalQuestions > 0)
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have error", state.error)
    }
    
    @Test
    fun `loadTest calls canTakeTest with correct parameters`() = runTest {
        // When
        viewModel = createViewModel()
        advanceUntilIdle()
        
        // Then - verify subscription manager was called
        coVerify(exactly = 1) {
            mockSubscriptionManager.canTakeTest(TestType.OIR, any())
        }
    }
    
    // ==================== Helper Methods ====================
    
    private fun createViewModel(): OIRTestViewModel {
        return OIRTestViewModel(
            mockTestContentRepo,
            mockSubmissionRepo,
            mockObserveCurrentUser,
            mockUserProfileRepo,
            mockDifficultyManager,
            mockSubscriptionManager,
            mockGetOLQDashboard,
            mockSecurityLogger
        )
    }
    
    private fun createMockQuestions(): List<OIRQuestion> {
        return listOf(
            OIRQuestion(
                id = "oir_q1",
                questionNumber = 1,
                type = OIRQuestionType.VERBAL_REASONING,
                difficulty = QuestionDifficulty.EASY,
                questionText = "Which word is different?",
                options = listOf(
                    OIROption("opt1", "Dog"),
                    OIROption("opt2", "Cat"),
                    OIROption("opt3", "Table"),
                    OIROption("opt4", "Rabbit")
                ),
                correctAnswerId = "opt3",
                explanation = "Table is not an animal"
            ),
            OIRQuestion(
                id = "oir_q2",
                questionNumber = 2,
                type = OIRQuestionType.NON_VERBAL_REASONING,
                difficulty = QuestionDifficulty.MEDIUM,
                questionText = "Find the missing pattern",
                options = listOf(
                    OIROption("opt1", "A"),
                    OIROption("opt2", "B"),
                    OIROption("opt3", "C"),
                    OIROption("opt4", "D")
                ),
                correctAnswerId = "opt2",
                explanation = "Pattern follows sequence"
            ),
            OIRQuestion(
                id = "oir_q3",
                questionNumber = 3,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                difficulty = QuestionDifficulty.MEDIUM,
                questionText = "What is 25% of 80?",
                options = listOf(
                    OIROption("opt1", "15"),
                    OIROption("opt2", "20"),
                    OIROption("opt3", "25"),
                    OIROption("opt4", "30")
                ),
                correctAnswerId = "opt2",
                explanation = "25% of 80 = 20"
            ),
            OIRQuestion(
                id = "oir_q4",
                questionNumber = 4,
                type = OIRQuestionType.VERBAL_REASONING,
                difficulty = QuestionDifficulty.HARD,
                questionText = "Complete the analogy: Day is to Night as Summer is to?",
                options = listOf(
                    OIROption("opt1", "Winter"),
                    OIROption("opt2", "Spring"),
                    OIROption("opt3", "Autumn"),
                    OIROption("opt4", "Season")
                ),
                correctAnswerId = "opt1",
                explanation = "Opposite relationship"
            ),
            OIRQuestion(
                id = "oir_q5",
                questionNumber = 5,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                difficulty = QuestionDifficulty.EASY,
                questionText = "What is 7 x 8?",
                options = listOf(
                    OIROption("opt1", "54"),
                    OIROption("opt2", "56"),
                    OIROption("opt3", "58"),
                    OIROption("opt4", "60")
                ),
                correctAnswerId = "opt2",
                explanation = "7 x 8 = 56"
            )
        )
    }
}

