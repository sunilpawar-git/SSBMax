package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.TATImageCacheDao
import com.ssbmax.core.data.local.entity.CachedTATImageEntity
import com.ssbmax.core.data.local.entity.TATBatchMetadataEntity
import com.ssbmax.core.domain.model.TATQuestion
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for TAT image progressive caching
 * Follows the same architecture as OIR, WAT, and SRT
 * 
 * Note: This caches image metadata and URLs. Actual image files can be
 * downloaded on-demand using Firebase Storage or Coil's caching.
 */
@Singleton
class TATImageCacheManager @Inject constructor(
    private val dao: TATImageCacheDao,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "TATCacheManager"
        private const val COLLECTION_PATH = "test_content/tat/image_batches"
        private const val METADATA_PATH = "test_content/tat/meta"
        private const val TARGET_CACHE_SIZE = 12 // Full TAT test size
        private const val MIN_CACHE_SIZE = 4 // Minimum before resyncing
    }
    
    /**
     * Initialize cache with first batch
     * Called when app starts or cache is empty
     */
    suspend fun initialSync(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting initial sync...")
            
            val currentCount = dao.getTotalImageCount()
            if (currentCount >= TARGET_CACHE_SIZE) {
                Log.d(TAG, "Cache already initialized ($currentCount images)")
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
            val imagesData = doc.get("images") as? List<Map<String, Any?>> 
                ?: throw Exception("No images found in batch $batchId")
            
            val version = doc.getString("version") ?: "1.0.0"
            
            val images = imagesData.mapNotNull { imageMap ->
                try {
                    CachedTATImageEntity(
                        id = imageMap["id"] as? String ?: return@mapNotNull null,
                        imageUrl = imageMap["imageUrl"] as? String ?: return@mapNotNull null,
                        localFilePath = null, // Will be set when image is downloaded
                        sequenceNumber = (imageMap["sequenceNumber"] as? Long)?.toInt() ?: 0,
                        prompt = imageMap["prompt"] as? String 
                            ?: "Write a story about what you see in the picture",
                        viewingTimeSeconds = (imageMap["viewingTimeSeconds"] as? Long)?.toInt() ?: 30,
                        writingTimeMinutes = (imageMap["writingTimeMinutes"] as? Long)?.toInt() ?: 4,
                        minCharacters = (imageMap["minCharacters"] as? Long)?.toInt() ?: 150,
                        maxCharacters = (imageMap["maxCharacters"] as? Long)?.toInt() ?: 800,
                        category = imageMap["category"] as? String,
                        difficulty = imageMap["difficulty"] as? String,
                        batchId = batchId,
                        cachedAt = System.currentTimeMillis(),
                        lastUsed = null,
                        usageCount = 0,
                        imageDownloaded = false
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse image: ${imageMap["id"]}", e)
                    null
                }
            }
            
            if (images.isEmpty()) {
                throw Exception("No valid images parsed from batch $batchId")
            }
            
            // Insert images and metadata
            dao.insertImages(images)
            dao.insertBatchMetadata(
                TATBatchMetadataEntity(
                    batchId = batchId,
                    downloadedAt = System.currentTimeMillis(),
                    imageCount = images.size,
                    version = version
                )
            )
            
            Log.d(TAG, "Downloaded batch $batchId: ${images.size} images")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download batch $batchId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get images for a test (12 images, prioritizing least-used)
     */
    suspend fun getImagesForTest(count: Int = 12): Result<List<TATQuestion>> {
        return try {
            Log.d(TAG, "Getting $count images for test")
            
            // Check if cache needs refresh
            val currentCount = dao.getTotalImageCount()
            if (currentCount < MIN_CACHE_SIZE) {
                Log.w(TAG, "Cache below minimum ($currentCount < $MIN_CACHE_SIZE), syncing...")
                initialSync().getOrThrow()
            }
            
            // Get least-used images to ensure variety
            val cachedImages = dao.getLeastUsedImages(count)
            
            if (cachedImages.isEmpty()) {
                throw Exception("No images in cache")
            }
            
            // Mark as used
            dao.markImagesAsUsed(cachedImages.map { it.id })
            
            // Convert to domain model
            val questions = cachedImages.map { entity ->
                TATQuestion(
                    id = entity.id,
                    imageUrl = entity.imageUrl,
                    sequenceNumber = entity.sequenceNumber,
                    prompt = entity.prompt,
                    viewingTimeSeconds = entity.viewingTimeSeconds,
                    writingTimeMinutes = entity.writingTimeMinutes,
                    minCharacters = entity.minCharacters,
                    maxCharacters = entity.maxCharacters
                )
            }
            
            Log.d(TAG, "Retrieved ${questions.size} images for test")
            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get images for test", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get cache status for diagnostics
     */
    suspend fun getCacheStatus(): TATCacheStatus {
        return try {
            val totalImages = dao.getTotalImageCount()
            val downloadedImages = dao.getDownloadedImageCount()
            val batches = dao.getTotalBatchCount()
            val batchMetadata = dao.getAllBatchMetadata()
            val lastSyncTime = batchMetadata.maxOfOrNull { it.downloadedAt }
            
            TATCacheStatus(
                cachedImages = totalImages,
                downloadedImages = downloadedImages,
                batchesDownloaded = batches,
                lastSyncTime = lastSyncTime
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache status", e)
            TATCacheStatus()
        }
    }
    
    /**
     * Clear all cache (for debugging/testing)
     */
    suspend fun clearCache() {
        try {
            Log.d(TAG, "Clearing cache...")
            dao.clearAllImages()
            Log.d(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }
}

/**
 * TAT cache status for diagnostics
 */
data class TATCacheStatus(
    val cachedImages: Int = 0,
    val downloadedImages: Int = 0, // Images with local file cache
    val batchesDownloaded: Int = 0,
    val lastSyncTime: Long? = null
)

