package com.ssbmax.shared.data.repository

import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.domain.model.CacheStatus
import com.ssbmax.shared.domain.model.OIRQuestionType
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * SQLDelight-backed port of the Android `OIRQuestionCacheManager` (Room-backed
 * via `OIRQuestionCacheDao`). Fourteenth (largest) Phase 2 Room DAO+manager
 * slice, spanning 3 tables (question cache, batch metadata, single-row
 * content-version sync metadata).
 *
 * Kept split into two classes -- this manager (sync/download/lifecycle) and
 * [GitLiveOIRQuestionSelector] (selection/distribution logic) -- mirroring the
 * Android original's own SRP split, and needed here to respect this repo's
 * 300-line-per-file limit; a merged class combining Firestore parsing +
 * selection + status mapping would exceed it.
 *
 * Same documented Firestore-decode deviation as the 8th-13th slices: a typed
 * `@Serializable` DTO ([OirBatchDocDto]) replaces Android's raw
 * `List<Map<String, Any>>` cast for the questions array; [selector] still
 * receives a `Map<String, Any?>`-shaped view per question ([toEntityMap]) so
 * [GitLiveOIRQuestionSelector.toEntity] can stay signature-compatible with the
 * Android original's `Map`-based `toEntity`.
 *
 * One behavior difference, documented rather than silently dropped: Android's
 * phase-2 background batch download used a Hilt-injected `@ApplicationScope`
 * `CoroutineScope` that outlives this manager. No such app-wide singleton
 * scope exists yet in `shared`'s Koin graph (Phase 3/4 scope per this repo's
 * DI-parity notes), so this port owns a private `SupervisorJob`-backed scope
 * instead. Functionally equivalent for a `single`-scoped Koin instance (which
 * lives for the app's process lifetime same as the Android singleton), but
 * revisit this if `GitLiveOIRQuestionCacheManager` ever becomes non-singleton.
 */
class GitLiveOIRQuestionCacheManager(
    private val database: SharedDatabase,
    private val selector: GitLiveOIRQuestionSelector
) {
    private val queries get() = database.sharedDatabaseQueries
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private companion object {
        const val FIRESTORE_COLLECTION = "test_content"
        const val FIRESTORE_OIR_DOC = "oir"
        const val FIRESTORE_BATCHES = "batches"
        const val FIRESTORE_META = "meta"
        const val FIRESTORE_META_CONFIG = "config"
        const val PHASE_1_LAST = 4
        const val LEGACY_BATCH_COUNT = 20
    }

    internal data class MetaConfig(val contentVersion: Int?, val batchCount: Int)

    private fun batchId(i: Int): String = "batch_pdf_" + i.toString().padStart(3, '0')

    /**
     * Content-version-aware initial sync: reconcile (clear + re-download) if the
     * remote contentVersion differs from local; otherwise top up missing batches.
     * Phase 1 (1..4) is blocking; the rest downloads in the background.
     */
    suspend fun initialSync(): Result<Unit> = try {
        val meta = fetchMetaConfig()
        val localVersion = queries.selectOIRSyncMetadata().executeAsOneOrNull()?.contentVersion?.toInt()
        val needsReconcile = meta.contentVersion != null && meta.contentVersion != localVersion

        var shouldSync = true
        if (needsReconcile) {
            queries.deleteAllOIRQuestions()
            queries.deleteAllOIRBatchMetadata()
        } else {
            val allPresent = (1..meta.batchCount).all { isBatchDownloaded(batchId(it)) }
            if (allPresent) {
                shouldSync = false
            }
        }

        if (shouldSync) {
            for (i in 1..minOf(PHASE_1_LAST, meta.batchCount)) {
                downloadBatch(batchId(i))
            }
            meta.contentVersion?.let {
                queries.upsertOIRSyncMetadata(contentVersion = it.toLong(), lastSyncAt = Clock.System.now().toEpochMilliseconds())
            }

            backgroundScope.launch {
                for (i in (PHASE_1_LAST + 1)..meta.batchCount) {
                    downloadBatch(batchId(i))
                }
            }
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun isBatchDownloaded(batchId: String): Boolean =
        queries.countOIRBatchMetadataById(batchId).executeAsOne() > 0

    /** Read `test_content/oir/meta/config`; missing/unreadable doc falls back to the legacy shape. */
    internal suspend fun fetchMetaConfig(): MetaConfig = try {
        val doc = Firebase.firestore.collection(FIRESTORE_COLLECTION)
            .document(FIRESTORE_OIR_DOC)
            .collection(FIRESTORE_META)
            .document(FIRESTORE_META_CONFIG)
            .get()
        if (!doc.exists) {
            MetaConfig(contentVersion = null, batchCount = LEGACY_BATCH_COUNT)
        } else {
            MetaConfig(
                contentVersion = doc.get<Long?>("contentVersion")?.toInt(),
                batchCount = doc.get<Long?>("batchCount")?.toInt() ?: LEGACY_BATCH_COUNT
            )
        }
    } catch (e: Exception) {
        MetaConfig(contentVersion = null, batchCount = LEGACY_BATCH_COUNT)
    }

    /** Download a single batch from Firestore and persist it. Idempotent -- returns early if already local. */
    suspend fun downloadBatch(batchId: String): Result<Unit> = try {
        if (isBatchDownloaded(batchId)) {
            Result.success(Unit)
        } else {
            val doc = Firebase.firestore.collection(FIRESTORE_COLLECTION)
                .document(FIRESTORE_OIR_DOC)
                .collection(FIRESTORE_BATCHES)
                .document(batchId)
                .get()

            if (!doc.exists) {
                Result.failure(Exception("Batch $batchId not found in Firestore"))
            } else {
                val batch = doc.data(OirBatchDocDto.serializer())
                val questionCount = batch.totalQuestions ?: batch.questions.size
                val version = batch.version ?: "1.0.0"
                val now = Clock.System.now().toEpochMilliseconds()

                val entities = batch.questions.mapIndexed { index, q -> selector.toEntity(q.toEntityMap(), batchId, index, now) }
                queries.transaction {
                    entities.forEach { e ->
                        queries.insertOrReplaceOIRQuestion(
                            id = e.id,
                            questionNumber = e.questionNumber,
                            type = e.type,
                            subtype = e.subtype,
                            questionText = e.questionText,
                            optionsJson = e.optionsJson,
                            correctAnswerId = e.correctAnswerId,
                            explanation = e.explanation,
                            questionImageUrl = e.questionImageUrl,
                            difficulty = e.difficulty,
                            tags = e.tags,
                            batchId = e.batchId,
                            cachedAt = e.cachedAt,
                            lastUsed = e.lastUsed,
                            usageCount = e.usageCount,
                            correctAnswerIds = e.correctAnswerIds
                        )
                    }
                    queries.insertOrReplaceOIRBatchMetadata(
                        batchId = batchId,
                        downloadedAt = now,
                        questionCount = questionCount.toLong(),
                        version = version
                    )
                }
                Result.success(Unit)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Delegates question selection to [GitLiveOIRQuestionSelector]; triggers a sync first if the cache is thin. */
    suspend fun getTestQuestions(count: Int = 50, difficulty: String? = null): Result<List<com.ssbmax.shared.domain.model.OIRQuestion>> {
        val cachedCount = queries.selectTotalOIRQuestionCount().executeAsOne()
        if (cachedCount < count) {
            initialSync()
        }
        return selector.selectQuestions(count, difficulty)
    }

    suspend fun markQuestionsUsed(questionIds: List<String>) {
        try {
            queries.markOIRQuestionsUsed(timestamp = Clock.System.now().toEpochMilliseconds(), questionIds = questionIds)
        } catch (_: Exception) {
            // Best-effort usage tracking -- matches Android original's swallow-and-log.
        }
    }

    suspend fun getCacheStatus(): CacheStatus = try {
        val count = queries.selectTotalOIRQuestionCount().executeAsOne()
        val batches = queries.selectAllOIRBatchMetadata().executeAsList()
        CacheStatus(
            cachedQuestions = count.toInt(),
            batchesDownloaded = batches.size,
            lastSyncTime = batches.maxOfOrNull { it.downloadedAt },
            verbalCount = queries.selectOIRQuestionCountByType(OIRQuestionType.VERBAL_REASONING.name).executeAsOne().toInt(),
            nonVerbalCount = queries.selectOIRQuestionCountByType(OIRQuestionType.NON_VERBAL_REASONING.name).executeAsOne().toInt(),
            numericalCount = queries.selectOIRQuestionCountByType(OIRQuestionType.NUMERICAL_ABILITY.name).executeAsOne().toInt(),
            spatialCount = queries.selectOIRQuestionCountByType(OIRQuestionType.SPATIAL_REASONING.name).executeAsOne().toInt()
        )
    } catch (e: Exception) {
        CacheStatus(0, 0, null, 0, 0, 0, 0)
    }

    suspend fun clearCache(): Result<Unit> = try {
        queries.deleteAllOIRQuestions()
        queries.deleteAllOIRBatchMetadata()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/** Firestore batch document shape for `test_content/oir/batches/{batchId}`. */
@Serializable
internal data class OirBatchDocDto(
    val questions: List<OirQuestionFirestoreDto> = emptyList(),
    val totalQuestions: Int? = null,
    val version: String? = null
)

@Serializable
internal data class OirQuestionFirestoreDto(
    val id: String? = null,
    val questionNumber: Int? = null,
    val type: String? = null,
    val subtype: String? = null,
    val questionText: String? = null,
    val options: List<OirOptionFirestoreDto>? = null,
    val correctAnswerId: String? = null,
    val correctAnswerIds: List<String>? = null,
    val explanation: String? = null,
    val questionImageUrl: String? = null,
    val difficulty: String? = null,
    val tags: List<String>? = null
)

@Serializable
internal data class OirOptionFirestoreDto(
    val id: String? = null,
    val text: String? = null,
    val imageUrl: String? = null
)

/**
 * Adapts the typed Firestore DTO back to the `Map<String, Any>`-ish shape
 * [GitLiveOIRQuestionSelector.toEntity] expects, so that helper stays
 * signature-compatible with the Android original's `Map`-based `toEntity`
 * (which parses Firestore's loosely-typed documents directly). Only the keys
 * `toEntity` actually reads are populated.
 */
internal fun OirQuestionFirestoreDto.toEntityMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "questionNumber" to questionNumber,
    "type" to type,
    "subtype" to subtype,
    "questionText" to questionText,
    "options" to options?.map { mapOf("id" to it.id, "text" to it.text, "imageUrl" to it.imageUrl) },
    "correctAnswerId" to correctAnswerId,
    "correctAnswerIds" to correctAnswerIds,
    "explanation" to explanation,
    "questionImageUrl" to questionImageUrl,
    "difficulty" to difficulty,
    "tags" to tags
)
