package com.ssbmax.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.FirebaseTestHelper
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.TestType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for [GradingQueueRepositoryImpl] with Firebase Emulator.
 *
 * Ported (KMP Phase 7) from a Hilt/`BaseRepositoryTest`-based version that predated the
 * Phase 3 Hilt->Koin migration and never compiled afterwards (missing `hilt-android-testing`
 * dependency, deleted `ThemePreferenceManager` import, and an API surface — `getPendingSubmissions`,
 * `claimSubmission`, `getInstructorQueue`, `submitGrading`, `getSubmissionDetails`,
 * `observeQueueUpdates` — that no longer matches [com.ssbmax.shared.domain.repository.GradingQueueRepository]'s
 * real methods). Rewritten against the actual current interface, using this module's plain-JUnit +
 * `FirebaseTestHelper.getEmulatorFirestore()` pattern (matches `TestSubmissionRepositoryImplTest`,
 * `TestContentRepositoryImplTest`), not the abandoned Hilt-DI harness.
 *
 * Prerequisites: Firebase Emulator running (`firebase emulators:start --only firestore`).
 */
class GradingQueueRepositoryImplTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: GradingQueueRepositoryImpl
    private val seededIds = mutableListOf<String>()

    @Before
    fun setUp() {
        firestore = FirebaseTestHelper.getEmulatorFirestore()
        repository = GradingQueueRepositoryImpl(firestore)
    }

    @After
    fun tearDown() = runTest {
        seededIds.forEach { id ->
            try {
                firestore.collection("submissions").document(id).delete().await()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        seededIds.clear()
    }

    // Why this matters: instructors triage the queue oldest-first so nothing gets neglected,
    // and already-graded/not-yet-submitted work must never reappear in their pending queue.
    @Test
    fun observePendingSubmissions_excludes_graded_and_draft_and_sorts_oldest_first() = runTest {
        val now = System.currentTimeMillis()
        val oldest = seedSubmission(status = SubmissionStatus.SUBMITTED_PENDING_REVIEW, submittedAt = now - 100_000)
        val newer = seedSubmission(status = SubmissionStatus.UNDER_REVIEW, submittedAt = now - 10_000)
        seedSubmission(status = SubmissionStatus.GRADED, submittedAt = now - 200_000)
        seedSubmission(status = SubmissionStatus.DRAFT, submittedAt = now - 300_000)

        val items = repository.observePendingSubmissions("instructor_001").first()

        assertEquals("Should only surface the pending+under_review items", 2, items.size)
        assertEquals("Oldest submission must be first", oldest, items[0].submissionId)
        assertEquals("Newer submission must be second", newer, items[1].submissionId)
    }

    // Why this matters: the instructor's per-test-type queue view (e.g. "only show me TAT")
    // must not leak submissions of other test types.
    @Test
    fun observeSubmissionsByTestType_filters_to_requested_type_only() = runTest {
        val tatId = seedSubmission(testType = TestType.TAT, status = SubmissionStatus.SUBMITTED_PENDING_REVIEW)
        seedSubmission(testType = TestType.WAT, status = SubmissionStatus.SUBMITTED_PENDING_REVIEW)

        val items = repository.observeSubmissionsByTestType(TestType.TAT).first()

        assertEquals("Only TAT submissions should be returned", 1, items.size)
        assertEquals(tatId, items[0].submissionId)
    }

    // Why this matters: instructor batches must be strictly isolated - grading a different
    // batch's submissions by accident is a real workflow bug, not a cosmetic one.
    @Test
    fun observeSubmissionsByBatch_filters_to_requested_batch_only() = runTest {
        val batchAId = seedSubmission(batchId = "batch_A", status = SubmissionStatus.SUBMITTED_PENDING_REVIEW)
        seedSubmission(batchId = "batch_B", status = SubmissionStatus.SUBMITTED_PENDING_REVIEW)

        val items = repository.observeSubmissionsByBatch("batch_A").first()

        assertEquals("Only batch_A submissions should be returned", 1, items.size)
        assertEquals(batchAId, items[0].submissionId)
    }

    // Why this matters: this is the mutual-exclusion mechanism preventing two instructors
    // from grading the same submission simultaneously - it must actually persist the claim.
    @Test
    fun markSubmissionUnderReview_claims_submission_for_instructor() = runTest {
        val id = seedSubmission(status = SubmissionStatus.SUBMITTED_PENDING_REVIEW)

        val result = repository.markSubmissionUnderReview(id, "instructor_001")

        assertTrue("Claim should succeed", result.isSuccess)
        val doc = firestore.collection("submissions").document(id).get().await()
        assertEquals(SubmissionStatus.UNDER_REVIEW.name, doc.getString("status"))
        assertEquals("instructor_001", doc.getString("gradedByInstructorId"))
    }

    // Why this matters: an instructor abandoning a review must return the submission to the
    // shared pending pool, unclaimed - otherwise it is silently lost from every instructor's queue.
    @Test
    fun releaseSubmissionFromReview_reverts_to_pending_and_clears_claim() = runTest {
        val id = seedSubmission(status = SubmissionStatus.UNDER_REVIEW)
        firestore.collection("submissions").document(id)
            .update("gradedByInstructorId", "instructor_001").await()

        val result = repository.releaseSubmissionFromReview(id)

        assertTrue("Release should succeed", result.isSuccess)
        val doc = firestore.collection("submissions").document(id).get().await()
        assertEquals(SubmissionStatus.SUBMITTED_PENDING_REVIEW.name, doc.getString("status"))
        assertNull("Instructor claim must be cleared", doc.getString("gradedByInstructorId"))
    }

    // Why this matters: the instructor dashboard's headline counters (pending/graded/today/week)
    // drive their workload picture - wrong counts mean instructors misjudge their queue.
    // NOTE: `observeGradingStats` (production code, unchanged here) listens to the entire
    // "submissions" collection with no per-instructor/per-run filter, so this asserts
    // "at least" the counts this test seeded rather than exact equality - a real emulator
    // instance may carry documents from other test classes in the same run.
    @Test
    fun observeGradingStats_computes_pending_and_graded_counts() = runTest {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        seedSubmission(status = SubmissionStatus.SUBMITTED_PENDING_REVIEW)
        seedSubmission(status = SubmissionStatus.UNDER_REVIEW)
        seedSubmission(status = SubmissionStatus.GRADED, gradedAt = now - (2 * 60 * 60 * 1000L)) // today
        seedSubmission(status = SubmissionStatus.GRADED, gradedAt = now - (8 * oneDay)) // outside the week window

        val stats = repository.observeGradingStats("instructor_001").first()

        assertTrue("Pending + under-review both count as pending", stats.totalPending >= 2)
        assertTrue("Both graded submissions should count as graded", stats.totalGraded >= 2)
        assertTrue("The recent grading should count as today", stats.todayGraded >= 1)
        assertTrue("The recent grading falls within the week window", stats.weekGraded >= 1)
    }

    // ==================== HELPER METHODS ====================

    private suspend fun seedSubmission(
        testType: TestType = TestType.TAT,
        status: SubmissionStatus,
        submittedAt: Long = System.currentTimeMillis(),
        batchId: String? = null,
        gradedAt: Long? = null
    ): String {
        val id = "submission_${System.nanoTime()}"
        val data = mutableMapOf<String, Any?>(
            "id" to id,
            "userId" to "student_001",
            "testType" to testType.name,
            "status" to status.name,
            "submittedAt" to submittedAt,
            "batchId" to batchId
        )
        if (gradedAt != null) {
            data["gradedAt"] = gradedAt
        }
        firestore.collection("submissions").document(id).set(data).await()
        seededIds.add(id)
        return id
    }
}
