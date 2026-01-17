package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.PPDTImageCacheDao
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import com.ssbmax.core.data.local.entity.PPDTBatchMetadataEntity
import com.ssbmax.core.domain.model.PPDTQuestion
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for PPDT image progressive caching
 * Follows the same architecture as TAT
 * 
 * Note: This caches image metadata and URLs. Actual image files can be
 * downloaded on-demand using Firebase Storage or Coil's caching.
 */
@Singleton
class PPDTImageCacheManager @Inject constructor(
    private val dao: PPDTImageCacheDao,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "PPDTCacheManager"
        private const val COLLECTION_PATH = "test_content/ppdt/image_batches"
        private const val METADATA_PATH = "test_content/ppdt/meta"
        private const val TARGET_CACHE_SIZE = 15 // Multiple PPDT images per test
        private const val MIN_CACHE_SIZE = 5 // Minimum before resyncing
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
            downloadBatch("batch_002_context_enhanced").getOrThrow()
            
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
                    CachedPPDTImageEntity(
                        id = imageMap["id"] as? String ?: return@mapNotNull null,
                        imageUrl = imageMap["imageUrl"] as? String ?: return@mapNotNull null,
                        localFilePath = null, // Will be set when image is downloaded
                        imageDescription = imageMap["imageDescription"] as? String 
                            ?: "Picture showing an ambiguous scene",
                        context = imageMap["context"] as? String ?: "",
                        viewingTimeSeconds = (imageMap["viewingTimeSeconds"] as? Long)?.toInt() ?: 30,
                        writingTimeMinutes = (imageMap["writingTimeMinutes"] as? Long)?.toInt() ?: 4,
                        minCharacters = (imageMap["minCharacters"] as? Long)?.toInt() ?: 200,
                        maxCharacters = (imageMap["maxCharacters"] as? Long)?.toInt() ?: 1000,
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
                PPDTBatchMetadataEntity(
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
     * Get a single image for PPDT test
     * Note: PPDT typically shows one image at a time
     */
    suspend fun getImageForTest(): Result<PPDTQuestion> {
        return try {
            Log.d(TAG, "Getting image for PPDT test")
            
            // Check if cache needs refresh
            val currentCount = dao.getTotalImageCount()
            if (currentCount < MIN_CACHE_SIZE) {
                Log.w(TAG, "Cache below minimum ($currentCount < $MIN_CACHE_SIZE), syncing...")
                initialSync().getOrThrow()
            }
            
            // Get least-used image to ensure variety
            val cachedImages = dao.getLeastUsedImages(1)
            
            if (cachedImages.isEmpty()) {
                throw Exception("No images in cache")
            }
            
            val image = cachedImages.first()
            
            // Mark as used
            dao.markImagesAsUsed(listOf(image.id))
            
            // Convert to domain model
            val question = PPDTQuestion(
                id = image.id,
                imageUrl = image.imageUrl,
                imageDescription = image.imageDescription,
                context = image.context,
                viewingTimeSeconds = image.viewingTimeSeconds,
                writingTimeMinutes = image.writingTimeMinutes,
                minCharacters = image.minCharacters,
                maxCharacters = image.maxCharacters
            )
            
            Log.d(TAG, "Retrieved image for PPDT test: ${question.id}")
            Result.success(question)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get image for test", e)
            Result.failure(e)
        }
    }

    /**
     * Get a specific image by ID
     */
    suspend fun getImageById(imageId: String): Result<PPDTQuestion> {
        return try {
            val entity = dao.getImageById(imageId)
            if (entity != null) {
                // Return cached version
                return Result.success(
                    PPDTQuestion(
                        id = entity.id,
                        imageUrl = entity.imageUrl,
                        imageDescription = entity.imageDescription,
                        context = entity.context, // Map context
                        viewingTimeSeconds = entity.viewingTimeSeconds,
                        writingTimeMinutes = entity.writingTimeMinutes,
                        minCharacters = entity.minCharacters,
                        maxCharacters = entity.maxCharacters
                    )
                )
            }

            // Not in cache, try to fetch from batch metadata to see if we can find it
            // For now, simpler to just return failure if not in cache, as we expect relevant images to be cached
            // In a robust implementation, we might fetch from Firestore directly here if needed.
            // But strict caching policy prevents ad-hoc Firestore reads for content.
            Result.failure(Exception("Image not found in cache: $imageId"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get image by ID: $imageId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get multiple images for test (if needed for group discussions)
     */
    suspend fun getImagesForTest(count: Int = 1): Result<List<PPDTQuestion>> {
        return try {
            Log.d(TAG, "Getting $count images for PPDT test")
            
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
                PPDTQuestion(
                    id = entity.id,
                    imageUrl = entity.imageUrl,
                    imageDescription = entity.imageDescription,
                    context = entity.context,
                    viewingTimeSeconds = entity.viewingTimeSeconds,
                    writingTimeMinutes = entity.writingTimeMinutes,
                    minCharacters = entity.minCharacters,
                    maxCharacters = entity.maxCharacters
                )
            }
            
            Log.d(TAG, "Retrieved ${questions.size} images for PPDT test")
            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get images for test", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get cache status for diagnostics
     */
    suspend fun getCacheStatus(): PPDTCacheStatus {
        return try {
            val totalImages = dao.getTotalImageCount()
            val downloadedImages = dao.getDownloadedImageCount()
            val batches = dao.getTotalBatchCount()
            val batchMetadata = dao.getAllBatchMetadata()
            val lastSyncTime = batchMetadata.maxOfOrNull { it.downloadedAt }
            
            PPDTCacheStatus(
                cachedImages = totalImages,
                downloadedImages = downloadedImages,
                batchesDownloaded = batches,
                lastSyncTime = lastSyncTime
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache status", e)
            PPDTCacheStatus()
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
 * PPDT cache status for diagnostics
 */
data class PPDTCacheStatus(
    val cachedImages: Int = 0,
    val downloadedImages: Int = 0, // Images with local file cache
    val batchesDownloaded: Int = 0,
    val lastSyncTime: Long? = null
)

