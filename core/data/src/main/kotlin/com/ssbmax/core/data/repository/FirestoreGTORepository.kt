package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ssbmax.core.domain.model.gto.*
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.GTORepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of GTORepository
 * 
 * Collections:
 * - `test_content/gto/topics/gd/batches/{batchId}`: GD topics
 * - `test_content/gto/topics/lecturette/batches/{batchId}`: Lecturette topics
 * - `test_content/gto/scenarios/gpe/batches/{batchId}`: GPE scenarios
 * - `test_content/gto/obstacles/{testType}/{setId}`: Obstacle configurations
 * - `gto_submissions/{submissionId}`: User submissions
 * - `user_gto_progress/{userId}`: User progress tracking
 */
@Singleton
class FirestoreGTORepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val taskCacheManager: GTOTaskCacheManager,
    private val testContentRepository: com.ssbmax.core.domain.repository.TestContentRepository
) : GTORepository {
    
    companion object {
        private const val TAG = "GTORepository"
        
        // Collection paths
        private const val COLLECTION_TEST_CONTENT = "test_content"
        private const val COLLECTION_GTO_SUBMISSIONS = "submissions"  // Changed from gto_submissions to match other tests
        private const val COLLECTION_USER_GTO_PROGRESS = "user_progress"
        
        // Subcollection paths
        private const val PATH_GTO = "gto"
        private const val PATH_TOPICS = "topics"
        private const val PATH_SCENARIOS = "scenarios"
        private const val PATH_OBSTACLES = "obstacles"
        private const val PATH_BATCHES = "batches"
        
        // Document fields
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TEST_ID = "testId"
        private const val FIELD_TEST_TYPE = "testType"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SUBMITTED_AT = "submittedAt"
        private const val FIELD_OLQ_SCORES = "olqScores"
        private const val FIELD_COMPLETED_TESTS = "completedTests"
        private const val FIELD_TESTS_USED_THIS_MONTH = "testsUsedThisMonth"
        
        // Default batch IDs
        private const val DEFAULT_BATCH_ID = "batch_001"
    }
    
    // ==================== Test Content Management ====================
    
    override suspend fun getRandomTest(testType: GTOTestType): Result<GTOTest> {
        return try {
            when (testType) {
                GTOTestType.GROUP_DISCUSSION -> {
                    val topic = testContentRepository.getRandomGDTopic().getOrThrow()
                    Result.success(
                        GTOTest.GDTest(
                            id = UUID.randomUUID().toString(),
                            topic = topic
                        )
                    )
                }
                
                GTOTestType.GROUP_PLANNING_EXERCISE -> {
                    getRandomGPEScenario()
                }
                
                GTOTestType.LECTURETTE -> {
                    // Lecturette topics are now fetched directly by TestContentRepository
                    // But for the Test object creation here, we might need them?
                    // Actually, getRandomLecturetteTopics(count) in TestContentRepository returns a list.
                    // Let's use it.
                    val topics = testContentRepository.getRandomLecturetteTopics(4).getOrThrow()
                    Result.success(
                        GTOTest.LecturetteTest(
                            id = UUID.randomUUID().toString(),
                            topicChoices = topics
                        )
                    )
                }
                
                else -> {
                    // For animation-based tests, get obstacles
                    val obstacles = getObstaclesForTest(testType).getOrThrow()
                    Result.success(createAnimationTest(testType, obstacles))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get random GTO test: $testType", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getTestById(testId: String): Result<GTOTest> {
        return try {
            // For now, return a random test (can be enhanced to fetch specific test)
            val testType = GTOTestType.GROUP_DISCUSSION // Default
            getRandomTest(testType)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get GTO test by ID: $testId", e)
            Result.failure(e)
        }
    }
    
    // getRandomGDTopic and getRandomLecturetteTopics moved to TestContentRepository

    override suspend fun getRandomGPEScenario(): Result<GTOTest.GPETest> {
        return try {
            val path = "$COLLECTION_TEST_CONTENT/$PATH_GTO/$PATH_SCENARIOS/gpe/$PATH_BATCHES/$DEFAULT_BATCH_ID"
            val doc = firestore.document(path).get().await()
            
            val scenarios = doc.get("scenarios") as? List<*>
            val scenarioObjects = scenarios?.filterIsInstance<Map<*, *>>() ?: emptyList()
            
            if (scenarioObjects.isEmpty()) {
                return Result.failure(Exception("No GPE scenarios available"))
            }
            
            val randomScenario = scenarioObjects.random()
            
            Result.success(
                GTOTest.GPETest(
                    id = randomScenario["id"] as? String ?: UUID.randomUUID().toString(),
                    imageUrl = randomScenario["imageUrl"] as? String ?: "",
                    scenario = randomScenario["scenario"] as? String ?: "",
                    resources = (randomScenario["resources"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    difficulty = randomScenario["difficulty"] as? String ?: "Medium"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get random GPE scenario", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getObstaclesForTest(testType: GTOTestType): Result<List<ObstacleConfig>> {
        return try {
            val testTypePath = when (testType) {
                GTOTestType.PROGRESSIVE_GROUP_TASK -> "pgt"
                GTOTestType.HALF_GROUP_TASK -> "hgt"
                GTOTestType.GROUP_OBSTACLE_RACE -> "gor"
                GTOTestType.INDIVIDUAL_OBSTACLES -> "io"
                GTOTestType.COMMAND_TASK -> "ct"
                else -> return Result.failure(Exception("Test type does not have obstacles"))
            }
            
            val path = "$COLLECTION_TEST_CONTENT/$PATH_GTO/$PATH_OBSTACLES/$testTypePath/obstacle_set_001"
            val doc = firestore.document(path).get().await()
            
            val obstacles = doc.get("obstacles") as? List<*>
            val obstacleObjects = obstacles?.filterIsInstance<Map<*, *>>() ?: emptyList()
            
            val obstacleConfigs = obstacleObjects.map { obstacle ->
                ObstacleConfig(
                    id = obstacle["id"] as? String ?: UUID.randomUUID().toString(),
                    name = obstacle["name"] as? String ?: "",
                    description = obstacle["description"] as? String ?: "",
                    difficulty = (obstacle["difficulty"] as? Long)?.toInt() ?: 1,
                    animationAsset = obstacle["animationAsset"] as? String ?: "",
                    resources = (obstacle["resources"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    height = (obstacle["height"] as? Number)?.toFloat(),
                    width = (obstacle["width"] as? Number)?.toFloat(),
                    depth = (obstacle["depth"] as? Number)?.toFloat()
                )
            }
            
            Result.success(obstacleConfigs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get obstacles for test: $testType", e)
            Result.failure(e)
        }
    }
    
    // ==================== Submission Management ====================
    
    // submitTest moved to SubmissionRepository
    
    override suspend fun getSubmission(submissionId: String): Result<GTOSubmission> {
        return try {
            val doc = firestore.collection(COLLECTION_GTO_SUBMISSIONS)
                .document(submissionId)
                .get()
                .await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Submission not found: $submissionId"))
            }
            
            val submission = mapToSubmission(doc.data ?: emptyMap())
            Result.success(submission)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get submission: $submissionId", e)
            Result.failure(e)
        }
    }
    
    override fun observeSubmission(submissionId: String): Flow<GTOSubmission?> = callbackFlow {
        val listener = firestore.collection(COLLECTION_GTO_SUBMISSIONS)
            .document(submissionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing submission: $submissionId", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot?.exists() == true) {
                    try {
                        val submission = mapToSubmission(snapshot.data ?: emptyMap())
                        trySend(submission)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping submission", e)
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun updateSubmissionStatus(
        submissionId: String,
        status: GTOSubmissionStatus
    ): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_GTO_SUBMISSIONS)
                .document(submissionId)
                .update(FIELD_STATUS, status.name)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update submission status", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateSubmissionOLQScores(
        submissionId: String,
        olqScores: Map<OLQ, OLQScore>
    ): Result<Unit> {
        return try {
            val scoresMap = olqScores.mapKeys { it.key.name }.mapValues { entry ->
                mapOf(
                    "score" to entry.value.score,
                    "confidence" to entry.value.confidence,
                    "reasoning" to entry.value.reasoning
                )
            }
            
            firestore.collection(COLLECTION_GTO_SUBMISSIONS)
                .document(submissionId)
                .update(
                    mapOf(
                        FIELD_OLQ_SCORES to scoresMap,
                        FIELD_STATUS to GTOSubmissionStatus.COMPLETED.name
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update submission OLQ scores", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getUserSubmissions(
        userId: String,
        testType: GTOTestType?
    ): Result<List<GTOSubmission>> {
        return try {
            var query: Query = firestore.collection(COLLECTION_GTO_SUBMISSIONS)
                .whereEqualTo(FIELD_USER_ID, userId)
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.DESCENDING)
            
            if (testType != null) {
                query = query.whereEqualTo(FIELD_TEST_TYPE, testType.name)
            }
            
            val snapshot = query.get().await()
            val submissions = snapshot.documents.mapNotNull { doc ->
                try {
                    mapToSubmission(doc.data ?: emptyMap())
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping submission: ${doc.id}", e)
                    null
                }
            }
            
            Result.success(submissions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user submissions", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getPendingSubmissions(limit: Int): Result<List<GTOSubmission>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_GTO_SUBMISSIONS)
                .whereEqualTo(FIELD_STATUS, GTOSubmissionStatus.PENDING_ANALYSIS.name)
                .orderBy(FIELD_SUBMITTED_AT, Query.Direction.ASCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val submissions = snapshot.documents.mapNotNull { doc ->
                try {
                    mapToSubmission(doc.data ?: emptyMap())
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping pending submission: ${doc.id}", e)
                    null
                }
            }
            
            Result.success(submissions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pending submissions", e)
            Result.failure(e)
        }
    }
    
    // ==================== Progress Tracking ====================
    
    override suspend fun getUserProgress(userId: String): Result<GTOProgress> {
        return try {
            val doc = firestore.collection(COLLECTION_USER_GTO_PROGRESS)
                .document(userId)
                .get()
                .await()
            
            if (!doc.exists()) {
                // Return empty progress for new users
                return Result.success(GTOProgress(userId = userId))
            }
            
            val progress = mapToProgress(userId, doc.data ?: emptyMap())
            Result.success(progress)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user progress", e)
            Result.failure(e)
        }
    }
    
    override fun observeUserProgress(userId: String): Flow<GTOProgress?> = callbackFlow {
        val listener = firestore.collection(COLLECTION_USER_GTO_PROGRESS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing user progress", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot?.exists() == true) {
                    try {
                        val progress = mapToProgress(userId, snapshot.data ?: emptyMap())
                        trySend(progress)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping progress", e)
                        trySend(null)
                    }
                } else {
                    trySend(GTOProgress(userId = userId))
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun updateProgress(
        userId: String,
        completedTestType: GTOTestType
    ): Result<Unit> {
        return try {
            val progressRef = firestore.collection(COLLECTION_USER_GTO_PROGRESS)
                .document(userId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(progressRef)
                val currentData = snapshot.data ?: emptyMap()
                
                val completedTests = (currentData[FIELD_COMPLETED_TESTS] as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.map { GTOTestType.valueOf(it) }
                    ?.toMutableList() ?: mutableListOf()
                
                if (completedTestType !in completedTests) {
                    completedTests.add(completedTestType)
                }
                
                val nextOrder = completedTests.maxOfOrNull { it.order }?.plus(1) ?: 1
                
                val updates = mapOf(
                    FIELD_COMPLETED_TESTS to completedTests.map { it.name },
                    "currentSequentialOrder" to nextOrder,
                    "lastCompletedAt" to System.currentTimeMillis()
                )
                
                transaction.set(progressRef, updates, com.google.firebase.firestore.SetOptions.merge())
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update progress", e)
            Result.failure(e)
        }
    }
    
    override suspend fun canUserTakeTest(
        userId: String,
        testType: GTOTestType
    ): Result<Boolean> {
        return try {
            val progress = getUserProgress(userId).getOrThrow()
            Result.success(progress.isTestUnlocked(testType))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check test access", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getCompletedTests(userId: String): Result<List<GTOTestType>> {
        return try {
            val progress = getUserProgress(userId).getOrThrow()
            Result.success(progress.completedTests)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get completed tests", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getNextAvailableTest(userId: String): Result<GTOTestType?> {
        return try {
            val progress = getUserProgress(userId).getOrThrow()
            Result.success(progress.getNextTest())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get next available test", e)
            Result.failure(e)
        }
    }
    
    // ==================== Test Usage Tracking ====================
    
    override suspend fun recordTestUsage(
        userId: String,
        testType: GTOTestType,
        submissionId: String
    ): Result<Unit> {
        return try {
            val progressRef = firestore.collection(COLLECTION_USER_GTO_PROGRESS)
                .document(userId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(progressRef)
                val currentData = snapshot.data ?: emptyMap()
                
                @Suppress("UNCHECKED_CAST")
                val testsUsed = (currentData[FIELD_TESTS_USED_THIS_MONTH] as? Map<String, Long>)
                    ?.toMutableMap() ?: mutableMapOf()
                
                val currentCount = testsUsed[testType.name]?.toInt() ?: 0
                testsUsed[testType.name] = (currentCount + 1).toLong()
                
                val updates = mapOf(
                    FIELD_TESTS_USED_THIS_MONTH to testsUsed
                )
                
                transaction.set(progressRef, updates, com.google.firebase.firestore.SetOptions.merge())
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record test usage", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getTestUsageCount(
        userId: String,
        testType: GTOTestType
    ): Result<Int> {
        return try {
            val progress = getUserProgress(userId).getOrThrow()
            val count = progress.testsUsedThisMonth[testType] ?: 0
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get test usage count", e)
            Result.failure(e)
        }
    }
    
    override suspend fun resetMonthlyUsage(userId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_USER_GTO_PROGRESS)
                .document(userId)
                .update(
                    mapOf(
                        FIELD_TESTS_USED_THIS_MONTH to emptyMap<String, Int>(),
                        "lastResetDate" to System.currentTimeMillis()
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset monthly usage", e)
            Result.failure(e)
        }
    }
    
    // ==================== Results & Analysis ====================
    
    override suspend fun getTestResult(submissionId: String): Result<GTOResult> {
        return try {
            val submission = getSubmission(submissionId).getOrThrow()
            
            if (submission.status != GTOSubmissionStatus.COMPLETED) {
                return Result.failure(Exception("Submission not yet analyzed"))
            }
            
            if (submission.olqScores.isEmpty()) {
                return Result.failure(Exception("No OLQ scores available"))
            }
            
            val overallScore = submission.olqScores.values.map { it.score }.average().toFloat()
            
            val result = GTOResult(
                submissionId = submissionId,
                userId = submission.userId,
                testType = submission.testType,
                olqScores = submission.olqScores,
                overallScore = overallScore,
                overallRating = calculateRating(overallScore),
                aiConfidence = submission.olqScores.values.map { it.confidence }.average().toInt()
            )
            
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get test result", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getUserResults(
        userId: String,
        testType: GTOTestType?
    ): Result<List<GTOResult>> {
        return try {
            val submissions = getUserSubmissions(userId, testType).getOrThrow()
            val results = submissions
                .filter { it.status == GTOSubmissionStatus.COMPLETED }
                .mapNotNull { submission ->
                    try {
                        getTestResult(submission.id).getOrNull()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting result for submission: ${submission.id}", e)
                        null
                    }
                }
            
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user results", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getAverageOLQScores(userId: String): Result<Map<OLQ, Float>> {
        return try {
            val results = getUserResults(userId).getOrThrow()
            
            if (results.isEmpty()) {
                return Result.success(emptyMap())
            }
            
            val olqAverages = OLQ.entries.associateWith { olq ->
                val scores = results.mapNotNull { it.olqScores[olq]?.score }
                if (scores.isNotEmpty()) scores.average().toFloat() else 0f
            }
            
            Result.success(olqAverages)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get average OLQ scores", e)
            Result.failure(e)
        }
    }
    
    // ==================== Cache Management ====================

    /**
     * Cache test content for offline access
     * Used for animation tests (PGT, HGT, GOR, IO, CT) to pre-download obstacle assets
     */
    override suspend fun cacheTestContent(testType: GTOTestType): Result<Unit> {
        return try {
            Log.d(TAG, "Caching content for: ${testType.displayName}")

            val result = when (testType) {
                GTOTestType.GROUP_DISCUSSION,
                GTOTestType.LECTURETTE -> {
                    // Text-based tests use GTOTaskCacheManager
                    taskCacheManager.initialSync()
                }
                GTOTestType.GROUP_PLANNING_EXERCISE -> {
                    // Image-based test uses GPEImageCacheManager (already implemented)
                    Result.success(Unit)
                }
                GTOTestType.PROGRESSIVE_GROUP_TASK,
                GTOTestType.HALF_GROUP_TASK,
                GTOTestType.GROUP_OBSTACLE_RACE,
                GTOTestType.INDIVIDUAL_OBSTACLES,
                GTOTestType.COMMAND_TASK -> {
                    // Animation tests - cache obstacles and animation assets
                    cacheAnimationAssets(testType)
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache content for ${testType.name}", e)
            Result.failure(e)
        }
    }

    /**
     * Clear cached content for specific test type or all tests
     */
    override suspend fun clearCache(testType: GTOTestType?): Result<Unit> {
        return try {
            Log.d(TAG, "Clearing cache for: ${testType?.displayName ?: "all tests"}")

            if (testType == null) {
                // Clear all GTO caches
                taskCacheManager.clearCache()
                Log.d(TAG, "✅ Cleared all GTO caches")
            } else {
                // Clear specific test cache
                when (testType) {
                    GTOTestType.GROUP_DISCUSSION,
                    GTOTestType.LECTURETTE -> {
                        // Clear all tasks (no type-specific clearing in current implementation)
                        taskCacheManager.clearCache()
                    }
                    GTOTestType.GROUP_PLANNING_EXERCISE -> {
                        // GPE has its own cache manager
                        Log.d(TAG, "GPE cache managed by GPEImageCacheManager")
                    }
                    else -> {
                        // Animation tests - clear obstacle cache
                        Log.d(TAG, "Animation asset cache clearing not yet implemented")
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
            Result.failure(e)
        }
    }

    /**
     * Check if content is cached and ready for offline use
     */
    override suspend fun isContentCached(testType: GTOTestType): Result<Boolean> {
        return try {
            val isCached = when (testType) {
                GTOTestType.GROUP_DISCUSSION,
                GTOTestType.LECTURETTE -> {
                    // Check if we have at least MIN_CACHE_SIZE tasks cached
                    val cacheStatus = taskCacheManager.getCacheStatus()
                    cacheStatus.cachedTasks >= 5
                }
                GTOTestType.GROUP_PLANNING_EXERCISE -> {
                    // GPE uses dedicated image cache
                    true // Managed by GPEImageCacheManager
                }
                GTOTestType.PROGRESSIVE_GROUP_TASK,
                GTOTestType.HALF_GROUP_TASK,
                GTOTestType.GROUP_OBSTACLE_RACE,
                GTOTestType.INDIVIDUAL_OBSTACLES,
                GTOTestType.COMMAND_TASK -> {
                    // Animation tests - check if obstacles are cached
                    false // Will be implemented in Phase 4
                }
            }

            Result.success(isCached)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check cache status", e)
            Result.failure(Exception("Failed to check cache status", e))
        }
    }

    /**
     * Cache animation assets for obstacle-based tests
     * Will be fully implemented in Phase 4 when animation tests are added
     */
    private suspend fun cacheAnimationAssets(testType: GTOTestType): Result<Unit> {
        return try {
            Log.d(TAG, "Animation asset caching placeholder for ${testType.displayName}")
            // Phase 4: Download Lottie animations, obstacle configurations, etc.
            // For now, return success to allow method to be called without errors
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Helper Functions ====================
    
    private fun createAnimationTest(testType: GTOTestType, obstacles: List<ObstacleConfig>): GTOTest {
        return when (testType) {
            GTOTestType.PROGRESSIVE_GROUP_TASK -> GTOTest.PGTTest(
                id = UUID.randomUUID().toString(),
                obstacles = obstacles
            )
            GTOTestType.HALF_GROUP_TASK -> GTOTest.HGTTest(
                id = UUID.randomUUID().toString(),
                obstacle = obstacles.firstOrNull() ?: ObstacleConfig(
                    id = "default",
                    name = "Default Obstacle",
                    description = "Default obstacle",
                    difficulty = 1,
                    animationAsset = ""
                )
            )
            GTOTestType.GROUP_OBSTACLE_RACE -> GTOTest.GORTest(
                id = UUID.randomUUID().toString(),
                obstacles = obstacles
            )
            GTOTestType.INDIVIDUAL_OBSTACLES -> GTOTest.IOTest(
                id = UUID.randomUUID().toString(),
                obstacles = obstacles
            )
            GTOTestType.COMMAND_TASK -> GTOTest.CTTest(
                id = UUID.randomUUID().toString(),
                scenario = "Default command scenario",
                obstacle = obstacles.firstOrNull() ?: ObstacleConfig(
                    id = "default",
                    name = "Default Obstacle",
                    description = "Default obstacle",
                    difficulty = 1,
                    animationAsset = ""
                )
            )
            else -> throw IllegalArgumentException("Invalid animation test type: $testType")
        }
    }
    
    // submissionToMap removed as it's no longer used within this repository for submission creation
    // mapToSubmission is used for simple retrieval but logic should eventually move to SubmissionRepository
    
    private fun mapToSubmission(data: Map<String, Any>): GTOSubmission {
        val testTypeStr = data[FIELD_TEST_TYPE] as? String 
            ?: throw IllegalArgumentException("Missing test type")
        
        // Map from TestType enum values to GTOTestType enum values
        val testType = when (testTypeStr) {
            "GTO_GD", "GROUP_DISCUSSION" -> GTOTestType.GROUP_DISCUSSION
            "GTO_LECTURETTE", "LECTURETTE" -> GTOTestType.LECTURETTE
            "GTO_GPE", "GROUP_PLANNING_EXERCISE" -> GTOTestType.GROUP_PLANNING_EXERCISE
            else -> throw IllegalArgumentException("Unknown GTO test type: $testTypeStr")
        }
        
        val id = data["id"] as? String ?: ""
        val userId = data[FIELD_USER_ID] as? String ?: ""
        val testId = data[FIELD_TEST_ID] as? String ?: ""
        val submittedAt = (data[FIELD_SUBMITTED_AT] as? Long) ?: System.currentTimeMillis()
        val timeSpent = ((data["timeSpent"] as? Number)?.toInt()) ?: 0
        val statusStr = data[FIELD_STATUS] as? String ?: GTOSubmissionStatus.PENDING_ANALYSIS.name
        val status = try {
            GTOSubmissionStatus.valueOf(statusStr)
        } catch (e: Exception) {
            GTOSubmissionStatus.PENDING_ANALYSIS
        }
        
        @Suppress("UNCHECKED_CAST")
        val olqScoresMap = (data[FIELD_OLQ_SCORES] as? Map<String, Map<String, Any>>) ?: emptyMap()
        val olqScores = olqScoresMap.mapKeys { OLQ.valueOf(it.key) }.mapValues { entry ->
            OLQScore(
                score = ((entry.value["score"] as? Number)?.toInt()) ?: 6,
                confidence = ((entry.value["confidence"] as? Number)?.toInt()) ?: 0,
                reasoning = entry.value["reasoning"] as? String ?: ""
            )
        }
        
        return when (testType) {
            GTOTestType.GROUP_DISCUSSION -> GTOSubmission.GDSubmission(
                id = id,
                userId = userId,
                testId = testId,
                topic = data["topic"] as? String ?: "",
                response = data["response"] as? String ?: "",
                wordCount = ((data["wordCount"] as? Number)?.toInt()) ?: 0,
                submittedAt = submittedAt,
                timeSpent = timeSpent,
                status = status,
                olqScores = olqScores
            )
            GTOTestType.GROUP_PLANNING_EXERCISE -> GTOSubmission.GPESubmission(
                id = id,
                userId = userId,
                testId = testId,
                imageUrl = data["imageUrl"] as? String ?: "",
                scenario = data["scenario"] as? String ?: "",
                plan = data["plan"] as? String ?: "",
                characterCount = ((data["characterCount"] as? Number)?.toInt()) ?: 0,
                submittedAt = submittedAt,
                timeSpent = timeSpent,
                status = status,
                olqScores = olqScores
            )
            GTOTestType.LECTURETTE -> GTOSubmission.LecturetteSubmission(
                id = id,
                userId = userId,
                testId = testId,
                topicChoices = (data["topicChoices"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                selectedTopic = data["selectedTopic"] as? String ?: "",
                speechTranscript = data["speechTranscript"] as? String ?: "",
                wordCount = ((data["wordCount"] as? Number)?.toInt()) ?: 0,
                submittedAt = submittedAt,
                timeSpent = timeSpent,
                status = status,
                olqScores = olqScores
            )
            GTOTestType.PROGRESSIVE_GROUP_TASK -> GTOSubmission.PGTSubmission(
                id = id,
                userId = userId,
                testId = testId,
                obstacles = parseObstacleConfigList(data["obstacles"]),
                solutions = parseObstacleSolutionList(data["solutions"]),
                submittedAt = submittedAt,
                timeSpent = timeSpent,
                status = status,
                olqScores = olqScores
            )
            GTOTestType.HALF_GROUP_TASK -> GTOSubmission.HGTSubmission(
                id = id,
                userId = userId,
                testId = testId,
                obstacle = parseObstacleConfig(data["obstacle"]),
                solution = parseObstacleSolution(data["solution"]),
                leadershipDecisions = data["leadershipDecisions"] as? String ?: "",
                submittedAt = submittedAt,
                timeSpent = timeSpent,
                status = status,
                olqScores = olqScores
            )
            GTOTestType.GROUP_OBSTACLE_RACE -> GTOSubmission.GORSubmission(
                id = id,
                userId = userId,
                testId = testId,
                obstacles = parseObstacleConfigList(data["obstacles"]),
                coordinationStrategy = data["coordinationStrategy"] as? String ?: "",
                submittedAt = submittedAt,
                timeSpent = timeSpent,
                status = status,
                olqScores = olqScores
            )
            GTOTestType.INDIVIDUAL_OBSTACLES -> GTOSubmission.IOSubmission(
                id = id,
                userId = userId,
                testId = testId,
                obstacles = parseObstacleConfigList(data["obstacles"]),
                approach = data["approach"] as? String ?: "",
                submittedAt = submittedAt,
                timeSpent = timeSpent,
                status = status,
                olqScores = olqScores
            )
            GTOTestType.COMMAND_TASK -> GTOSubmission.CTSubmission(
                id = id,
                userId = userId,
                testId = testId,
                scenario = data["scenario"] as? String ?: "",
                obstacle = parseObstacleConfig(data["obstacle"]),
                commandDecisions = data["commandDecisions"] as? String ?: "",
                resourceAllocation = data["resourceAllocation"] as? String ?: "",
                submittedAt = submittedAt,
                timeSpent = timeSpent,
                status = status,
                olqScores = olqScores
            )
        }
    }
    
    private fun mapToProgress(userId: String, data: Map<String, Any>): GTOProgress {
        val completedTestsStr = (data[FIELD_COMPLETED_TESTS] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val completedTests = completedTestsStr.mapNotNull { 
            try { GTOTestType.valueOf(it) } catch (e: Exception) { null }
        }
        
        @Suppress("UNCHECKED_CAST")
        val testsUsedMap = (data[FIELD_TESTS_USED_THIS_MONTH] as? Map<String, Long>) ?: emptyMap()
        val testsUsed = testsUsedMap.mapKeys { 
            try { GTOTestType.valueOf(it.key) } catch (e: Exception) { null }
        }.filterKeys { it != null }.mapKeys { it.key!! }.mapValues { it.value.toInt() }
        
        return GTOProgress(
            userId = userId,
            completedTests = completedTests,
            testsUsedThisMonth = testsUsed,
            lastResetDate = (data["lastResetDate"] as? Long) ?: System.currentTimeMillis(),
            currentSequentialOrder = ((data["currentSequentialOrder"] as? Number)?.toInt()) ?: 1,
            lastCompletedAt = data["lastCompletedAt"] as? Long
        )
    }
    
    private fun obstacleToMap(obstacle: ObstacleConfig): Map<String, Any?> {
        return mapOf(
            "id" to obstacle.id,
            "name" to obstacle.name,
            "description" to obstacle.description,
            "difficulty" to obstacle.difficulty,
            "animationAsset" to obstacle.animationAsset,
            "resources" to obstacle.resources,
            "height" to obstacle.height,
            "width" to obstacle.width,
            "depth" to obstacle.depth
        )
    }
    
    private fun solutionToMap(solution: ObstacleSolution): Map<String, Any?> {
        return mapOf(
            "obstacleId" to solution.obstacleId,
            "solutionText" to solution.solutionText,
            "resourcesUsed" to solution.resourcesUsed,
            "estimatedTime" to solution.estimatedTime
        )
    }
    
    private fun calculateRating(score: Float): String {
        return when {
            score <= 3f -> "Exceptional"
            score <= 4f -> "Excellent"
            score <= 5f -> "Very Good"
            score <= 6f -> "Good"
            score <= 7f -> "Average"
            score <= 8f -> "Below Average"
            else -> "Poor"
        }
    }
    
    /**
     * Parse single ObstacleConfig from Firestore map
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseObstacleConfig(data: Any?): ObstacleConfig {
        val map = data as? Map<String, Any> ?: return ObstacleConfig(
            id = "",
            name = "",
            description = "",
            difficulty = 1,
            animationAsset = ""
        )
        
        return ObstacleConfig(
            id = map["id"] as? String ?: "",
            name = map["name"] as? String ?: "",
            description = map["description"] as? String ?: "",
            difficulty = ((map["difficulty"] as? Number)?.toInt()) ?: 1,
            animationAsset = map["animationAsset"] as? String ?: "",
            resources = (map["resources"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            height = (map["height"] as? Number)?.toFloat(),
            width = (map["width"] as? Number)?.toFloat(),
            depth = (map["depth"] as? Number)?.toFloat()
        )
    }
    
    /**
     * Parse list of ObstacleConfig from Firestore list
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseObstacleConfigList(data: Any?): List<ObstacleConfig> {
        val list = data as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            try {
                parseObstacleConfig(item)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse ObstacleConfig", e)
                null
            }
        }
    }
    
    /**
     * Parse single ObstacleSolution from Firestore map
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseObstacleSolution(data: Any?): ObstacleSolution {
        val map = data as? Map<String, Any> ?: return ObstacleSolution(
            obstacleId = "",
            solutionText = ""
        )
        
        return ObstacleSolution(
            obstacleId = map["obstacleId"] as? String ?: "",
            solutionText = map["solutionText"] as? String ?: "",
            resourcesUsed = (map["resourcesUsed"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            estimatedTime = (map["estimatedTime"] as? Number)?.toInt()
        )
    }
    
    /**
     * Parse list of ObstacleSolution from Firestore list
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseObstacleSolutionList(data: Any?): List<ObstacleSolution> {
        val list = data as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            try {
                parseObstacleSolution(item)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse ObstacleSolution", e)
                null
            }
        }
    }
}
