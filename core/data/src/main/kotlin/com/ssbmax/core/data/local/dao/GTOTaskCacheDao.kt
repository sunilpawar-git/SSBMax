package com.ssbmax.core.data.local.dao

import androidx.room.*
import com.ssbmax.core.data.local.entity.CachedGTOTaskEntity
import com.ssbmax.core.data.local.entity.GTOBatchMetadataEntity

/**
 * DAO for managing cached GTO tasks
 * Follows the same pattern as other test types
 */
@Dao
interface GTOTaskCacheDao {
    
    // ============ Task Queries ============
    
    /**
     * Get all cached tasks
     */
    @Query("SELECT * FROM cached_gto_tasks ORDER BY id ASC")
    suspend fun getAllTasks(): List<CachedGTOTaskEntity>
    
    /**
     * Get tasks from specific batch
     */
    @Query("SELECT * FROM cached_gto_tasks WHERE batchId = :batchId ORDER BY id ASC")
    suspend fun getTasksByBatch(batchId: String): List<CachedGTOTaskEntity>
    
    /**
     * Get tasks by type (GD, GPE, PGT, etc.)
     */
    @Query("SELECT * FROM cached_gto_tasks WHERE taskType = :taskType ORDER BY id ASC")
    suspend fun getTasksByType(taskType: String): List<CachedGTOTaskEntity>
    
    /**
     * Get random selection of tasks
     * Prioritizes less-used tasks
     */
    @Query("""
        SELECT * FROM cached_gto_tasks 
        ORDER BY usageCount ASC, RANDOM() 
        LIMIT :count
    """)
    suspend fun getRandomTasks(count: Int): List<CachedGTOTaskEntity>
    
    /**
     * Get tasks by category
     */
    @Query("SELECT * FROM cached_gto_tasks WHERE category = :category ORDER BY id ASC")
    suspend fun getTasksByCategory(category: String): List<CachedGTOTaskEntity>
    
    /**
     * Get tasks by difficulty
     */
    @Query("SELECT * FROM cached_gto_tasks WHERE difficultyLevel = :difficulty ORDER BY RANDOM() LIMIT :count")
    suspend fun getTasksByDifficulty(difficulty: String, count: Int): List<CachedGTOTaskEntity>
    
    /**
     * Get least used tasks
     */
    @Query("SELECT * FROM cached_gto_tasks ORDER BY usageCount ASC, lastUsed ASC LIMIT :count")
    suspend fun getLeastUsedTasks(count: Int): List<CachedGTOTaskEntity>
    
    /**
     * Get tasks by type with usage prioritization
     */
    @Query("""
        SELECT * FROM cached_gto_tasks 
        WHERE taskType = :taskType 
        ORDER BY usageCount ASC, RANDOM() 
        LIMIT :count
    """)
    suspend fun getLeastUsedTasksByType(taskType: String, count: Int): List<CachedGTOTaskEntity>
    
    /**
     * Mark tasks as used (increment usage count and update last used timestamp)
     */
    @Query("""
        UPDATE cached_gto_tasks 
        SET usageCount = usageCount + 1, lastUsed = :timestamp 
        WHERE id IN (:taskIds)
    """)
    suspend fun markTasksAsUsed(taskIds: List<String>, timestamp: Long = System.currentTimeMillis())
    
    // ============ Insert/Update/Delete ============
    
    /**
     * Insert a single task
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: CachedGTOTaskEntity)
    
    /**
     * Insert multiple tasks (batch)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<CachedGTOTaskEntity>)
    
    /**
     * Delete all tasks
     */
    @Query("DELETE FROM cached_gto_tasks")
    suspend fun clearAllTasks()
    
    /**
     * Delete tasks from specific batch
     */
    @Query("DELETE FROM cached_gto_tasks WHERE batchId = :batchId")
    suspend fun deleteTasksByBatch(batchId: String)
    
    // ============ Statistics ============
    
    /**
     * Get total number of cached tasks
     */
    @Query("SELECT COUNT(*) FROM cached_gto_tasks")
    suspend fun getTotalTaskCount(): Int
    
    /**
     * Get task count by type
     */
    @Query("SELECT COUNT(*) FROM cached_gto_tasks WHERE taskType = :taskType")
    suspend fun getTaskCountByType(taskType: String): Int
    
    /**
     * Get task count by category
     */
    @Query("SELECT COUNT(*) FROM cached_gto_tasks WHERE category = :category")
    suspend fun getTaskCountByCategory(category: String): Int
    
    /**
     * Get task count by difficulty
     */
    @Query("SELECT COUNT(*) FROM cached_gto_tasks WHERE difficultyLevel = :difficulty")
    suspend fun getTaskCountByDifficulty(difficulty: String): Int
    
    /**
     * Get all task types (for diagnostics)
     */
    @Query("SELECT DISTINCT taskType FROM cached_gto_tasks")
    suspend fun getAllTaskTypes(): List<String>
    
    /**
     * Get all categories (for diagnostics)
     */
    @Query("SELECT DISTINCT category FROM cached_gto_tasks WHERE category IS NOT NULL")
    suspend fun getAllCategories(): List<String>
    
    // ============ Batch Metadata ============
    
    /**
     * Insert batch metadata
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchMetadata(metadata: GTOBatchMetadataEntity)
    
    /**
     * Get all batch metadata
     */
    @Query("SELECT * FROM gto_batch_metadata ORDER BY downloadedAt DESC")
    suspend fun getAllBatchMetadata(): List<GTOBatchMetadataEntity>
    
    /**
     * Get specific batch metadata
     */
    @Query("SELECT * FROM gto_batch_metadata WHERE batchId = :batchId")
    suspend fun getBatchMetadata(batchId: String): GTOBatchMetadataEntity?
    
    /**
     * Delete batch metadata
     */
    @Query("DELETE FROM gto_batch_metadata WHERE batchId = :batchId")
    suspend fun deleteBatchMetadata(batchId: String)
    
    /**
     * Get total number of batches
     */
    @Query("SELECT COUNT(*) FROM gto_batch_metadata")
    suspend fun getTotalBatchCount(): Int
}

