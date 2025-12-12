package com.ssbmax.core.domain.constants

/**
 * Constants for interview feature used across modules (domain, data, app)
 *
 * These constants are placed in domain layer so they can be accessed by:
 * - core/domain (use cases)
 * - core/data (repositories)
 * - app (ViewModels, workers)
 */
object InterviewConstants {

    // Question distribution
    /** Target total questions per interview session (20-25 recommended) */
    const val TARGET_TOTAL_QUESTIONS = 25

    /** Target number of PIQ-based questions to generate (70% of total) */
    const val TARGET_PIQ_QUESTION_COUNT = 18

    /** Target number of generic questions (25% of total) */
    const val TARGET_GENERIC_QUESTION_COUNT = 6

    /** PIQ-based questions ratio (70%) */
    const val PIQ_QUESTION_RATIO = 0.70f

    /** Generic questions ratio (25%) */
    const val GENERIC_QUESTION_RATIO = 0.25f

    /** Adaptive questions ratio - reserved for future (5%) */
    const val ADAPTIVE_QUESTION_RATIO = 0.05f

    // Cache settings
    /** Default question cache expiration in days */
    const val DEFAULT_CACHE_EXPIRATION_DAYS = 30

    // Retry settings
    /** Maximum retry attempts for background workers */
    const val MAX_WORKER_RETRY_ATTEMPTS = 3

    // Question difficulty
    /** Medium difficulty level for questions */
    const val MEDIUM_DIFFICULTY = 3

    // Timing constants
    /** Delay between API calls to avoid rate limiting (ms) */
    const val API_CALL_DELAY_MS = 500L

    /** Delay between retry attempts (ms) */
    const val RETRY_DELAY_MS = 2000L

    /** AI analysis timeout per response (ms) */
    const val AI_ANALYSIS_TIMEOUT_MS = 20000L

    /** Default estimated interview duration (minutes) */
    const val DEFAULT_DURATION_MINUTES = 30

    // OLQ Score defaults (SSB 1-10 scale, lower is better)
    /** Neutral fallback OLQ score when AI fails */
    const val FALLBACK_OLQ_SCORE = 6

    /** Low confidence indicator for fallback scores */
    const val FALLBACK_CONFIDENCE = 30
}














