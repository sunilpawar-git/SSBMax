package com.ssbmax.shared.data.repository

/**
 * GitLive-Firebase-backed port of the Android `SubmissionArchiveRepository`.
 *
 * **Not ported, explicitly deferred:** `archiveOldSubmissions`/`deleteOldArchivedSubmissions` both
 * need to read an arbitrary submission document as a raw `Map<String, Any?>` (to copy it verbatim
 * into `archived_submissions` before deleting the original) and to run a `collectionGroup("submissions")`
 * query. The `collectionGroup` query itself is fine (GitLive's `FirebaseFirestore.collectionGroup`
 * exists and returns a `Query`, verified in `firestore.kt`) — the blocker is the same one documented
 * in [GitLiveCommonSubmissionRepository]'s class doc: GitLive's `DocumentSnapshot` has no public
 * raw-`Map<String, Any>` decode path, only typed `data<reified T>()`. Copying a document byte-for-byte
 * without knowing its shape isn't achievable through the typed decode API. Revisit alongside
 * `GitLiveCommonSubmissionRepository`'s deferred read methods — the same fix (a hand-written
 * contextual `Any` serializer, or reworking these two Android-original methods to go through the
 * per-test-type typed repos instead of a raw collection-group copy) unblocks both at once.
 */
class GitLiveSubmissionArchiveRepository {

    suspend fun archiveOldSubmissions(beforeTimestamp: Long): Result<Int> = Result.failure(
        UnsupportedOperationException(
            "Not yet portable: needs a raw Map<String, Any> Firestore document read; " +
                "see GitLiveSubmissionArchiveRepository's class doc."
        )
    )
}
