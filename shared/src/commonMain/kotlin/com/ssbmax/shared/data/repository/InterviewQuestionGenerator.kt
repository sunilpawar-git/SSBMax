package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.constants.InterviewConstants
import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.QuestionCacheRepository
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.service.AIService

/**
 * KMP port of the Android original's `InterviewQuestionGenerator` (same package-private helper
 * class the Android `FirestoreInterviewRepository` delegates question generation to).
 *
 * Strategy (unchanged from the Android original):
 * 1. Try to get questions from cache (fast, no API cost) — 70% PIQ-based, 25% generic pool.
 * 2. If cache is short, use AI ([AIService.generatePIQBasedQuestions]) to fill the gap from PIQ data.
 * 3. Cache the AI-generated questions for future use (30-day expiration).
 * 4. Fall back to [FALLBACK_INTERVIEW_QUESTIONS] only if AI also fails.
 *
 * The Android original took an Android `Context` (to read the bundled JSON fallback asset) and a
 * `PIQDataMapper` (`@Inject`-constructed helper, no state). Neither survives the port as
 * constructor dependencies: the fallback list is [FALLBACK_INTERVIEW_QUESTIONS] (see its file doc
 * for why), and PIQ-context building is the stateless [PIQContextBuilder] object.
 */
class InterviewQuestionGenerator(
    private val questionCacheRepository: QuestionCacheRepository,
    private val aiService: AIService,
    private val submissionRepository: SubmissionRepository
) {

    suspend fun generateQuestions(piqSnapshotId: String, count: Int): Result<List<InterviewQuestion>> = try {
        val piqCount = (count * InterviewConstants.PIQ_QUESTION_RATIO).toInt()
        val genericCount = (count * InterviewConstants.GENERIC_QUESTION_RATIO).toInt()

        // STEP 1: Try to get questions from cache
        val piqQuestions = questionCacheRepository.getPIQQuestions(
            piqSnapshotId = piqSnapshotId,
            limit = piqCount,
            excludeUsed = true
        ).getOrDefault(emptyList())

        val genericQuestions = questionCacheRepository.getGenericQuestions(
            targetOLQs = null,
            difficulty = InterviewConstants.MEDIUM_DIFFICULTY,
            limit = genericCount,
            excludeUsed = true
        ).getOrDefault(emptyList())

        val allQuestions = (piqQuestions + genericQuestions).toMutableList()

        // STEP 2: If cache has insufficient questions, generate missing via AI
        if (allQuestions.size < count) {
            val missingCount = count - allQuestions.size
            allQuestions.addAll(generateAIQuestions(piqSnapshotId, missingCount))
        }

        // STEP 3: If still insufficient, use fallback questions
        if (allQuestions.size < count) {
            val stillMissing = count - allQuestions.size
            allQuestions.addAll(FALLBACK_INTERVIEW_QUESTIONS.take(stillMissing))
        }

        Result.success(allQuestions.shuffled().take(count))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Generate questions using AI from PIQ data, via the comprehensive [PIQContextBuilder] context
     * (all ~60 PIQ fields), matching the Android original's `generateAIQuestions`.
     */
    private suspend fun generateAIQuestions(piqSnapshotId: String, count: Int): List<InterviewQuestion> {
        val piqSubmissionMap = submissionRepository.getSubmission(piqSnapshotId).getOrNull() ?: return emptyList()

        val piqContext = PIQContextBuilder.buildComprehensivePIQContext(piqSubmissionMap)
        if (piqContext.isBlank() || piqContext.contains("Error processing PIQ")) {
            return emptyList()
        }

        val aiQuestions = aiService.generatePIQBasedQuestions(
            piqData = piqContext,
            targetOLQs = null,
            count = count,
            difficulty = InterviewConstants.MEDIUM_DIFFICULTY
        ).getOrNull()

        if (aiQuestions.isNullOrEmpty()) {
            return emptyList()
        }

        // Cache the AI-generated questions for future use
        questionCacheRepository.cachePIQQuestions(
            piqSnapshotId = piqSnapshotId,
            questions = aiQuestions,
            expirationDays = InterviewConstants.DEFAULT_CACHE_EXPIRATION_DAYS
        )

        return aiQuestions
    }
}
