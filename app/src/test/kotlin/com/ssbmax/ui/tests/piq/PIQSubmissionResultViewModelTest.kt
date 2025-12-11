package com.ssbmax.ui.tests.piq

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
 * Unit tests for PIQSubmissionResultViewModel
 * Tests submission data loading, parsing, and AI quality scores
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PIQSubmissionResultViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: PIQSubmissionResultViewModel
    private val mockSubmissionRepo = mockk<SubmissionRepository>(relaxed = true)
    
    @Before
    fun setup() {
        // Will create viewModel in each test with different mock setups
    }
    
    // ==================== Submission Loading Tests ====================
    
    @Test
    fun `initial state shows loading`() = runTest {
        viewModel = PIQSubmissionResultViewModel(mockSubmissionRepo)
        
        val state = viewModel.uiState.value
        assertTrue("Initial state should be loading", state.isLoading)
        assertNull("Initial state should have no submission", state.submission)
        assertNull("Initial state should have no error", state.error)
    }
    
    @Test
    fun `loadSubmission with valid data parses PIQ submission correctly`() = runTest {
        // Given
        val mockSubmissionData = createMockSubmissionData(
            submissionId = "sub_piq_001",
            userId = "user_123",
            fullName = "John Doe",
            includeAIScore = true
        )
        
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_piq_001") 
        } returns Result.success(mockSubmissionData)
        
        // When
        viewModel = PIQSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_piq_001")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            
            assertFalse("Should not be loading", state.isLoading)
            assertNull("Should not have error", state.error)
            assertNotNull("Should have submission", state.submission)
            
            val submission = state.submission!!
            assertEquals("sub_piq_001", submission.id)
            assertEquals("user_123", submission.userId)
            assertEquals("John Doe", submission.fullName)
            assertNotNull("Should have AI score", submission.aiPreliminaryScore)
        }
    }
    
    @Test
    fun `loadSubmission parses AI quality score with all fields`() = runTest {
        // Given
        val mockSubmissionData = createMockSubmissionData(
            submissionId = "sub_piq_002",
            includeAIScore = true
        )
        
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_piq_002") 
        } returns Result.success(mockSubmissionData)
        
        // When
        viewModel = PIQSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_piq_002")
        advanceUntilIdle()
        
        // Then
        val submission = viewModel.uiState.value.submission!!
        val aiScore = submission.aiPreliminaryScore!!
        
        assertTrue("Overall score should be > 0", aiScore.overallScore > 0f)
        assertTrue("Personal info score should be > 0", aiScore.personalInfoScore > 0f)
        assertTrue("Family info score should be > 0", aiScore.familyInfoScore > 0f)
        assertTrue("Motivation score should be > 0", aiScore.motivationScore > 0f)
        assertTrue("Self-assessment score should be > 0", aiScore.selfAssessmentScore > 0f)
        assertNotNull("Should have feedback", aiScore.feedback)
        assertTrue("Should have strengths", aiScore.strengths.isNotEmpty())
        assertTrue("Should have areas for improvement", aiScore.areasForImprovement.isNotEmpty())
        assertTrue("Completeness should be >= 0", aiScore.completenessPercentage >= 0)
        assertTrue("Clarity score should be > 0", aiScore.clarityScore > 0f)
        assertTrue("Consistency score should be > 0", aiScore.consistencyScore > 0f)
    }
    
    @Test
    fun `loadSubmission parses all PIQ fields correctly`() = runTest {
        // Given
        val mockSubmissionData = createMockSubmissionData(
            submissionId = "sub_piq_003",
            fullName = "Jane Smith",
            dateOfBirth = "15/08/1995",
            phone = "9876543210",
            includeAIScore = true
        )
        
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_piq_003") 
        } returns Result.success(mockSubmissionData)
        
        // When
        viewModel = PIQSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_piq_003")
        advanceUntilIdle()
        
        // Then
        val submission = viewModel.uiState.value.submission!!
        assertEquals("Jane Smith", submission.fullName)
        assertEquals("15/08/1995", submission.dateOfBirth)
        assertEquals("9876543210", submission.phone)
        assertTrue("Should have education details", submission.education10th.level.isNotEmpty())
        assertTrue("Should allow empty work experience list", submission.workExperience.isEmpty())
    }
    
    @Test
    fun `loadSubmission handles null AI score gracefully`() = runTest {
        // Given - submission without AI score
        val mockSubmissionData = createMockSubmissionData(
            submissionId = "sub_piq_004",
            includeAIScore = false
        )
        
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_piq_004") 
        } returns Result.success(mockSubmissionData)
        
        // When
        viewModel = PIQSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_piq_004")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have submission", state.submission)
        assertNull("Should not have AI score", state.submission?.aiPreliminaryScore)
    }
    
    @Test
    fun `loadSubmission shows error when submission not found`() = runTest {
        // Given - submission doesn't exist
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_piq_999") 
        } returns Result.success(null)
        
        // When
        viewModel = PIQSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_piq_999")
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
            mockSubmissionRepo.getSubmission("sub_piq_error") 
        } returns Result.failure(Exception("Network error"))
        
        // When
        viewModel = PIQSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_piq_error")
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
            "id" to "sub_piq_malformed",
            "userId" to "user_123",
            // Missing "data" field entirely
            "status" to "SUBMITTED"
        )
        
        coEvery { 
            mockSubmissionRepo.getSubmission("sub_piq_malformed") 
        } returns Result.success(malformedData)
        
        // When
        viewModel = PIQSubmissionResultViewModel(mockSubmissionRepo)
        viewModel.loadSubmission("sub_piq_malformed")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have submission", state.submission)
        assertNotNull("Should have error", state.error)
        assertTrue(
            "Error should indicate parsing failure",
            state.error!!.contains("Submission not found") // Because parsePIQSubmission returns null
        )
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Create mock submission data matching Firestore structure
     */
    private fun createMockSubmissionData(
        submissionId: String,
        userId: String = "user_123",
        fullName: String = "Test User",
        dateOfBirth: String = "01/01/2000",
        phone: String = "1234567890",
        includeAIScore: Boolean = true
    ): Map<String, Any> {
        val aiScoreMap = if (includeAIScore) {
            mapOf(
                "overallScore" to 75.5f,
                "personalInfoScore" to 20.0f,
                "familyInfoScore" to 18.5f,
                "motivationScore" to 19.0f,
                "selfAssessmentScore" to 18.0f,
                "feedback" to "Good PIQ. Most fields completed with adequate detail.",
                "strengths" to listOf(
                    "Comprehensive personal information",
                    "Clear defense forces motivation",
                    "Self-awareness of strengths"
                ),
                "areasForImprovement" to listOf(
                    "Elaborate on defense forces motivation",
                    "Add more detail to weakness section"
                ),
                "completenessPercentage" to 85,
                "clarityScore" to 8.5f,
                "consistencyScore" to 8.2f,
                "analysisTimestamp" to System.currentTimeMillis()
            )
        } else {
            null
        }
        
        val dataMap = mutableMapOf<String, Any?>(
            "id" to submissionId,
            "userId" to userId,
            "testId" to "piq_standard",
            "fullName" to fullName,
            "dateOfBirth" to dateOfBirth,
            "age" to "25",
            "gender" to "Male",
            "phone" to phone,
            "email" to "test@example.com",
            "permanentAddress" to "123 Test Street",
            "presentAddress" to "456 Current Avenue",
            "fatherName" to "Father Name",
            "fatherOccupation" to "Engineer",
            "fatherEducation" to "B.Tech",
            "fatherIncome" to "1000000",
            "motherName" to "Mother Name",
            "motherOccupation" to "Teacher",
            "motherEducation" to "M.A.",
            "siblings" to listOf<Map<String, Any>>(),
            "education10th" to mapOf(
                "level" to "10th",
                "institution" to "Test School",
                "board" to "CBSE",
                "year" to "2015",
                "percentage" to "85.5"
            ),
            "education12th" to mapOf(
                "level" to "12th",
                "institution" to "Test College",
                "board" to "CBSE",
                "stream" to "Science",
                "year" to "2017",
                "percentage" to "88.0"
            ),
            "educationGraduation" to mapOf(
                "level" to "Graduation",
                "institution" to "Test University",
                "board" to "University",
                "year" to "2021",
                "cgpa" to "8.5"
            ),
            "hobbies" to "Reading, Cricket",
            "sports" to "Cricket, Football",
            "workExperience" to listOf<Map<String, Any>>(),
            "whyDefenseForces" to "Passionate about serving the nation",
            "strengths" to "Leadership, Teamwork",
            "weaknesses" to "Sometimes overcommit",
            "status" to "SUBMITTED",
            "submittedAt" to System.currentTimeMillis(),
            "lastModifiedAt" to System.currentTimeMillis()
        )
        
        if (aiScoreMap != null) {
            dataMap["aiPreliminaryScore"] = aiScoreMap
        }
        
        return mapOf(
            "id" to submissionId,
            "userId" to userId,
            "testId" to "piq_standard",
            "testType" to "PIQ",
            "status" to "SUBMITTED",
            "submittedAt" to System.currentTimeMillis(),
            "data" to dataMap
        )
    }
}

