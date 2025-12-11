package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.core.domain.constants.InterviewConstants
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.utils.ErrorLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker for generating interview questions after PIQ submission
 *
 * Triggered immediately after PIQ is submitted, this worker:
 * 1. Fetches PIQ data from Firestore
 * 2. Generates 18 personalized questions using Gemini AI
 * 3. Caches questions for 30 days
 * 4. Enables instant interview start when user is ready
 *
 * Constraints:
 * - Requires network connection (for Gemini API)
 * - Requires battery not low (to avoid draining)
 * - Retry policy: 3 attempts with exponential backoff
 */
@HiltWorker
class InterviewQuestionGenerationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val aiService: AIService,
    private val submissionRepository: SubmissionRepository,
    private val questionCacheRepository: QuestionCacheRepository,
    private val piqDataMapper: com.ssbmax.core.data.repository.interview.PIQDataMapper
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "QuestionGenWorker"
        const val KEY_PIQ_SUBMISSION_ID = "piq_submission_id"
        const val KEY_NOTIFY_ON_COMPLETE = "notify_on_complete"
    }

    override suspend fun doWork(): Result {
        return try {
            val piqSubmissionId = inputData.getString(KEY_PIQ_SUBMISSION_ID)
                ?: return Result.failure()

            Log.d(TAG, "🔄 Starting background question generation for PIQ: $piqSubmissionId")

            // 1. Fetch PIQ data from Firestore
            Log.d(TAG, "   Step 1: Fetching PIQ data from Firestore...")
            val piqResult = submissionRepository.getSubmission(piqSubmissionId)
            val piq = piqResult.getOrNull() ?: run {
                ErrorLogger.log(
                    Exception("PIQ submission not found: $piqSubmissionId"),
                    "Failed to fetch PIQ data for question generation"
                )
                return Result.failure()
            }

            Log.d(TAG, "   ✅ PIQ data fetched successfully")

            // 2. Build comprehensive PIQ context (extracts all 60+ fields)
            Log.d(TAG, "   Step 2a: Building comprehensive PIQ context from all 60+ fields...")
            val piqContext = piqDataMapper.buildComprehensivePIQContext(piq)
            Log.d(TAG, "   ✅ Built comprehensive context (${piqContext.length} chars)")

            // 3. Generate 18 PIQ-based questions with Gemini using comprehensive context
            Log.d(TAG, "   Step 2b: Generating ${InterviewConstants.TARGET_PIQ_QUESTION_COUNT} PIQ-based questions with Gemini AI...")
            val questionsResult = aiService.generatePIQBasedQuestions(
                piqData = piqContext,
                targetOLQs = null, // Let AI determine best OLQs based on PIQ data
                count = InterviewConstants.TARGET_PIQ_QUESTION_COUNT,
                difficulty = InterviewConstants.MEDIUM_DIFFICULTY
            )

            val questions = questionsResult.getOrElse { error ->
                ErrorLogger.log(error, "Failed to generate PIQ questions in background")
                return if (runAttemptCount < InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS) {
                    Log.w(TAG, "   ⚠️ Retry attempt ${runAttemptCount + 1}/${InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS}")
                    Result.retry()
                } else {
                    Log.e(TAG, "   ❌ Max retries reached, failing")
                    Result.failure()
                }
            }

            Log.d(TAG, "   ✅ Generated ${questions.size} questions")

            // 4. Cache questions for 30 days
            Log.d(TAG, "   Step 3: Caching questions to Firestore (${InterviewConstants.DEFAULT_CACHE_EXPIRATION_DAYS}-day expiration)...")
            questionCacheRepository.cachePIQQuestions(
                piqSnapshotId = piqSubmissionId,
                questions = questions,
                expirationDays = InterviewConstants.DEFAULT_CACHE_EXPIRATION_DAYS
            ).getOrElse { error ->
                ErrorLogger.log(error, "Failed to cache PIQ questions")
                return if (runAttemptCount < InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }

            Log.d(TAG, "✅ Successfully generated and cached ${questions.size} PIQ questions")
            Log.d(TAG, "   Interview will start instantly when user is ready!")

            // 4. Optional: Show notification
            if (inputData.getBoolean(KEY_NOTIFY_ON_COMPLETE, false)) {
                // TODO: Implement notification (future enhancement)
                // showCompletionNotification()
            }

            Result.success()
        } catch (e: Exception) {
            ErrorLogger.log(e, "Background question generation failed unexpectedly")
            if (runAttemptCount < InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS) {
                Log.w(TAG, "⚠️ Unexpected error, retrying... (attempt ${runAttemptCount + 1}/${InterviewConstants.MAX_WORKER_RETRY_ATTEMPTS})")
                Result.retry()
            } else {
                Log.e(TAG, "❌ Max retries reached after unexpected error")
                Result.failure()
            }
        }
    }

}
