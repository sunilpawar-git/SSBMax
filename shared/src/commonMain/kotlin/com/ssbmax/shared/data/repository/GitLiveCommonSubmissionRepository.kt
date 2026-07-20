package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.SubmissionStatus
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.datetime.Clock

/**
 * GitLive-Firebase-backed port of the Android `CommonSubmissionRepository`.
 *
 * **Partial port, real blocker documented (not silently dropped):** the Android original's
 * read methods (`getSubmission`, `getUserSubmissions`, `getUserSubmissionsByTestType`,
 * `observeSubmission`, `observeUserSubmissions`, `getPendingSubmissionsForInstructor`) all return
 * a raw `Map<String, Any>` decoded from an arbitrary Firestore document shape (the "submissions"
 * collection stores a different `data` sub-shape per test type). The Android Firestore SDK
 * supports this directly (`DocumentSnapshot.getData(): Map<String, Any>`), but the GitLive
 * Firestore SDK (2.1.0, verified by reading its `firestore.kt`/`SafeValue.kt` sources directly,
 * not guessed) only exposes **typed** decode: `DocumentSnapshot.data<reified T>()` /
 * `data(DeserializationStrategy<T>)`, both of which resolve a `KSerializer` for `T` at compile
 * time. There is no public raw-`Map<String, Any?>` decode path — the internal `encodedData()`/
 * `getEncoded()` functions that would allow it are `@PublishedApi internal` (usable only from
 * GitLive's own inline functions, not from a consuming module like `shared`). Decoding into
 * `Map<String, Any>` would need either a hand-written contextual serializer for `Any` (no prior
 * slice in this codebase has established one) or changing the domain `SubmissionRepository`
 * interface's generic-`Map` methods to typed DTOs — both out of scope for this slice.
 *
 * The **write-only** methods below don't hit this blocker: Firestore field updates go through
 * `DocumentReference.update(vararg Pair<String, Any?>)`, which GitLive encodes via its untyped
 * `Any.safeValue` conversion (primitives/maps/lists pass through as-is, verified in
 * `SafeValue.kt`) rather than a `KSerializer`, so they're ported below. The blocked read/observe
 * methods are left unimplemented with a clear `Result.failure` explaining why, per this
 * migration's "never silently skip work" rule, rather than faked with an empty/wrong result.
 */
class GitLiveCommonSubmissionRepository {

    private val submissionsCollection = Firebase.firestore.collection(SUBMISSIONS_COLLECTION)

    suspend fun updateSubmissionStatus(submissionId: String, status: SubmissionStatus): Result<Unit> = try {
        submissionsCollection.document(submissionId).update(FIELD_STATUS to status.name)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to update status: ${e.message}", e))
    }

    suspend fun updateWithInstructorGrading(
        submissionId: String,
        instructorId: String,
        status: SubmissionStatus = SubmissionStatus.GRADED
    ): Result<Unit> = try {
        submissionsCollection.document(submissionId).update(
            FIELD_STATUS to status.name,
            FIELD_GRADED_BY_INSTRUCTOR_ID to instructorId,
            FIELD_GRADING_TIMESTAMP to Clock.System.now().toEpochMilliseconds()
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to update grading: ${e.message}", e))
    }

    suspend fun deleteSubmission(submissionId: String): Result<Unit> = try {
        submissionsCollection.document(submissionId).delete()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("Failed to delete submission: ${e.message}", e))
    }

    suspend fun getSubmission(submissionId: String): Result<Map<String, Any>?> = genericMapReadBlocked()

    suspend fun getUserSubmissions(userId: String, limit: Int): Result<List<Map<String, Any>>> = genericMapReadBlocked()

    suspend fun getUserSubmissionsByTestType(
        userId: String,
        testType: com.ssbmax.shared.domain.model.TestType,
        limit: Int
    ): Result<List<Map<String, Any>>> = genericMapReadBlocked()

    suspend fun getPendingSubmissionsForInstructor(
        batchId: String? = null,
        limit: Int = 100
    ): Result<List<Map<String, Any>>> = genericMapReadBlocked()

    private fun <T> genericMapReadBlocked(): Result<T> = Result.failure(UnsupportedOperationException(GENERIC_MAP_BLOCKER_MESSAGE))

    private companion object {
        const val SUBMISSIONS_COLLECTION = "submissions"
        const val FIELD_STATUS = "status"
        const val FIELD_GRADED_BY_INSTRUCTOR_ID = "gradedByInstructorId"
        const val FIELD_GRADING_TIMESTAMP = "gradingTimestamp"
        const val GENERIC_MAP_BLOCKER_MESSAGE =
            "Not yet portable: GitLive Firestore has no public raw Map<String, Any> decode path " +
                "(see GitLiveCommonSubmissionRepository's class doc for the verified reason)."
    }
}
