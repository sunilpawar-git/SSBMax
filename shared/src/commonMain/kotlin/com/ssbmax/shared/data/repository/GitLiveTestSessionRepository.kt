package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.TestSessionRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

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
 */
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

    override suspend fun createTestSession(userId: String, testId: String, testType: TestType): Result<String> = try {
        val now = Clock.System.now().toEpochMilliseconds()
        val sessionId = "${userId}_${testId}_$now"
        val dto = TestSessionDto(
            id = sessionId,
            userId = userId,
            testId = testId,
            testType = testType.name,
            startTime = now,
            expiryTime = now + TWO_HOURS_MS,
            isActive = true
        )
        sessionsCollection.document(sessionId).set(dto)
        Result.success(sessionId)
    } catch (e: Exception) {
        Result.failure(e)
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
