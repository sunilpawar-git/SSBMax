package com.ssbmax.core.data.repository

import app.cash.turbine.test
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.FirebaseTestHelper
import com.ssbmax.core.domain.model.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for TestSubmissionRepositoryImpl with Firebase Emulator
 * 
 * Tests:
 * - Test submission CRUD operations
 * - Real-time submission updates
 * - Student submission filtering
 * - Pending submissions for assessors
 * - Grading workflow
 */
class TestSubmissionRepositoryImplTest {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: TestSubmissionRepositoryImpl
    
    private val testUserId = "test-user-${System.currentTimeMillis()}"
    private val testAssessorId = "test-assessor-${System.currentTimeMillis()}"
    private val submissionIds = mutableListOf<String>()
    
    @Before
    fun setUp() {
        // Get Firestore instance configured for emulator
        firestore = FirebaseTestHelper.getEmulatorFirestore()
        repository = TestSubmissionRepositoryImpl(firestore)
    }
    
    @After
    fun tearDown() = runTest {
        // Clean up test submissions
        submissionIds.forEach { submissionId ->
            try {
                firestore.collection("submissions")
                    .document(submissionId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        submissionIds.clear()
    }
    
    // ==================== Submit Test ====================
    
    @Test
    fun submitTest_successfully_persists_to_firestore() = runTest {
        // Given
        val submission = createTestSubmission()
        
        // When
        val result = repository.submitTest(submission)
        
        // Then
        assertTrue("Submission should succeed", result.isSuccess)
        
        // Verify in Firestore
        val doc = firestore.collection("submissions")
            .document(submission.id)
            .get()
            .await()
        
        assertTrue("Document should exist", doc.exists())
        assertEquals(submission.userId, doc.getString("userId"))
        assertEquals(submission.testType.name, doc.getString("testType"))
        assertEquals(submission.phase.name, doc.getString("phase"))
    }
    
    @Test
    fun submitTest_stores_all_submission_fields() = runTest {
        // Given
        val submission = createTestSubmission(
            aiPreliminaryScore = 75.5f,
            timeSpent = 1800000L, // 30 minutes
            batchId = "batch-123"
        )
        
        // When
        repository.submitTest(submission)
        
        // Then
        val doc = firestore.collection("submissions")
            .document(submission.id)
            .get()
            .await()
        
        assertEquals(75.5, doc.getDouble("aiPreliminaryScore")!!, 0.01)
        assertEquals(1800000L, doc.getLong("timeSpent"))
        assertEquals("batch-123", doc.getString("batchId"))
    }
    
    // ==================== Get Submission By ID ====================
    
    @Test
    fun getSubmissionById_returns_submission_successfully() = runTest {
        // Given
        val submission = createTestSubmission()
        repository.submitTest(submission)
        
        // When
        val result = repository.getSubmissionById(submission.id)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        val retrieved = result.getOrNull()
        assertNotNull("Submission should not be null", retrieved)
        assertEquals(submission.id, retrieved?.id)
        assertEquals(submission.userId, retrieved?.userId)
        assertEquals(submission.testType, retrieved?.testType)
    }
    
    @Test
    fun getSubmissionById_returns_failure_for_non_existent_submission() = runTest {
        // When
        val result = repository.getSubmissionById("non-existent-id")
        
        // Then
        assertTrue("Should return failure", result.isFailure)
    }
    
    // ==================== Get Submissions For Student ====================
    
    @Test
    fun getSubmissionsForStudent_returns_empty_list_initially() = runTest {
        // When/Then
        repository.getSubmissionsForStudent(testUserId).test(timeout = 30.seconds) {
            val submissions = awaitItem()
            assertTrue("Should be empty", submissions.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getSubmissionsForStudent_returns_student_submissions() = runTest {
        // Given
        val submission1 = createTestSubmission(testType = TestType.TAT)
        val submission2 = createTestSubmission(testType = TestType.WAT)
        repository.submitTest(submission1)
        repository.submitTest(submission2)
        
        // When/Then
        repository.getSubmissionsForStudent(testUserId).test(timeout = 30.seconds) {
            val submissions = awaitItem()
            assertEquals(2, submissions.size)
            assertTrue("Should contain TAT submission", 
                submissions.any { it.testType == TestType.TAT })
            assertTrue("Should contain WAT submission", 
                submissions.any { it.testType == TestType.WAT })
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getSubmissionsForStudent_orders_by_submission_date_descending() = runTest {
        // Given - Submit in order: old, medium, new
        val oldTime = System.currentTimeMillis() - 3000
        val mediumTime = System.currentTimeMillis() - 2000
        val newTime = System.currentTimeMillis() - 1000
        
        val oldSubmission = createTestSubmission(submittedAt = oldTime)
        val mediumSubmission = createTestSubmission(submittedAt = mediumTime)
        val newSubmission = createTestSubmission(submittedAt = newTime)
        
        repository.submitTest(oldSubmission)
        repository.submitTest(mediumSubmission)
        repository.submitTest(newSubmission)
        
        // When/Then
        repository.getSubmissionsForStudent(testUserId).test(timeout = 30.seconds) {
            val submissions = awaitItem()
            
            assertEquals(3, submissions.size)
            // Should be ordered newest first
            assertTrue("First should be newest", 
                submissions[0].submittedAt > submissions[1].submittedAt)
            assertTrue("Second should be newer than third", 
                submissions[1].submittedAt > submissions[2].submittedAt)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getSubmissionsForStudent_filters_by_student_id() = runTest {
        // Given - Different users' submissions
        val otherUserId = "other-user-${System.currentTimeMillis()}"
        
        val userSubmission = createTestSubmission()
        val otherSubmission = createTestSubmission(userId = otherUserId)
        
        repository.submitTest(userSubmission)
        repository.submitTest(otherSubmission)
        
        // When/Then
        repository.getSubmissionsForStudent(testUserId).test(timeout = 30.seconds) {
            val submissions = awaitItem()
            
            assertEquals(1, submissions.size)
            assertEquals(testUserId, submissions.first().userId)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getSubmissionsForStudent_emits_realtime_updates() = runTest {
        // When - Start observing
        repository.getSubmissionsForStudent(testUserId).test(timeout = 10.seconds) {
            // Then - Initial empty state
            val initial = awaitItem()
            assertEquals(0, initial.size)
            
            // When - Add submission
            val submission = createTestSubmission()
            repository.submitTest(submission)
            
            // Then - Receives update
            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals(submission.id, updated.first().id)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // ==================== Get Pending Submissions ====================
    
    @Test
    fun getPendingSubmissions_returns_only_pending_submissions() = runTest {
        // Given
        val pendingSubmission = createTestSubmission(
            gradingStatus = GradingStatus.PENDING
        )
        val gradedSubmission = createTestSubmission(
            gradingStatus = GradingStatus.GRADED
        )
        
        repository.submitTest(pendingSubmission)
        repository.submitTest(gradedSubmission)
        
        // When/Then
        repository.getPendingSubmissions(testAssessorId).test(timeout = 30.seconds) {
            val submissions = awaitItem()
            
            // Should only return pending submissions
            assertEquals(1, submissions.size)
            assertEquals(GradingStatus.PENDING, submissions.first().gradingStatus)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPendingSubmissions_orders_by_submission_date() = runTest {
        // Given - Multiple pending submissions
        val oldTime = System.currentTimeMillis() - 2000
        val newTime = System.currentTimeMillis() - 1000
        
        val oldPending = createTestSubmission(
            submittedAt = oldTime,
            gradingStatus = GradingStatus.PENDING
        )
        val newPending = createTestSubmission(
            submittedAt = newTime,
            gradingStatus = GradingStatus.PENDING
        )
        
        repository.submitTest(oldPending)
        repository.submitTest(newPending)
        
        // When/Then
        repository.getPendingSubmissions(testAssessorId).test(timeout = 30.seconds) {
            val submissions = awaitItem()
            
            assertEquals(2, submissions.size)
            // Newest should be first
            assertTrue("Should be ordered newest first", 
                submissions[0].submittedAt > submissions[1].submittedAt)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // ==================== Update Submission (Grading) ====================
    
    @Test
    fun updateSubmission_updates_grading_status() = runTest {
        // Given - Submit test
        val submission = createTestSubmission()
        repository.submitTest(submission)
        
        // When - Grade the submission
        val gradedSubmission = submission.copy(
            gradingStatus = GradingStatus.GRADED,
            instructorId = testAssessorId,
            instructorScore = 85f,
            instructorFeedback = "Great work!",
            finalScore = 85f,
            gradedAt = System.currentTimeMillis()
        )
        val result = repository.updateSubmission(gradedSubmission)
        
        // Then
        assertTrue("Update should succeed", result.isSuccess)
        
        // Verify in Firestore
        val doc = firestore.collection("submissions")
            .document(submission.id)
            .get()
            .await()
        
        assertEquals("GRADED", doc.getString("gradingStatus"))
        assertEquals(testAssessorId, doc.getString("instructorId"))
        assertEquals(85.0, doc.getDouble("instructorScore")!!, 0.01)
        assertEquals("Great work!", doc.getString("instructorFeedback"))
    }
    
    @Test
    fun updateSubmission_preserves_original_submission_data() = runTest {
        // Given
        val originalSubmission = createTestSubmission()
        repository.submitTest(originalSubmission)
        
        // When - Update only grading fields
        val updatedSubmission = originalSubmission.copy(
            instructorScore = 90f
        )
        repository.updateSubmission(updatedSubmission)
        
        // Then - Original data preserved
        val result = repository.getSubmissionById(originalSubmission.id)
        val retrieved = result.getOrNull()!!
        
        assertEquals(originalSubmission.userId, retrieved.userId)
        assertEquals(originalSubmission.testType, retrieved.testType)
        assertEquals(originalSubmission.submittedAt, retrieved.submittedAt)
        assertEquals(90f, retrieved.instructorScore)
    }
    
    // ==================== Delete Submission ====================
    
    @Test
    fun deleteSubmission_removes_submission_from_firestore() = runTest {
        // Given
        val submission = createTestSubmission()
        repository.submitTest(submission)
        
        // When
        val result = repository.deleteSubmission(submission.id)
        
        // Then
        assertTrue("Delete should succeed", result.isSuccess)
        
        // Verify deletion
        val doc = firestore.collection("submissions")
            .document(submission.id)
            .get()
            .await()
        
        assertFalse("Document should not exist", doc.exists())
    }
    
    @Test
    fun deleteSubmission_handles_non_existent_submission() = runTest {
        // When - Try to delete non-existent submission
        val result = repository.deleteSubmission("non-existent-id")
        
        // Then - Should still succeed (idempotent)
        assertTrue("Should succeed even if doesn't exist", result.isSuccess)
    }
    
    // ==================== Complex Workflows ====================
    
    @Test
    fun complete_grading_workflow() = runTest {
        // Given - Student submits test
        val submission = createTestSubmission(
            gradingStatus = GradingStatus.PENDING
        )
        repository.submitTest(submission)
        
        // Step 1: Appears in pending queue
        repository.getPendingSubmissions(testAssessorId).test(timeout = 30.seconds) {
            val pending = awaitItem()
            assertEquals(1, pending.size)
            cancelAndIgnoreRemainingEvents()
        }
        
            // Step 2: AI grades it
            val aiGraded = submission.copy(
                gradingStatus = GradingStatus.AUTO_GRADED,
                aiPreliminaryScore = 78f
            )
            repository.updateSubmission(aiGraded)
            
            // Step 3: Instructor reviews and grades
            val instructorGraded = aiGraded.copy(
                gradingStatus = GradingStatus.GRADED,
                instructorId = testAssessorId,
                instructorScore = 82f,
                instructorFeedback = "Good understanding shown",
                finalScore = 82f,
                gradedAt = System.currentTimeMillis()
            )
            repository.updateSubmission(instructorGraded)
            
            // Step 4: Verify final state
            val result = repository.getSubmissionById(submission.id)
            val final = result.getOrNull()!!
            
            assertEquals(GradingStatus.GRADED, final.gradingStatus)
        assertEquals(78f, final.aiPreliminaryScore)
        assertEquals(82f, final.instructorScore)
        assertEquals(82f, final.finalScore)
    }
    
    @Test
    fun supports_all_test_types() = runTest {
        val testTypes = listOf(
            TestType.OIR, TestType.PPDT, TestType.TAT,
            TestType.WAT, TestType.SRT, TestType.SD,
            TestType.GTO, TestType.IO
        )
        
        testTypes.forEach { testType ->
            // When
            val submission = createTestSubmission(testType = testType)
            val result = repository.submitTest(submission)
            
            // Then
            assertTrue("Should submit $testType successfully", result.isSuccess)
            
            // Verify
            val retrieved = repository.getSubmissionById(submission.id)
            assertEquals(testType, retrieved.getOrNull()?.testType)
        }
    }
    
    @Test
    fun supports_all_grading_statuses() = runTest {
        val statuses = listOf(
            GradingStatus.PENDING,
            GradingStatus.IN_REVIEW,
            GradingStatus.AUTO_GRADED,
            GradingStatus.GRADED,
            GradingStatus.NEEDS_REVISION
        )
        
        statuses.forEach { status ->
            // When
            val submission = createTestSubmission(
                gradingStatus = status,
                id = "test-${status.name}-${System.currentTimeMillis()}"
            )
            repository.submitTest(submission)
            
            // Then
            val retrieved = repository.getSubmissionById(submission.id)
            assertEquals(status, retrieved.getOrNull()?.gradingStatus)
        }
    }
    
    @Test
    fun handles_submissions_without_scores() = runTest {
        // Given - Submission pending AI grading (no scores yet)
        val submission = createTestSubmission(
            aiPreliminaryScore = null,
            instructorScore = null,
            finalScore = null
        )
        
        // When
        repository.submitTest(submission)
        
        // Then
        val result = repository.getSubmissionById(submission.id)
        val retrieved = result.getOrNull()!!
        
        assertNull(retrieved.aiPreliminaryScore)
        assertNull(retrieved.instructorScore)
        assertNull(retrieved.finalScore)
    }
    
    @Test
    fun handles_batch_submissions() = runTest {
        // Given - Multiple submissions in same batch
        val batchId = "batch-${System.currentTimeMillis()}"
        val submissions = (1..5).map { 
            createTestSubmission(batchId = batchId)
        }
        
        // When - Submit all
        submissions.forEach { repository.submitTest(it) }
        
        // Then - All should be retrievable
        repository.getSubmissionsForStudent(testUserId).test(timeout = 30.seconds) {
            val retrieved = awaitItem()
            
            assertEquals(5, retrieved.size)
            assertTrue("All should have same batchId", 
                retrieved.all { it.batchId == batchId })
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // ==================== Helper Methods ====================
    
    private fun createTestSubmission(
        id: String = "test-submission-${UUID.randomUUID()}",
        userId: String = testUserId,
        testType: TestType = TestType.TAT,
        phase: TestPhase = TestPhase.PHASE_2,
        submittedAt: Long = System.currentTimeMillis(),
        gradingStatus: GradingStatus = GradingStatus.PENDING,
        aiPreliminaryScore: Float? = null,
        instructorScore: Float? = null,
        finalScore: Float? = null,
        timeSpent: Long = 0L,
        batchId: String? = null
    ): TestSubmission {
        submissionIds.add(id)
        
        // Create dummy responses
        val responses = listOf(
            TestResponse.MultipleChoice(
                questionId = "q1",
                timestamp = submittedAt,
                selectedOption = 0,
                isCorrect = null
            )
        )
        
        return TestSubmission(
            id = id,
            testId = "test-${testType.name}",
            userId = userId,
            testType = testType,
            phase = phase,
            submittedAt = submittedAt,
            responses = responses,
            aiPreliminaryScore = aiPreliminaryScore,
            instructorScore = instructorScore,
            finalScore = finalScore,
            gradingStatus = gradingStatus,
            instructorId = null,
            instructorFeedback = null,
            gradedAt = null,
            timeSpent = timeSpent,
            batchId = batchId
        )
    }
}

