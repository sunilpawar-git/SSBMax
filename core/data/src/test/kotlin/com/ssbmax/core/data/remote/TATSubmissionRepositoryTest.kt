package com.ssbmax.core.data.remote

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ssbmax.core.data.util.failureTask
import com.ssbmax.core.data.util.successTask
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TATSubmissionRepository.finalizeTATAnalysisResult].
 *
 * Verifies the finalization contract:
 * - OLQ result is persisted to `psych_results` before COMPLETED is written.
 * - Submission metadata includes resultUpdatedAt, hasOlqResult, resultSource.
 * - userId is preserved for Firestore security rule compliance.
 * - Returns failure when any persistence step fails.
 */
class TATSubmissionRepositoryTest {

    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockSubmissionsCollection: CollectionReference
    private lateinit var mockPsychResultsCollection: CollectionReference
    private lateinit var mockSubmissionDoc: DocumentReference
    private lateinit var mockPsychResultDoc: DocumentReference
    private lateinit var repository: TATSubmissionRepository

    private val submissionId = "sub-123"
    private val userId = "user-456"

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        mockkStatic(FirebaseFirestore::class)
        mockFirestore = mockk(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns mockFirestore

        mockSubmissionsCollection = mockk(relaxed = true)
        mockPsychResultsCollection = mockk(relaxed = true)
        every { mockFirestore.collection("submissions") } returns mockSubmissionsCollection
        every { mockFirestore.collection("psych_results") } returns mockPsychResultsCollection

        mockSubmissionDoc = mockk(relaxed = true)
        mockPsychResultDoc = mockk(relaxed = true)
        every { mockSubmissionsCollection.document(submissionId) } returns mockSubmissionDoc
        every { mockPsychResultsCollection.document(submissionId) } returns mockPsychResultDoc

        repository = TATSubmissionRepository()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ===========================
    // finalizeTATAnalysisResult
    // ===========================

    @Test
    fun `finalizeTATAnalysisResult succeeds when both writes succeed`() = runTest {
        val snapshot = buildSnapshot(userId)
        every { mockSubmissionDoc.get() } returns successTask(snapshot)
        every { mockPsychResultDoc.set(any<Map<String, Any>>(), any<SetOptions>()) } returns successTask(null)
        every { mockSubmissionDoc.update(any<Map<String, Any>>()) } returns successTask(null)

        val result = repository.finalizeTATAnalysisResult(submissionId, buildOlqResult())

        assertTrue(result.isSuccess)
    }

    @Test
    fun `finalizeTATAnalysisResult fails when psych_results write fails`() = runTest {
        val snapshot = buildSnapshot(userId)
        val writeError = Exception("Firestore write quota exceeded")
        every { mockSubmissionDoc.get() } returns successTask(snapshot)
        every { mockPsychResultDoc.set(any<Map<String, Any>>(), any<SetOptions>()) } returns failureTask(writeError)

        val result = repository.finalizeTATAnalysisResult(submissionId, buildOlqResult())

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `finalizeTATAnalysisResult fails when submission metadata write fails`() = runTest {
        val snapshot = buildSnapshot(userId)
        val updateError = Exception("Firestore network unavailable")
        every { mockSubmissionDoc.get() } returns successTask(snapshot)
        every { mockPsychResultDoc.set(any<Map<String, Any>>(), any<SetOptions>()) } returns successTask(null)
        every { mockSubmissionDoc.update(any<Map<String, Any>>()) } returns failureTask(updateError)

        val result = repository.finalizeTATAnalysisResult(submissionId, buildOlqResult())

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `finalizeTATAnalysisResult fails when userId cannot be found`() = runTest {
        val snapshot = buildSnapshot(userId = null)
        every { mockSubmissionDoc.get() } returns successTask(snapshot)

        val result = repository.finalizeTATAnalysisResult(submissionId, buildOlqResult())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("userId") == true)
    }

    @Test
    fun `finalizeTATAnalysisResult preserves userId for security rules`() = runTest {
        val snapshot = buildSnapshot(userId)
        val capturedPayload = slot<Map<String, Any>>()
        every { mockSubmissionDoc.get() } returns successTask(snapshot)
        every {
            mockPsychResultDoc.set(capture(capturedPayload), any<SetOptions>())
        } returns successTask(null)
        every { mockSubmissionDoc.update(any<Map<String, Any>>()) } returns successTask(null)

        repository.finalizeTATAnalysisResult(submissionId, buildOlqResult())

        assertEquals(userId, capturedPayload.captured["userId"])
    }

    @Test
    fun `finalizeTATAnalysisResult writes psych_results before marking COMPLETED`() = runTest {
        val snapshot = buildSnapshot(userId)
        every { mockSubmissionDoc.get() } returns successTask(snapshot)
        every { mockPsychResultDoc.set(any<Map<String, Any>>(), any<SetOptions>()) } returns successTask(null)
        every { mockSubmissionDoc.update(any<Map<String, Any>>()) } returns successTask(null)

        repository.finalizeTATAnalysisResult(submissionId, buildOlqResult())

        coVerifyOrder {
            mockPsychResultDoc.set(any<Map<String, Any>>(), any<SetOptions>())
            mockSubmissionDoc.update(any<Map<String, Any>>())
        }
    }

    @Test
    fun `finalizeTATAnalysisResult includes self-describing metadata in submission update`() = runTest {
        val snapshot = buildSnapshot(userId)
        val capturedUpdate = slot<Map<String, Any>>()
        every { mockSubmissionDoc.get() } returns successTask(snapshot)
        every { mockPsychResultDoc.set(any<Map<String, Any>>(), any<SetOptions>()) } returns successTask(null)
        every { mockSubmissionDoc.update(capture(capturedUpdate)) } returns successTask(null)

        repository.finalizeTATAnalysisResult(submissionId, buildOlqResult())

        assertEquals(SubmissionConstants.ANALYSIS_STATUS_COMPLETED, capturedUpdate.captured["data.analysisStatus"])
        assertEquals(true, capturedUpdate.captured["data.hasOlqResult"])
        assertEquals("psych_results", capturedUpdate.captured["data.resultSource"])
        assertNotNull(capturedUpdate.captured["data.resultUpdatedAt"])
    }

    // ===========================
    // updateTATOLQResult (legacy)
    // ===========================

    @Test
    fun `updateTATOLQResult legacy path is not used by synthesis finalization contract`() {
        // Compile-time safety net: verify both methods exist on the class (suspend functions are
        // compiled to JVM methods whose names contain the Kotlin function name as a prefix).
        val methodNames = TATSubmissionRepository::class.java.declaredMethods.map { it.name }
        assertTrue(
            "finalizeTATAnalysisResult must exist as a JVM method",
            methodNames.any { it.contains("finalizeTATAnalysisResult") }
        )
        assertTrue(
            "updateTATOLQResult must remain for legacy workers",
            methodNames.any { it.contains("updateTATOLQResult") }
        )
    }

    // ===========================
    // Helpers
    // ===========================

    private fun buildSnapshot(userId: String?): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { snapshot.exists() } returns true
        every { snapshot.getString("userId") } returns userId
        return snapshot
    }

    private fun buildOlqResult() = OLQAnalysisResult(
        submissionId = submissionId,
        testType = TestType.TAT,
        olqScores = OLQ.entries.associateWith { OLQScore(score = 6, confidence = 80, reasoning = "test") },
        overallScore = 6.0f,
        overallRating = "Good",
        strengths = listOf("EI (6)"),
        weaknesses = listOf("OB (7)"),
        recommendations = listOf("Practice narratives"),
        analyzedAt = 1_000_000L,
        aiConfidence = 80
    )
}
