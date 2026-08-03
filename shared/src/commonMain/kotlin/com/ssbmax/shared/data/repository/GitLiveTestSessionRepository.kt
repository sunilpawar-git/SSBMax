package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.TestSessionRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * GitLive-Firebase-backed port of the Android `core:data`
 * `TestSessionManagerImpl` (best-effort Firestore persistence, same
 * `test_sessions` collection and field names; the Android original's
 * in-memory `ConcurrentHashMap` fast-path is dropped — `shared` code runs on
 * both platforms with no shared-process guarantee to make an in-memory map
 * meaningful the same way, and every real caller of this interface only
 * calls `createTestSession`/`endTestSession` once per session, never
 * `hasActiveTestSession` in a hot loop — so the Firestore round trip isn't
 * a regression in practice).
 *
 * `createTestSession` preserves the Android original's other real behavior: session IDs are
 * opaque/collision-free (`Uuid.random()`, the KMP-safe equivalent of the original's
 * `UUID.randomUUID()` — NOT a `"$userId-$testId-$timestamp"` composite key, which could collide
 * and silently overwrite an in-flight session on a double-tap/retry), and a Firestore write
 * failure does not fail the whole call — it still returns `Result.success` so a candidate on a
 * flaky connection isn't blocked from starting OIR/TAT/PPDT/SDT/SRT/WAT (every caller either
 * `.getOrThrow()`s or shows a blocking "cloud connection required" error on failure). This mirrors
 * the Android original's own try/catch-and-log-warning fallback around its Firestore `.set()` call.
 */
@OptIn(ExperimentalUuidApi::class)
class GitLiveTestSessionRepository : TestSessionRepository {

    private val sessionsCollection = Firebase.firestore.collection(COLLECTION)

    override suspend fun hasActiveTestSession(userId: String, testId: String): Result<Boolean> = try {
        val snapshot = sessionsCollection
            .where { "userId" equalTo userId }
            .where { "testId" equalTo testId }
            .where { "isActive" equalTo true }
            .get()
        Result.success(snapshot.documents.isNotEmpty())
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createTestSession(userId: String, testId: String, testType: TestType): Result<String> {
        val now = Clock.System.now().toEpochMilliseconds()
        val sessionId = Uuid.random().toString()
        val dto = TestSessionDto(
            id = sessionId,
            userId = userId,
            testId = testId,
            testType = testType.name,
            startTime = now,
            expiryTime = now + TWO_HOURS_MS,
            isActive = true
        )
        return try {
            sessionsCollection.document(sessionId).set(dto)
            Result.success(sessionId)
        } catch (e: Exception) {
            // Best-effort fallback, matching the Android original: a transient Firestore failure
            // must not block a candidate from starting their test. The session then only exists
            // client-side for this process's lifetime (same durability the original's dropped
            // in-memory map had) — endTestSession for it will fail (no doc to update), same as
            // the Android original's identical gap for a session that took this same fallback path.
            Result.success(sessionId)
        }
    }

    override suspend fun endTestSession(sessionId: String): Result<Unit> = try {
        sessionsCollection.document(sessionId).update(
            "isActive" to false,
            "endTime" to Clock.System.now().toEpochMilliseconds()
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        const val COLLECTION = "test_sessions"
        const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L
    }
}

@Serializable
private data class TestSessionDto(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val testType: String = "",
    val startTime: Long = 0L,
    val expiryTime: Long = 0L,
    val isActive: Boolean = true
)
