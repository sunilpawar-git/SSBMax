package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
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
 * Collections: question_cache, generic_questions, question_usage
 * @see QuestionCacheMappers for data conversion
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
    }

    private val cacheCollection = firestore.collection(COLLECTION_CACHE)
    private val genericCollection = firestore.collection(COLLECTION_GENERIC)
    private val usageCollection = firestore.collection(COLLECTION_USAGE)

    override suspend fun cachePIQQuestions(
        piqSnapshotId: String,
        questions: List<InterviewQuestion>,
        expirationDays: Int
    ): Result<Unit> = try {
        val secondsPerDay = 86400L
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
            batch.set(cacheCollection.document(cacheEntry.id), QuestionCacheMappers.cacheEntryToMap(cacheEntry))
        }

        batch.commit().await()
        Log.d(TAG, "Cached ${questions.size} PIQ-based questions for $piqSnapshotId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to cache PIQ questions", e)
        Result.failure(e)
    }

    override suspend fun getPIQQuestions(
        piqSnapshotId: String,
        limit: Int,
        excludeUsed: Boolean
    ): Result<List<InterviewQuestion>> = try {
        var query = cacheCollection
            .whereEqualTo(QuestionCacheMappers.FIELD_CACHE_KEY, piqSnapshotId)
            .whereEqualTo(QuestionCacheMappers.FIELD_CACHE_TYPE, QuestionCacheType.PIQ_BASED.name)
            .whereGreaterThan(QuestionCacheMappers.FIELD_EXPIRES_AT, Instant.now().toEpochMilli())
            .orderBy(QuestionCacheMappers.FIELD_EXPIRES_AT, Query.Direction.DESCENDING)

        if (excludeUsed) {
            query = query.orderBy(QuestionCacheMappers.FIELD_USAGE_COUNT, Query.Direction.ASCENDING)
        }

        val snapshot = query.limit(limit.toLong()).get().await()
        val questions = snapshot.documents.mapNotNull { doc ->
            QuestionCacheMappers.mapToCacheEntry(doc.data ?: return@mapNotNull null)?.question
        }

        Log.d(TAG, "Retrieved ${questions.size} PIQ-based questions from cache")
        Result.success(questions)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get PIQ questions", e)
        Result.failure(e)
    }

    override suspend fun getGenericQuestions(
        targetOLQs: List<OLQ>?,
        difficulty: Int?,
        limit: Int,
        excludeUsed: Boolean
    ): Result<List<InterviewQuestion>> = try {
        var query: Query = genericCollection

        if (difficulty != null) {
            query = query.whereEqualTo(QuestionCacheMappers.FIELD_DIFFICULTY, difficulty)
        }
        if (excludeUsed) {
            query = query.orderBy(QuestionCacheMappers.FIELD_USAGE_COUNT, Query.Direction.ASCENDING)
        }

        val snapshot = query.limit(limit.toLong()).get().await()
        var questions = snapshot.documents.mapNotNull { doc ->
            QuestionCacheMappers.mapToQuestion(doc.data ?: return@mapNotNull null)
        }

        // Filter by target OLQs if specified
        if (targetOLQs != null) {
            questions = questions.filter { q -> q.expectedOLQs.any { it in targetOLQs } }
        }

        Log.d(TAG, "Retrieved ${questions.size} generic questions from pool")
        Result.success(questions.take(limit))
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get generic questions", e)
        Result.failure(e)
    }

    override suspend fun markQuestionUsed(questionId: String, sessionId: String): Result<Unit> = try {
        // Update usage count in cache
        val cacheQuery = cacheCollection.whereEqualTo("question.id", questionId).limit(1).get().await()
        if (!cacheQuery.isEmpty) {
            cacheQuery.documents[0].reference.update(
                mapOf(
                    QuestionCacheMappers.FIELD_USAGE_COUNT to com.google.firebase.firestore.FieldValue.increment(1),
                    QuestionCacheMappers.FIELD_LAST_USED_AT to Instant.now().toEpochMilli()
                )
            ).await()
        }

        // Also update generic questions if applicable
        val genericQuery = genericCollection.whereEqualTo("id", questionId).limit(1).get().await()
        if (!genericQuery.isEmpty) {
            genericQuery.documents[0].reference.update(
                mapOf(
                    QuestionCacheMappers.FIELD_USAGE_COUNT to com.google.firebase.firestore.FieldValue.increment(1),
                    QuestionCacheMappers.FIELD_LAST_USED_AT to Instant.now().toEpochMilli()
                )
            ).await()
        }

        // Track usage for analytics
        usageCollection.add(
            mapOf("questionId" to questionId, "sessionId" to sessionId, "usedAt" to Instant.now().toEpochMilli())
        ).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to mark question as used", e)
        Result.failure(e)
    }

    override suspend fun cleanupExpired(): Result<Int> = try {
        val expiredQuery = cacheCollection.whereLessThan(QuestionCacheMappers.FIELD_EXPIRES_AT, Instant.now().toEpochMilli()).get().await()
        val batch = firestore.batch()
        expiredQuery.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        val deletedCount = expiredQuery.documents.size
        Log.d(TAG, "Cleaned up $deletedCount expired cache entries")
        Result.success(deletedCount)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to cleanup expired cache", e)
        Result.failure(e)
    }

    override suspend fun getCacheStats(userId: String?): Result<QuestionCacheStats> = try {
        val totalCached = cacheCollection.get().await().size()
        val piqBased = cacheCollection.whereEqualTo(QuestionCacheMappers.FIELD_CACHE_TYPE, QuestionCacheType.PIQ_BASED.name).get().await().size()
        val expired = cacheCollection.whereLessThan(QuestionCacheMappers.FIELD_EXPIRES_AT, Instant.now().toEpochMilli()).get().await().size()
        val genericCount = genericCollection.get().await().size()

        val mostUsedSnapshot = cacheCollection.orderBy(QuestionCacheMappers.FIELD_USAGE_COUNT, Query.Direction.DESCENDING).limit(5).get().await()
        val mostUsed = mostUsedSnapshot.documents.mapNotNull { QuestionCacheMappers.mapToCacheEntry(it.data ?: return@mapNotNull null) }

        Result.success(QuestionCacheStats(
            totalCached = totalCached,
            piqBasedCount = piqBased,
            genericCount = genericCount,
            expiredCount = expired,
            mostUsedQuestions = mostUsed,
            cacheHitRate = 0f,
            avgGenerationTime = 0L
        ))
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get cache stats", e)
        Result.failure(e)
    }

    override suspend fun invalidatePIQCache(piqSnapshotId: String): Result<Unit> = try {
        val query = cacheCollection.whereEqualTo(QuestionCacheMappers.FIELD_CACHE_KEY, piqSnapshotId).get().await()
        val batch = firestore.batch()
        query.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        Log.d(TAG, "Invalidated cache for PIQ: $piqSnapshotId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to invalidate PIQ cache", e)
        Result.failure(e)
    }
}
