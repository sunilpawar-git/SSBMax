package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity
import com.ssbmax.core.domain.model.CacheStatus
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.core.domain.model.OIRQuestionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local caching of OIR questions from Firestore.
 *
 * Responsibilities (single-responsibility split):
 *  - [OIRQuestionCacheManager] — sync / download / cache lifecycle operations
 *  - [OIRQuestionSelector]     — question selection & type-distribution logic
 *
 * Caching strategy:
 *   Phase 1 (blocking): batches 1-4 downloaded on first launch (enough for first test)
 *   Phase 2 (background): batches 5-20 downloaded while user takes the first test
 *   Cost: 20 Firestore reads on first install, 0 on subsequent launches.
 */
@Singleton
class OIRQuestionCacheManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cacheDao: OIRQuestionCacheDao,
    private val gson: Gson,
    private val selector: OIRQuestionSelector
) {
    companion object {
        private const val TAG                  = "OIRCacheManager"
        private const val FIRESTORE_COLLECTION = "test_content"
        private const val FIRESTORE_OIR_DOC    = "oir"
        private const val FIRESTORE_BATCHES    = "batches"
        private val PHASE_1_BATCH_RANGE        = 1..4
        private val PHASE_2_BATCH_RANGE        = 5..20
    }

    /**
     * Two-phase initial sync.
     * Phase 1 (blocking): batches 1-4 — returns once phase-1 batch metadata is complete.
     * Phase 2 (background): batches 5-20 — downloads while user is already in the test.
     *
     * Skip condition uses per-batch metadata (not total question count) so a single corrupt row
     * cannot prevent the sync from being skipped on subsequent launches.
     */
    suspend fun initialSync(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting initial sync...")
            val phase1Complete = PHASE_1_BATCH_RANGE.all { i ->
                cacheDao.isBatchDownloaded("batch_pdf_%03d".format(i))
            }
            if (phase1Complete) {
                Log.d(TAG, "Phase 1 batches already complete, skipping initial sync")
                return Result.success(Unit)
            }

            // Phase 1: blocking
            for (i in PHASE_1_BATCH_RANGE) {
                val batchId = "batch_pdf_%03d".format(i)
                downloadBatch(batchId).getOrElse { e ->
                    Log.w(TAG, "Phase 1: failed to download $batchId: ${e.message}")
                }
            }
            Log.d(TAG, "Initial sync phase 1 complete: batches 1-4 ready for first test")

            // Phase 2: background
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                for (i in PHASE_2_BATCH_RANGE) {
                    val batchId = "batch_pdf_%03d".format(i)
                    downloadBatch(batchId).getOrElse { e ->
                        Log.w(TAG, "Phase 2: failed to download $batchId: ${e.message}")
                    }
                }
                Log.d(TAG, "Initial sync phase 2 complete: all 20 batches ready")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Initial sync failed", e)
            Result.failure(e)
        }
    }

    /** Download a single batch from Firestore and persist to Room. */
    @Suppress("UNCHECKED_CAST")
    suspend fun downloadBatch(batchId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Downloading batch: $batchId")
            if (cacheDao.isBatchDownloaded(batchId)) {
                Log.d(TAG, "Batch $batchId already downloaded")
                return Result.success(Unit)
            }

            val batchDoc = firestore.collection(FIRESTORE_COLLECTION)
                .document(FIRESTORE_OIR_DOC)
                .collection(FIRESTORE_BATCHES)
                .document(batchId)
                .get().await()

            if (!batchDoc.exists()) throw Exception("Batch $batchId not found in Firestore")
            val data = batchDoc.data ?: throw Exception("Batch data is null")
            val questions = data["questions"] as? List<Map<String, Any>>
                ?: throw Exception("Questions field missing or invalid")

            val questionCount = (data["totalQuestions"] as? Long)
                ?: questions.size.toLong()
            val version       = data["version"] as? String ?: "1.0.0"

            val entities = questions.mapIndexed { index, map -> selector.toEntity(map, batchId, index) }
            val timestamp = System.currentTimeMillis()
            cacheDao.insertQuestions(entities)
            cacheDao.insertBatchMetadata(
                OIRBatchMetadataEntity(
                    batchId       = batchId,
                    downloadedAt  = timestamp,
                    questionCount = questionCount.toInt(),
                    version       = version
                )
            )
            Log.d(TAG, "Downloaded and cached $batchId: ${entities.size} questions")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download batch $batchId", e)
            Result.failure(e)
        }
    }

    /** Delegate question selection to [OIRQuestionSelector]. */
    suspend fun getTestQuestions(count: Int = 50, difficulty: String? = null): Result<List<OIRQuestion>> {
        val cachedCount = cacheDao.getCachedQuestionCount()
        if (cachedCount < count) {
            Log.w(TAG, "Not enough questions cached ($cachedCount < $count), triggering sync")
            initialSync()
        }
        return selector.selectQuestions(count, difficulty)
    }

    suspend fun markQuestionsUsed(questionIds: List<String>) {
        try {
            cacheDao.markQuestionsUsed(questionIds, System.currentTimeMillis())
            Log.d(TAG, "Marked ${questionIds.size} questions as used")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark questions as used", e)
        }
    }

    suspend fun getCacheStatus(): CacheStatus {
        return try {
            val count   = cacheDao.getCachedQuestionCount()
            val batches = cacheDao.getAllBatchMetadata()
            CacheStatus(
                cachedQuestions   = count,
                batchesDownloaded = batches.size,
                lastSyncTime      = batches.maxOfOrNull { it.downloadedAt },
                verbalCount       = cacheDao.getQuestionCountByType(OIRQuestionType.VERBAL_REASONING.name),
                nonVerbalCount    = cacheDao.getQuestionCountByType(OIRQuestionType.NON_VERBAL_REASONING.name),
                numericalCount    = cacheDao.getQuestionCountByType(OIRQuestionType.NUMERICAL_ABILITY.name),
                spatialCount      = cacheDao.getQuestionCountByType(OIRQuestionType.SPATIAL_REASONING.name)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache status", e)
            CacheStatus(0, 0, null, 0, 0, 0, 0)
        }
    }

    suspend fun clearCache(): Result<Unit> {
        return try {
            cacheDao.deleteAllQuestions()
            cacheDao.deleteAllBatchMetadata()
            Log.d(TAG, "Cache cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
            Result.failure(e)
        }
    }
}
