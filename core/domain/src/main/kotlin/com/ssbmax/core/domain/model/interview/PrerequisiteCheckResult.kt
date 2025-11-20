package com.ssbmax.core.domain.model.interview

/**
 * Result of prerequisite validation for interview eligibility
 *
 * @param isEligible Whether user meets all prerequisites
 * @param piqStatus PIQ completion and AI scoring status
 * @param oirStatus OIR completion and score status
 * @param ppdtStatus PPDT completion status
 * @param subscriptionStatus Subscription tier and limit status
 * @param failureReasons List of reasons for ineligibility (if any)
 */
data class PrerequisiteCheckResult(
    val isEligible: Boolean,
    val piqStatus: PIQStatus,
    val oirStatus: OIRStatus,
    val ppdtStatus: PPDTStatus,
    val subscriptionStatus: SubscriptionStatus,
    val failureReasons: List<String> = emptyList()
) {
    init {
        if (!isEligible) {
            require(failureReasons.isNotEmpty()) { "Ineligible result must have failure reasons" }
        }
    }

    /**
     * Get user-friendly eligibility message
     */
    fun getEligibilityMessage(): String {
        return if (isEligible) {
            "You are eligible to start the interview"
        } else {
            "You must complete the following requirements:\n${failureReasons.joinToString("\n• ", "• ")}"
        }
    }

    /**
     * Get completion progress (0-100)
     */
    fun getCompletionProgress(): Int {
        var completedSteps = 0
        var totalSteps = 4

        if (piqStatus is PIQStatus.Completed) completedSteps++
        if (oirStatus is OIRStatus.Completed) completedSteps++
        if (ppdtStatus is PPDTStatus.Completed) completedSteps++
        if (subscriptionStatus is SubscriptionStatus.Available) completedSteps++

        return (completedSteps * 100) / totalSteps
    }
}

/**
 * PIQ completion and AI scoring status
 */
sealed class PIQStatus {
    /**
     * PIQ not started yet
     */
    object NotStarted : PIQStatus()

    /**
     * PIQ submitted but AI scoring in progress
     */
    object ScoringInProgress : PIQStatus()

    /**
     * PIQ completed with AI score available
     *
     * @param submissionId PIQ submission ID for fetching data
     * @param aiScore AI-generated score (0-100)
     */
    data class Completed(
        val submissionId: String,
        val aiScore: Float
    ) : PIQStatus() {
        init {
            require(submissionId.isNotBlank()) { "Submission ID cannot be blank" }
            require(aiScore in 0f..100f) { "AI score must be between 0 and 100" }
        }
    }

    val displayName: String
        get() = when (this) {
            is NotStarted -> "Not Started"
            is ScoringInProgress -> "Scoring in Progress"
            is Completed -> "Completed"
        }
}

/**
 * OIR completion and score status
 */
sealed class OIRStatus {
    /**
     * OIR not started yet
     */
    object NotStarted : OIRStatus()

    /**
     * OIR completed but score is below 50%
     *
     * @param score OIR score (0-100)
     */
    data class CompletedBelowThreshold(val score: Float) : OIRStatus() {
        init {
            require(score in 0f..100f) { "Score must be between 0 and 100" }
            require(score < 50f) { "This status is only for scores below 50%" }
        }
    }

    /**
     * OIR completed with score >= 50%
     *
     * @param submissionId OIR submission ID
     * @param score OIR score (50-100)
     */
    data class Completed(
        val submissionId: String,
        val score: Float
    ) : OIRStatus() {
        init {
            require(submissionId.isNotBlank()) { "Submission ID cannot be blank" }
            require(score in 50f..100f) { "Score must be between 50 and 100 for completed status" }
        }
    }

    val displayName: String
        get() = when (this) {
            is NotStarted -> "Not Started"
            is CompletedBelowThreshold -> "Score Below 50%"
            is Completed -> "Completed"
        }
}

/**
 * PPDT completion status
 */
sealed class PPDTStatus {
    /**
     * PPDT not started yet
     */
    object NotStarted : PPDTStatus()

    /**
     * PPDT completed
     *
     * @param submissionId PPDT submission ID
     */
    data class Completed(val submissionId: String) : PPDTStatus() {
        init {
            require(submissionId.isNotBlank()) { "Submission ID cannot be blank" }
        }
    }

    val displayName: String
        get() = when (this) {
            is NotStarted -> "Not Started"
            is Completed -> "Completed"
        }
}

/**
 * Subscription tier and interview limit status
 */
sealed class SubscriptionStatus {
    /**
     * User has free tier (no interview access)
     */
    object FreeTier : SubscriptionStatus()

    /**
     * User has Pro/Premium but reached interview limit
     *
     * @param tier Subscription tier
     * @param used Number of interviews used
     * @param limit Maximum interviews allowed
     */
    data class LimitReached(
        val tier: String,
        val used: Int,
        val limit: Int
    ) : SubscriptionStatus() {
        init {
            require(used >= limit) { "Used must be >= limit for this status" }
        }
    }

    /**
     * User has Pro/Premium with remaining interviews
     *
     * @param tier Subscription tier
     * @param remaining Number of interviews remaining
     * @param mode Available interview mode (text or voice)
     */
    data class Available(
        val tier: String,
        val remaining: Int,
        val mode: InterviewMode
    ) : SubscriptionStatus() {
        init {
            require(remaining > 0) { "Remaining must be positive for available status" }
        }
    }

    val displayName: String
        get() = when (this) {
            is FreeTier -> "Free Tier"
            is LimitReached -> "Limit Reached"
            is Available -> "Available"
        }
}
