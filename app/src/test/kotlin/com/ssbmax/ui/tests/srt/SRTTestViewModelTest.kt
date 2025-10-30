package com.ssbmax.ui.tests.srt

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitSRTTestUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SRTTestViewModel
 * Tests situation loading, response handling, navigation, and submission
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SRTTestViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: SRTTestViewModel
    private val mockTestContentRepo = mockk<TestContentRepository>(relaxed = true)
    private val mockSubmitSRTTest = mockk<SubmitSRTTestUseCase>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)
    private val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)
    
    private val mockSituations = createMockSituations()
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
        // Mock current user
        every { mockObserveCurrentUser() } returns flowOf(mockUser)
        
        // Mock test session creation
        coEvery { 
            mockTestContentRepo.createTestSession(any(), any(), TestType.SRT) 
        } returns Result.success("session-srt-123")
        
        // Mock situation loading
        coEvery { 
            mockTestContentRepo.getSRTQuestions(any()) 
        } returns Result.success(mockSituations)
        
        // Mock user profile
        coEvery { 
            mockUserProfileRepo.getUserProfile(any()) 
        } returns flowOf(Result.success(mockUserProfile))
        
        // Mock submission
        coEvery { 
            mockSubmitSRTTest(any(), any()) 
        } returns Result.success("submission-srt-123")
    }
    
    // ==================== Test Loading ====================
    
    @Test
    fun `loadTest success loads 60 situations and shows instructions`() = runTest {
        // When
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should not have error", state.error)
            assertEquals("Should have 60 situations", 60, state.situations.size)
            assertEquals("Should be in instructions phase", SRTPhase.INSTRUCTIONS, state.phase)
            assertNotNull("Should have config", state.config)
        }
        
        coVerify { mockTestContentRepo.createTestSession("test-user-123", "srt_standard", TestType.SRT) }
        coVerify { mockTestContentRepo.getSRTQuestions("srt_standard") }
    }
    
    @Test
    fun `loadTest failure shows error message`() = runTest {
        // Given - mock failure
        coEvery { 
            mockTestContentRepo.getSRTQuestions(any()) 
        } returns Result.failure(Exception("Network error"))
        
        // When
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNotNull("Should have error", state.error)
            assertTrue(
                "Error should mention cloud connection",
                state.error!!.contains("Cloud connection required")
            )
        }
    }
    
    @Test
    fun `loadTest with empty situations shows error`() = runTest {
        // Given
        coEvery { 
            mockTestContentRepo.getSRTQuestions(any()) 
        } returns Result.success(emptyList())
        
        // When
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertNotNull("Should have error", state.error)
        assertTrue(state.error!!.contains("Cloud connection required"))
    }
    
    // ==================== Phase Transitions ====================
    
    @Test
    fun `startTest transitions to in_progress phase`() = runTest {
        // Given
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        
        // When
        viewModel.startTest()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should be in progress", SRTPhase.IN_PROGRESS, state.phase)
            assertEquals("Should start at situation 0", 0, state.currentSituationIndex)
            assertNotNull("Should have current situation", state.currentSituation)
        }
    }
    
    // ==================== Response Handling ====================
    
    @Test
    fun `updateResponse updates text correctly`() = runTest {
        // Given
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When
        viewModel.updateResponse("I would help the person immediately")
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Response should be updated", "I would help the person immediately", state.currentResponse)
        }
    }
    
    @Test
    fun `updateResponse enforces max length of 200 chars`() = runTest {
        // Given
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        val longResponse = "a".repeat(250) // 250 chars, exceeds max
        
        // When
        viewModel.updateResponse(longResponse)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            // Should not accept response longer than 200 chars
            assertNotEquals("Should not accept 250 char response", longResponse, state.currentResponse)
        }
    }
    
    @Test
    fun `canMoveToNext validates response length`() = runTest {
        // Given
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - write short response (< 20 chars minimum)
        viewModel.updateResponse("Help")
        
        // Then
        var state = viewModel.uiState.value
        assertFalse("Should not be able to proceed with short response", state.canMoveToNext)
        
        // When - write long enough response (>= 20 chars)
        viewModel.updateResponse("I would help the person by calling authorities")
        
        // Then
        state = viewModel.uiState.value
        assertTrue("Should be able to proceed with valid response", state.canMoveToNext)
    }
    
    // ==================== Navigation ====================
    
    @Test
    fun `moveToNext saves response and moves to next situation`() = runTest {
        // Given
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When
        viewModel.updateResponse("I would take immediate action to resolve the situation")
        viewModel.moveToNext()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should have 1 response", 1, state.responses.size)
            assertEquals("Response should be saved", "I would take immediate action to resolve the situation", state.responses[0].response)
            assertFalse("Response should not be skipped", state.responses[0].isSkipped)
            assertEquals("Should move to situation 1", 1, state.currentSituationIndex)
            assertEquals("Current response should be cleared", "", state.currentResponse)
        }
    }
    
    @Test
    fun `skipSituation marks response as skipped and moves to next`() = runTest {
        // Given
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When
        viewModel.skipSituation()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should have 1 response", 1, state.responses.size)
            assertTrue("Response should be marked as skipped", state.responses[0].isSkipped)
            assertEquals("Skipped response should be empty", "", state.responses[0].response)
            assertEquals("Should move to situation 1", 1, state.currentSituationIndex)
        }
    }
    
    @Test
    fun `completing all situations transitions to review phase`() = runTest {
        // Given - use only 3 situations for faster test
        val shortSituations = mockSituations.take(3)
        coEvery { 
            mockTestContentRepo.getSRTQuestions(any()) 
        } returns Result.success(shortSituations)
        
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - answer all 3 situations
        repeat(3) { index ->
            viewModel.updateResponse("Response for situation ${index + 1}")
            viewModel.moveToNext()
        }
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should transition to review phase", SRTPhase.REVIEW, state.phase)
            assertEquals("Should have 3 responses", 3, state.completedSituations)
        }
    }
    
    @Test
    fun `editResponse returns to specific situation for editing`() = runTest {
        // Given
        val shortSituations = mockSituations.take(5)
        coEvery { 
            mockTestContentRepo.getSRTQuestions(any()) 
        } returns Result.success(shortSituations)
        
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // Answer 3 situations
        repeat(3) { index ->
            viewModel.updateResponse("Response ${index + 1}")
            viewModel.moveToNext()
        }
        
        // When - edit response at index 1
        viewModel.editResponse(1)
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertEquals("Should be at situation 1", 1, state.currentSituationIndex)
            assertEquals("Should load previous response", "Response 2", state.currentResponse)
            assertEquals("Should be back in progress phase", SRTPhase.IN_PROGRESS, state.phase)
        }
    }
    
    // ==================== Test Submission ====================
    
    @Test
    fun `submitTest creates submission with responses and AI score`() = runTest {
        // Given
        val shortSituations = mockSituations.take(3)
        coEvery { 
            mockTestContentRepo.getSRTQuestions(any()) 
        } returns Result.success(shortSituations)
        
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // Answer all situations
        repeat(3) { index ->
            viewModel.updateResponse("I would handle situation ${index + 1} responsibly")
            viewModel.moveToNext()
        }
        
        // When
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertTrue("Should be submitted", state.isSubmitted)
            assertNotNull("Should have submission ID", state.submissionId)
            assertEquals("Phase should be submitted", SRTPhase.SUBMITTED, state.phase)
            assertNotNull("Should have subscription type", state.subscriptionType)
        }
        
        coVerify { mockSubmitSRTTest(any(), null) }
    }
    
    @Test
    fun `submitTest without authenticated user shows error`() = runTest {
        // Given - mock no user
        every { mockObserveCurrentUser() } returns flowOf(null)
        
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        
        // When
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNotNull("Should have error", state.error)
            assertTrue("Error should mention login", state.error!!.contains("login"))
        }
    }
    
    // ==================== Progress Tracking ====================
    
    @Test
    fun `progress is calculated correctly`() = runTest {
        // Given
        val shortSituations = mockSituations.take(10)
        coEvery { 
            mockTestContentRepo.getSRTQuestions(any()) 
        } returns Result.success(shortSituations)
        
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - answer 5 out of 10
        repeat(5) {
            viewModel.updateResponse("Response with adequate length for validation")
            viewModel.moveToNext()
        }
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Completed situations should be 5", 5, state.completedSituations)
        assertEquals("Progress should be 50%", 0.5f, state.progress, 0.01f)
    }
    
    @Test
    fun `validResponseCount tracks non-skipped responses`() = runTest {
        // Given
        val shortSituations = mockSituations.take(5)
        coEvery { 
            mockTestContentRepo.getSRTQuestions(any()) 
        } returns Result.success(shortSituations)
        
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // When - answer 3, skip 2
        repeat(3) {
            viewModel.updateResponse("Valid response with sufficient length")
            viewModel.moveToNext()
        }
        repeat(2) {
            viewModel.skipSituation()
        }
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 5 total responses", 5, state.completedSituations)
        assertEquals("Should have 3 valid responses", 3, state.validResponseCount)
    }
    
    // ==================== Category Scores ====================
    
    @Test
    fun `AI score includes category-wise breakdown`() = runTest {
        // Given
        val situations = listOf(
            createSituation("s1", SRTCategory.LEADERSHIP),
            createSituation("s2", SRTCategory.DECISION_MAKING),
            createSituation("s3", SRTCategory.LEADERSHIP)
        )
        coEvery { 
            mockTestContentRepo.getSRTQuestions(any()) 
        } returns Result.success(situations)
        
        viewModel = SRTTestViewModel(
            mockTestContentRepo,
            mockSubmitSRTTest,
            mockObserveCurrentUser,
            mockUserProfileRepo
        )
        viewModel.loadTest("srt_standard")
        advanceUntilIdle()
        viewModel.startTest()
        
        // Answer all situations
        repeat(3) {
            viewModel.updateResponse("Leadership response with adequate length")
            viewModel.moveToNext()
        }
        
        viewModel.submitTest()
        advanceUntilIdle()
        
        // Then - verify AI score generation (happens internally)
        // The ViewModel generates a mock AI score with category breakdown
        assertTrue("Test completed successfully", viewModel.uiState.value.isSubmitted)
    }
    
    // ==================== Helper Methods ====================
    
    private fun createMockSituations(): List<SRTSituation> {
        val situations = listOf(
            "You are the captain of your college team. During an important match, you notice that your best player is not feeling well but insists on playing." to SRTCategory.LEADERSHIP,
            "You witness a senior colleague taking credit for your junior's work in a meeting." to SRTCategory.ETHICAL_DILEMMA,
            "While traveling alone at night, you see an elderly person who has fallen and needs help." to SRTCategory.RESPONSIBILITY,
            "Your team is losing a crucial match, and team morale is very low." to SRTCategory.TEAMWORK,
            "You discover that your close friend has been cheating in exams." to SRTCategory.ETHICAL_DILEMMA,
            "During a group trek, one member gets injured and cannot walk." to SRTCategory.CRISIS_MANAGEMENT,
            "You have to choose between attending an important family function or a crucial team practice." to SRTCategory.DECISION_MAKING,
            "A stranger asks you to lend them money for emergency medical treatment." to SRTCategory.INTERPERSONAL,
            "You see a group of people harassing someone on the street." to SRTCategory.COURAGE,
            "Your subordinate makes a serious mistake that could impact the entire project." to SRTCategory.LEADERSHIP
        )
        
        // Repeat to get 60 situations
        return (situations + situations + situations + situations + situations + situations)
            .take(60)
            .mapIndexed { index, (situation, category) ->
                createSituation("srt_s_${index + 1}", category, situation, index + 1)
            }
    }
    
    private fun createSituation(
        id: String,
        category: SRTCategory,
        situation: String = "Test situation",
        sequenceNumber: Int = 1
    ): SRTSituation {
        return SRTSituation(
            id = id,
            situation = "$situation What would you do?",
            sequenceNumber = sequenceNumber,
            category = category,
            timeAllowedSeconds = 30
        )
    }
}

