package com.ssbmax.core.data.repository

import com.ssbmax.core.data.repository.interview.InterviewQuestionManager
import com.ssbmax.core.data.repository.interview.InterviewResponseManager
import com.ssbmax.core.data.repository.interview.InterviewResultManager
import com.ssbmax.core.data.repository.interview.InterviewSessionManager
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.PrerequisiteCheckResult
import com.ssbmax.core.domain.repository.InterviewRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight, high-performance facade implementation of [InterviewRepository].
 * Delegates actual logic to specific sub-manager components:
 * - [InterviewSessionManager] for session creation, lifecycle and abandonment
 * - [InterviewQuestionManager] for generation, retrieval and caching of questions
 * - [InterviewResponseManager] for submitting and updating candidate responses
 * - [InterviewResultManager] for grading calculations, final feedback and monetization locks
 *
 * This keeps each single component strictly under the 300-line requirement.
 */
@Singleton
class FirestoreInterviewRepository @Inject constructor(
    private val sessionManager: InterviewSessionManager,
    private val questionManager: InterviewQuestionManager,
    private val responseManager: InterviewResponseManager,
    private val resultManager: InterviewResultManager
) : InterviewRepository {

    // Prerequisites validation (Delegated to dedicated use cases)
    override suspend fun checkPrerequisites(userId: String): Result<PrerequisiteCheckResult> {
        return Result.failure(UnsupportedOperationException("Use CheckInterviewPrerequisitesUseCase"))
    }

    override suspend fun checkInterviewLimits(userId: String, mode: InterviewMode): Result<Boolean> {
        return Result.failure(UnsupportedOperationException("Use CheckInterviewLimitsUseCase"))
    }

    // Session Management
    override suspend fun createSession(
        userId: String,
        mode: InterviewMode,
        piqSnapshotId: String,
        consentGiven: Boolean
    ): Result<InterviewSession> = sessionManager.createSession(userId, mode, piqSnapshotId, consentGiven)

    override suspend fun getActiveSession(userId: String): Result<InterviewSession?> =
        sessionManager.getActiveSession(userId)

    override suspend fun getSession(sessionId: String): Result<InterviewSession> =
        sessionManager.getSession(sessionId)

    override suspend fun updateSession(session: InterviewSession): Result<Unit> =
        sessionManager.updateSession(session)

    override suspend fun abandonSession(sessionId: String): Result<Unit> =
        sessionManager.abandonSession(sessionId)

    // Question Management
    override suspend fun generateQuestions(
        sessionId: String,
        piqSnapshotId: String,
        count: Int
    ): Result<List<InterviewQuestion>> = questionManager.generateQuestions(sessionId, piqSnapshotId, count)

    override suspend fun getQuestion(questionId: String): Result<InterviewQuestion> =
        questionManager.getQuestion(questionId)

    override suspend fun cacheQuestions(
        piqSnapshotId: String,
        questions: List<InterviewQuestion>
    ): Result<Unit> = questionManager.cacheQuestions(piqSnapshotId, questions)

    override suspend fun getCachedQuestions(
        piqSnapshotId: String,
        limit: Int
    ): Result<List<InterviewQuestion>> = questionManager.getCachedQuestions(piqSnapshotId, limit)

    // Response Management
    override suspend fun submitResponse(response: InterviewResponse): Result<InterviewResponse> =
        responseManager.submitResponse(response)

    override suspend fun updateResponse(response: InterviewResponse): Result<InterviewResponse> =
        responseManager.updateResponse(response)

    override suspend fun getResponses(sessionId: String): Result<List<InterviewResponse>> =
        responseManager.getResponses(sessionId)

    override suspend fun getResponse(responseId: String): Result<InterviewResponse> =
        responseManager.getResponse(responseId)

    // Result Management
    override suspend fun completeInterview(sessionId: String): Result<InterviewResult> =
        resultManager.completeInterview(sessionId)

    override suspend fun getResult(sessionId: String): Result<InterviewResult> =
        resultManager.getResult(sessionId)

    override suspend fun getResultById(resultId: String): Result<InterviewResult> =
        resultManager.getResultById(resultId)

    override fun getUserResults(userId: String): Flow<List<InterviewResult>> =
        resultManager.getUserResults(userId)

    override suspend fun getLatestResult(userId: String): Result<InterviewResult?> =
        resultManager.getLatestResult(userId)

    // Analytics & Stats
    override suspend fun getInterviewStats(userId: String): Result<Map<InterviewMode, Int>> =
        resultManager.getInterviewStats(userId)

    override suspend fun getRemainingInterviews(userId: String, mode: InterviewMode): Result<Int> =
        resultManager.getRemainingInterviews(userId, mode)
}
