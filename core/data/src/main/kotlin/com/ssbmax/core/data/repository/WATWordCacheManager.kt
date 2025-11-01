package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.WATWordCacheDao
import com.ssbmax.core.data.local.entity.CachedWATWordEntity
import com.ssbmax.core.data.local.entity.WATBatchMetadataEntity
import com.ssbmax.core.domain.model.WATWord
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for WAT word progressive caching
 * Follows the same architecture as OIRQuestionCacheManager
 */
@Singleton
class WATWordCacheManager @Inject constructor(
    private val dao: WATWordCacheDao,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "WATCacheManager"
        private const val COLLECTION_PATH = "test_content/wat/word_batches"
        private const val METADATA_PATH = "test_content/wat/meta"
        private const val TARGET_CACHE_SIZE = 60 // Full WAT test size
        private const val MIN_CACHE_SIZE = 20 // Minimum before resyncing
    }
    
    /**
     * Initialize cache with first batch
     * Called when app starts or cache is empty
     */
    suspend fun initialSync(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting initial sync...")
            
            val currentCount = dao.getTotalWordCount()
            if (currentCount >= TARGET_CACHE_SIZE) {
                Log.d(TAG, "Cache already initialized ($currentCount words)")
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
            val wordsData = doc.get("words") as? List<Map<String, Any?>> 
                ?: throw Exception("No words found in batch $batchId")
            
            val version = doc.getString("version") ?: "1.0.0"
            
            val words = wordsData.mapNotNull { wordMap ->
                try {
                    CachedWATWordEntity(
                        id = wordMap["id"] as? String ?: return@mapNotNull null,
                        word = wordMap["word"] as? String ?: return@mapNotNull null,
                        sequenceNumber = (wordMap["sequenceNumber"] as? Long)?.toInt() ?: 0,
                        timeAllowedSeconds = (wordMap["timeAllowedSeconds"] as? Long)?.toInt() ?: 15,
                        category = wordMap["category"] as? String,
                        difficulty = wordMap["difficulty"] as? String,
                        batchId = batchId,
                        cachedAt = System.currentTimeMillis(),
                        lastUsed = null,
                        usageCount = 0
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse word: ${wordMap["id"]}", e)
                    null
                }
            }
            
            if (words.isEmpty()) {
                throw Exception("No valid words parsed from batch $batchId")
            }
            
            // Insert words and metadata
            dao.insertWords(words)
            dao.insertBatchMetadata(
                WATBatchMetadataEntity(
                    batchId = batchId,
                    downloadedAt = System.currentTimeMillis(),
                    wordCount = words.size,
                    version = version
                )
            )
            
            Log.d(TAG, "Downloaded batch $batchId: ${words.size} words")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download batch $batchId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get words for a test (60 words, smart selection)
     */
    suspend fun getWordsForTest(count: Int = 60): Result<List<WATWord>> {
        return try {
            Log.d(TAG, "Getting $count words for test")
            
            // Check if cache needs refresh
            val currentCount = dao.getTotalWordCount()
            if (currentCount < MIN_CACHE_SIZE) {
                Log.w(TAG, "Cache below minimum ($currentCount < $MIN_CACHE_SIZE), syncing...")
                initialSync().getOrThrow()
            }
            
            // Get least used words to ensure variety
            val cachedWords = dao.getLeastUsedWords(count)
            
            if (cachedWords.isEmpty()) {
                throw Exception("No words in cache")
            }
            
            // Mark as used
            dao.markWordsAsUsed(cachedWords.map { it.id })
            
            // Convert to domain model
            val words = cachedWords.map { entity ->
                WATWord(
                    id = entity.id,
                    word = entity.word,
                    sequenceNumber = entity.sequenceNumber,
                    timeAllowedSeconds = entity.timeAllowedSeconds
                )
            }
            
            Log.d(TAG, "Retrieved ${words.size} words for test")
            Result.success(words)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get words for test", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get cache status for diagnostics
     */
    suspend fun getCacheStatus(): WATCacheStatus {
        return try {
            val totalWords = dao.getTotalWordCount()
            val batches = dao.getTotalBatchCount()
            val batchMetadata = dao.getAllBatchMetadata()
            val lastSyncTime = batchMetadata.maxOfOrNull { it.downloadedAt }
            
            // Get category distribution (if available)
            val leadershipCount = try { dao.getWordCountByCategory("leadership") } catch (e: Exception) { 0 }
            val moralValuesCount = try { dao.getWordCountByCategory("moral_values") } catch (e: Exception) { 0 }
            val militaryVirtuesCount = try { dao.getWordCountByCategory("military_virtues") } catch (e: Exception) { 0 }
            val personalityCount = try { dao.getWordCountByCategory("personality") } catch (e: Exception) { 0 }
            
            WATCacheStatus(
                cachedWords = totalWords,
                batchesDownloaded = batches,
                lastSyncTime = lastSyncTime,
                leadershipCount = leadershipCount,
                moralValuesCount = moralValuesCount,
                militaryVirtuesCount = militaryVirtuesCount,
                personalityCount = personalityCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache status", e)
            WATCacheStatus()
        }
    }
    
    /**
     * Clear all cache (for debugging/testing)
     */
    suspend fun clearCache() {
        try {
            Log.d(TAG, "Clearing cache...")
            dao.clearAllWords()
            Log.d(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }
}

/**
 * WAT cache status for diagnostics
 */
data class WATCacheStatus(
    val cachedWords: Int = 0,
    val batchesDownloaded: Int = 0,
    val lastSyncTime: Long? = null,
    val leadershipCount: Int = 0,
    val moralValuesCount: Int = 0,
    val militaryVirtuesCount: Int = 0,
    val personalityCount: Int = 0
)

