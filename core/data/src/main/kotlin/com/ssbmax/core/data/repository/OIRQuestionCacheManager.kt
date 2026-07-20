package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity
import com.ssbmax.core.data.local.entity.OIRSyncMetadataEntity
import com.ssbmax.shared.domain.model.CacheStatus
import com.ssbmax.shared.domain.model.OIRQuestion
import com.ssbmax.shared.domain.model.OIRQuestionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Manages local caching of OIR questions from Firestore.
 *
 * Responsibilities (single-responsibility split):
 *  - [OIRQuestionCacheManager] — sync / download / cache lifecycle operations
 *  - [OIRQuestionSelector]     — question selection & type-distribution logic
 *
 * Caching strategy (content-version reconciliation; Firestore is SSOT):
 *   - Read `test_content/oir/meta/config` → { contentVersion, batchCount }.
 *   - If the remote contentVersion differs from the locally-stored one, clear and
 *     re-download every batch (existing installs self-heal to current content).
 *   - Otherwise download only the batches still missing (idempotent fast path).
 *   Phase 1 (blocking): batches 1-4 — enough for the first test.
 *   Phase 2 (background): batches 5..batchCount — downloaded while the user takes the test.
 */
class OIRQuestionCacheManager(
    private val firestore: FirebaseFirestore,
    private val cacheDao: OIRQuestionCacheDao,
    private val gson: Gson,
    private val selector: OIRQuestionSelector,
    private val backgroundScope: CoroutineScope
) {
    companion object {
        private const val TAG                  = "OIRCacheManager"
        private const val FIRESTORE_COLLECTION = "test_content"
        private const val FIRESTORE_OIR_DOC    = "oir"
        private const val FIRESTORE_BATCHES    = "batches"
        private const val FIRESTORE_META       = "meta"
        private const val FIRESTORE_META_CONFIG = "config"
        private const val PHASE_1_LAST         = 4
        /** Used only when the meta doc is absent (legacy/rollout fallback). */
        private const val LEGACY_BATCH_COUNT   = 20
    }

    /** Remote OIR content config, from `test_content/oir/meta/config`. */
    internal data class MetaConfig(val contentVersion: Int?, val batchCount: Int)

    private fun batchId(i: Int) = "batch_pdf_%03d".format(i)

    /**
     * Content-version-aware initial sync.
     *  1. Read the meta doc.
     *  2. If remote contentVersion != local → reconcile (clear + re-download all).
     *  3. Else if some batches are missing → download just those (idempotent).
     *  4. Else skip.
     * Phase 1 (1..4) is blocking for first-test latency; the rest downloads in the background.
     */
    suspend fun initialSync(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting initial sync...")
            val meta = fetchMetaConfig()
            val localVersion = cacheDao.getSyncMetadata()?.contentVersion
            val needsReconcile = meta.contentVersion != null && meta.contentVersion != localVersion

            if (needsReconcile) {
                Log.d(TAG, "Content version changed (local=$localVersion, remote=${meta.contentVersion}) — reconciling")
                cacheDao.deleteAllQuestions()
                cacheDao.deleteAllBatchMetadata()
            } else {
                val allPresent = (1..meta.batchCount).all { cacheDao.isBatchDownloaded(batchId(it)) }
                if (allPresent) {
                    Log.d(TAG, "All ${meta.batchCount} batches present at version $localVersion, skipping sync")
                    return Result.success(Unit)
                }
                Log.d(TAG, "Some batches missing, topping up (idempotent)")
            }

            // Phase 1: blocking — enough batches for the first test.
            for (i in 1..minOf(PHASE_1_LAST, meta.batchCount)) {
                downloadBatch(batchId(i)).getOrElse { e -> Log.w(TAG, "Phase 1: $i failed: ${e.message}") }
            }
            // Persist the version now; any phase-2 gaps are caught by the missing-batch top-up next launch.
            meta.contentVersion?.let {
                cacheDao.upsertSyncMetadata(OIRSyncMetadataEntity(contentVersion = it, lastSyncAt = System.currentTimeMillis()))
            }
            Log.d(TAG, "Initial sync phase 1 complete (batches 1..${minOf(PHASE_1_LAST, meta.batchCount)})")

            // Phase 2: background — the remaining batches up to batchCount.
            backgroundScope.launch {
                for (i in (PHASE_1_LAST + 1)..meta.batchCount) {
                    downloadBatch(batchId(i)).getOrElse { e -> Log.w(TAG, "Phase 2: $i failed: ${e.message}") }
                }
                Log.d(TAG, "Initial sync phase 2 complete: all ${meta.batchCount} batches ready")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Initial sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Read `test_content/oir/meta/config`. Missing/unreadable doc → legacy fallback
     * (no version, [LEGACY_BATCH_COUNT] batches) so the app still works before the meta
     * doc is published.
     */
    internal suspend fun fetchMetaConfig(): MetaConfig {
        return try {
            val doc = firestore.collection(FIRESTORE_COLLECTION)
                .document(FIRESTORE_OIR_DOC)
                .collection(FIRESTORE_META)
                .document(FIRESTORE_META_CONFIG)
                .get().await()
            if (!doc.exists()) {
                Log.w(TAG, "Meta config absent — using legacy fallback ($LEGACY_BATCH_COUNT batches)")
                return MetaConfig(contentVersion = null, batchCount = LEGACY_BATCH_COUNT)
            }
            MetaConfig(
                contentVersion = (doc.getLong("contentVersion"))?.toInt(),
                batchCount = (doc.getLong("batchCount"))?.toInt() ?: LEGACY_BATCH_COUNT
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read meta config, using legacy fallback: ${e.message}")
            MetaConfig(contentVersion = null, batchCount = LEGACY_BATCH_COUNT)
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
