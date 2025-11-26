package com.ssbmax.utils

/**
 * Application-wide constants
 */
object AppConstants {

    /**
     * Time constants
     */
    object Time {
        /** Auto-save debounce delay in milliseconds (2 seconds) */
        const val AUTOSAVE_DEBOUNCE_MS = 2000L

        /** Seconds per day (24 hours * 60 minutes * 60 seconds) */
        const val SECONDS_PER_DAY = 86400L

        /** Milliseconds per second */
        const val MILLIS_PER_SECOND = 1000L
    }

    /**
     * Interview & Question Cache constants
     */
    object Interview {
        /** Target total questions per interview session (20-25 recommended) */
        const val TARGET_TOTAL_QUESTIONS = 25

        /** Target number of PIQ-based questions to generate (70% of total) */
        const val TARGET_PIQ_QUESTION_COUNT = 18

        /** Target number of generic questions (25% of total) */
        const val TARGET_GENERIC_QUESTION_COUNT = 6

        /** Default question cache expiration in days */
        const val DEFAULT_CACHE_EXPIRATION_DAYS = 30

        /** Maximum retry attempts for background workers */
        const val MAX_WORKER_RETRY_ATTEMPTS = 3

        /** Medium difficulty level for questions */
        const val MEDIUM_DIFFICULTY = 3
    }

    /**
     * WorkManager constants
     */
    object WorkManager {
        /** Periodic cleanup interval in hours */
        const val CLEANUP_INTERVAL_HOURS = 24L

        /** Unique work name for question cache cleanup */
        const val CLEANUP_WORK_NAME = "question_cache_cleanup_periodic"

        /** Tag for PIQ question generation jobs */
        const val PIQ_GENERATION_TAG = "piq_question_generation"
    }
}
