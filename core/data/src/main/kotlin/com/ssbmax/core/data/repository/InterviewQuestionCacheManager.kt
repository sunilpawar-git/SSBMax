package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.InterviewQuestionCacheDao
import com.ssbmax.core.data.local.entity.CachedInterviewQuestionEntity
import com.ssbmax.core.data.local.entity.InterviewBatchMetadataEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for Interview question progressive caching
 * Follows the same architecture as other test types
 */
@Singleton
class InterviewQuestionCacheManager @Inject constructor(
    private val dao: InterviewQuestionCacheDao,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "InterviewCacheManager"
        private const val COLLECTION_PATH = "test_content/interview/question_batches"
        private const val METADATA_PATH = "test_content/interview/meta"
        private const val TARGET_CACHE_SIZE = 50 // Comprehensive interview prep
        private const val MIN_CACHE_SIZE = 15 // Minimum before resyncing
    }
    
    /**
     * Initialize cache with first batch
     * Called when app starts or cache is empty
     */
    suspend fun initialSync(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting initial sync...")
            
            val currentCount = dao.getTotalQuestionCount()
            if (currentCount >= TARGET_CACHE_SIZE) {
                Log.d(TAG, "Cache already initialized ($currentCount questions)")
                return Result.success(Unit)
            }
            
            // Download first batch
            downloadBatch("batch_001").getOrThrow()
            
            Log.d(TAG, "Initial sync complete")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Initial sync failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download a specific batch from Firestore
     */
    suspend fun downloadBatch(batchId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Downloading batch: $batchId")
            
            val doc = firestore.document("$COLLECTION_PATH/$batchId").get().await()
            
            if (!doc.exists()) {
                throw Exception("Batch $batchId not found in Firestore")
            }
            
            @Suppress("UNCHECKED_CAST")
            val questionsData = doc.get("questions") as? List<Map<String, Any?>> 
                ?: throw Exception("No questions found in batch $batchId")
            
            val version = doc.getString("version") ?: "1.0.0"
            
            val questions = questionsData.mapNotNull { questionMap ->
                try {
                    CachedInterviewQuestionEntity(
                        id = questionMap["id"] as? String ?: return@mapNotNull null,
                        question = questionMap["question"] as? String ?: return@mapNotNull null,
                        category = questionMap["category"] as? String ?: "general",
                        difficulty = questionMap["difficulty"] as? String,
                        suggestedAnswer = questionMap["suggestedAnswer"] as? String,
                        keyPoints = questionMap["keyPoints"] as? String,
                        commonMistakes = questionMap["commonMistakes"] as? String,
                        followUpQuestions = questionMap["followUpQuestions"] as? String,
                        batchId = batchId,
                        cachedAt = System.currentTimeMillis(),
                        lastUsed = null,
                        usageCount = 0
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse question: ${questionMap["id"]}", e)
                    null
                }
            }
            
            if (questions.isEmpty()) {
                throw Exception("No valid questions parsed from batch $batchId")
            }
            
            // Insert questions and metadata
            dao.insertQuestions(questions)
            dao.insertBatchMetadata(
                InterviewBatchMetadataEntity(
                    batchId = batchId,
                    downloadedAt = System.currentTimeMillis(),
                    questionCount = questions.size,
                    version = version
                )
            )
            
            Log.d(TAG, "Downloaded batch $batchId: ${questions.size} questions")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download batch $batchId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get questions for interview practice (balanced across categories)
     */
    suspend fun getQuestionsForPractice(count: Int = 10): Result<List<InterviewQuestion>> {
        return try {
            Log.d(TAG, "Getting $count questions for practice")
            
            // Check if cache needs refresh
            val currentCount = dao.getTotalQuestionCount()
            if (currentCount < MIN_CACHE_SIZE) {
                Log.w(TAG, "Cache below minimum ($currentCount < $MIN_CACHE_SIZE), syncing...")
                initialSync().getOrThrow()
            }
            
            // Get least-used questions to ensure variety
            val cachedQuestions = dao.getLeastUsedQuestions(count)
            
            if (cachedQuestions.isEmpty()) {
                throw Exception("No questions in cache")
            }
            
            // Mark as used
            dao.markQuestionsAsUsed(cachedQuestions.map { it.id })
            
            // Convert to domain model
            val questions = cachedQuestions.map { entity ->
                InterviewQuestion(
                    id = entity.id,
                    question = entity.question,
                    category = entity.category,
                    difficulty = entity.difficulty,
                    suggestedAnswer = entity.suggestedAnswer
                )
            }
            
            Log.d(TAG, "Retrieved ${questions.size} questions for practice")
            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get questions for practice", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get questions by specific category
     */
    suspend fun getQuestionsByCategory(category: String, count: Int = 5): Result<List<InterviewQuestion>> {
        return try {
            Log.d(TAG, "Getting $count questions for category: $category")
            
            // Check if cache needs refresh
            val currentCount = dao.getTotalQuestionCount()
            if (currentCount < MIN_CACHE_SIZE) {
                Log.w(TAG, "Cache below minimum ($currentCount < $MIN_CACHE_SIZE), syncing...")
                initialSync().getOrThrow()
            }
            
            // Get least-used questions of this category
            val cachedQuestions = dao.getBalancedByCategory(category, count)
            
            if (cachedQuestions.isEmpty()) {
                throw Exception("No questions of category $category in cache")
            }
            
            // Mark as used
            dao.markQuestionsAsUsed(cachedQuestions.map { it.id })
            
            // Convert to domain model
            val questions = cachedQuestions.map { entity ->
                InterviewQuestion(
                    id = entity.id,
                    question = entity.question,
                    category = entity.category,
                    difficulty = entity.difficulty,
                    suggestedAnswer = entity.suggestedAnswer
                )
            }
            
            Log.d(TAG, "Retrieved ${questions.size} questions for category $category")
            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get questions for category $category", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get cache status for diagnostics
     */
    suspend fun getCacheStatus(): InterviewCacheStatus {
        return try {
            val totalQuestions = dao.getTotalQuestionCount()
            val batches = dao.getTotalBatchCount()
            val batchMetadata = dao.getAllBatchMetadata()
            val lastSyncTime = batchMetadata.maxOfOrNull { it.downloadedAt }
            
            // Get category distribution
            val personalCount = try { dao.getQuestionCountByCategory("personal") } catch (e: Exception) { 0 }
            val educationalCount = try { dao.getQuestionCountByCategory("educational") } catch (e: Exception) { 0 }
            val currentAffairsCount = try { dao.getQuestionCountByCategory("current_affairs") } catch (e: Exception) { 0 }
            val leadershipCount = try { dao.getQuestionCountByCategory("leadership") } catch (e: Exception) { 0 }
            
            InterviewCacheStatus(
                cachedQuestions = totalQuestions,
                batchesDownloaded = batches,
                lastSyncTime = lastSyncTime,
                personalCount = personalCount,
                educationalCount = educationalCount,
                currentAffairsCount = currentAffairsCount,
                leadershipCount = leadershipCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache status", e)
            InterviewCacheStatus()
        }
    }
    
    /**
     * Clear all cache (for debugging/testing)
     */
    suspend fun clearCache() {
        try {
            Log.d(TAG, "Clearing cache...")
            dao.clearAllQuestions()
            Log.d(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }
}

/**
 * Interview cache status for diagnostics
 */
data class InterviewCacheStatus(
    val cachedQuestions: Int = 0,
    val batchesDownloaded: Int = 0,
    val lastSyncTime: Long? = null,
    val personalCount: Int = 0,
    val educationalCount: Int = 0,
    val currentAffairsCount: Int = 0,
    val leadershipCount: Int = 0
)

/**
 * Interview Question domain model (lightweight version)
 */
data class InterviewQuestion(
    val id: String,
    val question: String,
    val category: String,
    val difficulty: String?,
    val suggestedAnswer: String?
)

