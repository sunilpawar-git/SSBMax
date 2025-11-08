package com.ssbmax.ui.tests.ppdt

import app.cash.turbine.test
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PPDTSubmissionResultViewModel
 * Tests submission data loading, parsing, AI scores, and instructor reviews
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
            includeAIScore = true,
            includeInstructorReview = false
        )
        
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_ppdt_001") 
        } returns Result.success(mockSubmissionData)
        
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
            assertNotNull("Should have AI score", submission.aiPreliminaryScore)
            assertNull("Should not have instructor review", submission.instructorReview)
        }
    }
    
    @Test
    fun `loadSubmission parses AI preliminary score with all fields`() = runTest {
        // Given
        val mockSubmissionData = createMockSubmissionData(
            submissionId = "sub_ppdt_002",
            includeAIScore = true,
            includeInstructorReview = false
        )
        
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_ppdt_002") 
        } returns Result.success(mockSubmissionData)
        
        // When
        viewModel = PPDTSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_ppdt_002")
        advanceUntilIdle()
        
        // Then
        val submission = viewModel.uiState.value.submission!!
        val aiScore = submission.aiPreliminaryScore!!
        
        assertTrue("Perception score should be > 0", aiScore.perceptionScore > 0f)
        assertTrue("Imagination score should be > 0", aiScore.imaginationScore > 0f)
        assertTrue("Narration score should be > 0", aiScore.narrationScore > 0f)
        assertTrue("Character depiction score should be > 0", aiScore.characterDepictionScore > 0f)
        assertTrue("Positivity score should be > 0", aiScore.positivityScore > 0f)
        assertTrue("Overall score should be > 0", aiScore.overallScore > 0f)
        assertNotNull("Should have feedback", aiScore.feedback)
        assertTrue("Should have strengths", aiScore.strengths.isNotEmpty())
        assertTrue("Should have areas for improvement", aiScore.areasForImprovement.isNotEmpty())
    }
    
    @Test
    fun `loadSubmission parses instructor review with detailed scores`() = runTest {
        // Given
        val mockSubmissionData = createMockSubmissionData(
            submissionId = "sub_ppdt_003",
            includeAIScore = true,
            includeInstructorReview = true
        )
        
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_ppdt_003") 
        } returns Result.success(mockSubmissionData)
        
        // When
        viewModel = PPDTSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_ppdt_003")
        advanceUntilIdle()
        
        // Then
        val submission = viewModel.uiState.value.submission!!
        val instructorReview = submission.instructorReview!!
        
        assertTrue("Review ID should be set", instructorReview.reviewId.isNotEmpty())
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
    fun `loadSubmission handles null AI score gracefully`() = runTest {
        // Given - submission without AI score
        val mockSubmissionData = createMockSubmissionData(
            submissionId = "sub_ppdt_004",
            includeAIScore = false,
            includeInstructorReview = false
        )
        
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_ppdt_004") 
        } returns Result.success(mockSubmissionData)
        
        // When
        viewModel = PPDTSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_ppdt_004")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have submission", state.submission)
        assertNull("Should not have AI score", state.submission?.aiPreliminaryScore)
        assertNull("Should not have instructor review", state.submission?.instructorReview)
    }
    
    @Test
    fun `loadSubmission shows error when submission not found`() = runTest {
        // Given - submission doesn't exist
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_ppdt_999") 
        } returns Result.success(null)
        
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
        // Given - repository returns failure
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_ppdt_error") 
        } returns Result.failure(Exception("Network error"))
        
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
        // Given - malformed submission data (missing required fields)
        val malformedData = mapOf(
            "id" to "sub_ppdt_malformed",
            "userId" to "user_123",
            // Missing "data" field entirely
            "status" to "SUBMITTED_PENDING_REVIEW"
        )
        
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_ppdt_malformed") 
        } returns Result.success(malformedData)
        
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
            state.error!!.contains("parse")
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
        includeAIScore: Boolean = true,
        includeInstructorReview: Boolean = false
    ): Map<String, Any> {
        val aiScoreMap = if (includeAIScore) {
            mapOf(
                "perceptionScore" to 75.5f,
                "imaginationScore" to 80.0f,
                "narrationScore" to 70.5f,
                "characterDepictionScore" to 78.0f,
                "positivityScore" to 85.5f,
                "overallScore" to 77.9f,
                "feedback" to "Good story with clear leadership qualities demonstrated.",
                "strengths" to listOf("Clear narration", "Positive outlook", "Leadership focus"),
                "areasForImprovement" to listOf("Add more character depth", "Extend the story")
            )
        } else {
            null
        }
        
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
        
        if (aiScoreMap != null) {
            dataMap["aiPreliminaryScore"] = aiScoreMap
        }
        
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

