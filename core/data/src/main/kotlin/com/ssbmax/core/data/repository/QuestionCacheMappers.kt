package com.ssbmax.core.data.repository

import android.util.Log
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionCacheEntry
import com.ssbmax.core.domain.model.interview.QuestionCacheType
import com.ssbmax.core.domain.model.interview.QuestionSource
import java.time.Instant

/**
 * Mappers for converting between domain models and Firestore data structures
 * for the question cache system.
 */
internal object QuestionCacheMappers {

    private const val TAG = "QuestionCacheMappers"

    // Field constants
    const val FIELD_QUESTION = "question"
    const val FIELD_CACHE_KEY = "cacheKey"
    const val FIELD_CACHE_TYPE = "cacheType"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_EXPIRES_AT = "expiresAt"
    const val FIELD_USAGE_COUNT = "usageCount"
    const val FIELD_LAST_USED_AT = "lastUsedAt"
    const val FIELD_TARGET_OLQS = "targetOLQs"
    const val FIELD_DIFFICULTY = "difficulty"

    /** Convert cache entry to Firestore map */
    fun cacheEntryToMap(entry: QuestionCacheEntry): Map<String, Any?> = mapOf(
        "id" to entry.id,
        FIELD_QUESTION to questionToMap(entry.question),
        FIELD_CACHE_KEY to entry.cacheKey,
        FIELD_CACHE_TYPE to entry.cacheType.name,
        FIELD_CREATED_AT to entry.createdAt.toEpochMilli(),
        FIELD_USAGE_COUNT to entry.usageCount,
        FIELD_LAST_USED_AT to entry.lastUsedAt?.toEpochMilli(),
        FIELD_EXPIRES_AT to entry.expiresAt?.toEpochMilli()
    )

    /** Convert question to Firestore map */
    fun questionToMap(question: InterviewQuestion): Map<String, Any?> = mapOf(
        "id" to question.id,
        "questionText" to question.questionText,
        FIELD_TARGET_OLQS to question.expectedOLQs.map { it.name },
        "context" to question.context,
        "source" to question.source.name
    )

    /** Map Firestore data to cache entry */
    fun mapToCacheEntry(data: Map<String, Any>): QuestionCacheEntry? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val questionData = data[FIELD_QUESTION] as? Map<String, Any> ?: return null
            val question = mapToQuestion(questionData) ?: return null

            QuestionCacheEntry(
                id = data["id"] as? String ?: return null,
                question = question,
                cacheKey = data[FIELD_CACHE_KEY] as? String ?: return null,
                cacheType = QuestionCacheType.valueOf(data[FIELD_CACHE_TYPE] as? String ?: return null),
                createdAt = Instant.ofEpochMilli(data[FIELD_CREATED_AT] as? Long ?: return null),
                usageCount = (data[FIELD_USAGE_COUNT] as? Long)?.toInt() ?: 0,
                lastUsedAt = (data[FIELD_LAST_USED_AT] as? Long)?.let { Instant.ofEpochMilli(it) },
                expiresAt = (data[FIELD_EXPIRES_AT] as? Long)?.let { Instant.ofEpochMilli(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to map cache entry", e)
            null
        }
    }

    /** Map Firestore data to interview question */
    fun mapToQuestion(data: Map<String, Any>): InterviewQuestion? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val targetOLQNames = data[FIELD_TARGET_OLQS] as? List<String> ?: return null
            val expectedOLQs = targetOLQNames.mapNotNull { name -> OLQ.entries.find { it.name == name } }

            val sourceName = data["source"] as? String ?: "GENERIC_POOL"
            val source = try {
                QuestionSource.valueOf(sourceName)
            } catch (e: Exception) {
                QuestionSource.GENERIC_POOL
            }

            // Support both 'questionText' (preferred) and 'text' (legacy) field names
            val questionText = data["questionText"] as? String
                ?: data["text"] as? String
                ?: return null

            InterviewQuestion(
                id = data["id"] as? String ?: return null,
                questionText = questionText,
                expectedOLQs = expectedOLQs,
                context = data["context"] as? String,
                source = source
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to map question", e)
            null
        }
    }
}

