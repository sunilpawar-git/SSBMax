package com.ssbmax.shared.platform.worker

/**
 * Schedules the app's two periodic background jobs (previously scheduled
 * directly from `SSBMaxApplication.onCreate` via `androidx.work.WorkManager`).
 *
 * IMPORTANT — read before assuming iOS parity: Android's actual
 * ([WorkManager]-backed, in `shared/androidMain`) gives WorkManager's usual
 * execution guarantees. iOS's actual (`BGTaskScheduler`-backed, in
 * `shared/iosMain`) does **not** — the OS may defer or skip a background
 * task entirely depending on battery, usage patterns, and system load, with
 * no guaranteed execution window. This interface intentionally does NOT try
 * to paper over that difference with a fake "guaranteed" contract; callers
 * that need a stronger guarantee (e.g. a UX depending on a job actually
 * having run) must not assume it did.
 *
 * Per the KMP migration plan's own caution (Phase 4 scope, "Risks a Senior
 * App Developer Should Push Back On" #2): the TAT/PPDT async AI-analysis
 * flow is a *separate*, more UX-sensitive background-execution concern
 * (currently a one-off `WorkManager` job per submission, not a periodic
 * job) that is explicitly OUT of this shim's scope — redesigning that flow
 * for iOS's execution-guarantee gap is a product decision, not a Phase 4
 * platform-shim task. Those workers keep using `androidx.work.WorkManager`
 * directly in `app` (Android-only, unchanged by this phase).
 */
interface BackgroundTaskScheduler {
    /**
     * Daily cleanup of expired cached questions (PIQ-based questions older
     * than the app's cache-retention window). Requires network + not-low
     * battery on Android; best-effort on iOS (see class doc).
     */
    fun scheduleQuestionCacheCleanup()

    /**
     * Daily archival of submissions older than 6 months, moving them out of
     * the primary collection into an archive. Requires network + charging
     * on Android; best-effort on iOS (see class doc).
     */
    fun scheduleSubmissionArchival()
}
