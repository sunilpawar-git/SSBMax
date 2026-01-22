package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.core.data.ai.prompts.PsychologyTestPrompts
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.notifications.NotificationHelper
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class WATAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val submissionRepository: SubmissionRepository,
    private val aiService: AIService,
    private val notificationHelper: NotificationHelper,
    private val getOLQDashboard: GetOLQDashboardUseCase
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SUBMISSION_ID = "submission_id"
        private const val TAG = "WATAnalysisWorker"
        private const val MAX_AI_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    override suspend fun doWork(): Result {
        val submissionId = inputData.getString(KEY_SUBMISSION_ID) ?: return Result.failure()
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🔄 Starting WAT analysis for submission: $submissionId")

        return try {
            val submission = submissionRepository.getWATSubmission(submissionId).getOrNull()
                ?: return Result.failure()

            if (submission.analysisStatus != AnalysisStatus.PENDING_ANALYSIS) {
                return Result.success()
            }

            submissionRepository.updateWATAnalysisStatus(submissionId, AnalysisStatus.ANALYZING)
            val prompt = PsychologyTestPrompts.generateWATAnalysisPrompt(submission)
            val olqScores = analyzeSubmissionWithRetry(prompt) ?: return handleAnalysisFailure(submissionId)

            val olqResult = createOLQResult(submissionId, olqScores)
            // Note: updateWATOLQResult atomically sets BOTH olqResult AND analysisStatus=COMPLETED
            submissionRepository.updateWATOLQResult(submissionId, olqResult)

            // Invalidate dashboard cache AFTER result is saved
            // CRITICAL: Must happen after result is in Firestore, not at submission time
            try {
                getOLQDashboard.invalidateCache(submission.userId)
                Log.d(TAG, "   Dashboard cache invalidated for user: ${submission.userId}")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to invalidate cache: ${e.message}")
            }

            notificationHelper.showWATResultsReadyNotification(submissionId)
            Log.d(TAG, "🎉 WAT analysis completed in ${System.currentTimeMillis() - startTime}ms")
            Result.success()
        } catch (e: Exception) {
            ErrorLogger.log(e, "WAT analysis worker failed")
            if (runAttemptCount < MAX_AI_RETRIES) Result.retry() else {
                handleAnalysisFailure(submissionId)
                Result.failure()
            }
        }
    }

    private suspend fun analyzeSubmissionWithRetry(prompt: String): Map<OLQ, OLQScore>? {
        repeat(MAX_AI_RETRIES) { attempt ->
            try {
                val analysisResult = aiService.analyzeWATResponse(prompt)
                if (analysisResult.isSuccess) {
                    val analysis = analysisResult.getOrNull()!!
                    val olqScores = analysis.olqScores.mapValues { (_, scoreWithReasoning) ->
                        OLQScore(
                            score = scoreWithReasoning.score.toInt().coerceIn(1, 10),
                            confidence = analysis.overallConfidence,
                            reasoning = scoreWithReasoning.reasoning
                        )
                    }
                    if (olqScores.size >= 14) {
                        return if (olqScores.size == 15) olqScores else fillMissingOLQs(olqScores)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Analysis attempt ${attempt + 1} failed: ${e.message}")
            }
            if (attempt < MAX_AI_RETRIES - 1) delay(RETRY_DELAY_MS * (attempt + 1) * 2)
        }
        return null
    }

    private fun fillMissingOLQs(scores: Map<OLQ, OLQScore>): Map<OLQ, OLQScore> {
        val mutable = scores.toMutableMap()
        OLQ.entries.forEach { olq ->
            if (olq !in mutable) {
                mutable[olq] = OLQScore(score = 6, confidence = 30, reasoning = "AI did not assess this OLQ - neutral score assigned")
            }
        }
        return mutable
    }

    private fun createOLQResult(submissionId: String, olqScores: Map<OLQ, OLQScore>): OLQAnalysisResult {
        val overallScore = olqScores.values.map { it.score }.average().toFloat()
        val overallRating = when {
            overallScore <= 3.0f -> "Exceptional"
            overallScore <= 5.0f -> "Good"
            overallScore <= 7.0f -> "Average"
            else -> "Needs Improvement"
        }
        val strengths = olqScores.entries.sortedBy { it.value.score }.take(3)
            .map { "${it.key.displayName} (${it.value.score})" }
        val weaknesses = olqScores.entries.sortedByDescending { it.value.score }.take(3)
            .map { "${it.key.displayName} (${it.value.score})" }

        return OLQAnalysisResult(
            submissionId = submissionId,
            testType = TestType.WAT,
            olqScores = olqScores,
            overallScore = overallScore,
            overallRating = overallRating,
            strengths = strengths,
            weaknesses = weaknesses,
            recommendations = listOf(
                "Continue practicing WAT with diverse word associations",
                "Focus on improving identified weak areas",
                "Maintain quick and positive responses"
            ),
            analyzedAt = System.currentTimeMillis(),
            aiConfidence = 85
        )
    }

    private suspend fun handleAnalysisFailure(submissionId: String): Result {
        try {
            submissionRepository.updateWATAnalysisStatus(submissionId, AnalysisStatus.FAILED)
            notificationHelper.showWATAnalysisFailedNotification(submissionId)
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to update WAT submission status to FAILED")
        }
        return Result.failure()
    }
}
