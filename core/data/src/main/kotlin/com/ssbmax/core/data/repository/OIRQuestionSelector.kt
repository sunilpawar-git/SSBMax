package com.ssbmax.core.data.repository

import android.util.Log
import com.google.gson.Gson
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.domain.model.OIROption
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.core.domain.model.OIRQuestionType
import com.ssbmax.core.domain.model.QuestionDifficulty
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OIRQuestionSelector"

/**
 * Selects 50 questions from the local Room cache, maintaining the required
 * V40 / NV40 / N15 / S5 type distribution.
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
        private const val VERBAL_RATIO      = 0.40f
        private const val NON_VERBAL_RATIO  = 0.40f
        private const val NUMERICAL_RATIO   = 0.15f
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

        val verbalCount   = (count * VERBAL_RATIO).toInt()
        val nonVerbalCount = (count * NON_VERBAL_RATIO).toInt()
        val numericalCount = (count * NUMERICAL_RATIO).toInt()
        val spatialCount  = count - verbalCount - nonVerbalCount - numericalCount

        Log.d(TAG, "Distribution: V=$verbalCount, NV=$nonVerbalCount, N=$numericalCount, S=$spatialCount")

        val unusedThreshold = System.currentTimeMillis() - (UNUSED_THRESHOLD_DAYS * 24 * 60 * 60 * 1000L)

        var verbal    = fetchByType(OIRQuestionType.VERBAL_REASONING.name,    unusedThreshold, verbalCount,    difficulty)
        var nonVerbal = fetchByType(OIRQuestionType.NON_VERBAL_REASONING.name, unusedThreshold, nonVerbalCount, difficulty)
        var numerical = fetchByType(OIRQuestionType.NUMERICAL_ABILITY.name,   unusedThreshold, numericalCount,  difficulty)
        var spatial   = fetchByType(OIRQuestionType.SPATIAL_REASONING.name,   unusedThreshold, spatialCount,    difficulty)

        val currentTotal = verbal.size + nonVerbal.size + numerical.size + spatial.size
        if (currentTotal < count) {
            val shortage = count - currentTotal
            Log.d(TAG, "⚠️ Short by $shortage questions, applying smart redistribution...")
            Log.d(TAG, "Shortages: V=${verbalCount - verbal.size}, NV=${nonVerbalCount - nonVerbal.size}, " +
                    "N=${numericalCount - numerical.size}, S=${spatialCount - spatial.size}")

            var remaining = shortage
            if (verbalCount - verbal.size <= 0 && remaining > 0) {
                val extra = fetchByType(OIRQuestionType.VERBAL_REASONING.name, unusedThreshold, verbalCount + remaining, difficulty)
                val gained = extra.size - verbal.size
                if (gained > 0) { verbal = extra; remaining -= gained; Log.d(TAG, "✓ Added $gained extra verbal questions") }
            }
            if (nonVerbalCount - nonVerbal.size <= 0 && remaining > 0) {
                val extra = fetchByType(OIRQuestionType.NON_VERBAL_REASONING.name, unusedThreshold, nonVerbalCount + remaining, difficulty)
                val gained = extra.size - nonVerbal.size
                if (gained > 0) { nonVerbal = extra; remaining -= gained; Log.d(TAG, "✓ Added $gained extra non-verbal questions") }
            }
            if (numericalCount - numerical.size <= 0 && remaining > 0) {
                val extra = fetchByType(OIRQuestionType.NUMERICAL_ABILITY.name, unusedThreshold, numericalCount + remaining, difficulty)
                val gained = extra.size - numerical.size
                if (gained > 0) { numerical = extra; remaining -= gained; Log.d(TAG, "✓ Added $gained extra numerical questions") }
            }
            if (spatialCount - spatial.size <= 0 && remaining > 0) {
                val extra = fetchByType(OIRQuestionType.SPATIAL_REASONING.name, unusedThreshold, spatialCount + remaining, difficulty)
                val gained = extra.size - spatial.size
                if (gained > 0) { spatial = extra; Log.d(TAG, "✓ Added $gained extra spatial questions") }
            }

            Log.d(TAG, "✅ Smart redistribution complete: ${verbal.size + nonVerbal.size + numerical.size + spatial.size}/$count questions")
        }

        val all = (verbal + nonVerbal + numerical + spatial).shuffled()
        Log.d(TAG, "Selected ${all.size} questions")
        Result.success(all.map { toDomain(it) })

    } catch (e: Exception) {
        Log.e(TAG, "Failed to get test questions", e)
        Result.failure(e)
    }

    private suspend fun fetchByType(
        type: String,
        unusedThreshold: Long,
        count: Int,
        difficulty: String?
    ): List<CachedOIRQuestionEntity> {
        var questions = cacheDao.getUnusedQuestionsByType(type, unusedThreshold, count * 2)
        if (difficulty != null) {
            val filtered = questions.filter { it.difficulty == difficulty }
            questions = if (filtered.isNotEmpty()) filtered else questions
            Log.d(TAG, "Filtered to $difficulty: ${questions.size} $type questions")
        }
        if (questions.size < count) {
            Log.d(TAG, "Not enough unused $type questions (${questions.size}/$count), fetching any")
            var all = cacheDao.getQuestionsByType(type, count * 2)
            if (difficulty != null) {
                val filtered = all.filter { it.difficulty == difficulty }
                all = if (filtered.isNotEmpty()) filtered else {
                    Log.d(TAG, "No $type questions with difficulty=$difficulty, using any difficulty")
                    all
                }
            }
            questions = all
        }
        return questions.take(count)
    }

    fun toEntity(
        questionMap: Map<String, Any>,
        batchId: String,
        index: Int
    ): CachedOIRQuestionEntity {
        @Suppress("UNCHECKED_CAST")
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
            usageCount     = 0
        )
    }

    private fun toDomain(entity: CachedOIRQuestionEntity): OIRQuestion {
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
