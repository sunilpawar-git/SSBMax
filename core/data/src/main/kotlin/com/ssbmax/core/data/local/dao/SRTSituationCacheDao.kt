package com.ssbmax.core.data.local.dao

import androidx.room.*
import com.ssbmax.core.data.local.entity.CachedSRTSituationEntity
import com.ssbmax.core.data.local.entity.SRTBatchMetadataEntity

/**
 * DAO for managing cached SRT situations
 * Follows the same pattern as OIR and WAT
 */
@Dao
interface SRTSituationCacheDao {
    
    // ============ Situation Queries ============
    
    /**
     * Get all cached situations
     */
    @Query("SELECT * FROM cached_srt_situations ORDER BY sequenceNumber ASC")
    suspend fun getAllSituations(): List<CachedSRTSituationEntity>
    
    /**
     * Get situations from specific batch
     */
    @Query("SELECT * FROM cached_srt_situations WHERE batchId = :batchId ORDER BY sequenceNumber ASC")
    suspend fun getSituationsByBatch(batchId: String): List<CachedSRTSituationEntity>
    
    /**
     * Get random selection of situations (for a test)
     * Prioritizes less-used situations
     */
    @Query("""
        SELECT * FROM cached_srt_situations 
        ORDER BY usageCount ASC, RANDOM() 
        LIMIT :count
    """)
    suspend fun getRandomSituations(count: Int): List<CachedSRTSituationEntity>
    
    /**
     * Get situations by category
     */
    @Query("SELECT * FROM cached_srt_situations WHERE category = :category ORDER BY sequenceNumber ASC")
    suspend fun getSituationsByCategory(category: String): List<CachedSRTSituationEntity>
    
    /**
     * Get situations by difficulty
     */
    @Query("SELECT * FROM cached_srt_situations WHERE difficulty = :difficulty ORDER BY RANDOM() LIMIT :count")
    suspend fun getSituationsByDifficulty(difficulty: String, count: Int): List<CachedSRTSituationEntity>
    
    /**
     * Get least used situations
     */
    @Query("SELECT * FROM cached_srt_situations ORDER BY usageCount ASC, lastUsed ASC LIMIT :count")
    suspend fun getLeastUsedSituations(count: Int): List<CachedSRTSituationEntity>
    
    /**
     * Get balanced selection across categories
     * Returns mix of categories for variety
     */
    @Query("""
        SELECT * FROM cached_srt_situations 
        WHERE category = :category 
        ORDER BY usageCount ASC, RANDOM() 
        LIMIT :count
    """)
    suspend fun getBalancedByCategory(category: String, count: Int): List<CachedSRTSituationEntity>
    
    /**
     * Mark situations as used (increment usage count and update last used timestamp)
     */
    @Query("""
        UPDATE cached_srt_situations 
        SET usageCount = usageCount + 1, lastUsed = :timestamp 
        WHERE id IN (:situationIds)
    """)
    suspend fun markSituationsAsUsed(situationIds: List<String>, timestamp: Long = System.currentTimeMillis())
    
    // ============ Insert/Update/Delete ============
    
    /**
     * Insert a single situation
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSituation(situation: CachedSRTSituationEntity)
    
    /**
     * Insert multiple situations (batch)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSituations(situations: List<CachedSRTSituationEntity>)
    
    /**
     * Delete all situations
     */
    @Query("DELETE FROM cached_srt_situations")
    suspend fun clearAllSituations()
    
    /**
     * Delete situations from specific batch
     */
    @Query("DELETE FROM cached_srt_situations WHERE batchId = :batchId")
    suspend fun deleteSituationsByBatch(batchId: String)
    
    // ============ Statistics ============
    
    /**
     * Get total number of cached situations
     */
    @Query("SELECT COUNT(*) FROM cached_srt_situations")
    suspend fun getTotalSituationCount(): Int
    
    /**
     * Get situation count by category
     */
    @Query("SELECT COUNT(*) FROM cached_srt_situations WHERE category = :category")
    suspend fun getSituationCountByCategory(category: String): Int
    
    /**
     * Get situation count by difficulty
     */
    @Query("SELECT COUNT(*) FROM cached_srt_situations WHERE difficulty = :difficulty")
    suspend fun getSituationCountByDifficulty(difficulty: String): Int
    
    /**
     * Get all categories (for diagnostics)
     * Use this with getSituationCountByCategory to build distribution
     */
    @Query("SELECT DISTINCT category FROM cached_srt_situations")
    suspend fun getAllCategories(): List<String>
    
    // ============ Batch Metadata ============
    
    /**
     * Insert batch metadata
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchMetadata(metadata: SRTBatchMetadataEntity)
    
    /**
     * Get all batch metadata
     */
    @Query("SELECT * FROM srt_batch_metadata ORDER BY downloadedAt DESC")
    suspend fun getAllBatchMetadata(): List<SRTBatchMetadataEntity>
    
    /**
     * Get specific batch metadata
     */
    @Query("SELECT * FROM srt_batch_metadata WHERE batchId = :batchId")
    suspend fun getBatchMetadata(batchId: String): SRTBatchMetadataEntity?
    
    /**
     * Delete batch metadata
     */
    @Query("DELETE FROM srt_batch_metadata WHERE batchId = :batchId")
    suspend fun deleteBatchMetadata(batchId: String)
    
    /**
     * Get total number of batches
     */
    @Query("SELECT COUNT(*) FROM srt_batch_metadata")
    suspend fun getTotalBatchCount(): Int
}

