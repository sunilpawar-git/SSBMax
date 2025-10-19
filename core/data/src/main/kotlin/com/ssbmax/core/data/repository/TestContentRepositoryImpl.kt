package com.ssbmax.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TestContentRepository
 * Fetches test content from Firestore with in-memory caching
 * Never persists content to local storage to prevent APK sideloading
 */
@Singleton
class TestContentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TestContentRepository {

    // In-memory caches - cleared after test completion
    private val oirCache = ConcurrentHashMap<String, List<OIRQuestion>>()
    private val ppdtCache = ConcurrentHashMap<String, List<PPDTQuestion>>()
    private val tatCache = ConcurrentHashMap<String, List<TATQuestion>>()
    private val watCache = ConcurrentHashMap<String, List<WATWord>>()
    private val srtCache = ConcurrentHashMap<String, List<SRTSituation>>()
    
    // Active test sessions
    private val activeSessions = ConcurrentHashMap<String, TestSession>()

    private val testsCollection = firestore.collection("tests")
    private val sessionsCollection = firestore.collection("test_sessions")

    override suspend fun getOIRQuestions(testId: String): Result<List<OIRQuestion>> {
        return try {
            // Check cache first
            oirCache[testId]?.let { return Result.success(it) }

            // Fetch from Firestore
            val document = testsCollection.document(testId).get().await()
            val questions = document.get("questions") as? List<Map<String, Any?>> ?: emptyList()
            
            val oirQuestions = questions.mapNotNull { it.toOIRQuestion() }
            
            // Cache in memory
            oirCache[testId] = oirQuestions
            
            Result.success(oirQuestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPPDTQuestions(testId: String): Result<List<PPDTQuestion>> {
        return try {
            ppdtCache[testId]?.let { return Result.success(it) }

            val document = testsCollection.document(testId).get().await()
            val questions = document.get("questions") as? List<Map<String, Any?>> ?: emptyList()
            
            val ppdtQuestions = questions.mapNotNull { it.toPPDTQuestion() }
            ppdtCache[testId] = ppdtQuestions
            
            Result.success(ppdtQuestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTATQuestions(testId: String): Result<List<TATQuestion>> {
        return try {
            tatCache[testId]?.let { return Result.success(it) }

            val document = testsCollection.document(testId).get().await()
            val questions = document.get("questions") as? List<Map<String, Any?>> ?: emptyList()
            
            val tatQuestions = questions.mapNotNull { it.toTATQuestion() }
            tatCache[testId] = tatQuestions
            
            Result.success(tatQuestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWATQuestions(testId: String): Result<List<WATWord>> {
        return try {
            watCache[testId]?.let { return Result.success(it) }

            val document = testsCollection.document(testId).get().await()
            val questions = document.get("questions") as? List<Map<String, Any?>> ?: emptyList()
            
            val watQuestions = questions.mapNotNull { it.toWATWord() }
            watCache[testId] = watQuestions
            
            Result.success(watQuestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSRTQuestions(testId: String): Result<List<SRTSituation>> {
        return try {
            srtCache[testId]?.let { return Result.success(it) }

            val document = testsCollection.document(testId).get().await()
            val questions = document.get("questions") as? List<Map<String, Any?>> ?: emptyList()
            
            val srtQuestions = questions.mapNotNull { it.toSRTSituation() }
            srtCache[testId] = srtQuestions
            
            Result.success(srtQuestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasActiveTestSession(userId: String, testId: String): Result<Boolean> {
        return try {
            // Check local cache first
            val hasLocalSession = activeSessions.values.any { 
                it.userId == userId && it.testId == testId && !it.isExpired()
            }
            
            if (hasLocalSession) {
                return Result.success(true)
            }

            // Check Firestore for active session
            val snapshot = sessionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("testId", testId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createTestSession(
        userId: String,
        testId: String,
        testType: TestType
    ): Result<String> {
        return try {
            val sessionId = UUID.randomUUID().toString()
            val session = TestSession(
                id = sessionId,
                userId = userId,
                testId = testId,
                testType = testType,
                startTime = System.currentTimeMillis(),
                expiryTime = System.currentTimeMillis() + (2 * 60 * 60 * 1000), // 2 hours
                isActive = true
            )

            // Save to Firestore
            sessionsCollection.document(sessionId).set(session.toMap()).await()
            
            // Cache locally
            activeSessions[sessionId] = session

            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun endTestSession(sessionId: String): Result<Unit> {
        return try {
            // Remove from local cache
            activeSessions.remove(sessionId)

            // Update Firestore
            sessionsCollection.document(sessionId)
                .update("isActive", false, "endTime", System.currentTimeMillis())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun clearCache() {
        oirCache.clear()
        ppdtCache.clear()
        tatCache.clear()
        watCache.clear()
        srtCache.clear()
    }


    // Internal data class for test sessions
    private data class TestSession(
        val id: String,
        val userId: String,
        val testId: String,
        val testType: TestType,
        val startTime: Long,
        val expiryTime: Long,
        val isActive: Boolean
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiryTime

        fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "userId" to userId,
            "testId" to testId,
            "testType" to testType.name,
            "startTime" to startTime,
            "expiryTime" to expiryTime,
            "isActive" to isActive
        )
    }
}

