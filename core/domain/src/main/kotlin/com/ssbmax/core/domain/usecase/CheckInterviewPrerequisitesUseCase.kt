package com.ssbmax.core.domain.usecase

import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.interview.InterviewLimits
import com.ssbmax.core.domain.model.interview.OIRStatus
import com.ssbmax.core.domain.model.interview.PIQStatus
import com.ssbmax.core.domain.model.interview.PPDTStatus
import com.ssbmax.core.domain.model.interview.PrerequisiteCheckResult
import com.ssbmax.core.domain.model.interview.SubscriptionStatus
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.repository.SubscriptionRepository
import javax.inject.Inject

/**
 * Use case to check if user meets all prerequisites for starting an interview
 *
 * Prerequisites:
 * 1. PIQ must be completed (submitted with form data)
 * 2. OIR must be completed with score >= 50%
 * 3. PPDT must be completed
 * 4. User must have remaining interviews based on subscription:
 *    - FREE: 1 interview/month with Android TTS
 *    - PRO: 1 interview/month with Qwen TTS
 *    - PREMIUM: 3 interviews/month with Qwen TTS
 *
 * Note: PIQ AI quality score is optional user feedback, not required for interview.
 * The interview only needs the PIQ form data (background, family, education, etc.)
 */
class CheckInterviewPrerequisitesUseCase @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val interviewRepository: InterviewRepository
) {

    /**
     * Check all prerequisites for interview eligibility
     *
     * @param userId User to check
     * @param bypassSubscriptionCheck If true, skip subscription validation (for debug builds)
     * @return Prerequisite check result with detailed status
     */
    suspend operator fun invoke(
        userId: String,
        bypassSubscriptionCheck: Boolean = false
    ): Result<PrerequisiteCheckResult> {
        return try {
            // 1. Check PIQ status
            val piqStatus = checkPIQStatus(userId)

            // 2. Check OIR status
            val oirStatus = checkOIRStatus(userId)

            // 3. Check PPDT status
            val ppdtStatus = checkPPDTStatus(userId)

            // 4. Check subscription and limits (bypass in debug mode)
            val subscriptionStatus = if (bypassSubscriptionCheck) {
                // Debug mode: Mock as available with unlimited interviews
                SubscriptionStatus.Available(
                    tier = "DEBUG",
                    remaining = Int.MAX_VALUE
                )
            } else {
                checkSubscriptionStatus(userId)
            }

            // Collect failure reasons
            val failureReasons = mutableListOf<String>()

            when (piqStatus) {
                is PIQStatus.NotStarted -> failureReasons.add("Complete Personal Information Questionnaire (PIQ)")
                is PIQStatus.Completed -> {} // Valid
            }

            when (oirStatus) {
                is OIRStatus.NotStarted -> failureReasons.add("Complete Officer Intelligence Rating (OIR) test")
                is OIRStatus.CompletedBelowThreshold -> failureReasons.add("Score at least 50% in OIR test (current: ${oirStatus.score.toInt()}%)")
                is OIRStatus.Completed -> {} // Valid
            }

            when (ppdtStatus) {
                is PPDTStatus.NotStarted -> failureReasons.add("Complete Picture Perception & Description Test (PPDT)")
                is PPDTStatus.Completed -> {} // Valid
            }

            // Only add subscription failure if not bypassed
            if (!bypassSubscriptionCheck) {
                when (subscriptionStatus) {
                    is SubscriptionStatus.LimitReached -> failureReasons.add("Interview limit reached for ${subscriptionStatus.tier} tier (${subscriptionStatus.used}/${subscriptionStatus.limit})")
                    is SubscriptionStatus.Available -> {} // Valid
                }
            }

            val isEligible = failureReasons.isEmpty()

            val result = PrerequisiteCheckResult(
                isEligible = isEligible,
                piqStatus = piqStatus,
                oirStatus = oirStatus,
                ppdtStatus = ppdtStatus,
                subscriptionStatus = subscriptionStatus,
                failureReasons = failureReasons
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check PIQ completion status
     *
     * Only checks if PIQ is submitted with form data.
     * AI quality score is optional and not required for interview eligibility.
     */
    private suspend fun checkPIQStatus(userId: String): PIQStatus {
        // Get latest PIQ submission
        val piqResult = submissionRepository.getLatestPIQSubmission(userId)

        return if (piqResult.isFailure || piqResult.getOrNull() == null) {
            PIQStatus.NotStarted
        } else {
            val submission = piqResult.getOrNull()!!

            // PIQ is completed if it's submitted (has form data)
            // AI quality score is optional feedback, not a requirement
            PIQStatus.Completed(
                submissionId = submission.id,
                aiScore = submission.aiPreliminaryScore?.overallScore ?: 0f
            )
        }
    }

    /**
     * Check OIR completion and score threshold
     */
    private suspend fun checkOIRStatus(userId: String): OIRStatus {
        // Get latest OIR submission
        val oirResult = submissionRepository.getLatestOIRSubmission(userId)

        return if (oirResult.isFailure || oirResult.getOrNull() == null) {
            OIRStatus.NotStarted
        } else {
            val submission = oirResult.getOrNull()!!
            val score = submission.testResult.percentageScore

            if (score >= 50f) {
                OIRStatus.Completed(
                    submissionId = submission.id,
                    score = score
                )
            } else {
                OIRStatus.CompletedBelowThreshold(score)
            }
        }
    }

    /**
     * Check PPDT completion
     */
    private suspend fun checkPPDTStatus(userId: String): PPDTStatus {
        // Get latest PPDT submission
        val ppdtResult = submissionRepository.getLatestPPDTSubmission(userId)

        return if (ppdtResult.isFailure || ppdtResult.getOrNull() == null) {
            PPDTStatus.NotStarted
        } else {
            val submission = ppdtResult.getOrNull()!!
            PPDTStatus.Completed(submission.submissionId)
        }
    }

    /**
     * Check subscription tier and interview limits
     */
    private suspend fun checkSubscriptionStatus(userId: String): SubscriptionStatus {
        // Get current subscription tier
        val tierResult = subscriptionRepository.getSubscriptionTier(userId)

        if (tierResult.isFailure) {
            return SubscriptionStatus.LimitReached(
                tier = "Unknown",
                used = 0,
                limit = 0
            )
        }

        val tier = tierResult.getOrNull()
            ?: return SubscriptionStatus.LimitReached(
                tier = "Unknown",
                used = 0,
                limit = 0
            )

        // Convert SubscriptionTier to SubscriptionType
        val subscriptionType = com.ssbmax.core.domain.model.SubscriptionType.valueOf(tier.name)

        // Get used count (sum all interview modes for unified system)
        val statsResult = interviewRepository.getInterviewStats(userId)
        val stats = statsResult.getOrNull() ?: emptyMap()
        val used = stats.values.sum()

        // Calculate limits using new InterviewLimits API
        val limits = InterviewLimits.forSubscription(subscriptionType, used)

        return if (limits.canStartInterview()) {
            SubscriptionStatus.Available(
                tier = tier.displayName,
                remaining = limits.remaining
            )
        } else {
            SubscriptionStatus.LimitReached(
                tier = tier.displayName,
                used = limits.used,
                limit = limits.totalLimit
            )
        }
    }
}
