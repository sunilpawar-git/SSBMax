package com.ssbmax.shared.analysis

import com.ssbmax.shared.domain.model.interview.InterviewMode
import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.InterviewResponse
import com.ssbmax.shared.domain.model.interview.InterviewSession
import com.ssbmax.shared.domain.model.interview.InterviewStatus
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.model.interview.QuestionSource
import com.ssbmax.shared.domain.service.OLQScoreWithReasoning
import com.ssbmax.shared.domain.service.ResponseAnalysis
import com.ssbmax.shared.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.shared.presentation.testing.FakeAIService
import com.ssbmax.shared.presentation.testing.FakeGTORepository
import com.ssbmax.shared.presentation.testing.FakeInterviewRepository
import com.ssbmax.shared.presentation.testing.FakeSubmissionRepository
import com.ssbmax.shared.presentation.testing.RecordingLogger
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Phase 8 (KMP-convergence plan): characterization test pinning
 * [InterviewAnalysisOrchestrator] -- `app`'s `InterviewAnalysisWorker` now delegates the
 * whole per-response AI-analysis + aggregation flow to this class instead of
 * re-implementing it.
 */
class InterviewAnalysisOrchestratorTest {

    private fun singleOlqAnalysis(): ResponseAnalysis = ResponseAnalysis(
        olqScores = mapOf(OLQ.INITIATIVE to OLQScoreWithReasoning(OLQ.INITIATIVE, 4f, "reasoning")),
        overallConfidence = 80,
        keyInsights = listOf("insight")
    )

    private fun session() = InterviewSession(
        id = "session-1",
        userId = "user-1",
        mode = InterviewMode.VOICE_BASED,
        status = InterviewStatus.PENDING_ANALYSIS,
        startedAt = Instant.fromEpochMilliseconds(0),
        piqSnapshotId = "piq-1",
        consentGiven = true,
        questionIds = listOf("q-1")
    )

    private fun response() = InterviewResponse(
        id = "resp-1",
        sessionId = "session-1",
        questionId = "q-1",
        responseText = "my answer",
        responseMode = InterviewMode.VOICE_BASED,
        respondedAt = Instant.fromEpochMilliseconds(0),
        thinkingTimeSec = 10
    )

    @Test
    fun `analyze scores each response and completes the interview`() = kotlinx.coroutines.test.runTest {
        val interviewRepo = FakeInterviewRepository().apply {
            getSessionResult = Result.success(session())
            getResponsesResult = Result.success(listOf(response()))
            getQuestionResult = Result.success(
                InterviewQuestion(id = "q-1", questionText = "text", expectedOLQs = listOf(OLQ.INITIATIVE), source = QuestionSource.GENERIC_POOL)
            )
            getResponseResult = Result.success(response().copy(olqScores = mapOf(OLQ.INITIATIVE to OLQScore(4, 80, "reasoning"))))
            completeInterviewResult = Result.success(
                com.ssbmax.shared.domain.model.interview.InterviewResult(
                    id = "result-1", sessionId = "session-1", userId = "user-1", mode = InterviewMode.VOICE_BASED,
                    completedAt = Instant.fromEpochMilliseconds(0), durationSec = 60, totalQuestions = 1, totalResponses = 1,
                    overallOLQScores = emptyMap(), categoryScores = emptyMap(), overallConfidence = 80,
                    strengths = emptyList(), weaknesses = emptyList(), feedback = "feedback", overallRating = 4
                )
            )
        }
        val aiService = FakeAIService().apply { responseAnalysisResult = Result.success(singleOlqAnalysis()) }
        val orchestrator = InterviewAnalysisOrchestrator(
            interviewRepo, aiService,
            GetOLQDashboardUseCase(FakeSubmissionRepository(), FakeGTORepository(), interviewRepo, RecordingLogger()),
            RecordingLogger()
        )

        orchestrator.analyze("session-1")

        assertEquals(1, interviewRepo.updatedResponses.size)
        assertEquals(4, interviewRepo.updatedResponses.first().olqScores[OLQ.INITIATIVE]?.score)
    }

    @Test
    fun `analyze skips sessions that are not PENDING_ANALYSIS`() = kotlinx.coroutines.test.runTest {
        val interviewRepo = FakeInterviewRepository().apply {
            getSessionResult = Result.success(
                session().copy(status = InterviewStatus.COMPLETED, completedAt = Instant.fromEpochMilliseconds(0))
            )
        }
        val aiService = FakeAIService()
        val orchestrator = InterviewAnalysisOrchestrator(
            interviewRepo, aiService,
            GetOLQDashboardUseCase(FakeSubmissionRepository(), FakeGTORepository(), interviewRepo, RecordingLogger()),
            RecordingLogger()
        )

        orchestrator.analyze("session-1")

        assertEquals(0, interviewRepo.updatedResponses.size)
    }
}
