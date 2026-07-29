package com.ssbmax.shared.analysis

import com.ssbmax.shared.domain.constants.InterviewConstants
import com.ssbmax.shared.domain.model.interview.InterviewResponse
import com.ssbmax.shared.domain.model.interview.InterviewStatus
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.shared.domain.scoring.EntryType
import com.ssbmax.shared.domain.service.AIService
import com.ssbmax.shared.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.validation.ValidationIntegration
import kotlinx.coroutines.delay

/**
 * KMP port of `app`'s `InterviewAnalysisWorker`. Per-response retry uses its own loop
 * rather than [AnalysisRetry.withRetry]: a single interview response is scored against
 * only its question's *expected* OLQs (a subset), not all 15, so the shared helper's
 * "accept only if >=14/15 OLQs present" acceptance rule doesn't apply here -- this
 * matches the Android original's own separate, simpler `analyzeResponseWithRetry`.
 */
class InterviewAnalysisOrchestrator(
    private val interviewRepository: InterviewRepository,
    private val aiService: AIService,
    private val getOLQDashboard: GetOLQDashboardUseCase,
    private val logger: DomainLogger
) {
    suspend fun analyze(sessionId: String) {
        val session = interviewRepository.getSession(sessionId).getOrNull() ?: run {
            logger.e(TAG, "Interview session not found: $sessionId")
            return
        }
        if (session.status != InterviewStatus.PENDING_ANALYSIS) return

        val responses = interviewRepository.getResponses(sessionId).getOrNull()
        if (responses.isNullOrEmpty()) {
            logger.e(TAG, "No responses found for interview session: $sessionId")
            markSessionFailed(sessionId)
            return
        }

        responses.forEachIndexed { index, response ->
            val analysis = analyzeResponseWithRetry(response)
            val updated = if (analysis != null) {
                response.copy(olqScores = analysis, confidenceScore = analysis.values.map { it.confidence }.average().toInt())
            } else {
                response.copy(olqScores = fallbackOLQScores(), confidenceScore = InterviewConstants.FALLBACK_CONFIDENCE)
            }
            interviewRepository.updateResponse(updated)
            if (index < responses.size - 1) delay(InterviewConstants.API_CALL_DELAY_MS)
        }

        val aggregated = aggregateOLQScores(
            responses.mapNotNull { interviewRepository.getResponse(it.id).getOrNull()?.olqScores }
        )
        if (aggregated.isNotEmpty()) {
            ValidationIntegration.validateScores(aggregated, EntryType.NDA)
        }

        val resultResult = interviewRepository.completeInterview(sessionId)
        if (resultResult.isFailure) {
            logger.e(TAG, "Failed to complete interview: $sessionId")
            markSessionFailed(sessionId)
            return
        }

        runCatching { getOLQDashboard.invalidateCache(session.userId) }
            .onFailure { logger.w(TAG, "Failed to invalidate dashboard cache: ${it.message}") }
    }

    private suspend fun analyzeResponseWithRetry(response: InterviewResponse): Map<OLQ, OLQScore>? {
        repeat(InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS) { attempt ->
            try {
                val question = interviewRepository.getQuestion(response.questionId).getOrNull() ?: return null
                val result = aiService.analyzeResponse(question, response.responseText, response.responseMode.name)
                if (result.isSuccess) {
                    val analysis = result.getOrNull()!!
                    return analysis.olqScores.mapValues { (_, scoreWithReasoning) ->
                        OLQScore(
                            score = scoreWithReasoning.score.toInt().coerceIn(1, 10),
                            confidence = analysis.overallConfidence,
                            reasoning = scoreWithReasoning.reasoning
                        )
                    }
                }
            } catch (e: Exception) {
                logger.w(TAG, "Interview response analysis attempt ${attempt + 1} failed: ${e.message}")
            }
            if (attempt < InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS - 1) {
                delay(InterviewConstants.RETRY_DELAY_MS * (attempt + 1))
            }
        }
        return null
    }

    private fun fallbackOLQScores(): Map<OLQ, OLQScore> = OLQ.entries.take(5).associateWith {
        OLQScore(
            score = InterviewConstants.FALLBACK_OLQ_SCORE,
            confidence = InterviewConstants.FALLBACK_CONFIDENCE,
            reasoning = "AI analysis unavailable - neutral score assigned"
        )
    }

    private fun aggregateOLQScores(allScores: List<Map<OLQ, OLQScore>>): Map<OLQ, OLQScore> {
        if (allScores.isEmpty()) return emptyMap()
        return OLQ.entries.associateWith { olq ->
            val scoresForOlq = allScores.mapNotNull { it[olq] }
            if (scoresForOlq.isEmpty()) {
                OLQScore(score = 6, confidence = 30, reasoning = "No data")
            } else {
                OLQScore(
                    score = scoresForOlq.map { it.score }.average().toInt().coerceIn(1, 10),
                    confidence = scoresForOlq.map { it.confidence }.average().toInt(),
                    reasoning = "Aggregated from ${scoresForOlq.size} responses"
                )
            }
        }
    }

    private suspend fun markSessionFailed(sessionId: String) {
        val session = interviewRepository.getSession(sessionId).getOrNull() ?: return
        interviewRepository.updateSession(session.copy(status = InterviewStatus.FAILED))
    }

    private companion object {
        const val TAG = "InterviewAnalysisOrchestrator"
    }
}
