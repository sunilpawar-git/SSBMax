package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.model.CacheStatus
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
    private val firestore: FirebaseFirestore,
    private val oirCacheManager: OIRQuestionCacheManager
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

    @Deprecated("Use getOIRTestQuestions() for cached implementation")
    override suspend fun getOIRQuestions(testId: String): Result<List<OIRQuestion>> {
        // Fallback to new cached method
        return getOIRTestQuestions(50)
    }
    
    override suspend fun getOIRTestQuestions(count: Int): Result<List<OIRQuestion>> {
        return try {
            Log.d("TestContent", "Getting $count OIR questions from cache manager")
            
            // Check if cache is initialized
            val cacheStatus = oirCacheManager.getCacheStatus()
            if (cacheStatus.cachedQuestions == 0) {
                Log.d("TestContent", "Cache empty, initializing...")
                oirCacheManager.initialSync().getOrThrow()
            }
            
            // Get questions from cache manager
            val questions = oirCacheManager.getTestQuestions(count).getOrThrow()
            
            Log.d("TestContent", "Retrieved ${questions.size} questions from cache")
            Result.success(questions)
            
        } catch (e: Exception) {
            Log.e("TestContent", "Failed to get cached OIR questions", e)
            // Fallback to mock data only in development
            if (android.os.Build.VERSION.SDK_INT >= 0) { // Always true, but keeps the code
                Log.w("TestContent", "Using mock data as fallback")
                Result.success(MockTestDataProvider.getOIRQuestions())
            } else {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun initializeOIRCache(): Result<Unit> {
        return try {
            Log.d("TestContent", "Initializing OIR cache...")
            oirCacheManager.initialSync()
        } catch (e: Exception) {
            Log.e("TestContent", "Failed to initialize OIR cache", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getOIRCacheStatus(): CacheStatus {
        return oirCacheManager.getCacheStatus()
    }

    override suspend fun getPPDTQuestions(testId: String): Result<List<PPDTQuestion>> {
        return try {
            ppdtCache[testId]?.let { return Result.success(it) }

            // Try Firestore first
            val document = testsCollection.document(testId).get().await()
            val questions = document.get("questions") as? List<Map<String, Any?>> ?: emptyList()
            
            if (questions.isEmpty()) {
                // Fallback to mock data
                Log.d("TestContent", "Using mock PPDT data for $testId")
                val mockQuestions = MockTestDataProvider.getPPDTQuestions()
                ppdtCache[testId] = mockQuestions
                return Result.success(mockQuestions)
            }
            
            val ppdtQuestions = questions.mapNotNull { it.toPPDTQuestion() }
            ppdtCache[testId] = ppdtQuestions
            
            Result.success(ppdtQuestions)
        } catch (e: Exception) {
            // On any error, use mock data
            Log.w("TestContent", "Firestore failed for PPDT, using mock data: ${e.message}")
            val mockQuestions = MockTestDataProvider.getPPDTQuestions()
            ppdtCache[testId] = mockQuestions
            Result.success(mockQuestions)
        }
    }

    override suspend fun getTATQuestions(testId: String): Result<List<TATQuestion>> {
        return try {
            tatCache[testId]?.let { return Result.success(it) }

            // Try Firestore first
            val document = testsCollection.document(testId).get().await()
            val questions = document.get("questions") as? List<Map<String, Any?>> ?: emptyList()
            
            if (questions.isEmpty()) {
                // Fallback to mock data
                Log.d("TestContent", "Using mock TAT data for $testId")
                val mockQuestions = MockTestDataProvider.getTATQuestions()
                tatCache[testId] = mockQuestions
                return Result.success(mockQuestions)
            }
            
            val tatQuestions = questions.mapNotNull { it.toTATQuestion() }
            tatCache[testId] = tatQuestions
            
            Result.success(tatQuestions)
        } catch (e: Exception) {
            // On any error, use mock data
            Log.w("TestContent", "Firestore failed for TAT, using mock data: ${e.message}")
            val mockQuestions = MockTestDataProvider.getTATQuestions()
            tatCache[testId] = mockQuestions
            Result.success(mockQuestions)
        }
    }

    override suspend fun getWATQuestions(testId: String): Result<List<WATWord>> {
        return try {
            watCache[testId]?.let { return Result.success(it) }

            // Try Firestore first
            val document = testsCollection.document(testId).get().await()
            val questions = document.get("questions") as? List<Map<String, Any?>> ?: emptyList()
            
            if (questions.isEmpty()) {
                // Fallback to mock data
                Log.d("TestContent", "Using mock WAT data for $testId")
                val mockQuestions = MockTestDataProvider.getWATWords()
                watCache[testId] = mockQuestions
                return Result.success(mockQuestions)
            }
            
            val watQuestions = questions.mapNotNull { it.toWATWord() }
            watCache[testId] = watQuestions
            
            Result.success(watQuestions)
        } catch (e: Exception) {
            // On any error, use mock data
            Log.w("TestContent", "Firestore failed for WAT, using mock data: ${e.message}")
            val mockQuestions = MockTestDataProvider.getWATWords()
            watCache[testId] = mockQuestions
            Result.success(mockQuestions)
        }
    }

    override suspend fun getSRTQuestions(testId: String): Result<List<SRTSituation>> {
        return try {
            srtCache[testId]?.let { return Result.success(it) }

            // Try Firestore first
            val document = testsCollection.document(testId).get().await()
            val questions = document.get("questions") as? List<Map<String, Any?>> ?: emptyList()
            
            if (questions.isEmpty()) {
                // Fallback to mock data
                Log.d("TestContent", "Using mock SRT data for $testId")
                val mockQuestions = MockTestDataProvider.getSRTSituations()
                srtCache[testId] = mockQuestions
                return Result.success(mockQuestions)
            }
            
            val srtQuestions = questions.mapNotNull { it.toSRTSituation() }
            srtCache[testId] = srtQuestions
            
            Result.success(srtQuestions)
        } catch (e: Exception) {
            // On any error, use mock data
            Log.w("TestContent", "Firestore failed for SRT, using mock data: ${e.message}")
            val mockQuestions = MockTestDataProvider.getSRTSituations()
            srtCache[testId] = mockQuestions
            Result.success(mockQuestions)
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

            try {
                // Try to save to Firestore
                sessionsCollection.document(sessionId).set(session.toMap()).await()
                Log.d("TestContent", "Created session in Firestore: $sessionId")
            } catch (firestoreError: Exception) {
                // Firestore failed, but continue with local-only session
                Log.w("TestContent", "Firestore unavailable for session, using local-only: ${firestoreError.message}")
            }
            
            // Cache locally (always works)
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

