package com.ssbmax.core.data.local.dao

import androidx.room.*
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity

/**
 * Data Access Object for OIR question caching
 * 
 * Provides efficient queries for:
 * - Fetching unused questions
 * - Tracking usage
 * - Managing batches
 * - Cache cleanup
 */
@Dao
interface OIRQuestionCacheDao {
    
    // ============ Question Queries ============
    
    /**
     * Get questions from specific batches
     */
    @Query("SELECT * FROM cached_oir_questions WHERE batchId IN (:batchIds)")
    suspend fun getQuestionsByBatches(batchIds: List<String>): List<CachedOIRQuestionEntity>
    
    /**
     * Get unused questions of a specific type
     * Prioritizes questions not used recently
     * 
     * @param type Question type (VERBAL_REASONING, etc.)
     * @param olderThan Timestamp - questions used before this are considered "unused"
     * @param count Number of questions to fetch
     */
    @Query("""
        SELECT * FROM cached_oir_questions 
        WHERE type = :type 
        AND (lastUsed IS NULL OR lastUsed < :olderThan) 
        ORDER BY RANDOM() 
        LIMIT :count
    """)
    suspend fun getUnusedQuestionsByType(
        type: String, 
        olderThan: Long, 
        count: Int
    ): List<CachedOIRQuestionEntity>
    
    /**
     * Get all questions of a specific type (for fallback)
     */
    @Query("SELECT * FROM cached_oir_questions WHERE type = :type ORDER BY RANDOM() LIMIT :count")
    suspend fun getQuestionsByType(type: String, count: Int): List<CachedOIRQuestionEntity>
    
    /**
     * Get a specific question by ID
     */
    @Query("SELECT * FROM cached_oir_questions WHERE id = :questionId")
    suspend fun getQuestionById(questionId: String): CachedOIRQuestionEntity?
    
    /**
     * Get all cached questions (for debugging/admin)
     */
    @Query("SELECT * FROM cached_oir_questions")
    suspend fun getAllQuestions(): List<CachedOIRQuestionEntity>
    
    /**
     * Count total cached questions
     */
    @Query("SELECT COUNT(*) FROM cached_oir_questions")
    suspend fun getCachedQuestionCount(): Int
    
    /**
     * Count questions by type
     */
    @Query("SELECT COUNT(*) FROM cached_oir_questions WHERE type = :type")
    suspend fun getQuestionCountByType(type: String): Int
    
    // ============ Question Mutations ============
    
    /**
     * Insert questions (replace on conflict)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<CachedOIRQuestionEntity>)
    
    /**
     * Insert single question
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: CachedOIRQuestionEntity)
    
    /**
     * Mark questions as used
     * Updates lastUsed timestamp and increments usage count
     */
    @Query("""
        UPDATE cached_oir_questions 
        SET lastUsed = :timestamp, usageCount = usageCount + 1 
        WHERE id IN (:questionIds)
    """)
    suspend fun markQuestionsUsed(questionIds: List<String>, timestamp: Long)
    
    /**
     * Delete questions from a specific batch
     */
    @Query("DELETE FROM cached_oir_questions WHERE batchId = :batchId")
    suspend fun deleteBatch(batchId: String)
    
    /**
     * Delete all questions (for cache reset)
     */
    @Query("DELETE FROM cached_oir_questions")
    suspend fun deleteAllQuestions()
    
    /**
     * Delete oldest questions if cache exceeds limit
     */
    @Query("""
        DELETE FROM cached_oir_questions 
        WHERE id IN (
            SELECT id FROM cached_oir_questions 
            ORDER BY cachedAt ASC 
            LIMIT :count
        )
    """)
    suspend fun deleteOldestQuestions(count: Int)
    
    // ============ Batch Metadata Queries ============
    
    /**
     * Get all batch metadata
     */
    @Query("SELECT * FROM oir_batch_metadata ORDER BY downloadedAt DESC")
    suspend fun getAllBatchMetadata(): List<OIRBatchMetadataEntity>
    
    /**
     * Get specific batch metadata
     */
    @Query("SELECT * FROM oir_batch_metadata WHERE batchId = :batchId")
    suspend fun getBatchMetadata(batchId: String): OIRBatchMetadataEntity?
    
    /**
     * Check if batch exists locally
     */
    @Query("SELECT COUNT(*) > 0 FROM oir_batch_metadata WHERE batchId = :batchId")
    suspend fun isBatchDownloaded(batchId: String): Boolean
    
    /**
     * Count total batches
     */
    @Query("SELECT COUNT(*) FROM oir_batch_metadata")
    suspend fun getBatchCount(): Int
    
    // ============ Batch Metadata Mutations ============
    
    /**
     * Insert batch metadata
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchMetadata(metadata: OIRBatchMetadataEntity)
    
    /**
     * Delete batch metadata
     */
    @Query("DELETE FROM oir_batch_metadata WHERE batchId = :batchId")
    suspend fun deleteBatchMetadata(batchId: String)
    
    /**
     * Delete all batch metadata
     */
    @Query("DELETE FROM oir_batch_metadata")
    suspend fun deleteAllBatchMetadata()
    
    // ============ Analytics Queries ============
    
    /**
     * Get question usage statistics
     */
    @Query("""
        SELECT 
            type,
            COUNT(*) as total,
            AVG(usageCount) as avg_usage,
            SUM(CASE WHEN lastUsed IS NULL THEN 1 ELSE 0 END) as unused_count
        FROM cached_oir_questions 
        GROUP BY type
    """)
    suspend fun getUsageStatistics(): List<QuestionUsageStats>
    
    /**
     * Get most used questions
     */
    @Query("""
        SELECT * FROM cached_oir_questions 
        ORDER BY usageCount DESC 
        LIMIT :limit
    """)
    suspend fun getMostUsedQuestions(limit: Int = 10): List<CachedOIRQuestionEntity>
    
    /**
     * Get least used questions
     */
    @Query("""
        SELECT * FROM cached_oir_questions 
        WHERE lastUsed IS NOT NULL
        ORDER BY usageCount ASC 
        LIMIT :limit
    """)
    suspend fun getLeastUsedQuestions(limit: Int = 10): List<CachedOIRQuestionEntity>
}

/**
 * Data class for usage statistics
 */
data class QuestionUsageStats(
    val type: String,
    val total: Int,
    val avg_usage: Double,
    val unused_count: Int
)

