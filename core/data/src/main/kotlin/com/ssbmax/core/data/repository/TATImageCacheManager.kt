package com.ssbmax.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.ssbmax.core.data.local.dao.TATImageCacheDao
import com.ssbmax.core.data.local.entity.CachedTATImageEntity
import com.ssbmax.core.data.local.entity.TATBatchMetadataEntity
import com.ssbmax.core.domain.model.GenderTag
import com.ssbmax.core.domain.model.TATQuestion
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the 167-image TAT pool in Room.
 * - All 167 images live in one Firestore document (batch_001).
 * - getImagesForTest assembles 11 cards by card position (1–11), one per position,
 *   picking the least-used image that matches the user's gender (or MIXED).
 * - Card 12 is the programmatic blank card — never stored in Room.
 * - 24h TTL gate mirrors PPDT: at most one Firestore version check per user per day.
 */
@Singleton
class TATImageCacheManager @Inject constructor(
    private val dao: TATImageCacheDao,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "TATCacheManager"
        private const val COLLECTION_PATH = "test_content/tat/image_batches"
        private const val TARGET_CACHE_SIZE = 167
        private const val MINIMUM_CACHE_SIZE = 33 // 3 per position triggers resync
        private const val BLANK_CARD_POSITION = 12
    }

    private val gson = Gson()

    private fun logD(message: String) {
        try {
            Class.forName("android.util.Log")
                .getMethod("d", String::class.java, String::class.java)
                .invoke(null, TAG, message)
        } catch (_: Exception) { println("$TAG: $message") }
    }

    private fun logW(message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Class.forName("android.util.Log")
                    .getMethod("w", String::class.java, String::class.java, Throwable::class.java)
                    .invoke(null, TAG, message, throwable)
            } else {
                Class.forName("android.util.Log")
                    .getMethod("w", String::class.java, String::class.java)
                    .invoke(null, TAG, message)
            }
        } catch (_: Exception) { println("$TAG: $message${throwable?.let { " - $it" } ?: ""}") }
    }

    private fun logE(message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Class.forName("android.util.Log")
                    .getMethod("e", String::class.java, String::class.java, Throwable::class.java)
                    .invoke(null, TAG, message, throwable)
            } else {
                Class.forName("android.util.Log")
                    .getMethod("e", String::class.java, String::class.java)
                    .invoke(null, TAG, message)
            }
        } catch (_: Exception) { System.err.println("$TAG: $message${throwable?.let { " - $it" } ?: ""}") }
    }

    // < 33 images → download all 167 immediately; >= 33 → 24h TTL staleness gate.
    suspend fun initialSync(): Result<Unit> {
        return try {
            logD("Starting initial sync…")
            val currentCount = dao.getTotalImageCount()
            if (currentCount >= TARGET_CACHE_SIZE) {
                if (!isCacheStale("batch_001")) {
                    logD("Cache current ($currentCount images, version unchanged)")
                    return Result.success(Unit)
                }
                logD("Firestore has newer version — clearing stale cache and re-downloading")
                dao.clearAllImages()
            } else if (currentCount >= MINIMUM_CACHE_SIZE) {
                // Enough to serve tests but below full pool — still check version
                if (!isCacheStale("batch_001")) {
                    logD("Cache sufficient ($currentCount images), version current")
                    return Result.success(Unit)
                }
                logD("Version changed — clearing and re-downloading")
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

    // True if Firestore has a newer version; skips the read if last check was < 24h ago.
    private suspend fun isCacheStale(batchId: String): Boolean {
        return try {
            val localMeta = dao.getBatchMetadata(batchId) ?: return true
            val hoursSinceCheck = (System.currentTimeMillis() - localMeta.lastStalenessCheckAt) / 3_600_000
            if (hoursSinceCheck < 24) return false
            val doc = firestore.document("$COLLECTION_PATH/$batchId").get().await()
            val remoteVersion = doc.getString("version") ?: return false
            val isStale = remoteVersion != localMeta.version
            if (isStale) logD("Cache stale: local=${localMeta.version}, remote=$remoteVersion")
            dao.insertBatchMetadata(localMeta.copy(lastStalenessCheckAt = System.currentTimeMillis()))
            isStale
        } catch (e: Exception) {
            logW("Version check failed, assuming cache is current: ${e.message}")
            false
        }
    }

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
                    CachedTATImageEntity(
                        id = imageMap["id"] as? String ?: return@mapNotNull null,
                        imageUrl = imageMap["imageUrl"] as? String ?: return@mapNotNull null,
                        cardPosition = (imageMap["cardPosition"] as? Long)?.toInt() ?: return@mapNotNull null,
                        genderTag = genderTag,
                        imageContextJson = imageContextJson,
                        viewingTimeSeconds = (imageMap["viewingTimeSeconds"] as? Long)?.toInt() ?: 30,
                        writingTimeMinutes = (imageMap["writingTimeMinutes"] as? Long)?.toInt() ?: 4,
                        minCharacters = (imageMap["minCharacters"] as? Long)?.toInt() ?: 150,
                        maxCharacters = (imageMap["maxCharacters"] as? Long)?.toInt() ?: 1500,
                        category = imageMap["category"] as? String,
                        difficulty = imageMap["difficulty"] as? String,
                        batchId = batchId,
                        cachedAt = System.currentTimeMillis(),
                        lastUsed = null,
                        usageCount = 0
                    )
                } catch (e: Exception) {
                    logW("Failed to parse image: ${imageMap["id"]}", e)
                    null
                }
            }

            if (images.isEmpty()) throw Exception("No valid images parsed from batch $batchId")

            dao.insertImages(images)
            dao.insertBatchMetadata(
                TATBatchMetadataEntity(
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

    // Assembles 12 TATQuestions: positions 1–11 from pool (least-used, gender-matched), position 12 = blank card.
    suspend fun getImagesForTest(genderTag: GenderTag? = null): Result<List<TATQuestion>> {
        return try {
            logD("Assembling TAT test (genderTag=$genderTag)")

            val currentCount = dao.getTotalImageCount()
            if (currentCount < MINIMUM_CACHE_SIZE) {
                logW("Cache below minimum ($currentCount < $MINIMUM_CACHE_SIZE), syncing…")
                initialSync().getOrThrow()
            }

            val resolvedTag = genderTag?.name ?: GenderTag.MIXED.name
            val selected = mutableListOf<TATQuestion>()
            val usedIds = mutableListOf<String>()

            for (position in 1..11) {
                val entity = dao.getLeastUsedImageByPosition(position, resolvedTag)
                    ?: dao.getLeastUsedImageByPosition(position, GenderTag.MIXED.name)
                    ?: throw NoSuchElementException("No image available for cardPosition=$position (genderTag=$resolvedTag)")

                usedIds += entity.id
                selected += entityToQuestion(entity)
            }

            // Position 12 — programmatic blank card
            selected += blankCard()

            dao.markImagesAsUsed(usedIds)
            logD("Assembled ${selected.size} TAT questions")
            Result.success(selected)
        } catch (e: Exception) {
            logE("Failed to assemble TAT test", e)
            Result.failure(e)
        }
    }

    suspend fun getCacheStatus(): TATCacheStatus {
        return try {
            val totalImages = dao.getTotalImageCount()
            val batches = dao.getTotalBatchCount()
            val lastSyncTime = dao.getAllBatchMetadata().maxOfOrNull { it.downloadedAt }
            TATCacheStatus(cachedImages = totalImages, batchesDownloaded = batches, lastSyncTime = lastSyncTime)
        } catch (e: Exception) {
            logE("Failed to get cache status", e)
            TATCacheStatus()
        }
    }

    suspend fun clearCache() {
        try {
            logD("Clearing cache…")
            dao.clearAllImages()
            logD("Cache cleared")
        } catch (e: Exception) {
            logE("Failed to clear cache", e)
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun entityToQuestion(entity: CachedTATImageEntity): TATQuestion {
        return TATQuestion(
            id = entity.id,
            imageUrl = normalizeUrl(entity.imageUrl),
            cardPosition = entity.cardPosition,
            imageContextJson = entity.imageContextJson,
            genderTag = entity.genderTag,
            viewingTimeSeconds = entity.viewingTimeSeconds,
            writingTimeMinutes = entity.writingTimeMinutes,
            minCharacters = entity.minCharacters,
            maxCharacters = entity.maxCharacters
        )
    }

    private fun blankCard(): TATQuestion = TATQuestion(
        id = "blank_card",
        imageUrl = "",
        cardPosition = BLANK_CARD_POSITION,
        imageContextJson = "{}",
        viewingTimeSeconds = 30,
        writingTimeMinutes = 4,
        minCharacters = 150,
        maxCharacters = 1500
    )

    @Suppress("UNCHECKED_CAST")
    private fun imageContextJsonFromMap(imageMap: Map<String, Any?>): String {
        val contextMap = imageMap["imageContext"] as? Map<String, Any?>
        return if (contextMap != null) gson.toJson(contextMap) else "{}"
    }

    private fun genderTagFromString(raw: String?): String =
        GenderTag.entries.firstOrNull { it.name == raw?.uppercase() }?.name ?: GenderTag.MIXED.name

    private fun normalizeUrl(raw: String): String =
        if (raw.startsWith("gs://")) raw.replaceFirst("gs://", "https://storage.googleapis.com/") else raw

}

data class TATCacheStatus(
    val cachedImages: Int = 0,
    val batchesDownloaded: Int = 0,
    val lastSyncTime: Long? = null
)
