package com.ssbmax.ui.tests.ppdt

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
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
 * Unit tests for PPDTSubmissionResultViewModel
 * Tests submission data loading, parsing, AI scores, and instructor reviews
 * 
 * Note: ViewModel uses observeSubmission() (Flow-based) not getSubmission()
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PPDTSubmissionResultViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: PPDTSubmissionResultViewModel
    private val mockSubmissionRepo = mockk<SubmissionRepository>(relaxed = true)
    
    @Before
    fun setup() {
        // Will create viewModel in each test with different mock setups
    }
    
    // ==================== Submission Loading Tests ====================
    
    @Test
    fun `loadSubmission with valid data parses PPDT submission correctly`() = runTest {
        // Given
        val mockSubmissionData = createMockSubmissionData(
            submissionId = "sub_ppdt_001",
            userId = "user_123",
            story = "This is a detailed story about the hazy picture showing leadership qualities.",
            includeInstructorReview = false
        )
        
        every { 
            mockSubmissionRepo.observeSubmission("sub_ppdt_001") 
        } returns flowOf(mockSubmissionData)
        
        // When
        viewModel = PPDTSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_ppdt_001")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should not have error", state.error)
            assertNotNull("Should have submission", state.submission)
            
            val submission = state.submission!!
            assertEquals("sub_ppdt_001", submission.submissionId)
            assertEquals("user_123", submission.userId)
            assertTrue("Story should be parsed", submission.story.isNotEmpty())
            assertNull("Should not have instructor review", submission.instructorReview)
        }
    }
    
    @Test
    fun `loadSubmission parses submission without instructor review`() = runTest {
        // Given 
        val mockSubmissionData = createMockSubmissionData(
            submissionId = "sub_ppdt_002",
            includeInstructorReview = false
        )

        every {
            mockSubmissionRepo.observeSubmission("sub_ppdt_002")
        } returns flowOf(mockSubmissionData)

        // When
        viewModel = PPDTSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_ppdt_002")
        advanceUntilIdle()

        // Then
        val submission = viewModel.uiState.value.submission

        assertNotNull("Should have submission", submission)
        assertEquals("sub_ppdt_002", submission!!.submissionId)
        assertNull("Should not have instructor review", submission.instructorReview)
        assertTrue("Should have story", submission.story.isNotEmpty())
    }
    
    @Test
    fun `loadSubmission parses instructor review with detailed scores`() = runTest {
        // Given
        val mockSubmissionData = createMockSubmissionData(
            submissionId = "sub_ppdt_003",
            includeInstructorReview = true
        )
        
        every { 
            mockSubmissionRepo.observeSubmission("sub_ppdt_003") 
        } returns flowOf(mockSubmissionData)
        
        // When
        viewModel = PPDTSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_ppdt_003")
        advanceUntilIdle()
        
        // Then
        val submission = viewModel.uiState.value.submission
        assertNotNull("Should have submission", submission)
        
        val instructorReview = submission!!.instructorReview
        assertNotNull("Should have instructor review", instructorReview)
        
        assertTrue("Review ID should be set", instructorReview!!.reviewId.isNotEmpty())
        assertTrue("Instructor ID should be set", instructorReview.instructorId.isNotEmpty())
        assertTrue("Instructor name should be set", instructorReview.instructorName.isNotEmpty())
        assertTrue("Final score should be > 0", instructorReview.finalScore > 0f)
        assertTrue("Feedback should be present", instructorReview.feedback.isNotEmpty())
        assertTrue("Reviewed at timestamp should be set", instructorReview.reviewedAt > 0L)
        
        // Check detailed scores
        val detailedScores = instructorReview.detailedScores
        assertTrue("Perception score > 0", detailedScores.perception > 0f)
        assertTrue("Imagination score > 0", detailedScores.imagination > 0f)
        assertTrue("Narration score > 0", detailedScores.narration > 0f)
        assertTrue("Character depiction score > 0", detailedScores.characterDepiction > 0f)
        assertTrue("Positivity score > 0", detailedScores.positivity > 0f)
    }
    
    @Test
    fun `loadSubmission handles submission without instructor review`() = runTest {
        // Given - submission without instructor review
        val mockSubmissionData = createMockSubmissionData(
            submissionId = "sub_ppdt_004",
            includeInstructorReview = false
        )

        every {
            mockSubmissionRepo.observeSubmission("sub_ppdt_004")
        } returns flowOf(mockSubmissionData)

        // When
        viewModel = PPDTSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_ppdt_004")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have submission", state.submission)
        assertNull("Should not have instructor review", state.submission?.instructorReview)
    }
    
    @Test
    fun `loadSubmission shows error when submission not found`() = runTest {
        // Given - observeSubmission emits null (submission doesn't exist)
        every { 
            mockSubmissionRepo.observeSubmission("sub_ppdt_999") 
        } returns flowOf(null)
        
        // When
        viewModel = PPDTSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_ppdt_999")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should not have submission", state.submission)
            assertNotNull("Should have error", state.error)
            assertTrue(
                "Error should mention not found",
                state.error!!.contains("not found")
            )
        }
    }
    
    @Test
    fun `loadSubmission shows error on repository failure`() = runTest {
        // Given - repository throws exception
        every { 
            mockSubmissionRepo.observeSubmission("sub_ppdt_error") 
        } throws RuntimeException("Network error")
        
        // When
        viewModel = PPDTSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_ppdt_error")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should not have submission", state.submission)
            assertNotNull("Should have error", state.error)
            assertTrue(
                "Error should mention failure",
                state.error!!.contains("Failed to load") || state.error!!.contains("Network error")
            )
        }
    }
    
    @Test
    fun `loadSubmission handles malformed data gracefully`() = runTest {
        // Given - malformed submission data (missing required fields in data)
        val malformedData = mapOf<String, Any>(
            "id" to "sub_ppdt_malformed",
            "userId" to "user_123",
            // Missing "data" field entirely
            "status" to "SUBMITTED_PENDING_REVIEW"
        )
        
        every { 
            mockSubmissionRepo.observeSubmission("sub_ppdt_malformed") 
        } returns flowOf(malformedData)
        
        // When
        viewModel = PPDTSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_ppdt_malformed")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have submission", state.submission)
        assertNotNull("Should have error", state.error)
        assertTrue(
            "Error should mention parsing failure",
            state.error!!.contains("parse") || state.error!!.contains("Failed")
        )
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Create mock submission data matching Firestore structure
     */
    private fun createMockSubmissionData(
        submissionId: String,
        userId: String = "user_123",
        story: String = "This is a detailed story about leadership and determination shown in the hazy picture. The characters demonstrated courage and teamwork.",
        includeInstructorReview: Boolean = false
    ): Map<String, Any> {
        val instructorReviewMap = if (includeInstructorReview) {
            mapOf(
                "reviewId" to "review_001",
                "instructorId" to "instructor_123",
                "instructorName" to "Col. Sharma",
                "finalScore" to 82.0f,
                "feedback" to "Excellent demonstration of OLQs. Story shows clear perception and imagination.",
                "detailedScores" to mapOf(
                    "perception" to 80.0f,
                    "imagination" to 85.0f,
                    "narration" to 78.0f,
                    "characterDepiction" to 84.0f,
                    "positivity" to 83.0f
                ),
                "agreedWithAI" to true,
                "reviewedAt" to System.currentTimeMillis(),
                "timeSpentMinutes" to 15
            )
        } else {
            null
        }
        
        val dataMap = mutableMapOf<String, Any?>(
            "submissionId" to submissionId,
            "questionId" to "ppdt_q_001",
            "userId" to userId,
            "userName" to "Test User",
            "userEmail" to "test@example.com",
            "batchId" to "batch_001",
            "story" to story,
            "charactersCount" to story.length,
            "viewingTimeTakenSeconds" to 30,
            "writingTimeTakenMinutes" to 4,
            "submittedAt" to System.currentTimeMillis()
        )

        if (instructorReviewMap != null) {
            dataMap["instructorReview"] = instructorReviewMap
        }
        
        return mapOf(
            "id" to submissionId,
            "userId" to userId,
            "testType" to "PPDT",
            "status" to "SUBMITTED_PENDING_REVIEW",
            "submittedAt" to System.currentTimeMillis(),
            "data" to dataMap
        )
    }
}
