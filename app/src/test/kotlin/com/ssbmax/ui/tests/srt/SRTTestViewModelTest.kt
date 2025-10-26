package com.ssbmax.ui.tests.srt

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitSRTTestUseCase
import com.ssbmax.testing.BaseViewModelTest
import com.ssbmax.testing.MockDataFactory
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SRTTestViewModel
 * 
 * SRT Test: 60 situations with 30-second response time each
 */
class SRTTestViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: SRTTestViewModel
    private lateinit var testContentRepository: TestContentRepository
    private lateinit var submitSRTTest: SubmitSRTTestUseCase
    private lateinit var observeCurrentUser: ObserveCurrentUserUseCase
    private lateinit var userProfileRepository: UserProfileRepository
    
    private val testId = "srt-test-1"
    private val mockUser = SSBMaxUser(
        id = "user123",
        email = "test@example.com",
        displayName = "Test User",
        role = UserRole.STUDENT,
        subscriptionTier = SubscriptionTier.BASIC
    )
    
    @Before
    fun setUp() {
        testContentRepository = mockk(relaxed = true)
        submitSRTTest = mockk(relaxed = true)
        observeCurrentUser = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        
        coEvery { observeCurrentUser() } returns flowOf(mockUser)
        coEvery { userProfileRepository.getUserProfile(any()) } returns flowOf(
            Result.success(MockDataFactory.createMockUserProfile())
        )
        
        viewModel = SRTTestViewModel(
            testContentRepository,
            submitSRTTest,
            observeCurrentUser,
            userProfileRepository
        )
    }
    
    // ==================== Loading Tests ====================
    
    @Test
    fun `loadTest - successfully loads situations`() = runTest {
        // Given
        val mockSituations = MockDataFactory.createMockSRTSituations(60)
        coEvery { testContentRepository.createTestSession(any(), any(), any()) } returns Result.success("session-123")
        coEvery { testContentRepository.getSRTQuestions(testId) } returns Result.success(mockSituations)
        
        // When
        viewModel.loadTest(testId)
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertEquals("Should have 60 situations", 60, state.situations.size)
        assertEquals("Should be in instructions", SRTPhase.INSTRUCTIONS, state.phase)
    }
    
    @Test
    fun `loadTest - handles empty situations list`() = runTest {
        // Given
        coEvery { testContentRepository.createTestSession(any(), any(), any()) } returns Result.success("session-123")
        coEvery { testContentRepository.getSRTQuestions(testId) } returns Result.success(emptyList())
        
        // When
        viewModel.loadTest(testId)
        advanceTimeBy(200)
        
        // Then
        assertNotNull("Should have error", viewModel.uiState.value.error)
    }
    
    @Test
    fun `loadTest - handles network failure`() = runTest {
        // Given
        coEvery { testContentRepository.createTestSession(any(), any(), any()) } returns 
            Result.failure(Exception("Network error"))
        
        // When
        viewModel.loadTest(testId)
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
    }
    
    // ==================== Test Flow ====================
    
    @Test
    fun `startTest - transitions to in progress`() = runTest {
        // Given
        setupWithSituations(3)
        
        // When
        viewModel.startTest()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should be in progress", SRTPhase.IN_PROGRESS, state.phase)
        assertEquals("Should start at situation 0", 0, state.currentSituationIndex)
    }
    
    @Test
    fun `updateResponse - updates current response`() = runTest {
        // Given
        setupWithSituations(1)
        viewModel.startTest()
        
        // When
        viewModel.updateResponse("I would take charge and organize the team")
        
        // Then
        val response = viewModel.uiState.value.currentResponse
        assertEquals("Response should be saved", "I would take charge and organize the team", response)
    }
    
    @Test
    fun `updateResponse - limits response length`() = runTest {
        // Given
        setupWithSituations(1)
        val longResponse = "This is a very long response " + "x".repeat(300)
        
        // When
        viewModel.updateResponse(longResponse)
        
        // Then
        val response = viewModel.uiState.value.currentResponse
        assertTrue("Response should be limited to 200 chars", response.length <= 200)
    }
    
    @Test
    fun `moveToNext - saves response and moves to next situation`() = runTest {
        // Given
        setupWithSituations(3)
        viewModel.startTest()
        
        // When
        viewModel.updateResponse("My response")
        viewModel.moveToNext()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 1 response", 1, state.responses.size)
        assertEquals("Response should be saved", "My response", state.responses[0].response)
        assertEquals("Should move to situation 1", 1, state.currentSituationIndex)
        assertEquals("Response should be cleared", "", state.currentResponse)
    }
    
    @Test
    fun `skipSituation - records skipped and moves next`() = runTest {
        // Given
        setupWithSituations(3)
        viewModel.startTest()
        
        // When
        viewModel.skipSituation()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 1 response", 1, state.responses.size)
        assertTrue("Should be marked as skipped", state.responses[0].isSkipped)
        assertEquals("Response should be empty", "", state.responses[0].response)
        assertEquals("Should move to next situation", 1, state.currentSituationIndex)
    }
    
    @Test
    fun `moves to review after last situation`() = runTest {
        // Given
        setupWithSituations(3)
        viewModel.startTest()
        
        // When - Complete all situations
        repeat(3) {
            viewModel.updateResponse("Response $it")
            viewModel.moveToNext()
        }
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should be in review", SRTPhase.REVIEW, state.phase)
        assertEquals("Should have 3 responses", 3, state.responses.size)
    }
    
    @Test
    fun `editResponse - allows editing previous response`() = runTest {
        // Given
        setupWithSituations(5)
        viewModel.startTest()
        
        // Complete 3 situations
        repeat(3) {
            viewModel.updateResponse("Original $it")
            viewModel.moveToNext()
        }
        
        // When - Edit response at index 1
        viewModel.editResponse(1)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should return to index 1", 1, state.currentSituationIndex)
        assertEquals("Should load previous response", "Original 1", state.currentResponse)
        assertEquals("Should return to in progress", SRTPhase.IN_PROGRESS, state.phase)
    }
    
    @Test
    fun `editResponse - handles invalid index`() = runTest {
        // Given
        setupWithSituations(3)
        viewModel.startTest()
        val initialIndex = viewModel.uiState.value.currentSituationIndex
        
        // When - Try invalid index
        viewModel.editResponse(99)
        
        // Then
        assertEquals("Index should not change", initialIndex, viewModel.uiState.value.currentSituationIndex)
    }
    
    @Test
    fun `editResponse - overwrites previous response`() = runTest {
        // Given
        setupWithSituations(3)
        viewModel.startTest()
        
        viewModel.updateResponse("First response")
        viewModel.moveToNext()
        viewModel.moveToNext()
        
        // When - Edit first response
        viewModel.editResponse(0)
        viewModel.updateResponse("Edited response")
        viewModel.moveToNext()
        
        // Then
        val response = viewModel.uiState.value.responses.find { it.situationId == viewModel.uiState.value.situations[0].id }
        assertEquals("Response should be updated", "Edited response", response?.response)
    }
    
    // ==================== Submission Tests ====================
    
    @Test
    fun `submitTest - successfully submits all responses`() = runTest {
        // Given
        setupWithSituations(3)
        coEvery { submitSRTTest(any(), any()) } returns Result.success("submission-123")
        
        viewModel.startTest()
        repeat(3) {
            viewModel.updateResponse("Response$it")
            viewModel.moveToNext()
        }
        
        // When
        viewModel.submitTest()
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue("Should be submitted", state.isSubmitted)
        assertEquals("Should have submission ID", "submission-123", state.submissionId)
    }
    
    @Test
    fun `submitTest - handles submission failure`() = runTest {
        // Given
        setupWithSituations(3)
        coEvery { submitSRTTest(any(), any()) } returns Result.failure(Exception("Network error"))
        
        viewModel.startTest()
        repeat(3) { viewModel.skipSituation() }
        
        // When
        viewModel.submitTest()
        advanceTimeBy(100)
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be submitted", state.isSubmitted)
        assertNotNull("Should have error", state.error)
    }
    
    @Test
    fun `submitTest - requires authentication`() = runTest {
        // Given
        setupWithSituations(3)
        coEvery { observeCurrentUser() } returns flowOf(null)
        
        // When
        viewModel.submitTest()
        advanceTimeBy(100)
        
        // Then
        assertNotNull("Should have error", viewModel.uiState.value.error)
        coVerify(exactly = 0) { submitSRTTest(any(), any()) }
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `currentSituation returns correct situation`() = runTest {
        // Given
        setupWithSituations(3)
        viewModel.startTest()
        
        // Then
        assertNotNull("Current situation should not be null", viewModel.uiState.value.currentSituation)
        assertEquals("Should be first situation", 0, viewModel.uiState.value.currentSituationIndex)
    }
    
    @Test
    fun `progress calculates correctly`() = runTest {
        // Given
        setupWithSituations(10)
        viewModel.startTest()
        
        // When - Complete 5 situations
        repeat(5) { viewModel.skipSituation() }
        
        // Then
        assertEquals("Progress should be 0.5", 0.5f, viewModel.uiState.value.progress, 0.01f)
    }
    
    @Test
    fun `completedSituations counts responses`() = runTest {
        // Given
        setupWithSituations(5)
        viewModel.startTest()
        
        // When
        repeat(3) {
            viewModel.updateResponse("Test")
            viewModel.moveToNext()
        }
        
        // Then
        assertEquals("Should have 3 completed", 3, viewModel.uiState.value.completedSituations)
    }
    
    @Test
    fun `responses include both answered and skipped`() = runTest {
        // Given
        setupWithSituations(5)
        viewModel.startTest()
        
        // When - Mix of answered and skipped
        viewModel.updateResponse("Answer1")
        viewModel.moveToNext()
        viewModel.skipSituation()
        viewModel.updateResponse("Answer2")
        viewModel.moveToNext()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 3 responses total", 3, state.responses.size)
        assertEquals("Should have 2 answered", 2, state.responses.count { !it.isSkipped })
        assertEquals("Should have 1 skipped", 1, state.responses.count { it.isSkipped })
    }
    
    @Test
    fun `phase transitions correctly through lifecycle`() = runTest {
        // Given
        setupWithSituations(2)
        
        // When - Start test
        assertEquals("Should start in instructions", SRTPhase.INSTRUCTIONS, viewModel.uiState.value.phase)
        
        viewModel.startTest()
        assertEquals("Should move to in progress", SRTPhase.IN_PROGRESS, viewModel.uiState.value.phase)
        
        // Complete all situations
        repeat(2) { viewModel.skipSituation() }
        assertEquals("Should move to review", SRTPhase.REVIEW, viewModel.uiState.value.phase)
    }
    
    // ==================== Helper Methods ====================
    
    private fun setupWithSituations(count: Int) {
        val mockSituations = MockDataFactory.createMockSRTSituations(count)
        coEvery { testContentRepository.createTestSession(any(), any(), any()) } returns Result.success("session-123")
        coEvery { testContentRepository.getSRTQuestions(any()) } returns Result.success(mockSituations)
        runTest {
            viewModel.loadTest(testId)
            advanceTimeBy(100)
        }
    }
}

