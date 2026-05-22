package com.ssbmax.core.data.repository.interview

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.domain.model.interview.InterviewLimits
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate class to handle all result-related operations for the Interview system,
 * including final assessments and limit tracking.
 * Keeps file sizes under the strict 300-line requirement.
 */
@Singleton
class InterviewResultManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: InterviewSessionManager,
    private val responseManager: InterviewResponseManager,
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionManager: SubscriptionManager
) {
    companion object {
        private const val TAG = "InterviewResultMgr"
        private const val COLLECTION_RESULTS = "interview_results"
        private const val COLLECTION_SESSIONS = "interview_sessions"
        private const val COLLECTION_SUBMISSIONS = "submissions"

        private const val FIELD_USER_ID = "userId"
        private const val FIELD_STATUS = "status"
        private const val FIELD_COMPLETED_AT = "completedAt"
        private const val FIELD_SESSION_ID = "sessionId"

        private const val FIELD_ID = "id"
        private const val FIELD_TEST_TYPE = "testType"
        private const val FIELD_SUBMITTED_AT = "submittedAt"
        private const val FIELD_SCORE = "score"
        private const val FIELD_TEST_ID = "testId"
    }

    suspend fun completeInterview(sessionId: String): Result<InterviewResult> {
        return try {
            val session = sessionManager.getSession(sessionId).getOrNull()
                ?: return Result.failure(IllegalStateException("Session not found"))

            val responses = responseManager.getResponses(sessionId).getOrNull() ?: emptyList()

            // Aggregate OLQ scores
            val olqScoresMap = mutableMapOf<OLQ, MutableList<OLQScore>>()
            responses.forEach { response ->
                response.olqScores.forEach { (olq, score) ->
                    olqScoresMap.getOrPut(olq) { mutableListOf() }.add(score)
                }
            }

            // Calculate average scores per OLQ
            val overallOLQScores = olqScoresMap.mapValues { (_, scores) ->
                val avgScore = scores.map { it.score }.average().toInt().coerceIn(1, 10)
                val avgConfidence = scores.map { it.confidence }.average().toInt()
                OLQScore(avgScore, avgConfidence, "Aggregated from ${scores.size} responses")
            }

            // Calculate category scores
            val categoryScores = OLQCategory.entries.associateWith { category ->
                val categoryOLQs = OLQ.entries.filter { it.category == category }
                val scores = categoryOLQs.mapNotNull { overallOLQScores[it]?.score }
                if (scores.isEmpty()) 0f else scores.average().toFloat()
            }

            // Identify strengths and weaknesses (lower scores = better in SSB)
            val sortedOLQs = overallOLQScores.entries.sortedBy { it.value.score }
            val strengths = sortedOLQs.take(3).map { it.key }
            val weaknesses = sortedOLQs.takeLast(3).map { it.key }

            val overallConfidence = responses.map { it.confidenceScore }.average().toInt()
            val avgScore = overallOLQScores.values.map { it.score }.average().toFloat()
            val overallRating = avgScore.toInt().coerceIn(1, 10)

            val result = InterviewResult(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                userId = session.userId,
                mode = session.mode,
                completedAt = Instant.now(),
                durationSec = session.getDurationSeconds(),
                totalQuestions = session.questionIds.size,
                totalResponses = responses.size,
                overallOLQScores = overallOLQScores,
                categoryScores = categoryScores,
                overallConfidence = overallConfidence,
                strengths = strengths,
                weaknesses = weaknesses,
                feedback = generateFeedback(strengths, weaknesses, overallRating),
                overallRating = overallRating
            )

            // Save result
            firestore.collection(COLLECTION_RESULTS)
                .document(result.id)
                .set(InterviewFirestoreMappers.resultToMap(result))
                .await()

            // Update session status
            sessionManager.updateSession(session.copy(status = InterviewStatus.COMPLETED, completedAt = Instant.now()))

            // Create submission record for progress tracking
            val submissionId = "interview_${result.id}"
            val submissionMap = mapOf(
                FIELD_ID to submissionId,
                FIELD_USER_ID to session.userId,
                FIELD_TEST_ID to sessionId,
                FIELD_TEST_TYPE to "IO",
                FIELD_STATUS to "COMPLETED",
                FIELD_SUBMITTED_AT to result.completedAt.toEpochMilli(),
                FIELD_SCORE to (10 - result.overallRating).toFloat() * 10,
                "resultId" to result.id,
                "mode" to result.mode.name
            )

            firestore.collection(COLLECTION_SUBMISSIONS)
                .document(submissionId)
                .set(submissionMap)
                .await()

            Log.d(TAG, "Created submission record for progress tracking: $submissionId")

            // Atomic usage increment to enforce monetization and lock down limit bypasses
            subscriptionManager.recordTestUsage(
                com.ssbmax.core.domain.model.TestType.IO,
                session.userId,
                submissionId
            )
            Log.d(TAG, "Recorded interview usage atomically via SubscriptionManager")

            Log.d(TAG, "Completed interview: $sessionId, result: ${result.id}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete interview: $sessionId", e)
            Result.failure(e)
        }
    }

    private fun generateFeedback(strengths: List<OLQ>, weaknesses: List<OLQ>, rating: Int): String {
        val strengthNames = strengths.joinToString(", ") { it.displayName }
        val weaknessNames = weaknesses.joinToString(", ") { it.displayName }

        return when {
            rating <= 5 -> "Excellent performance! Your strengths in $strengthNames stood out. " +
                    "Consider developing: $weaknessNames"
            rating <= 7 -> "Good performance. Strong areas: $strengthNames. " +
                    "Areas for improvement: $weaknessNames"
            else -> "Focus on developing: $weaknessNames. " +
                    "Build on your strengths in: $strengthNames"
        }
    }

    suspend fun getResult(sessionId: String): Result<InterviewResult> {
        return try {
            val snapshot = firestore.collection(COLLECTION_RESULTS)
                .whereEqualTo(FIELD_SESSION_ID, sessionId)
                .limit(1)
                .get()
                .await()

            val result = snapshot.documents.firstOrNull()?.let { doc ->
                InterviewFirestoreMappers.mapToResult(doc.data ?: return@let null)
            } ?: return Result.failure(IllegalStateException("Result not found for session: $sessionId"))

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get result for session: $sessionId", e)
            Result.failure(e)
        }
    }

    suspend fun getResultById(resultId: String): Result<InterviewResult> {
        return try {
            val doc = firestore.collection(COLLECTION_RESULTS)
                .document(resultId)
                .get()
                .await()

            val result = InterviewFirestoreMappers.mapToResult(
                doc.data ?: return Result.failure(IllegalStateException("Result not found: $resultId"))
            )

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get result: $resultId", e)
            Result.failure(e)
        }
    }

    fun getUserResults(userId: String): Flow<List<InterviewResult>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_RESULTS)
            .whereEqualTo(FIELD_USER_ID, userId)
            .orderBy(FIELD_COMPLETED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to user results for user: $userId")
                    return@addSnapshotListener
                }

                val results = snapshot?.documents?.mapNotNull { doc ->
                    InterviewFirestoreMappers.mapToResult(doc.data ?: return@mapNotNull null)
                } ?: emptyList()

                trySend(results)
            }

        awaitClose { listener.remove() }
    }

    suspend fun getLatestResult(userId: String): Result<InterviewResult?> {
        return try {
            val snapshot = firestore.collection(COLLECTION_RESULTS)
                .whereEqualTo(FIELD_USER_ID, userId)
                .orderBy(FIELD_COMPLETED_AT, Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val result = snapshot.documents.firstOrNull()?.data?.let { data ->
                InterviewFirestoreMappers.mapToResult(data)
            }

            Log.d(TAG, "getLatestResult for user $userId: ${result?.id ?: "null"}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get latest result for user: $userId", e)
            Result.failure(e)
        }
    }

    suspend fun getInterviewStats(userId: String): Result<Map<InterviewMode, Int>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SESSIONS)
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_STATUS, InterviewStatus.COMPLETED.name)
                .get()
                .await()

            val stats = snapshot.documents.mapNotNull { doc ->
                InterviewFirestoreMappers.mapToSession(doc.data ?: return@mapNotNull null)
            }.groupBy { it.mode }
                .mapValues { it.value.size }

            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get interview stats for user: $userId", e)
            Result.failure(e)
        }
    }

    suspend fun getRemainingInterviews(userId: String, mode: InterviewMode): Result<Int> {
        return try {
            val tierResult = subscriptionRepository.getSubscriptionTier(userId)
            if (tierResult.isFailure) return Result.success(0)

            val tier = tierResult.getOrNull() ?: return Result.success(0)

            // Convert SubscriptionTier to SubscriptionType
            val subscriptionType = com.ssbmax.core.domain.model.SubscriptionType.valueOf(tier.name)

            // Get used count from monthly usage
            val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val usageResult = subscriptionRepository.getMonthlyUsage(userId, currentMonth)
            val usageMap = usageResult.getOrNull() ?: emptyMap()
            val interviewUsage = usageMap["Interview"] ?: usageMap["interview"]
            val used = interviewUsage?.used ?: 0

            // Calculate limits
            val limits = InterviewLimits.forSubscription(subscriptionType, used)

            Result.success(limits.remaining)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get remaining interviews for user: $userId, mode: $mode", e)
            Result.failure(e)
        }
    }
}
