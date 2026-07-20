package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.ssbmax.core.data.local.dao.PPDTImageCacheDao
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import com.ssbmax.core.data.local.entity.PPDTBatchMetadataEntity
import com.ssbmax.shared.domain.model.GenderTag
import com.ssbmax.shared.domain.model.PPDTImageContext
import com.ssbmax.shared.domain.model.PPDTQuestion
import kotlinx.coroutines.tasks.await

/**
 * Manager for PPDT image progressive caching.
 * Phase 6: imageContextJson + genderTag now stored per-entity; gender filter is live.
 */
class PPDTImageCacheManager(
    private val dao: PPDTImageCacheDao,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "PPDTCacheManager"
        private const val COLLECTION_PATH = "test_content/ppdt/image_batches"
        private const val TARGET_CACHE_SIZE = 15
        private const val MIN_CACHE_SIZE = 5
    }

    private val gson = Gson()

    private fun logD(msg: String) { Log.d(TAG, msg) }
    private fun logW(msg: String, t: Throwable? = null) { if (t != null) Log.w(TAG, msg, t) else Log.w(TAG, msg) }
    private fun logE(msg: String, t: Throwable? = null) { if (t != null) Log.e(TAG, msg, t) else Log.e(TAG, msg) }

    /**
     * Initialize cache with batch_001 (64 Phase-5 images with gender tags and structured context).
     * Checks Firestore version; clears and re-downloads if the batch has been updated.
     */
    suspend fun initialSync(): Result<Unit> {
        return try {
            logD("Starting initial sync...")
            val currentCount = dao.getTotalImageCount()
            if (currentCount >= TARGET_CACHE_SIZE) {
                // Check if Firestore has a newer batch version before skipping
                if (!isCacheStale("batch_001")) {
                    logD("Cache already initialized ($currentCount images, version current)")
                    return Result.success(Unit)
                }
                logD("Firestore has newer batch version — clearing stale cache and re-downloading")
                dao.clearAllImages()
            }
            downloadBatch("batch_001").getOrThrow()
            logD("Initial sync complete")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Initial sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Returns true if the locally cached batch is outdated vs Firestore.
     * Skips the Firestore read entirely if the last check was within 24h (TTL gate).
     * After a Firestore read, persists the check timestamp so the next 24h skips the read.
     */
    private suspend fun isCacheStale(batchId: String): Boolean {
        return try {
            val localMeta = dao.getBatchMetadata(batchId) ?: return true
            val hoursSinceCheck = (System.currentTimeMillis() - localMeta.lastStalenessCheckAt) / 3_600_000
            if (hoursSinceCheck < 24) return false  // warm start: TTL still valid, skip Firestore
            val doc = firestore.document("$COLLECTION_PATH/$batchId").get().await()
            val remoteVersion = doc.getString("version") ?: return false
            val isStale = remoteVersion != localMeta.version
            if (isStale) logD("Cache stale: local=${localMeta.version}, remote=$remoteVersion")
            dao.insertBatchMetadata(localMeta.copy(lastStalenessCheckAt = System.currentTimeMillis()))
            isStale
        } catch (e: Exception) {
            logW("Version check failed, assuming cache is current: ${e.message}")
            false // on network error, don't wipe — serve stale rather than break
        }
    }

    /**
     * Download a specific batch from Firestore.
     * Reads imageContext map and genderTag string from each image document.
     */
    suspend fun downloadBatch(batchId: String): Result<Unit> {
        return try {
            logD("Downloading batch: $batchId")
            val doc = firestore.document("$COLLECTION_PATH/$batchId").get().await()
            if (!doc.exists()) throw Exception("Batch $batchId not found in Firestore")

            @Suppress("UNCHECKED_CAST")
            val imagesData = doc.get("images") as? List<Map<String, Any?>>
                ?: throw Exception("No images found in batch $batchId")

            val version = doc.getString("version") ?: "1.0.0"

            val images = imagesData.mapNotNull { imageMap ->
                try {
                    val imageContextJson = imageContextJsonFromMap(imageMap)
                    val genderTag = genderTagFromString(imageMap["genderTag"] as? String)
                    CachedPPDTImageEntity(
                        id = imageMap["id"] as? String ?: return@mapNotNull null,
                        imageUrl = imageMap["imageUrl"] as? String ?: return@mapNotNull null,
                        localFilePath = null,
                        imageDescription = imageMap["imageDescription"] as? String
                            ?: "Picture showing an ambiguous scene",
                        imageContextJson = imageContextJson,
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
                        imageDownloaded = 0,
                        genderTag = genderTag
                    )
                } catch (e: Exception) {
                    logW("Failed to parse image: ${imageMap["id"]}", e)
                    null
                }
            }

            if (images.isEmpty()) throw Exception("No valid images parsed from batch $batchId")

            dao.insertImages(images)
            dao.insertBatchMetadata(
                PPDTBatchMetadataEntity(
                    batchId = batchId,
                    downloadedAt = System.currentTimeMillis(),
                    imageCount = images.size,
                    version = version,
                    lastStalenessCheckAt = System.currentTimeMillis()
                )
            )
            logD("Downloaded batch $batchId: ${images.size} images")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Failed to download batch $batchId", e)
            Result.failure(e)
        }
    }

    /**
     * Get a single image for a PPDT test, optionally filtered by gender.
     * Gender-scoped requests go straight to the DAO — no in-memory post-filter (SSOT/SOLID SR).
     */
    suspend fun getImageForTest(genderTag: GenderTag? = null): Result<PPDTQuestion> {
        return try {
            logD("Getting image for PPDT test (genderTag=$genderTag)")
            val currentCount = dao.getTotalImageCount()
            if (currentCount < MIN_CACHE_SIZE) {
                logW("Cache below minimum ($currentCount < $MIN_CACHE_SIZE), syncing...")
                initialSync().getOrThrow()
            }
            val image = if (genderTag != null) {
                dao.getLeastUsedImagesByGender(genderTag.name, 1).firstOrNull()
            } else {
                dao.getLeastUsedImages(1).firstOrNull()
            } ?: throw NoSuchElementException("No images available for genderTag=$genderTag")

            dao.markImagesAsUsed(listOf(image.id))
            logD("Retrieved image for PPDT test: ${image.id}")
            Result.success(entityToQuestion(image))
        } catch (e: Exception) {
            logE("Failed to get image for test", e)
            Result.failure(e)
        }
    }

    /**
     * Get a specific image by ID.
     */
    suspend fun getImageById(imageId: String): Result<PPDTQuestion> {
        return try {
            val entity = dao.getImageById(imageId)
                ?: return Result.failure(Exception("Image not found in cache: $imageId"))
            Result.success(entityToQuestion(entity))
        } catch (e: Exception) {
            logE("Failed to get image by ID: $imageId", e)
            Result.failure(e)
        }
    }

    /**
     * Get multiple images for a test (e.g. group discussion practice).
     */
    suspend fun getImagesForTest(count: Int = 1): Result<List<PPDTQuestion>> {
        return try {
            logD("Getting $count images for PPDT test")
            val currentCount = dao.getTotalImageCount()
            if (currentCount < MIN_CACHE_SIZE) {
                logW("Cache below minimum ($currentCount < $MIN_CACHE_SIZE), syncing...")
                initialSync().getOrThrow()
            }
            val cachedImages = dao.getLeastUsedImages(count)
            if (cachedImages.isEmpty()) throw Exception("No images in cache")

            dao.markImagesAsUsed(cachedImages.map { it.id })
            logD("Retrieved ${cachedImages.size} images for PPDT test")
            Result.success(cachedImages.map { entityToQuestion(it) })
        } catch (e: Exception) {
            logE("Failed to get images for test", e)
            Result.failure(e)
        }
    }

    /**
     * Get cache status for diagnostics.
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
            logE("Failed to get cache status", e)
            PPDTCacheStatus()
        }
    }

    /**
     * Clear all cached images and metadata.
     */
    suspend fun clearCache() {
        try {
            logD("Clearing cache...")
            dao.clearAllImages()
            logD("Cache cleared")
        } catch (e: Exception) {
            logE("Failed to clear cache", e)
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun entityToQuestion(entity: CachedPPDTImageEntity): PPDTQuestion {
        val imageContext = try {
            gson.fromJson(entity.imageContextJson, PPDTImageContext::class.java)
                ?: PPDTImageContext()
        } catch (e: JsonSyntaxException) {
            logW("Failed to parse PPDTImageContext for id=${entity.id}", e)
            PPDTImageContext()
        }
        return PPDTQuestion(
            id = entity.id,
            imageUrl = normalizeUrl(entity.imageUrl) ?: entity.imageUrl,
            imageDescription = entity.imageDescription,
            imageContext = imageContext,
            viewingTimeSeconds = entity.viewingTimeSeconds,
            writingTimeMinutes = entity.writingTimeMinutes,
            minCharacters = entity.minCharacters,
            maxCharacters = entity.maxCharacters
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun imageContextJsonFromMap(imageMap: Map<String, Any?>): String {
        val contextMap = imageMap["imageContext"] as? Map<String, Any?>
        return if (contextMap != null) gson.toJson(contextMap) else "{}"
    }

    private fun genderTagFromString(raw: String?): GenderTag =
        GenderTag.entries.firstOrNull { it.name == raw?.uppercase() } ?: GenderTag.MIXED

    private fun normalizeUrl(raw: String?): String? =
        raw?.let {
            if (it.startsWith("gs://")) it.replaceFirst("gs://", "https://storage.googleapis.com/")
            else it
        }
}

data class PPDTCacheStatus(
    val cachedImages: Int = 0,
    val downloadedImages: Int = 0,
    val batchesDownloaded: Int = 0,
    val lastSyncTime: Long? = null
)
