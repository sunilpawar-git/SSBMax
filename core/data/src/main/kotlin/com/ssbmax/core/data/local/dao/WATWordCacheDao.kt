package com.ssbmax.core.data.local.dao

import androidx.room.*
import com.ssbmax.core.data.local.entity.CachedWATWordEntity
import com.ssbmax.core.data.local.entity.WATBatchMetadataEntity

/**
 * DAO for managing cached WAT words
 * Follows the same pattern as OIRQuestionCacheDao
 */
@Dao
interface WATWordCacheDao {
    
    // ============ Word Queries ============
    
    /**
     * Get all cached words
     */
    @Query("SELECT * FROM cached_wat_words ORDER BY sequenceNumber ASC")
    suspend fun getAllWords(): List<CachedWATWordEntity>
    
    /**
     * Get words from specific batch
     */
    @Query("SELECT * FROM cached_wat_words WHERE batchId = :batchId ORDER BY sequenceNumber ASC")
    suspend fun getWordsByBatch(batchId: String): List<CachedWATWordEntity>
    
    /**
     * Get random selection of words (for a test)
     * Prioritizes less-used words
     */
    @Query("""
        SELECT * FROM cached_wat_words 
        ORDER BY usageCount ASC, RANDOM() 
        LIMIT :count
    """)
    suspend fun getRandomWords(count: Int): List<CachedWATWordEntity>
    
    /**
     * Get words by category
     */
    @Query("SELECT * FROM cached_wat_words WHERE category = :category ORDER BY sequenceNumber ASC")
    suspend fun getWordsByCategory(category: String): List<CachedWATWordEntity>
    
    /**
     * Get words by difficulty
     */
    @Query("SELECT * FROM cached_wat_words WHERE difficulty = :difficulty ORDER BY RANDOM() LIMIT :count")
    suspend fun getWordsByDifficulty(difficulty: String, count: Int): List<CachedWATWordEntity>
    
    /**
     * Get least used words
     */
    @Query("SELECT * FROM cached_wat_words ORDER BY usageCount ASC, lastUsed ASC LIMIT :count")
    suspend fun getLeastUsedWords(count: Int): List<CachedWATWordEntity>
    
    /**
     * Mark words as used (increment usage count and update last used timestamp)
     */
    @Query("""
        UPDATE cached_wat_words 
        SET usageCount = usageCount + 1, lastUsed = :timestamp 
        WHERE id IN (:wordIds)
    """)
    suspend fun markWordsAsUsed(wordIds: List<String>, timestamp: Long = System.currentTimeMillis())
    
    // ============ Insert/Update/Delete ============
    
    /**
     * Insert a single word
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: CachedWATWordEntity)
    
    /**
     * Insert multiple words (batch)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<CachedWATWordEntity>)
    
    /**
     * Delete all words
     */
    @Query("DELETE FROM cached_wat_words")
    suspend fun clearAllWords()
    
    /**
     * Delete words from specific batch
     */
    @Query("DELETE FROM cached_wat_words WHERE batchId = :batchId")
    suspend fun deleteWordsByBatch(batchId: String)
    
    // ============ Statistics ============
    
    /**
     * Get total number of cached words
     */
    @Query("SELECT COUNT(*) FROM cached_wat_words")
    suspend fun getTotalWordCount(): Int
    
    /**
     * Get word count by category
     */
    @Query("SELECT COUNT(*) FROM cached_wat_words WHERE category = :category")
    suspend fun getWordCountByCategory(category: String): Int
    
    /**
     * Get word count by difficulty
     */
    @Query("SELECT COUNT(*) FROM cached_wat_words WHERE difficulty = :difficulty")
    suspend fun getWordCountByDifficulty(difficulty: String): Int
    
    // ============ Batch Metadata ============
    
    /**
     * Insert batch metadata
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchMetadata(metadata: WATBatchMetadataEntity)
    
    /**
     * Get all batch metadata
     */
    @Query("SELECT * FROM wat_batch_metadata ORDER BY downloadedAt DESC")
    suspend fun getAllBatchMetadata(): List<WATBatchMetadataEntity>
    
    /**
     * Get specific batch metadata
     */
    @Query("SELECT * FROM wat_batch_metadata WHERE batchId = :batchId")
    suspend fun getBatchMetadata(batchId: String): WATBatchMetadataEntity?
    
    /**
     * Delete batch metadata
     */
    @Query("DELETE FROM wat_batch_metadata WHERE batchId = :batchId")
    suspend fun deleteBatchMetadata(batchId: String)
    
    /**
     * Get total number of batches
     */
    @Query("SELECT COUNT(*) FROM wat_batch_metadata")
    suspend fun getTotalBatchCount(): Int
}

