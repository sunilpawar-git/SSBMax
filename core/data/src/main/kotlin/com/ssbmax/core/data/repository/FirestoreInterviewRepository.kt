package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ssbmax.core.data.repository.interview.InterviewFirestoreMappers
import com.ssbmax.core.data.repository.interview.InterviewQuestionGenerator
import com.ssbmax.core.domain.constants.InterviewConstants
import com.ssbmax.core.domain.model.interview.InterviewLimits
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.interview.PrerequisiteCheckResult
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of InterviewRepository
 *
 * Uses helper classes for specific concerns:
 * - [InterviewFirestoreMappers] for entity <-> Firestore mapping
 * - [InterviewQuestionGenerator] for question generation logic
 *
 * **Collections**:
 * - `interview_sessions`: Active and historical interview sessions
 * - `interview_responses`: Candidate responses with OLQ scores
 * - `interview_results`: Final assessment results
 * - `interview_questions`: Temporary question storage during session
 */
@Singleton
class FirestoreInterviewRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val questionCacheRepository: QuestionCacheRepository,
    private val questionGenerator: InterviewQuestionGenerator,
    private val subscriptionRepository: SubscriptionRepository
) : InterviewRepository {

    companion object {
        private const val TAG = "InterviewRepository"
        private const val COLLECTION_SESSIONS = "interview_sessions"
        private const val COLLECTION_RESPONSES = "interview_responses"
        private const val COLLECTION_RESULTS = "interview_results"
        private const val COLLECTION_QUESTIONS = "interview_questions"
        private const val COLLECTION_SUBMISSIONS = "submissions"

        private const val FIELD_USER_ID = "userId"
        private const val FIELD_STATUS = "status"
        private const val FIELD_STARTED_AT = "startedAt"
        private const val FIELD_COMPLETED_AT = "completedAt"
        private const val FIELD_SESSION_ID = "sessionId"
        private const val FIELD_RESPONDED_AT = "respondedAt"
        
        // Submission fields for progress tracking
        private const val FIELD_ID = "id"
        private const val FIELD_TEST_TYPE = "testType"
        private const val FIELD_SUBMITTED_AT = "submittedAt"
        private const val FIELD_SCORE = "score"
        private const val FIELD_TEST_ID = "testId"
    }

    // ============================================
    // PREREQUISITES (Delegated to UseCases)
    // ============================================

    override suspend fun checkPrerequisites(userId: String): Result<PrerequisiteCheckResult> {
        return Result.failure(UnsupportedOperationException("Use CheckInterviewPrerequisitesUseCase"))
    }

    override suspend fun checkInterviewLimits(userId: String, mode: InterviewMode): Result<Boolean> {
        return Result.failure(UnsupportedOperationException("Use CheckInterviewLimitsUseCase"))
    }

    // ============================================
    // SESSION MANAGEMENT
    // ============================================

    override suspend fun createSession(
        userId: String,
        mode: InterviewMode,
        piqSnapshotId: String,
        consentGiven: Boolean
    ): Result<InterviewSession> {
        return try {
            val questionsResult = questionGenerator.generateQuestions(
                piqSnapshotId,
                InterviewConstants.TARGET_TOTAL_QUESTIONS
            )

            if (questionsResult.isFailure) {
                return Result.failure(
                    questionsResult.exceptionOrNull() ?: Exception("Failed to generate questions")
                )
            }

            val questions = questionsResult.getOrNull() ?: emptyList()

            val session = InterviewSession(
                id = UUID.randomUUID().toString(),
                userId = userId,
                mode = mode,
                status = InterviewStatus.IN_PROGRESS,
                startedAt = Instant.now(),
                completedAt = null,
                piqSnapshotId = piqSnapshotId,
                consentGiven = consentGiven,
                questionIds = questions.map { it.id },
                currentQuestionIndex = 0,
                estimatedDuration = InterviewConstants.DEFAULT_DURATION_MINUTES
            )

            // Store questions and session in batch
            val batch = firestore.batch()
            questions.forEach { question ->
                val questionDoc = firestore.collection(COLLECTION_QUESTIONS).document(question.id)
                batch.set(questionDoc, InterviewFirestoreMappers.questionToMap(question))
            }

            val sessionDoc = firestore.collection(COLLECTION_SESSIONS).document(session.id)
            batch.set(sessionDoc, InterviewFirestoreMappers.sessionToMap(session))

            batch.commit().await()

            Log.d(TAG, "Created interview session: ${session.id} with ${questions.size} questions")
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create interview session", e)
            Result.failure(e)
        }
    }

    override suspend fun getActiveSession(userId: String): Result<InterviewSession?> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SESSIONS)
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_STATUS, InterviewStatus.IN_PROGRESS.name)
                .orderBy(FIELD_STARTED_AT, Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val session = snapshot.documents.firstOrNull()?.let { doc ->
                InterviewFirestoreMappers.mapToSession(doc.data ?: return@let null)
            }

            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active session for user: $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun getSession(sessionId: String): Result<InterviewSession> {
        return try {
            val doc = firestore.collection(COLLECTION_SESSIONS)
                .document(sessionId)
                .get()
                .await()

            val session = InterviewFirestoreMappers.mapToSession(
                doc.data ?: return Result.failure(IllegalStateException("Session not found: $sessionId"))
            )

            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get session: $sessionId", e)
            Result.failure(e)
        }
    }

    override suspend fun updateSession(session: InterviewSession): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_SESSIONS)
                .document(session.id)
                .set(InterviewFirestoreMappers.sessionToMap(session))
                .await()

            Log.d(TAG, "Updated session: ${session.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update session: ${session.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun abandonSession(sessionId: String): Result<Unit> {
        return try {
            val sessionResult = getSession(sessionId)
            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: Exception("Session not found"))
            }

            val session = sessionResult.getOrNull() ?: return Result.failure(
                IllegalStateException("Session is null despite successful result")
            )

            updateSession(session.copy(status = InterviewStatus.ABANDONED, completedAt = Instant.now()))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to abandon session: $sessionId", e)
            Result.failure(e)
        }
    }

    // ============================================
    // QUESTION MANAGEMENT
    // ============================================

    override suspend fun generateQuestions(
        sessionId: String,
        piqSnapshotId: String,
        count: Int
    ): Result<List<InterviewQuestion>> {
        return questionGenerator.generateQuestions(piqSnapshotId, count)
    }

    override suspend fun getQuestion(questionId: String): Result<InterviewQuestion> {
        return try {
            val doc = firestore.collection(COLLECTION_QUESTIONS)
                .document(questionId)
                .get()
                .await()

            val question = InterviewFirestoreMappers.mapToQuestion(
                doc.data ?: return Result.failure(IllegalStateException("Question not found: $questionId"))
            )

            Result.success(question)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get question: $questionId", e)
            Result.failure(e)
        }
    }

    override suspend fun cacheQuestions(
        piqSnapshotId: String,
        questions: List<InterviewQuestion>
    ): Result<Unit> {
        return questionCacheRepository.cachePIQQuestions(
            piqSnapshotId = piqSnapshotId,
            questions = questions,
            expirationDays = InterviewConstants.DEFAULT_CACHE_EXPIRATION_DAYS
        )
    }

    override suspend fun getCachedQuestions(
        piqSnapshotId: String,
        limit: Int
    ): Result<List<InterviewQuestion>> {
        return questionCacheRepository.getPIQQuestions(
            piqSnapshotId = piqSnapshotId,
            limit = limit,
            excludeUsed = true
        )
    }

    // ============================================
    // RESPONSE MANAGEMENT
    // ============================================

    override suspend fun submitResponse(response: InterviewResponse): Result<InterviewResponse> {
        return try {
            val session = getSession(response.sessionId).getOrNull()
                ?: return Result.failure(Exception("Session not found"))

            val responseMap = InterviewFirestoreMappers.responseToMap(response).toMutableMap()
            responseMap["userId"] = session.userId

            firestore.collection(COLLECTION_RESPONSES)
                .document(response.id)
                .set(responseMap)
                .await()

            Log.d(TAG, "Submitted response: ${response.id}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit response: ${response.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateResponse(response: InterviewResponse): Result<InterviewResponse> {
        return try {
            val session = getSession(response.sessionId).getOrNull()
                ?: return Result.failure(Exception("Session not found"))

            val responseMap = InterviewFirestoreMappers.responseToMap(response).toMutableMap()
            responseMap["userId"] = session.userId

            firestore.collection(COLLECTION_RESPONSES)
                .document(response.id)
                .set(responseMap)
                .await()

            Log.d(TAG, "Updated response with OLQ scores: ${response.id}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update response: ${response.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun getResponses(sessionId: String): Result<List<InterviewResponse>> {
        return try {
            val session = getSession(sessionId).getOrNull()
                ?: return Result.failure(Exception("Session not found"))

            val snapshot = firestore.collection(COLLECTION_RESPONSES)
                .whereEqualTo(FIELD_SESSION_ID, sessionId)
                .whereEqualTo(FIELD_USER_ID, session.userId)
                .orderBy(FIELD_RESPONDED_AT, Query.Direction.ASCENDING)
                .get()
                .await()

            val responses = snapshot.documents.mapNotNull { doc ->
                InterviewFirestoreMappers.mapToResponse(doc.data ?: return@mapNotNull null)
            }

            Log.d(TAG, "Retrieved ${responses.size} responses for session: $sessionId")
            Result.success(responses)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get responses for session: $sessionId", e)
            Result.failure(e)
        }
    }

    override suspend fun getResponse(responseId: String): Result<InterviewResponse> {
        return try {
            val doc = firestore.collection(COLLECTION_RESPONSES)
                .document(responseId)
                .get()
                .await()

            val response = InterviewFirestoreMappers.mapToResponse(
                doc.data ?: return Result.failure(IllegalStateException("Response not found: $responseId"))
            )

            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get response: $responseId", e)
            Result.failure(e)
        }
    }

    // ============================================
    // RESULT MANAGEMENT
    // ============================================

    override suspend fun completeInterview(sessionId: String): Result<InterviewResult> {
        return try {
            val session = getSession(sessionId).getOrNull()
                ?: return Result.failure(IllegalStateException("Session not found"))

            val responses = getResponses(sessionId).getOrNull() ?: emptyList()

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
            updateSession(session.copy(status = InterviewStatus.COMPLETED, completedAt = Instant.now()))

            // Create submission record for progress tracking
            // This ensures the interview shows up in "Your Progress" section
            val submissionId = "interview_${result.id}"
            val submissionMap = mapOf(
                FIELD_ID to submissionId,
                FIELD_USER_ID to session.userId,
                FIELD_TEST_ID to sessionId,
                FIELD_TEST_TYPE to "IO",
                FIELD_STATUS to "COMPLETED",
                FIELD_SUBMITTED_AT to result.completedAt.toEpochMilli(),
                FIELD_SCORE to (10 - result.overallRating).toFloat() * 10, // Convert 1-10 scale (lower=better) to 0-100 (higher=better)
                "resultId" to result.id,
                "mode" to result.mode.name
            )
            
            firestore.collection(COLLECTION_SUBMISSIONS)
                .document(submissionId)
                .set(submissionMap)
                .await()
            
            Log.d(TAG, "Created submission record for progress tracking: $submissionId")

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

    override suspend fun getResult(sessionId: String): Result<InterviewResult> {
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

    override suspend fun getResultById(resultId: String): Result<InterviewResult> {
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

    override fun getUserResults(userId: String): Flow<List<InterviewResult>> = callbackFlow {
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

    override suspend fun getLatestResult(userId: String): Result<InterviewResult?> {
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

    // ============================================
    // ANALYTICS
    // ============================================

    override suspend fun getInterviewStats(userId: String): Result<Map<InterviewMode, Int>> {
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

    override suspend fun getRemainingInterviews(userId: String, mode: InterviewMode): Result<Int> {
        return try {
            val tierResult = subscriptionRepository.getSubscriptionTier(userId)
            if (tierResult.isFailure) return Result.success(0)

            val tier = tierResult.getOrNull() ?: return Result.success(0)

            // Convert SubscriptionTier to SubscriptionType
            val subscriptionType = com.ssbmax.core.domain.model.SubscriptionType.valueOf(tier.name)

            // Get total used interviews (unified system sums all modes)
            val stats = getInterviewStats(userId).getOrDefault(emptyMap())
            val totalUsed = stats.values.sum()

            // Calculate limits using new API
            val limits = InterviewLimits.forSubscription(subscriptionType, totalUsed)

            Result.success(limits.remaining)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get remaining interviews for user: $userId, mode: $mode", e)
            Result.failure(e)
        }
    }
}
