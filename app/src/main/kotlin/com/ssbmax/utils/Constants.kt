package com.ssbmax.utils

import com.ssbmax.shared.domain.constants.InterviewConstants

/**
 * Application-wide constants
 *
 * Note: Interview-specific constants are defined in [InterviewConstants]
 * in the domain layer for cross-module access.
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
     * Interview constants - delegates to domain layer for cross-module access
     *
     * @see InterviewConstants for all interview-related constants
     */
    object Interview {
        // Re-export commonly used constants for convenience in app module
        val TARGET_TOTAL_QUESTIONS = InterviewConstants.TARGET_TOTAL_QUESTIONS
        val TARGET_PIQ_QUESTION_COUNT = InterviewConstants.TARGET_PIQ_QUESTION_COUNT
        val TARGET_GENERIC_QUESTION_COUNT = InterviewConstants.TARGET_GENERIC_QUESTION_COUNT
    }

    /**
     * WorkManager constants
     *
     * CLEANUP_INTERVAL_HOURS/CLEANUP_WORK_NAME moved (Phase 4 platform shim)
     * into com.ssbmax.shared.platform.worker.WorkManagerBackgroundTaskScheduler
     * (same literal values) since periodic-cleanup scheduling itself moved
     * there; PIQ_GENERATION_TAG stays here (a one-off worker tag, not a
     * shared-scheduler concern).
     */
    object WorkManager {
        /** Tag for PIQ question generation jobs */
        const val PIQ_GENERATION_TAG = "piq_question_generation"
    }
}
