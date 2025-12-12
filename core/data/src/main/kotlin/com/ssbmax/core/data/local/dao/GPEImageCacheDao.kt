package com.ssbmax.core.data.local.dao

import androidx.room.*
import com.ssbmax.core.data.local.entity.CachedGPEImageEntity
import com.ssbmax.core.data.local.entity.GPEBatchMetadataEntity

/**
 * DAO for managing cached GPE (Group Planning Exercise) images
 * Follows the same pattern as PPDT
 */
@Dao
interface GPEImageCacheDao {

    // ============ Image Queries ============

    /**
     * Get all cached images
     */
    @Query("SELECT * FROM cached_gpe_images ORDER BY id ASC")
    suspend fun getAllImages(): List<CachedGPEImageEntity>

    /**
     * Get images from specific batch
     */
    @Query("SELECT * FROM cached_gpe_images WHERE batchId = :batchId ORDER BY id ASC")
    suspend fun getImagesByBatch(batchId: String): List<CachedGPEImageEntity>

    /**
     * Get random selection of images (for a test)
     * Prioritizes less-used images
     */
    @Query("""
        SELECT * FROM cached_gpe_images
        ORDER BY usageCount ASC, RANDOM()
        LIMIT :count
    """)
    suspend fun getRandomImages(count: Int): List<CachedGPEImageEntity>

    /**
     * Get images by category
     */
    @Query("SELECT * FROM cached_gpe_images WHERE category = :category ORDER BY id ASC")
    suspend fun getImagesByCategory(category: String): List<CachedGPEImageEntity>

    /**
     * Get images by difficulty
     */
    @Query("SELECT * FROM cached_gpe_images WHERE difficulty = :difficulty ORDER BY RANDOM() LIMIT :count")
    suspend fun getImagesByDifficulty(difficulty: String, count: Int): List<CachedGPEImageEntity>

    /**
     * Get least used images
     */
    @Query("SELECT * FROM cached_gpe_images ORDER BY usageCount ASC, lastUsed ASC LIMIT :count")
    suspend fun getLeastUsedImages(count: Int): List<CachedGPEImageEntity>

    /**
     * Get images that have been downloaded locally
     */
    @Query("SELECT * FROM cached_gpe_images WHERE imageDownloaded = 1 ORDER BY id ASC")
    suspend fun getDownloadedImages(): List<CachedGPEImageEntity>

    /**
     * Get images that need to be downloaded
     */
    @Query("SELECT * FROM cached_gpe_images WHERE imageDownloaded = 0 LIMIT :limit")
    suspend fun getImagesNeedingDownload(limit: Int = 10): List<CachedGPEImageEntity>

    /**
     * Mark images as used (increment usage count and update last used timestamp)
     */
    @Query("""
        UPDATE cached_gpe_images
        SET usageCount = usageCount + 1, lastUsed = :timestamp
        WHERE id IN (:imageIds)
    """)
    suspend fun markImagesAsUsed(imageIds: List<String>, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark image as downloaded
     */
    @Query("""
        UPDATE cached_gpe_images
        SET imageDownloaded = 1, localFilePath = :localPath
        WHERE id = :imageId
    """)
    suspend fun markImageAsDownloaded(imageId: String, localPath: String)

    // ============ Insert/Update/Delete ============

    /**
     * Insert a single image
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: CachedGPEImageEntity)

    /**
     * Insert multiple images (batch)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<CachedGPEImageEntity>)

    /**
     * Delete all images
     */
    @Query("DELETE FROM cached_gpe_images")
    suspend fun clearAllImages()

    /**
     * Delete images from specific batch
     */
    @Query("DELETE FROM cached_gpe_images WHERE batchId = :batchId")
    suspend fun deleteImagesByBatch(batchId: String)

    // ============ Statistics ============

    /**
     * Get total number of cached images
     */
    @Query("SELECT COUNT(*) FROM cached_gpe_images")
    suspend fun getTotalImageCount(): Int

    /**
     * Get count of downloaded images
     */
    @Query("SELECT COUNT(*) FROM cached_gpe_images WHERE imageDownloaded = 1")
    suspend fun getDownloadedImageCount(): Int

    /**
     * Get image count by category
     */
    @Query("SELECT COUNT(*) FROM cached_gpe_images WHERE category = :category")
    suspend fun getImageCountByCategory(category: String): Int

    /**
     * Get image count by difficulty
     */
    @Query("SELECT COUNT(*) FROM cached_gpe_images WHERE difficulty = :difficulty")
    suspend fun getImageCountByDifficulty(difficulty: String): Int

    /**
     * Get all categories (for diagnostics)
     */
    @Query("SELECT DISTINCT category FROM cached_gpe_images WHERE category IS NOT NULL")
    suspend fun getAllCategories(): List<String>

    // ============ Batch Metadata ============

    /**
     * Insert batch metadata
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchMetadata(metadata: GPEBatchMetadataEntity)

    /**
     * Get all batch metadata
     */
    @Query("SELECT * FROM gpe_batch_metadata ORDER BY downloadedAt DESC")
    suspend fun getAllBatchMetadata(): List<GPEBatchMetadataEntity>

    /**
     * Get specific batch metadata
     */
    @Query("SELECT * FROM gpe_batch_metadata WHERE batchId = :batchId")
    suspend fun getBatchMetadata(batchId: String): GPEBatchMetadataEntity?

    /**
     * Delete batch metadata
     */
    @Query("DELETE FROM gpe_batch_metadata WHERE batchId = :batchId")
    suspend fun deleteBatchMetadata(batchId: String)

    /**
     * Get total number of batches
     */
    @Query("SELECT COUNT(*) FROM gpe_batch_metadata")
    suspend fun getTotalBatchCount(): Int
}
