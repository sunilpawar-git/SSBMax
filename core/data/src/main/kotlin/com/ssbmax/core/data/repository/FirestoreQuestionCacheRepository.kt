package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.interview.QuestionCacheEntry
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import com.ssbmax.core.domain.model.interview.QuestionCacheStats
import com.ssbmax.core.domain.model.interview.QuestionCacheType
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of question cache repository
 *
 * **Collections**:
 * - `question_cache`: Cached PIQ-based questions
 * - `generic_questions`: Pre-curated generic question pool
 * - `question_usage`: Usage tracking for analytics
 *
 * **Indexes Required** (create in Firebase Console):
 * - question_cache: (cacheKey ASC, expiresAt ASC, usageCount ASC)
 * - generic_questions: (targetOLQs ARRAY, difficulty ASC)
 * - question_usage: (questionId ASC, sessionId ASC, usedAt DESC)
 */
@Singleton
class FirestoreQuestionCacheRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : QuestionCacheRepository {

    companion object {
        private const val TAG = "QuestionCacheRepo"
        private const val COLLECTION_CACHE = "question_cache"
        private const val COLLECTION_GENERIC = "generic_questions"
        private const val COLLECTION_USAGE = "question_usage"

        // Field names
        private const val FIELD_CACHE_KEY = "cacheKey"
        private const val FIELD_CACHE_TYPE = "cacheType"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_EXPIRES_AT = "expiresAt"
        private const val FIELD_USAGE_COUNT = "usageCount"
        private const val FIELD_LAST_USED_AT = "lastUsedAt"
        private const val FIELD_QUESTION = "question"
        private const val FIELD_TARGET_OLQS = "targetOLQs"
        private const val FIELD_DIFFICULTY = "difficulty"
    }

    private val cacheCollection = firestore.collection(COLLECTION_CACHE)
    private val genericCollection = firestore.collection(COLLECTION_GENERIC)
    private val usageCollection = firestore.collection(COLLECTION_USAGE)

    override suspend fun cachePIQQuestions(
        piqSnapshotId: String,
        questions: List<InterviewQuestion>,
        expirationDays: Int
    ): Result<Unit> {
        return try {
            val secondsPerDay = 86400L // 24 hours * 60 minutes * 60 seconds
            val expiresAt = Instant.now().plusSeconds(expirationDays * secondsPerDay)
            val batch = firestore.batch()

            questions.forEach { question ->
                val cacheEntry = QuestionCacheEntry(
                    id = UUID.randomUUID().toString(),
                    question = question,
                    cacheKey = piqSnapshotId,
                    cacheType = QuestionCacheType.PIQ_BASED,
                    createdAt = Instant.now(),
                    usageCount = 0,
                    lastUsedAt = null,
                    expiresAt = expiresAt
                )

                val docRef = cacheCollection.document(cacheEntry.id)
                batch.set(docRef, cacheEntryToMap(cacheEntry))
            }

            batch.commit().await()
            Log.d(TAG, "Cached ${questions.size} PIQ-based questions for $piqSnapshotId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache PIQ questions", e)
            Result.failure(e)
        }
    }

    override suspend fun getPIQQuestions(
        piqSnapshotId: String,
        limit: Int,
        excludeUsed: Boolean
    ): Result<List<InterviewQuestion>> {
        return try {
            var query = cacheCollection
                .whereEqualTo(FIELD_CACHE_KEY, piqSnapshotId)
                .whereEqualTo(FIELD_CACHE_TYPE, QuestionCacheType.PIQ_BASED.name)
                .whereGreaterThan(FIELD_EXPIRES_AT, Instant.now().toEpochMilli())
                .orderBy(FIELD_EXPIRES_AT, Query.Direction.DESCENDING)

            if (excludeUsed) {
                query = query.orderBy(FIELD_USAGE_COUNT, Query.Direction.ASCENDING)
            }

            val snapshot = query.limit(limit.toLong()).get().await()

            val questions = snapshot.documents.mapNotNull { doc ->
                mapToCacheEntry(doc.data ?: return@mapNotNull null)?.question
            }

            Log.d(TAG, "Retrieved ${questions.size} PIQ-based questions from cache")
            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get PIQ questions", e)
            Result.failure(e)
        }
    }

    override suspend fun getGenericQuestions(
        targetOLQs: List<OLQ>?,
        difficulty: Int?,
        limit: Int,
        excludeUsed: Boolean
    ): Result<List<InterviewQuestion>> {
        return try {
            var query: Query = genericCollection

            if (difficulty != null) {
                query = query.whereEqualTo(FIELD_DIFFICULTY, difficulty)
            }

            if (excludeUsed) {
                query = query.orderBy(FIELD_USAGE_COUNT, Query.Direction.ASCENDING)
            }

            val snapshot = query.limit(limit.toLong()).get().await()

            var questions = snapshot.documents.mapNotNull { doc ->
                mapToQuestion(doc.data ?: return@mapNotNull null)
            }

            // Filter by target OLQs if specified (client-side since Firestore doesn't support complex array queries)
            if (targetOLQs != null) {
                questions = questions.filter { question ->
                    question.expectedOLQs.any { it in targetOLQs }
                }
            }

            Log.d(TAG, "Retrieved ${questions.size} generic questions from pool")
            Result.success(questions.take(limit))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get generic questions", e)
            Result.failure(e)
        }
    }

    override suspend fun markQuestionUsed(
        questionId: String,
        sessionId: String
    ): Result<Unit> {
        return try {
            // Update usage count in cache
            val cacheQuery = cacheCollection
                .whereEqualTo("question.id", questionId)
                .limit(1)
                .get()
                .await()

            if (!cacheQuery.isEmpty) {
                val docRef = cacheQuery.documents[0].reference
                docRef.update(
                    mapOf(
                        FIELD_USAGE_COUNT to com.google.firebase.firestore.FieldValue.increment(1),
                        FIELD_LAST_USED_AT to Instant.now().toEpochMilli()
                    )
                ).await()
            }

            // Also update generic questions if applicable
            val genericQuery = genericCollection
                .whereEqualTo("id", questionId)
                .limit(1)
                .get()
                .await()

            if (!genericQuery.isEmpty) {
                val docRef = genericQuery.documents[0].reference
                docRef.update(
                    mapOf(
                        FIELD_USAGE_COUNT to com.google.firebase.firestore.FieldValue.increment(1),
                        FIELD_LAST_USED_AT to Instant.now().toEpochMilli()
                    )
                ).await()
            }

            // Track usage for analytics
            usageCollection.add(
                mapOf(
                    "questionId" to questionId,
                    "sessionId" to sessionId,
                    "usedAt" to Instant.now().toEpochMilli()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark question as used", e)
            Result.failure(e)
        }
    }

    override suspend fun cleanupExpired(): Result<Int> {
        return try {
            val now = Instant.now().toEpochMilli()
            val expiredQuery = cacheCollection
                .whereLessThan(FIELD_EXPIRES_AT, now)
                .get()
                .await()

            val batch = firestore.batch()
            expiredQuery.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()
            val deletedCount = expiredQuery.documents.size

            Log.d(TAG, "Cleaned up $deletedCount expired cache entries")
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup expired cache", e)
            Result.failure(e)
        }
    }

    override suspend fun getCacheStats(userId: String?): Result<QuestionCacheStats> {
        return try {
            val totalCached = cacheCollection.get().await().size()
            val piqBased = cacheCollection
                .whereEqualTo(FIELD_CACHE_TYPE, QuestionCacheType.PIQ_BASED.name)
                .get()
                .await()
                .size()

            val expired = cacheCollection
                .whereLessThan(FIELD_EXPIRES_AT, Instant.now().toEpochMilli())
                .get()
                .await()
                .size()

            val genericCount = genericCollection.get().await().size()

            // Get most used questions
            val mostUsedSnapshot = cacheCollection
                .orderBy(FIELD_USAGE_COUNT, Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()

            val mostUsed = mostUsedSnapshot.documents.mapNotNull { doc ->
                mapToCacheEntry(doc.data ?: return@mapNotNull null)
            }

            val stats = QuestionCacheStats(
                totalCached = totalCached,
                piqBasedCount = piqBased,
                genericCount = genericCount,
                expiredCount = expired,
                mostUsedQuestions = mostUsed,
                cacheHitRate = 0f, // TODO: Calculate from usage analytics
                avgGenerationTime = 0L // TODO: Track generation time
            )

            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache stats", e)
            Result.failure(e)
        }
    }

    override suspend fun invalidatePIQCache(piqSnapshotId: String): Result<Unit> {
        return try {
            val query = cacheCollection
                .whereEqualTo(FIELD_CACHE_KEY, piqSnapshotId)
                .get()
                .await()

            val batch = firestore.batch()
            query.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()
            Log.d(TAG, "Invalidated cache for PIQ: $piqSnapshotId")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to invalidate PIQ cache", e)
            Result.failure(e)
        }
    }

    /**
     * Convert cache entry to Firestore map
     */
    private fun cacheEntryToMap(entry: QuestionCacheEntry): Map<String, Any?> {
        return mapOf(
            "id" to entry.id,
            FIELD_QUESTION to questionToMap(entry.question),
            FIELD_CACHE_KEY to entry.cacheKey,
            FIELD_CACHE_TYPE to entry.cacheType.name,
            FIELD_CREATED_AT to entry.createdAt.toEpochMilli(),
            FIELD_USAGE_COUNT to entry.usageCount,
            FIELD_LAST_USED_AT to entry.lastUsedAt?.toEpochMilli(),
            FIELD_EXPIRES_AT to entry.expiresAt?.toEpochMilli()
        )
    }

    /**
     * Convert question to Firestore map
     */
    private fun questionToMap(question: InterviewQuestion): Map<String, Any?> {
        return mapOf(
            "id" to question.id,
            "questionText" to question.questionText,
            FIELD_TARGET_OLQS to question.expectedOLQs.map { it.name },
            "context" to question.context,
            "source" to question.source.name
        )
    }

    /**
     * Map Firestore data to cache entry
     */
    private fun mapToCacheEntry(data: Map<String, Any>): QuestionCacheEntry? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val questionData = data[FIELD_QUESTION] as? Map<String, Any> ?: return null
            val question = mapToQuestion(questionData) ?: return null

            QuestionCacheEntry(
                id = data["id"] as? String ?: return null,
                question = question,
                cacheKey = data[FIELD_CACHE_KEY] as? String ?: return null,
                cacheType = QuestionCacheType.valueOf(
                    data[FIELD_CACHE_TYPE] as? String ?: return null
                ),
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

    /**
     * Map Firestore data to interview question
     */
    private fun mapToQuestion(data: Map<String, Any>): InterviewQuestion? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val targetOLQNames = data[FIELD_TARGET_OLQS] as? List<String> ?: return null
            val expectedOLQs = targetOLQNames.mapNotNull { name ->
                OLQ.entries.find { it.name == name }
            }

            val sourceName = data["source"] as? String ?: "GENERIC_POOL"
            val source = try {
                com.ssbmax.core.domain.model.interview.QuestionSource.valueOf(sourceName)
            } catch (e: Exception) {
                com.ssbmax.core.domain.model.interview.QuestionSource.GENERIC_POOL
            }

            InterviewQuestion(
                id = data["id"] as? String ?: return null,
                questionText = data["questionText"] as? String ?: return null,
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
