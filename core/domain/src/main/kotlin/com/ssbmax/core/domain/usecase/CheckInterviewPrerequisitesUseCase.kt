package com.ssbmax.core.domain.usecase

import com.ssbmax.core.domain.model.interview.InterviewLimits
import com.ssbmax.core.domain.model.interview.InterviewMode
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
 * 1. PIQ must be completed with AI score
 * 2. OIR must be completed with score >= 50%
 * 3. PPDT must be completed
 * 4. User must have Pro/Premium subscription with remaining interviews
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
     * @param desiredMode Desired interview mode (text or voice)
     * @param bypassSubscriptionCheck If true, skip subscription validation (for debug builds)
     * @return Prerequisite check result with detailed status
     */
    suspend operator fun invoke(
        userId: String,
        desiredMode: InterviewMode,
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
                    remaining = Int.MAX_VALUE,
                    mode = desiredMode
                )
            } else {
                checkSubscriptionStatus(userId, desiredMode)
            }

            // Collect failure reasons
            val failureReasons = mutableListOf<String>()

            when (piqStatus) {
                is PIQStatus.NotStarted -> failureReasons.add("Complete Personal Information Questionnaire (PIQ)")
                is PIQStatus.ScoringInProgress -> failureReasons.add("Wait for PIQ AI scoring to complete")
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
                    is SubscriptionStatus.FreeTier -> failureReasons.add("Upgrade to Pro or Premium subscription")
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
     * Check PIQ completion and AI scoring status
     */
    private suspend fun checkPIQStatus(userId: String): PIQStatus {
        // Get latest PIQ submission
        val piqResult = submissionRepository.getLatestPIQSubmission(userId)

        return if (piqResult.isFailure || piqResult.getOrNull() == null) {
            PIQStatus.NotStarted
        } else {
            val submission = piqResult.getOrNull()!!

            // Check if AI score is available
            val aiScore = submission.aiPreliminaryScore?.overallScore

            if (aiScore == null) {
                PIQStatus.ScoringInProgress
            } else {
                PIQStatus.Completed(
                    submissionId = submission.id,
                    aiScore = aiScore
                )
            }
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
    private suspend fun checkSubscriptionStatus(
        userId: String,
        desiredMode: InterviewMode
    ): SubscriptionStatus {
        // Get current subscription tier
        val tierResult = subscriptionRepository.getSubscriptionTier(userId)

        if (tierResult.isFailure) {
            return SubscriptionStatus.FreeTier
        }

        val tier = tierResult.getOrNull() ?: return SubscriptionStatus.FreeTier

        // Free tier has no access
        if (tier.displayName.uppercase() == "FREE") {
            return SubscriptionStatus.FreeTier
        }

        // Check if voice mode is requested but user has Pro tier
        if (desiredMode == InterviewMode.VOICE_BASED && tier.displayName.uppercase() == "PRO") {
            return SubscriptionStatus.FreeTier // Pro doesn't support voice mode
        }

        // Check remaining interviews
        val remainingResult = interviewRepository.getRemainingInterviews(userId, desiredMode)

        if (remainingResult.isFailure) {
            return SubscriptionStatus.FreeTier
        }

        val remaining = remainingResult.getOrNull() ?: 0

        // Get usage stats to determine used count
        val statsResult = interviewRepository.getInterviewStats(userId)
        val used = statsResult.getOrNull()?.get(desiredMode) ?: 0

        // Get limit from centralized constants
        val limit = InterviewLimits.getLimit(tier.displayName, desiredMode)

        return if (remaining > 0) {
            SubscriptionStatus.Available(
                tier = tier.displayName,
                remaining = remaining,
                mode = desiredMode
            )
        } else {
            SubscriptionStatus.LimitReached(
                tier = tier.displayName,
                used = used,
                limit = limit
            )
        }
    }
}
