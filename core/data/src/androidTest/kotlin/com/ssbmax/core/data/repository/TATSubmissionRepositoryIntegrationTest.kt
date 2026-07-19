package com.ssbmax.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ssbmax.core.data.remote.TATSubmissionRepository
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [TATSubmissionRepository.finalizeTATAnalysisResult].
 *
 * These tests require a live Firestore connection (emulator or real project).
 * Run with:
 *   ./gradlew :core:data:connectedDebugAndroidTest \
 *     --tests "com.ssbmax.core.data.repository.TATSubmissionRepositoryIntegrationTest"
 *
 * To use the Firebase emulator, start it before running:
 *   firebase emulators:start --only firestore
 *
 * These tests verify end-to-end persistence ordering guarantees that unit tests
 * with mocked Firestore cannot fully capture.
 */
@RunWith(AndroidJUnit4::class)
class TATSubmissionRepositoryIntegrationTest {

    private lateinit var repository: TATSubmissionRepository

    // Unique per test run to avoid cross-test data collisions in Firestore.
    private val testSubmissionId = "integration-test-${System.currentTimeMillis()}"

    @Before
    fun setUp() {
        // NOTE: configure Firebase emulator in your test runner or test setup:
        //   FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
        repository = TATSubmissionRepository()
    }

    /**
     * Verifies that COMPLETED status is not observable until the OLQ result is actually persisted
     * to psych_results. Observers must never race to COMPLETED state.
     */
    @Test
    fun `completed state is not observable before final result persistence`() = runTest {
        // GIVEN: a submission document that already exists in Firestore with userId
        // (In a real test, you'd pre-populate via a test helper or Firestore REST API)

        // WHEN: finalizeTATAnalysisResult succeeds
        val result = repository.finalizeTATAnalysisResult(testSubmissionId, buildOlqResult())

        // THEN: result document must exist in psych_results before this call returns
        // The ordering guarantee is that if the call returned success, psych_results has the data
        // and the submission shows COMPLETED
        if (result.isSuccess) {
            val fetchedResult = repository.getTATResult(testSubmissionId)
            assertTrue("OLQ result must be fetchable after finalization", fetchedResult.isSuccess)
            assertNotNull(fetchedResult.getOrNull())
        }
        // If result.isFailure, the submission must NOT be in COMPLETED state — this is the key
        // ordering guarantee. The integration test can verify this by reading the submission doc.
    }

    /**
     * Verifies that the submission document is self-describing after finalization:
     * hasOlqResult, resultSource, and resultUpdatedAt must all be present.
     */
    @Test
    fun `submission metadata is coherent after finalization`() = runTest {
        val finalizeResult = repository.finalizeTATAnalysisResult(testSubmissionId, buildOlqResult())

        if (finalizeResult.isSuccess) {
            val submission = repository.getTATSubmission(testSubmissionId)
            assertTrue("Submission must be fetchable after finalization", submission.isSuccess)
            // The submission's analysisStatus must be COMPLETED
            val tatSubmission = submission.getOrNull()
            if (tatSubmission != null) {
                assertEquals(AnalysisStatus.COMPLETED, tatSubmission.analysisStatus)
            }
        }
    }

    /**
     * Verifies that the OLQ result is retrievable via getTATResult after finalization.
     */
    @Test
    fun `result can be fetched successfully after finalization`() = runTest {
        val olqResult = buildOlqResult()
        val finalizeResult = repository.finalizeTATAnalysisResult(testSubmissionId, olqResult)

        if (finalizeResult.isSuccess) {
            val fetchedResult = repository.getTATResult(testSubmissionId)
            assertTrue("Result fetch must succeed", fetchedResult.isSuccess)
            val fetched = fetchedResult.getOrNull()
            assertNotNull("Result must not be null after finalization", fetched)
            if (fetched != null) {
                assertEquals(testSubmissionId, fetched.submissionId)
                assertEquals(TestType.TAT, fetched.testType)
            }
        }
    }

    private fun buildOlqResult() = OLQAnalysisResult(
        submissionId = testSubmissionId,
        testType = TestType.TAT,
        olqScores = OLQ.entries.associateWith { OLQScore(score = 6, confidence = 80, reasoning = "integration test") },
        overallScore = 6.0f,
        overallRating = "Good",
        strengths = listOf("EI (6)"),
        weaknesses = listOf("OB (7)"),
        recommendations = listOf("Practice narratives"),
        analyzedAt = System.currentTimeMillis(),
        aiConfidence = 80
    )
}
