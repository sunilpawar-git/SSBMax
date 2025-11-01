package com.ssbmax.core.data.local.dao

import androidx.room.*
import com.ssbmax.core.data.local.entity.CachedInterviewQuestionEntity
import com.ssbmax.core.data.local.entity.InterviewBatchMetadataEntity

/**
 * DAO for managing cached Interview questions
 * Follows the same pattern as other test types
 */
@Dao
interface InterviewQuestionCacheDao {
    
    // ============ Question Queries ============
    
    /**
     * Get all cached questions
     */
    @Query("SELECT * FROM cached_interview_questions ORDER BY id ASC")
    suspend fun getAllQuestions(): List<CachedInterviewQuestionEntity>
    
    /**
     * Get questions from specific batch
     */
    @Query("SELECT * FROM cached_interview_questions WHERE batchId = :batchId ORDER BY id ASC")
    suspend fun getQuestionsByBatch(batchId: String): List<CachedInterviewQuestionEntity>
    
    /**
     * Get questions by category
     */
    @Query("SELECT * FROM cached_interview_questions WHERE category = :category ORDER BY id ASC")
    suspend fun getQuestionsByCategory(category: String): List<CachedInterviewQuestionEntity>
    
    /**
     * Get random selection of questions
     * Prioritizes less-used questions
     */
    @Query("""
        SELECT * FROM cached_interview_questions 
        ORDER BY usageCount ASC, RANDOM() 
        LIMIT :count
    """)
    suspend fun getRandomQuestions(count: Int): List<CachedInterviewQuestionEntity>
    
    /**
     * Get questions by difficulty
     */
    @Query("SELECT * FROM cached_interview_questions WHERE difficulty = :difficulty ORDER BY RANDOM() LIMIT :count")
    suspend fun getQuestionsByDifficulty(difficulty: String, count: Int): List<CachedInterviewQuestionEntity>
    
    /**
     * Get least used questions
     */
    @Query("SELECT * FROM cached_interview_questions ORDER BY usageCount ASC, lastUsed ASC LIMIT :count")
    suspend fun getLeastUsedQuestions(count: Int): List<CachedInterviewQuestionEntity>
    
    /**
     * Get balanced questions across categories
     */
    @Query("""
        SELECT * FROM cached_interview_questions 
        WHERE category = :category 
        ORDER BY usageCount ASC, RANDOM() 
        LIMIT :count
    """)
    suspend fun getBalancedByCategory(category: String, count: Int): List<CachedInterviewQuestionEntity>
    
    /**
     * Mark questions as used (increment usage count and update last used timestamp)
     */
    @Query("""
        UPDATE cached_interview_questions 
        SET usageCount = usageCount + 1, lastUsed = :timestamp 
        WHERE id IN (:questionIds)
    """)
    suspend fun markQuestionsAsUsed(questionIds: List<String>, timestamp: Long = System.currentTimeMillis())
    
    // ============ Insert/Update/Delete ============
    
    /**
     * Insert a single question
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: CachedInterviewQuestionEntity)
    
    /**
     * Insert multiple questions (batch)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<CachedInterviewQuestionEntity>)
    
    /**
     * Delete all questions
     */
    @Query("DELETE FROM cached_interview_questions")
    suspend fun clearAllQuestions()
    
    /**
     * Delete questions from specific batch
     */
    @Query("DELETE FROM cached_interview_questions WHERE batchId = :batchId")
    suspend fun deleteQuestionsByBatch(batchId: String)
    
    // ============ Statistics ============
    
    /**
     * Get total number of cached questions
     */
    @Query("SELECT COUNT(*) FROM cached_interview_questions")
    suspend fun getTotalQuestionCount(): Int
    
    /**
     * Get question count by category
     */
    @Query("SELECT COUNT(*) FROM cached_interview_questions WHERE category = :category")
    suspend fun getQuestionCountByCategory(category: String): Int
    
    /**
     * Get question count by difficulty
     */
    @Query("SELECT COUNT(*) FROM cached_interview_questions WHERE difficulty = :difficulty")
    suspend fun getQuestionCountByDifficulty(difficulty: String): Int
    
    /**
     * Get all categories (for diagnostics)
     */
    @Query("SELECT DISTINCT category FROM cached_interview_questions")
    suspend fun getAllCategories(): List<String>
    
    // ============ Batch Metadata ============
    
    /**
     * Insert batch metadata
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchMetadata(metadata: InterviewBatchMetadataEntity)
    
    /**
     * Get all batch metadata
     */
    @Query("SELECT * FROM interview_batch_metadata ORDER BY downloadedAt DESC")
    suspend fun getAllBatchMetadata(): List<InterviewBatchMetadataEntity>
    
    /**
     * Get specific batch metadata
     */
    @Query("SELECT * FROM interview_batch_metadata WHERE batchId = :batchId")
    suspend fun getBatchMetadata(batchId: String): InterviewBatchMetadataEntity?
    
    /**
     * Delete batch metadata
     */
    @Query("DELETE FROM interview_batch_metadata WHERE batchId = :batchId")
    suspend fun deleteBatchMetadata(batchId: String)
    
    /**
     * Get total number of batches
     */
    @Query("SELECT COUNT(*) FROM interview_batch_metadata")
    suspend fun getTotalBatchCount(): Int
}

