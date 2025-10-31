package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity
import com.ssbmax.core.domain.model.CacheStatus
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.core.domain.model.OIRQuestionType
import com.ssbmax.core.domain.model.QuestionDifficulty
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local caching of OIR questions from Firestore
 * 
 * Key Features:
 * - Progressive caching (download batches as needed)
 * - Smart question selection (maintains distribution, avoids repetition)
 * - Usage tracking
 * - Cache management (cleanup, rotation)
 * 
 * Caching Strategy:
 * - Initial: Download batch_001 (100 questions) immediately
 * - Background: Download more batches when idle
 * - Selection: Pick 50 questions maintaining 40/40/15/5 distribution
 * - Rotation: Prefer unused or least-recently-used questions
 */
@Singleton
class OIRQuestionCacheManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cacheDao: OIRQuestionCacheDao,
    private val gson: Gson
) {
    
    companion object {
        private const val TAG = "OIRCacheManager"
        private const val FIRESTORE_COLLECTION = "test_content"
        private const val FIRESTORE_OIR_DOC = "oir"
        private const val FIRESTORE_BATCHES = "question_batches"
        
        // Distribution ratios for test generation
        private const val VERBAL_RATIO = 0.40f  // 40%
        private const val NON_VERBAL_RATIO = 0.40f  // 40%
        private const val NUMERICAL_RATIO = 0.15f  // 15%
        private const val SPATIAL_RATIO = 0.05f  // 5%
        
        // Cache management
        private const val UNUSED_THRESHOLD_DAYS = 7 // Questions unused for 7 days are "fresh"
        private const val MAX_CACHE_QUESTIONS = 300 // Maximum questions to keep cached
    }
    
    /**
     * Initial sync: Download first batch of questions
     * Called on first app launch or when cache is empty
     */
    suspend fun initialSync(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting initial sync...")
            
            // Check if we already have questions cached
            val cachedCount = cacheDao.getCachedQuestionCount()
            if (cachedCount >= 50) {
                Log.d(TAG, "Cache already has $cachedCount questions, skipping initial sync")
                return Result.success(Unit)
            }
            
            // Download batch_001
            downloadBatch("batch_001").getOrThrow()
            
            val newCount = cacheDao.getCachedQuestionCount()
            Log.d(TAG, "Initial sync complete: $newCount questions cached")
            
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
            
            // Check if already downloaded
            if (cacheDao.isBatchDownloaded(batchId)) {
                Log.d(TAG, "Batch $batchId already downloaded")
                return Result.success(Unit)
            }
            
            // Fetch from Firestore
            val batchDoc = firestore.collection(FIRESTORE_COLLECTION)
                .document(FIRESTORE_OIR_DOC)
                .collection(FIRESTORE_BATCHES)
                .document(batchId)
                .get()
                .await()
            
            if (!batchDoc.exists()) {
                throw Exception("Batch $batchId not found in Firestore")
            }
            
            val data = batchDoc.data ?: throw Exception("Batch data is null")
            val questions = data["questions"] as? List<Map<String, Any>> 
                ?: throw Exception("Questions field missing or invalid")
            
            val questionCount = data["question_count"] as? Long ?: questions.size.toLong()
            val version = data["version"] as? String ?: "1.0.0"
            
            // Convert to entities
            val entities = questions.mapIndexed { index, questionMap ->
                convertToEntity(questionMap, batchId, index)
            }
            
            // Save to database
            val timestamp = System.currentTimeMillis()
            cacheDao.insertQuestions(entities)
            cacheDao.insertBatchMetadata(
                OIRBatchMetadataEntity(
                    batchId = batchId,
                    downloadedAt = timestamp,
                    questionCount = questionCount.toInt(),
                    version = version
                )
            )
            
            Log.d(TAG, "Downloaded and cached $batchId: ${entities.size} questions")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download batch $batchId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get questions for a test (50 questions with proper distribution)
     * 
     * Distribution:
     * - Verbal: 20 questions (40%)
     * - Non-Verbal: 20 questions (40%)
     * - Numerical: 8 questions (15%)
     * - Spatial: 2 questions (5%)
     */
    suspend fun getTestQuestions(count: Int = 50): Result<List<OIRQuestion>> {
        return try {
            Log.d(TAG, "Generating test with $count questions")
            
            // Ensure we have enough questions cached
            val cachedCount = cacheDao.getCachedQuestionCount()
            if (cachedCount < count) {
                Log.w(TAG, "Not enough questions cached ($cachedCount < $count), triggering sync")
                initialSync()
            }
            
            // Calculate distribution
            val verbalCount = (count * VERBAL_RATIO).toInt()
            val nonVerbalCount = (count * NON_VERBAL_RATIO).toInt()
            val numericalCount = (count * NUMERICAL_RATIO).toInt()
            val spatialCount = count - verbalCount - nonVerbalCount - numericalCount // Remainder
            
            Log.d(TAG, "Distribution: V=$verbalCount, NV=$nonVerbalCount, N=$numericalCount, S=$spatialCount")
            
            // Timestamp for "unused" threshold (7 days ago)
            val unusedThreshold = System.currentTimeMillis() - (UNUSED_THRESHOLD_DAYS * 24 * 60 * 60 * 1000L)
            
            // Fetch questions by type
            val verbalQuestions = getQuestionsByType(OIRQuestionType.VERBAL_REASONING.name, unusedThreshold, verbalCount)
            val nonVerbalQuestions = getQuestionsByType(OIRQuestionType.NON_VERBAL_REASONING.name, unusedThreshold, nonVerbalCount)
            val numericalQuestions = getQuestionsByType(OIRQuestionType.NUMERICAL_ABILITY.name, unusedThreshold, numericalCount)
            val spatialQuestions = getQuestionsByType(OIRQuestionType.SPATIAL_REASONING.name, unusedThreshold, spatialCount)
            
            // Combine and shuffle
            val allQuestions = (verbalQuestions + nonVerbalQuestions + numericalQuestions + spatialQuestions).shuffled()
            
            Log.d(TAG, "Selected ${allQuestions.size} questions")
            
            // Convert to domain models
            val domainQuestions = allQuestions.map { convertToDomain(it) }
            
            Result.success(domainQuestions)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get test questions", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get questions of a specific type, preferring unused ones
     */
    private suspend fun getQuestionsByType(
        type: String, 
        unusedThreshold: Long, 
        count: Int
    ): List<CachedOIRQuestionEntity> {
        // Try to get unused questions first
        var questions = cacheDao.getUnusedQuestionsByType(type, unusedThreshold, count)
        
        // If not enough unused questions, get any questions of this type
        if (questions.size < count) {
            Log.d(TAG, "Not enough unused $type questions (${questions.size}/$count), fetching any")
            questions = cacheDao.getQuestionsByType(type, count)
        }
        
        return questions.take(count)
    }
    
    /**
     * Mark questions as used after a test
     */
    suspend fun markQuestionsUsed(questionIds: List<String>) {
        try {
            val timestamp = System.currentTimeMillis()
            cacheDao.markQuestionsUsed(questionIds, timestamp)
            Log.d(TAG, "Marked ${questionIds.size} questions as used")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark questions as used", e)
        }
    }
    
    /**
     * Get cache status for monitoring
     */
    suspend fun getCacheStatus(): CacheStatus {
        return try {
            val questionCount = cacheDao.getCachedQuestionCount()
            val batches = cacheDao.getAllBatchMetadata()
            val verbalCount = cacheDao.getQuestionCountByType(OIRQuestionType.VERBAL_REASONING.name)
            val nonVerbalCount = cacheDao.getQuestionCountByType(OIRQuestionType.NON_VERBAL_REASONING.name)
            val numericalCount = cacheDao.getQuestionCountByType(OIRQuestionType.NUMERICAL_ABILITY.name)
            val spatialCount = cacheDao.getQuestionCountByType(OIRQuestionType.SPATIAL_REASONING.name)
            
            CacheStatus(
                cachedQuestions = questionCount,
                batchesDownloaded = batches.size,
                lastSyncTime = batches.maxOfOrNull { it.downloadedAt },
                verbalCount = verbalCount,
                nonVerbalCount = nonVerbalCount,
                numericalCount = numericalCount,
                spatialCount = spatialCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache status", e)
            CacheStatus(0, 0, null, 0, 0, 0, 0)
        }
    }
    
    /**
     * Clear all cached questions and metadata
     */
    suspend fun clearCache(): Result<Unit> {
        return try {
            Log.d(TAG, "Clearing cache...")
            cacheDao.deleteAllQuestions()
            cacheDao.deleteAllBatchMetadata()
            Log.d(TAG, "Cache cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
            Result.failure(e)
        }
    }
    
    /**
     * Convert Firestore map to entity
     */
    private fun convertToEntity(
        questionMap: Map<String, Any>,
        batchId: String,
        index: Int
    ): CachedOIRQuestionEntity {
        return CachedOIRQuestionEntity(
            id = questionMap["id"] as? String ?: "oir_q_${System.currentTimeMillis()}_$index",
            questionNumber = (questionMap["questionNumber"] as? Long)?.toInt() ?: index + 1,
            type = questionMap["type"] as? String ?: "VERBAL_REASONING",
            subtype = questionMap["subtype"] as? String,
            questionText = questionMap["questionText"] as? String ?: "",
            optionsJson = gson.toJson(questionMap["options"]),
            correctAnswerId = questionMap["correctAnswerId"] as? String ?: "",
            explanation = questionMap["explanation"] as? String ?: "",
            difficulty = questionMap["difficulty"] as? String ?: "MEDIUM",
            tags = (questionMap["tags"] as? List<String>)?.joinToString(",") ?: "",
            batchId = batchId,
            cachedAt = System.currentTimeMillis(),
            lastUsed = null,
            usageCount = 0
        )
    }
    
    /**
     * Convert entity to domain model
     */
    private fun convertToDomain(entity: CachedOIRQuestionEntity): OIRQuestion {
        val optionsArrayType = object : com.google.gson.reflect.TypeToken<List<Map<String, String>>>() {}.type
        val optionsList = gson.fromJson<List<Map<String, String>>>(entity.optionsJson, optionsArrayType)
        val optionsArray = optionsList
        val options = optionsArray.map { optionMap ->
            com.ssbmax.core.domain.model.OIROption(
                id = optionMap["id"] ?: "",
                text = optionMap["text"] ?: "",
                imageUrl = optionMap["imageUrl"]
            )
        }
        
        return OIRQuestion(
            id = entity.id,
            questionNumber = entity.questionNumber,
            type = OIRQuestionType.valueOf(entity.type),
            questionText = entity.questionText,
            options = options,
            correctAnswerId = entity.correctAnswerId,
            explanation = entity.explanation,
            difficulty = QuestionDifficulty.valueOf(entity.difficulty),
            timeSeconds = 60
        )
    }
}

