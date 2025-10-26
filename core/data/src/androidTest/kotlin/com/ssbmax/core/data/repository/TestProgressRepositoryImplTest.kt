package com.ssbmax.core.data.repository

import app.cash.turbine.test
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.FirebaseTestHelper
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for TestProgressRepositoryImpl with Firebase Emulator
 * 
 * Tests real-time progress calculation from submissions collection
 */
class TestProgressRepositoryImplTest {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: TestProgressRepositoryImpl
    
    private val testUserId = "test-user-${System.currentTimeMillis()}"
    private val submissionIds = mutableListOf<String>()
    
    @Before
    fun setUp() {
        // Get Firestore instance configured for emulator
        firestore = FirebaseTestHelper.getEmulatorFirestore()
        repository = TestProgressRepositoryImpl(firestore)
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
    
    // ==================== Phase 1 Progress Tests ====================
    
    @Test
    fun getPhase1Progress_returns_not_attempted_when_no_submissions() = runTest {
        // When/Then
        repository.getPhase1Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            assertEquals(TestStatus.NOT_ATTEMPTED, progress.oirProgress.status)
            assertEquals(TestStatus.NOT_ATTEMPTED, progress.ppdtProgress.status)
            assertNull(progress.oirProgress.latestScore)
            assertNull(progress.ppdtProgress.latestScore)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase1Progress_calculates_correct_completion_percentage() = runTest {
        // Given - Submit only OIR test
        createSubmission("OIR", "GRADED", score = 75f)
        
        // When/Then
        repository.getPhase1Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            assertEquals(50f, progress.completionPercentage, 0.01f)
            assertEquals(TestStatus.GRADED, progress.oirProgress.status)
            assertEquals(TestStatus.NOT_ATTEMPTED, progress.ppdtProgress.status)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase1Progress_tracks_oir_submission() = runTest {
        // Given
        val submittedAt = System.currentTimeMillis()
        createSubmission("OIR", "GRADED", score = 85f, submittedAt = submittedAt)
        
        // When/Then
        repository.getPhase1Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            assertEquals(TestStatus.GRADED, progress.oirProgress.status)
            assertEquals(85f, progress.oirProgress.latestScore)
            assertEquals(submittedAt, progress.oirProgress.lastAttemptDate)
            assertFalse(progress.oirProgress.isPendingReview)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase1Progress_tracks_ppdt_submission() = runTest {
        // Given
        val submittedAt = System.currentTimeMillis()
        createSubmission("PPDT", "SUBMITTED_PENDING_REVIEW", submittedAt = submittedAt)
        
        // When/Then
        repository.getPhase1Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            assertEquals(TestStatus.SUBMITTED_PENDING_REVIEW, progress.ppdtProgress.status)
            assertNull(progress.ppdtProgress.latestScore)
            assertTrue(progress.ppdtProgress.isPendingReview)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase1Progress_uses_most_recent_submission() = runTest {
        // Given - Multiple OIR submissions
        val oldSubmission = System.currentTimeMillis() - 10000
        val newSubmission = System.currentTimeMillis()
        
        createSubmission("OIR", "GRADED", score = 70f, submittedAt = oldSubmission)
        createSubmission("OIR", "GRADED", score = 90f, submittedAt = newSubmission)
        
        // When/Then
        repository.getPhase1Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            // Should use the most recent score
            assertEquals(90f, progress.oirProgress.latestScore)
            assertEquals(newSubmission, progress.oirProgress.lastAttemptDate)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase1Progress_emits_realtime_updates() = runTest {
        // When - Start observing
        repository.getPhase1Progress(testUserId).test(timeout = 10.seconds) {
            // Then - Initial state (no submissions)
            val initial = awaitItem()
            assertEquals(0f, initial.completionPercentage, 0.01f)
            
            // When - Add OIR submission
            createSubmission("OIR", "GRADED", score = 80f)
            
            // Then - Progress updated
            val withOir = awaitItem()
            assertEquals(50f, withOir.completionPercentage, 0.01f)
            assertEquals(80f, withOir.oirProgress.latestScore)
            
            // When - Add PPDT submission
            createSubmission("PPDT", "GRADED", score = 75f)
            
            // Then - Progress fully completed
            val completed = awaitItem()
            assertEquals(100f, completed.completionPercentage, 0.01f)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // ==================== Phase 2 Progress Tests ====================
    
    @Test
    fun getPhase2Progress_returns_not_attempted_when_no_submissions() = runTest {
        // When/Then
        repository.getPhase2Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            assertEquals(TestStatus.NOT_ATTEMPTED, progress.psychologyProgress.status)
            assertEquals(TestStatus.NOT_ATTEMPTED, progress.gtoProgress.status)
            assertEquals(TestStatus.NOT_ATTEMPTED, progress.interviewProgress.status)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase2Progress_calculates_correct_completion_percentage() = runTest {
        // Given - Only psychology test submitted
        createSubmission("TAT", "GRADED", score = 80f)
        
        // When/Then
        repository.getPhase2Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            // 1 out of 3 sections completed = 33.33%
            assertEquals(33.33f, progress.completionPercentage, 0.1f)
            assertEquals(TestStatus.GRADED, progress.psychologyProgress.status)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase2Progress_groups_psychology_tests_correctly() = runTest {
        // Given - Multiple psychology tests
        val tatTime = System.currentTimeMillis()
        val watTime = tatTime + 1000
        val srtTime = watTime + 1000
        
        createSubmission("TAT", "GRADED", score = 75f, submittedAt = tatTime)
        createSubmission("WAT", "GRADED", score = 80f, submittedAt = watTime)
        createSubmission("SRT", "GRADED", score = 85f, submittedAt = srtTime)
        
        // When/Then
        repository.getPhase2Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            // Should use the most recent psychology test (SRT)
            assertEquals(TestStatus.GRADED, progress.psychologyProgress.status)
            assertEquals(85f, progress.psychologyProgress.latestScore)
            assertEquals(srtTime, progress.psychologyProgress.lastAttemptDate)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase2Progress_tracks_gto_separately() = runTest {
        // Given
        createSubmission("GTO", "COMPLETED", score = 70f)
        
        // When/Then
        repository.getPhase2Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            assertEquals(TestStatus.COMPLETED, progress.gtoProgress.status)
            assertEquals(70f, progress.gtoProgress.latestScore)
            assertEquals(TestType.GTO, progress.gtoProgress.testType)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase2Progress_tracks_interview_separately() = runTest {
        // Given
        createSubmission("IO", "SUBMITTED_PENDING_REVIEW")
        
        // When/Then
        repository.getPhase2Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            assertEquals(TestStatus.SUBMITTED_PENDING_REVIEW, progress.interviewProgress.status)
            assertTrue(progress.interviewProgress.isPendingReview)
            assertEquals(TestType.IO, progress.interviewProgress.testType)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase2Progress_calculates_all_sections_completed() = runTest {
        // Given - All sections completed
        createSubmission("TAT", "GRADED", score = 80f)
        createSubmission("GTO", "GRADED", score = 75f)
        createSubmission("IO", "GRADED", score = 85f)
        
        // When/Then
        repository.getPhase2Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            assertEquals(100f, progress.completionPercentage, 0.01f)
            assertEquals(TestStatus.GRADED, progress.psychologyProgress.status)
            assertEquals(TestStatus.GRADED, progress.gtoProgress.status)
            assertEquals(TestStatus.GRADED, progress.interviewProgress.status)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase2Progress_supports_sd_test_in_psychology_group() = runTest {
        // Given - SD (Situation Description) test
        createSubmission("SD", "GRADED", score = 90f)
        
        // When/Then
        repository.getPhase2Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            // SD should count toward psychology progress
            assertEquals(TestStatus.GRADED, progress.psychologyProgress.status)
            assertEquals(90f, progress.psychologyProgress.latestScore)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun getPhase2Progress_emits_realtime_updates() = runTest {
        // When - Start observing
        repository.getPhase2Progress(testUserId).test(timeout = 10.seconds) {
            // Then - Initial state
            val initial = awaitItem()
            assertEquals(0f, initial.completionPercentage, 0.01f)
            
            // When - Add psychology test
            createSubmission("TAT", "GRADED", score = 80f)
            
            // Then - Updated progress
            val withPsych = awaitItem()
            assertEquals(33.33f, withPsych.completionPercentage, 0.1f)
            
            // When - Add GTO
            createSubmission("GTO", "GRADED", score = 75f)
            
            // Then - Further progress
            val withGto = awaitItem()
            assertEquals(66.66f, withGto.completionPercentage, 0.1f)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    fun progress_handles_submission_without_score() = runTest {
        // Given - Submission pending review (no score yet)
        createSubmission("OIR", "SUBMITTED_PENDING_REVIEW", score = null)
        
        // When/Then
        repository.getPhase1Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            assertEquals(TestStatus.SUBMITTED_PENDING_REVIEW, progress.oirProgress.status)
            assertNull(progress.oirProgress.latestScore)
            assertTrue(progress.oirProgress.isPendingReview)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun progress_ignores_invalid_test_types() = runTest {
        // Given - Invalid test type submission
        createSubmission("INVALID_TEST", "GRADED", score = 100f)
        
        // When/Then
        repository.getPhase1Progress(testUserId).test(timeout = 30.seconds) {
            val progress = awaitItem()
            
            // Should ignore invalid test type
            assertEquals(0f, progress.completionPercentage, 0.01f)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    // ==================== Helper Methods ====================
    
    private suspend fun createSubmission(
        testType: String,
        status: String,
        score: Float? = null,
        submittedAt: Long = System.currentTimeMillis()
    ): String {
        val submissionId = "test-submission-${System.currentTimeMillis()}-${(0..1000).random()}"
        submissionIds.add(submissionId)
        
        val data = mutableMapOf<String, Any>(
            "userId" to testUserId,
            "testType" to testType,
            "status" to status,
            "submittedAt" to submittedAt
        )
        
        if (score != null) {
            data["score"] = score
        }
        
        firestore.collection("submissions")
            .document(submissionId)
            .set(data)
            .await()
        
        // Small delay to ensure Firestore processes the write
        kotlinx.coroutines.delay(100)
        
        return submissionId
    }
}

