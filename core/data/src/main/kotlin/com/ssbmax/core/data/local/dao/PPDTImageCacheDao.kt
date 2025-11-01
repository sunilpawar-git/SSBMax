package com.ssbmax.core.data.local.dao

import androidx.room.*
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import com.ssbmax.core.data.local.entity.PPDTBatchMetadataEntity

/**
 * DAO for managing cached PPDT images
 * Follows the same pattern as TAT
 */
@Dao
interface PPDTImageCacheDao {
    
    // ============ Image Queries ============
    
    /**
     * Get all cached images
     */
    @Query("SELECT * FROM cached_ppdt_images ORDER BY id ASC")
    suspend fun getAllImages(): List<CachedPPDTImageEntity>
    
    /**
     * Get images from specific batch
     */
    @Query("SELECT * FROM cached_ppdt_images WHERE batchId = :batchId ORDER BY id ASC")
    suspend fun getImagesByBatch(batchId: String): List<CachedPPDTImageEntity>
    
    /**
     * Get random selection of images (for a test)
     * Prioritizes less-used images
     */
    @Query("""
        SELECT * FROM cached_ppdt_images 
        ORDER BY usageCount ASC, RANDOM() 
        LIMIT :count
    """)
    suspend fun getRandomImages(count: Int): List<CachedPPDTImageEntity>
    
    /**
     * Get images by category
     */
    @Query("SELECT * FROM cached_ppdt_images WHERE category = :category ORDER BY id ASC")
    suspend fun getImagesByCategory(category: String): List<CachedPPDTImageEntity>
    
    /**
     * Get images by difficulty
     */
    @Query("SELECT * FROM cached_ppdt_images WHERE difficulty = :difficulty ORDER BY RANDOM() LIMIT :count")
    suspend fun getImagesByDifficulty(difficulty: String, count: Int): List<CachedPPDTImageEntity>
    
    /**
     * Get least used images
     */
    @Query("SELECT * FROM cached_ppdt_images ORDER BY usageCount ASC, lastUsed ASC LIMIT :count")
    suspend fun getLeastUsedImages(count: Int): List<CachedPPDTImageEntity>
    
    /**
     * Get images that have been downloaded locally
     */
    @Query("SELECT * FROM cached_ppdt_images WHERE imageDownloaded = 1 ORDER BY id ASC")
    suspend fun getDownloadedImages(): List<CachedPPDTImageEntity>
    
    /**
     * Get images that need to be downloaded
     */
    @Query("SELECT * FROM cached_ppdt_images WHERE imageDownloaded = 0 LIMIT :limit")
    suspend fun getImagesNeedingDownload(limit: Int = 10): List<CachedPPDTImageEntity>
    
    /**
     * Mark images as used (increment usage count and update last used timestamp)
     */
    @Query("""
        UPDATE cached_ppdt_images 
        SET usageCount = usageCount + 1, lastUsed = :timestamp 
        WHERE id IN (:imageIds)
    """)
    suspend fun markImagesAsUsed(imageIds: List<String>, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Mark image as downloaded
     */
    @Query("""
        UPDATE cached_ppdt_images 
        SET imageDownloaded = 1, localFilePath = :localPath 
        WHERE id = :imageId
    """)
    suspend fun markImageAsDownloaded(imageId: String, localPath: String)
    
    // ============ Insert/Update/Delete ============
    
    /**
     * Insert a single image
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: CachedPPDTImageEntity)
    
    /**
     * Insert multiple images (batch)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<CachedPPDTImageEntity>)
    
    /**
     * Delete all images
     */
    @Query("DELETE FROM cached_ppdt_images")
    suspend fun clearAllImages()
    
    /**
     * Delete images from specific batch
     */
    @Query("DELETE FROM cached_ppdt_images WHERE batchId = :batchId")
    suspend fun deleteImagesByBatch(batchId: String)
    
    // ============ Statistics ============
    
    /**
     * Get total number of cached images
     */
    @Query("SELECT COUNT(*) FROM cached_ppdt_images")
    suspend fun getTotalImageCount(): Int
    
    /**
     * Get count of downloaded images
     */
    @Query("SELECT COUNT(*) FROM cached_ppdt_images WHERE imageDownloaded = 1")
    suspend fun getDownloadedImageCount(): Int
    
    /**
     * Get image count by category
     */
    @Query("SELECT COUNT(*) FROM cached_ppdt_images WHERE category = :category")
    suspend fun getImageCountByCategory(category: String): Int
    
    /**
     * Get image count by difficulty
     */
    @Query("SELECT COUNT(*) FROM cached_ppdt_images WHERE difficulty = :difficulty")
    suspend fun getImageCountByDifficulty(difficulty: String): Int
    
    /**
     * Get all categories (for diagnostics)
     */
    @Query("SELECT DISTINCT category FROM cached_ppdt_images WHERE category IS NOT NULL")
    suspend fun getAllCategories(): List<String>
    
    // ============ Batch Metadata ============
    
    /**
     * Insert batch metadata
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchMetadata(metadata: PPDTBatchMetadataEntity)
    
    /**
     * Get all batch metadata
     */
    @Query("SELECT * FROM ppdt_batch_metadata ORDER BY downloadedAt DESC")
    suspend fun getAllBatchMetadata(): List<PPDTBatchMetadataEntity>
    
    /**
     * Get specific batch metadata
     */
    @Query("SELECT * FROM ppdt_batch_metadata WHERE batchId = :batchId")
    suspend fun getBatchMetadata(batchId: String): PPDTBatchMetadataEntity?
    
    /**
     * Delete batch metadata
     */
    @Query("DELETE FROM ppdt_batch_metadata WHERE batchId = :batchId")
    suspend fun deleteBatchMetadata(batchId: String)
    
    /**
     * Get total number of batches
     */
    @Query("SELECT COUNT(*) FROM ppdt_batch_metadata")
    suspend fun getTotalBatchCount(): Int
}

