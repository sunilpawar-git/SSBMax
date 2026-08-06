package com.ssbmax.shared.domain.usecase

import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.TestEligibility
import com.ssbmax.shared.domain.model.interview.InterviewLimits
import com.ssbmax.shared.domain.model.interview.OIRStatus
import com.ssbmax.shared.domain.model.interview.PIQStatus
import com.ssbmax.shared.domain.model.interview.PPDTStatus
import com.ssbmax.shared.domain.model.interview.PrerequisiteCheckResult
import com.ssbmax.shared.domain.model.interview.SubscriptionStatus
import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.SubscriptionRepository
import com.ssbmax.shared.domain.usecase.subscription.nextMonthResetLabel

/**
 * Use case to check if user meets all prerequisites for starting an interview
 *
 * Prerequisites:
 * 1. PIQ must be completed (submitted with form data)
 * 2. OIR must be completed with score >= 50%
 * 3. PPDT must be completed
 * 4. User must have remaining interviews based on subscription — the monthly limit per tier is
 *    the "Interview" row of [com.ssbmax.shared.data.repository.SubscriptionLimits], the single
 *    source of truth; TTS quality (Android vs Qwen) is decided separately by
 *    [InterviewLimits.getTTSService].
 *
 * Note: PIQ AI quality score is optional user feedback, not required for interview.
 * The interview only needs the PIQ form data (background, family, education, etc.)
 */
class CheckInterviewPrerequisitesUseCase constructor(
    private val submissionRepository: SubmissionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val interviewRepository: InterviewRepository
) {

    /**
     * Check all prerequisites for interview eligibility
     *
     * @param userId User to check
     * @param bypassSubscriptionCheck If true, skip subscription validation (for debug builds)
     * @param bypassPrerequisites If true, skip all prerequisite checks (PIQ, OIR score, PPDT) for testing
     * @return Prerequisite check result with detailed status
     */
    suspend operator fun invoke(
        userId: String,
        bypassSubscriptionCheck: Boolean = false,
        bypassPrerequisites: Boolean = false
    ): Result<PrerequisiteCheckResult> {
        return try {
            // If bypassing prerequisites, return eligible result immediately
            if (bypassPrerequisites) {
                return Result.success(buildBypassResult(userId, bypassSubscriptionCheck))
            }

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

            val assessment = assessFailures(piqStatus, oirStatus, ppdtStatus, subscriptionStatus, bypassSubscriptionCheck)

            val result = PrerequisiteCheckResult(
                isEligible = assessment.failureReasons.isEmpty() && assessment.limitReached == null,
                piqStatus = piqStatus,
                oirStatus = oirStatus,
                ppdtStatus = ppdtStatus,
                subscriptionStatus = subscriptionStatus,
                failureReasons = assessment.failureReasons,
                limitReached = assessment.limitReached
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private class FailureAssessment(val failureReasons: List<String>, val limitReached: TestEligibility.LimitReached?)

    /**
     * Collects the plain-string failure reasons for PIQ/OIR/PPDT, plus the subscription outcome.
     * A genuine interview limit (real `subscriptionTier` on [SubscriptionStatus.LimitReached]) is
     * carried in `limitReached` instead of one more sentence here -- the two fallback paths (tier
     * lookup itself failed) have no real tier, so they still fall back to a plain string.
     */
    private fun assessFailures(
        piqStatus: PIQStatus,
        oirStatus: OIRStatus,
        ppdtStatus: PPDTStatus,
        subscriptionStatus: SubscriptionStatus,
        bypassSubscriptionCheck: Boolean
    ): FailureAssessment {
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

        var limitReached: TestEligibility.LimitReached? = null
        if (!bypassSubscriptionCheck && subscriptionStatus is SubscriptionStatus.LimitReached) {
            val genuineTier = subscriptionStatus.subscriptionTier
            if (genuineTier != null) {
                limitReached = TestEligibility.LimitReached(
                    tier = genuineTier,
                    limit = subscriptionStatus.limit,
                    usedCount = subscriptionStatus.used,
                    resetsAt = nextMonthResetLabel()
                )
            } else {
                failureReasons.add(
                    "Interview limit reached for ${subscriptionStatus.tier} tier " +
                        "(${subscriptionStatus.used}/${subscriptionStatus.limit})"
                )
            }
        }

        return FailureAssessment(failureReasons, limitReached)
    }

    /**
     * Fully-eligible result for [bypassPrerequisites][invoke] (debug testing only), mocking
     * any PIQ/OIR/PPDT submission that isn't already there.
     */
    private suspend fun buildBypassResult(userId: String, bypassSubscriptionCheck: Boolean): PrerequisiteCheckResult {
        val mockPiqStatus = try {
            val submission = submissionRepository.getLatestPIQSubmission(userId).getOrNull()
            if (submission != null) {
                PIQStatus.Completed(submissionId = submission.id, aiScore = submission.aiPreliminaryScore?.overallScore ?: 0f)
            } else {
                PIQStatus.Completed(submissionId = "DEBUG", aiScore = 0f)
            }
        } catch (e: Exception) {
            PIQStatus.Completed(submissionId = "DEBUG", aiScore = 0f)
        }

        val mockOirStatus = try {
            val submission = submissionRepository.getLatestOIRSubmission(userId).getOrNull()
            if (submission != null) {
                OIRStatus.Completed(submissionId = submission.id, score = submission.testResult.percentageScore)
            } else {
                OIRStatus.Completed(submissionId = "DEBUG", score = 100f)
            }
        } catch (e: Exception) {
            OIRStatus.Completed(submissionId = "DEBUG", score = 100f)
        }

        val mockPpdtStatus = try {
            val submission = submissionRepository.getLatestPPDTSubmission(userId).getOrNull()
            PPDTStatus.Completed(submission?.submissionId ?: "DEBUG")
        } catch (e: Exception) {
            PPDTStatus.Completed("DEBUG")
        }

        val subscriptionStatus = if (bypassSubscriptionCheck) {
            SubscriptionStatus.Available(tier = "DEBUG", remaining = Int.MAX_VALUE)
        } else {
            checkSubscriptionStatus(userId)
        }

        return PrerequisiteCheckResult(
            isEligible = true,
            piqStatus = mockPiqStatus,
            oirStatus = mockOirStatus,
            ppdtStatus = mockPpdtStatus,
            subscriptionStatus = subscriptionStatus,
            failureReasons = emptyList()
        )
    }

    /** PIQ is complete once submitted (has form data) -- AI quality score is optional feedback, not a requirement. */
    private suspend fun checkPIQStatus(userId: String): PIQStatus {
        val piqResult = submissionRepository.getLatestPIQSubmission(userId)
        return if (piqResult.isFailure || piqResult.getOrNull() == null) {
            PIQStatus.NotStarted
        } else {
            val submission = piqResult.getOrNull()!!
            PIQStatus.Completed(submissionId = submission.id, aiScore = submission.aiPreliminaryScore?.overallScore ?: 0f)
        }
    }

    /** OIR completion and score threshold (>= 50%). */
    private suspend fun checkOIRStatus(userId: String): OIRStatus {
        val oirResult = submissionRepository.getLatestOIRSubmission(userId)
        return if (oirResult.isFailure || oirResult.getOrNull() == null) {
            OIRStatus.NotStarted
        } else {
            val submission = oirResult.getOrNull()!!
            val score = submission.testResult.percentageScore
            if (score >= 50f) OIRStatus.Completed(submissionId = submission.id, score = score) else OIRStatus.CompletedBelowThreshold(score)
        }
    }

    /** PPDT completion. */
    private suspend fun checkPPDTStatus(userId: String): PPDTStatus {
        val ppdtResult = submissionRepository.getLatestPPDTSubmission(userId)
        return if (ppdtResult.isFailure || ppdtResult.getOrNull() == null) {
            PPDTStatus.NotStarted
        } else {
            PPDTStatus.Completed(ppdtResult.getOrNull()!!.submissionId)
        }
    }

    /** Subscription tier and interview limit, sourced from [InterviewLimits]. */
    private suspend fun checkSubscriptionStatus(userId: String): SubscriptionStatus {
        val tierResult = subscriptionRepository.getSubscriptionTier(userId)
        if (tierResult.isFailure) {
            return SubscriptionStatus.LimitReached(tier = "Unknown", used = 0, limit = 0)
        }
        val tier = tierResult.getOrNull()
            ?: return SubscriptionStatus.LimitReached(tier = "Unknown", used = 0, limit = 0)

        // Used count sums all interview modes for the unified interview system.
        val stats = interviewRepository.getInterviewStats(userId).getOrNull() ?: emptyMap()
        val limits = InterviewLimits.forSubscription(tier, stats.values.sum())

        return if (limits.canStartInterview()) {
            SubscriptionStatus.Available(tier = tier.displayName, remaining = limits.remaining)
        } else {
            SubscriptionStatus.LimitReached(
                tier = tier.displayName,
                used = limits.used,
                limit = limits.totalLimit,
                subscriptionTier = tier
            )
        }
    }
}
