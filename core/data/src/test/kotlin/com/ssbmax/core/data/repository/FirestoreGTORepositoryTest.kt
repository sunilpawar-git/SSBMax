package com.ssbmax.core.data.repository

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.ssbmax.core.domain.model.gto.GTOAnalysisUnavailableException
import com.ssbmax.core.domain.model.gto.GTOResultStatus
import com.ssbmax.core.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.repository.TestContentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.TimeUnit

/**
 * Unit tests for FirestoreGTORepository's result-resolution paths.
 *
 * Focuses on the behaviour that the dashboard depends on:
 * - Lazy self-heal: a legacy submission (scores embedded, missing from gto_results) is migrated
 *   into gto_results on read so subsequent reads skip the second round-trip.
 * - Worker-fail hardening: a submitted-but-unscored submission surfaces a typed
 *   GTOAnalysisUnavailableException (→ AnalysisPending) instead of a generic failure.
 * - getLatestResult returns NotAttempted when the user has no submission for a type.
 */
class FirestoreGTORepositoryTest {

    @get:Rule
    val timeout: Timeout = Timeout(60, TimeUnit.SECONDS)

    private lateinit var firestore: FirebaseFirestore
    private lateinit var taskCacheManager: GTOTaskCacheManager
    private lateinit var testContentRepository: TestContentRepository
    private lateinit var repository: FirestoreGTORepository

    private lateinit var gtoResultsCollection: CollectionReference
    private lateinit var submissionsCollection: CollectionReference

    private val gtoResultsRefs = mutableMapOf<String, DocumentReference>()
    private val submissionRefs = mutableMapOf<String, DocumentReference>()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        gtoResultsCollection = mockk(relaxed = true)
        submissionsCollection = mockk(relaxed = true)

        firestore = mockk(relaxed = true)
        every { firestore.collection("gto_results") } returns gtoResultsCollection
        every { firestore.collection("submissions") } returns submissionsCollection

        taskCacheManager = mockk(relaxed = true)
        testContentRepository = mockk(relaxed = true)

        repository = FirestoreGTORepository(
            firestore = firestore,
            taskCacheManager = taskCacheManager,
            testContentRepository = testContentRepository
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // ---- helpers ----

    private fun gtoResultRef(id: String): DocumentReference =
        gtoResultsRefs.getOrPut(id) {
            mockk<DocumentReference>(relaxed = true).also { every { gtoResultsCollection.document(id) } returns it }
        }

    private fun submissionRef(id: String): DocumentReference =
        submissionRefs.getOrPut(id) {
            mockk<DocumentReference>(relaxed = true).also { every { submissionsCollection.document(id) } returns it }
        }

    private fun docSnapshot(exists: Boolean, data: Map<String, Any>?): DocumentSnapshot {
        val snap = mockk<DocumentSnapshot>(relaxed = true)
        every { snap.exists() } returns exists
        every { snap.data } returns data
        every { snap.getString(any()) } answers { data?.get(firstArg<String>()) as? String }
        return snap
    }

    /** A GD submission document with embedded OLQ scores (legacy shape, never written to gto_results). */
    private fun legacyGdSubmissionData(id: String, status: GTOSubmissionStatus) = mapOf(
        "id" to id,
        "userId" to "user_1",
        "testType" to "GTO_GD",
        "status" to status.name,
        "olqScores" to mapOf(
            OLQ.EFFECTIVE_INTELLIGENCE.name to mapOf("score" to 5, "confidence" to 80, "reasoning" to "ok"),
            OLQ.SOCIAL_ADJUSTMENT.name to mapOf("score" to 4, "confidence" to 80, "reasoning" to "ok")
        )
    )

    /** A GD submission document with NO scores (worker pending / failed). */
    private fun unscoredGdSubmissionData(id: String) = mapOf(
        "id" to id,
        "userId" to "user_1",
        "testType" to "GTO_GD",
        "status" to GTOSubmissionStatus.PENDING_ANALYSIS.name
        // no olqScores
    )

    // ---- tests ----

    @Test
    fun `getTestResult self-heals legacy scores into gto_results on miss`() = runTest {
        val id = "sub_legacy"
        // Resolve refs first (this registers collection.document(id)); then stub their calls so we
        // never nest one every{} inside another.
        val resRef = gtoResultRef(id)
        val subRef = submissionRef(id)
        // gto_results miss
        every { resRef.get() } returns Tasks.forResult(docSnapshot(exists = false, data = null))
        every { resRef.set(any()) } returns Tasks.forResult(null)
        // submission has embedded scores
        every { subRef.get() } returns
            Tasks.forResult(docSnapshot(exists = true, data = legacyGdSubmissionData(id, GTOSubmissionStatus.COMPLETED)))

        val result = repository.getTestResult(id)

        assertTrue("Legacy scores must resolve to a result", result.isSuccess)
        assertEquals(id, result.getOrNull()?.submissionId)
        // The whole point: the legacy result is written back to gto_results so next read hits primary.
        verify { resRef.set(any()) }
    }

    @Test
    fun `getTestResult returns typed unavailable when submission has no scores anywhere`() = runTest {
        val id = "sub_unscored"
        val resRef = gtoResultRef(id)
        val subRef = submissionRef(id)
        every { resRef.get() } returns Tasks.forResult(docSnapshot(exists = false, data = null))
        every { subRef.get() } returns
            Tasks.forResult(docSnapshot(exists = true, data = unscoredGdSubmissionData(id)))

        val result = repository.getTestResult(id)

        assertTrue("Unscored submission must fail (not silently succeed)", result.isFailure)
        assertTrue(
            "Failure must be the typed pending signal, not a generic error",
            result.exceptionOrNull() is GTOAnalysisUnavailableException
        )
    }

    @Test
    fun `getLatestResult returns NotAttempted when user has no submission for the type`() = runTest {
        // Lean query chain → empty result set.
        val emptySnapshot = mockk<QuerySnapshot>(relaxed = true)
        every { emptySnapshot.documents } returns emptyList()

        val q1 = mockk<Query>(relaxed = true)
        val q2 = mockk<Query>(relaxed = true)
        val q3 = mockk<Query>(relaxed = true)
        every { submissionsCollection.whereEqualTo("userId", "user_1") } returns q1
        every { q1.whereEqualTo("testType", "GTO_GD") } returns q2
        every { q2.orderBy("submittedAt", Query.Direction.DESCENDING) } returns q3
        every { q3.limit(1) } returns q3
        every { q3.get() } returns Tasks.forResult(emptySnapshot)

        val result = repository.getLatestResult("user_1", GTOTestType.GROUP_DISCUSSION)

        assertTrue(result.isSuccess)
        assertEquals(GTOResultStatus.NotAttempted, result.getOrNull())
    }
}
