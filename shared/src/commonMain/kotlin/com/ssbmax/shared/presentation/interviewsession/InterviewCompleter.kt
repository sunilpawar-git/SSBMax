package com.ssbmax.shared.presentation.interviewsession

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.interview.InterviewMode
import com.ssbmax.shared.domain.model.interview.InterviewResponse
import com.ssbmax.shared.domain.model.interview.InterviewSession
import com.ssbmax.shared.domain.model.interview.InterviewStatus
import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger
import com.ssbmax.shared.domain.util.DomainLogger
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * KMP port of `app/.../ui/interview/session/InterviewCompleter.kt`.
 *
 * **Real finding, stated plainly per this session's brief:** the Android
 * original's `enqueueAnalysisWorker` calls `androidx.work.WorkManager`
 * directly with a concrete `OneTimeWorkRequestBuilder<InterviewAnalysisWorker>()`
 * -- `InterviewAnalysisWorker` is an `app`-module class, and `shared` cannot
 * depend on `app`. This is exactly the same shape as the PPDT/TAT/WAT/SRT/SDT/
 * GTO precedent already documented on [SubmissionAnalysisTrigger]'s own doc
 * comment: replaced here with `analysisTrigger.trigger(TestType.IO, sessionId)`.
 * `shared`'s own binding (`KtorSubmissionAnalysisTrigger`) now dispatches this
 * to a real `InterviewAnalysisOrchestrator` -- see [SubmissionAnalysisTrigger]'s
 * own doc for the current per-platform binding shape. `app/.../ui/interview`
 * itself is untouched -- this vertical is reachable only via `SSBMaxNavHost`,
 * same "not swapped into the live Android app" precedent as every prior
 * Phase 5 session.
 *
 * `UUID.randomUUID()` (JVM-only) -> `kotlin.uuid.Uuid.random()` (stdlib,
 * KMP-safe since Kotlin 1.9.20 behind `ExperimentalUuidApi`, already the
 * convention this plan's earlier slices adopted for new-ID generation).
 *
 * Follows best-effort pattern: partial failures don't block completion.
 */
@OptIn(ExperimentalUuidApi::class)
class InterviewCompleter(
    private val interviewRepository: InterviewRepository,
    private val analysisTrigger: SubmissionAnalysisTrigger,
    private val logger: DomainLogger
) {
    private val tag = "InterviewCompleter"

    /** Complete the interview by saving responses and triggering analysis. */
    suspend fun complete(
        sessionId: String,
        session: InterviewSession,
        pendingResponses: List<PendingResponse>,
        mode: InterviewMode
    ): Result<String> {
        return try {
            saveResponses(sessionId, pendingResponses, mode)

            val updatedSession = session.copy(status = InterviewStatus.PENDING_ANALYSIS)
            interviewRepository.updateSession(updatedSession)

            analysisTrigger.trigger(TestType.IO, sessionId)

            Result.success(sessionId)
        } catch (e: Exception) {
            logger.e(tag, "Exception completing interview", e)
            Result.failure(e)
        }
    }

    /** Save responses to repository (best-effort - partial failures logged but don't block). */
    private suspend fun saveResponses(
        sessionId: String,
        pendingResponses: List<PendingResponse>,
        mode: InterviewMode
    ) {
        for ((index, pending) in pendingResponses.withIndex()) {
            val response = InterviewResponse(
                id = Uuid.random().toString(),
                sessionId = sessionId,
                questionId = pending.questionId,
                responseText = pending.responseText,
                responseMode = mode,
                respondedAt = Instant.fromEpochMilliseconds(pending.respondedAt),
                thinkingTimeSec = pending.thinkingTimeSec,
                audioUrl = null,
                olqScores = emptyMap(),
                confidenceScore = 0
            )

            val submitResult = interviewRepository.submitResponse(response)
            if (submitResult.isFailure) {
                logger.w(tag, "Failed to save response ${index + 1}: ${submitResult.exceptionOrNull()?.message}")
            }
        }
    }
}
