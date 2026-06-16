package com.ssbmax.core.data.local.dao

import androidx.room.*
import com.ssbmax.core.data.local.entity.CachedTATImageEntity
import com.ssbmax.core.data.local.entity.TATBatchMetadataEntity

@Dao
interface TATImageCacheDao {

    // ============ Primary Selection Query ============

    /**
     * Select the least-used image for a given card position, preferring the requested
     * gender but always accepting MIXED images as a fallback within the same query.
     */
    @Query("""
        SELECT * FROM cached_tat_images
        WHERE cardPosition = :cardPosition
          AND (genderTag = :genderTag OR genderTag = 'MIXED')
        ORDER BY usageCount ASC, RANDOM()
        LIMIT 1
    """)
    suspend fun getLeastUsedImageByPosition(cardPosition: Int, genderTag: String): CachedTATImageEntity?

    // ============ General Image Queries ============

    @Query("SELECT * FROM cached_tat_images ORDER BY cardPosition ASC")
    suspend fun getAllImages(): List<CachedTATImageEntity>

    @Query("SELECT * FROM cached_tat_images WHERE batchId = :batchId ORDER BY cardPosition ASC")
    suspend fun getImagesByBatch(batchId: String): List<CachedTATImageEntity>

    @Query("SELECT * FROM cached_tat_images WHERE category = :category ORDER BY cardPosition ASC")
    suspend fun getImagesByCategory(category: String): List<CachedTATImageEntity>

    @Query("SELECT * FROM cached_tat_images WHERE difficulty = :difficulty ORDER BY RANDOM() LIMIT :count")
    suspend fun getImagesByDifficulty(difficulty: String, count: Int): List<CachedTATImageEntity>

    // ============ Usage Tracking ============

    @Query("""
        UPDATE cached_tat_images
        SET usageCount = usageCount + 1, lastUsed = :timestamp
        WHERE id IN (:imageIds)
    """)
    suspend fun markImagesAsUsed(imageIds: List<String>, timestamp: Long = System.currentTimeMillis())

    // ============ Insert / Delete ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: CachedTATImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<CachedTATImageEntity>)

    @Query("DELETE FROM cached_tat_images")
    suspend fun clearAllImages()

    @Query("DELETE FROM cached_tat_images WHERE batchId = :batchId")
    suspend fun deleteImagesByBatch(batchId: String)

    // ============ Statistics ============

    @Query("SELECT COUNT(*) FROM cached_tat_images")
    suspend fun getTotalImageCount(): Int

    @Query("SELECT COUNT(*) FROM cached_tat_images WHERE category = :category")
    suspend fun getImageCountByCategory(category: String): Int

    @Query("SELECT DISTINCT category FROM cached_tat_images WHERE category IS NOT NULL")
    suspend fun getAllCategories(): List<String>

    // ============ Batch Metadata ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchMetadata(metadata: TATBatchMetadataEntity)

    @Query("SELECT * FROM tat_batch_metadata ORDER BY downloadedAt DESC")
    suspend fun getAllBatchMetadata(): List<TATBatchMetadataEntity>

    @Query("SELECT * FROM tat_batch_metadata WHERE batchId = :batchId")
    suspend fun getBatchMetadata(batchId: String): TATBatchMetadataEntity?

    @Query("DELETE FROM tat_batch_metadata WHERE batchId = :batchId")
    suspend fun deleteBatchMetadata(batchId: String)

    @Query("SELECT COUNT(*) FROM tat_batch_metadata")
    suspend fun getTotalBatchCount(): Int
}
