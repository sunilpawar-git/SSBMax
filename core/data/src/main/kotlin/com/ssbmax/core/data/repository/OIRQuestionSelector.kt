package com.ssbmax.core.data.repository

import android.util.Log
import com.google.gson.Gson
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.domain.model.OIROption
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.core.domain.model.OIRQuestionDistribution
import com.ssbmax.core.domain.model.OIRQuestionType
import com.ssbmax.core.domain.model.QuestionDifficulty
import com.ssbmax.core.domain.validation.OIRQuestionValidator
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OIRQuestionSelector"

/**
 * Selects 50 questions from the local Room cache, maintaining the required
 * type distribution defined once in [OIRQuestionDistribution] (V40 / NV40 / N20).
 *
 * Extracted from [OIRQuestionCacheManager] to respect the 300-line file limit
 * and give this concern a clear single responsibility.
 */
@Singleton
class OIRQuestionSelector @Inject constructor(
    private val cacheDao: OIRQuestionCacheDao,
    private val gson: Gson
) {
    companion object {
        private const val UNUSED_THRESHOLD_DAYS = 7
    }

    /**
     * Returns [count] questions from Room, maintaining type distribution.
     * Pass [difficulty] to filter (null = any difficulty).
     */
    suspend fun selectQuestions(
        count: Int = 50,
        difficulty: String? = null
    ): Result<List<OIRQuestion>> = try {
        val diffStr = difficulty?.let { " (difficulty: $it)" } ?: ""
        Log.d(TAG, "Generating test with $count questions$diffStr")

        val targets = OIRQuestionDistribution.counts(count)
        Log.d(TAG, "Distribution: " + targets.entries.joinToString { "${it.key}=${it.value}" })

        val unusedThreshold = System.currentTimeMillis() - (UNUSED_THRESHOLD_DAYS * 24 * 60 * 60 * 1000L)

        // Selected questions per type (mutable so we can top-up on shortage).
        val selected = targets.mapValues { (type, target) ->
            fetchByType(type.name, unusedThreshold, target, difficulty)
        }.toMutableMap()

        val currentTotal = selected.values.sumOf { it.size }
        if (currentTotal < count) {
            var remaining = count - currentTotal
            Log.d(TAG, "⚠️ Short by $remaining questions, applying smart redistribution across live types...")

            // Pull extra from any type that met its target (i.e. has more in the pool).
            for ((type, target) in targets) {
                if (remaining <= 0) break
                if (selected.getValue(type).size < target) continue // this type is itself short — nothing extra to give
                val extra = fetchByType(type.name, unusedThreshold, target + remaining, difficulty)
                val gained = extra.size - selected.getValue(type).size
                if (gained > 0) {
                    selected[type] = extra
                    remaining -= gained
                    Log.d(TAG, "✓ Added $gained extra ${type.name} questions")
                }
            }

            Log.d(TAG, "✅ Smart redistribution complete: ${selected.values.sumOf { it.size }}/$count questions")
        }

        val all = selected.values.flatten().shuffled()
        Log.d(TAG, "Selected ${all.size} questions")
        Result.success(all.map { toDomain(it) })

    } catch (e: Exception) {
        Log.e(TAG, "Failed to get test questions", e)
        Result.failure(e)
    }

    /**
     * Returns up to [count] STRUCTURALLY-VALID questions of [type]. We over-fetch
     * (×3) and drop anything the domain [OIRQuestionValidator] rejects, so duds
     * still present in the pool (e.g. the legacy optionless fill-in-the-blank
     * questions) are skipped at selection-time rather than reaching the test and
     * being filtered out downstream — keeping the assembled test at full count.
     * The validator stays the single source of truth for validity; this enforces
     * it at selection. Duds are not deleted, so they re-enter automatically once a
     * future change makes them valid.
     */
    private suspend fun fetchByType(
        type: String,
        unusedThreshold: Long,
        count: Int,
        difficulty: String?
    ): List<CachedOIRQuestionEntity> {
        var questions = filterValid(
            applyDifficulty(cacheDao.getUnusedQuestionsByType(type, unusedThreshold, count * 3), difficulty)
        )
        if (questions.size < count) {
            Log.d(TAG, "Not enough unused valid $type (${questions.size}/$count), fetching any")
            questions = filterValid(applyDifficulty(cacheDao.getQuestionsByType(type, count * 3), difficulty))
        }
        return questions.take(count)
    }

    /** Prefer the requested difficulty, but fall back to any difficulty rather than starve the test. */
    private fun applyDifficulty(
        questions: List<CachedOIRQuestionEntity>,
        difficulty: String?
    ): List<CachedOIRQuestionEntity> {
        if (difficulty == null) return questions
        val filtered = questions.filter { it.difficulty == difficulty }
        return if (filtered.isNotEmpty()) filtered else questions
    }

    /** Keep only questions that pass the domain validator (drops duds at selection). */
    private fun filterValid(questions: List<CachedOIRQuestionEntity>): List<CachedOIRQuestionEntity> =
        questions.filter { OIRQuestionValidator.validate(toDomain(it)).isValid }

    fun toEntity(
        questionMap: Map<String, Any>,
        batchId: String,
        index: Int
    ): CachedOIRQuestionEntity {
        @Suppress("UNCHECKED_CAST")
        val correctAnswerIdsList = questionMap["correctAnswerIds"] as? List<String>
        return CachedOIRQuestionEntity(
            id             = questionMap["id"] as? String ?: "oir_q_${System.currentTimeMillis()}_$index",
            questionNumber = (questionMap["questionNumber"] as? Long)?.toInt() ?: index + 1,
            type           = questionMap["type"] as? String ?: "VERBAL_REASONING",
            subtype        = questionMap["subtype"] as? String,
            questionText   = questionMap["questionText"] as? String ?: "",
            optionsJson    = gson.toJson(questionMap["options"]),
            correctAnswerId = questionMap["correctAnswerId"] as? String ?: "",
            explanation    = questionMap["explanation"] as? String ?: "",
            questionImageUrl = questionMap["questionImageUrl"] as? String,
            difficulty     = questionMap["difficulty"] as? String ?: "MEDIUM",
            tags           = (questionMap["tags"] as? List<String>)?.joinToString(",") ?: "",
            batchId        = batchId,
            cachedAt       = System.currentTimeMillis(),
            lastUsed       = null,
            usageCount     = 0,
            correctAnswerIds = correctAnswerIdsList?.let { gson.toJson(it) }
        )
    }

    internal fun toDomain(entity: CachedOIRQuestionEntity): OIRQuestion {
        val type = object : com.google.gson.reflect.TypeToken<List<Map<String, String>>>() {}.type
        val options = gson.fromJson<List<Map<String, String>>>(entity.optionsJson, type)
            .map { OIROption(id = it["id"] ?: "", text = it["text"] ?: "", imageUrl = it["imageUrl"]) }
        return OIRQuestion(
            id              = entity.id,
            questionNumber  = entity.questionNumber,
            type            = OIRQuestionType.valueOf(entity.type),
            questionText    = entity.questionText,
            options         = options,
            correctAnswerId = entity.correctAnswerId,
            correctAnswerIds = entity.correctAnswerIds
                ?.let { gson.fromJson(it, Array<String>::class.java).toList() }
                ?: emptyList(),
            explanation     = entity.explanation,
            difficulty      = QuestionDifficulty.valueOf(entity.difficulty),
            timeSeconds     = 60,
            questionImageUrl = entity.questionImageUrl?.let { normalizeStorageUrl(it) }
        )
    }

    /** Normalize Firebase Storage gs:// URLs to HTTPS (same convention as TAT/PPDT). */
    private fun normalizeStorageUrl(raw: String): String =
        if (raw.startsWith("gs://")) raw.replaceFirst("gs://", "https://storage.googleapis.com/") else raw
}
