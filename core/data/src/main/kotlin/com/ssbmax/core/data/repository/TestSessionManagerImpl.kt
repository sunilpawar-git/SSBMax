package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.TestSessionRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestSessionManagerImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TestSessionRepository {

    private val activeSessions = ConcurrentHashMap<String, TestSession>()
    private val sessionsCollection = firestore.collection("test_sessions")

    override suspend fun hasActiveTestSession(userId: String, testId: String): Result<Boolean> {
        return try {
            val hasLocalSession = activeSessions.values.any {
                it.userId == userId && it.testId == testId && !it.isExpired()
            }
            if (hasLocalSession) return Result.success(true)

            val snapshot = sessionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("testId", testId)
                .whereEqualTo("isActive", true)
                .get().await()
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createTestSession(userId: String, testId: String, testType: TestType): Result<String> {
        return try {
            val sessionId = UUID.randomUUID().toString()
            val session = TestSession(
                id = sessionId, userId = userId, testId = testId, testType = testType,
                startTime = System.currentTimeMillis(),
                expiryTime = System.currentTimeMillis() + (2 * 60 * 60 * 1000),
                isActive = true
            )
            try {
                sessionsCollection.document(sessionId).set(session.toMap()).await()
                Log.d("TestSession", "Created session in Firestore: $sessionId")
            } catch (firestoreError: Exception) {
                Log.w("TestSession", "Firestore unavailable for session, using local-only: ${firestoreError.message}")
            }
            activeSessions[sessionId] = session
            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun endTestSession(sessionId: String): Result<Unit> {
        return try {
            activeSessions.remove(sessionId)
            sessionsCollection.document(sessionId)
                .update("isActive", false, "endTime", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class TestSession(
        val id: String, val userId: String, val testId: String,
        val testType: TestType, val startTime: Long, val expiryTime: Long, val isActive: Boolean
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiryTime
        fun toMap(): Map<String, Any?> = mapOf(
            "id" to id, "userId" to userId, "testId" to testId,
            "testType" to testType.name, "startTime" to startTime,
            "expiryTime" to expiryTime, "isActive" to isActive
        )
    }
}
