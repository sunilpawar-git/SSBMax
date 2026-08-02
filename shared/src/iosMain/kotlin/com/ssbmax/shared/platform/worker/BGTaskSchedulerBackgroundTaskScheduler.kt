package com.ssbmax.shared.platform.worker

import com.ssbmax.shared.domain.util.DomainLogger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateByAddingTimeInterval

/**
 * iOS actual, backed by `BGTaskScheduler`. Submits requests for the two
 * periodic jobs using the identifiers below.
 *
 * **Not equivalent to the Android actual's guarantees** — see
 * [BackgroundTaskScheduler]'s class doc. `BGTaskScheduler` only *requests*
 * execution; iOS decides if/when to actually run it based on battery,
 * usage patterns, and system load, with no minimum-interval guarantee even
 * though `earliestBeginDate` sets a lower bound.
 *
 * **Known gap, explicitly deferred to Phase 6 (iOS shell), not silently
 * assumed complete:** `BGTaskScheduler.register(forTaskWithIdentifier:...)`
 * must be called during app launch (before
 * `application(_:didFinishLaunchingWithOptions:)` returns) and the task
 * identifiers below must be declared in the iOS app's `Info.plist` under
 * `BGTaskSchedulerPermittedIdentifiers` — neither the registration call nor
 * the Info.plist entries exist yet in `iosApp` (the Phase 0 spike shell).
 * Calling [scheduleQuestionCacheCleanup]/[scheduleSubmissionArchival]
 * before that wiring exists will fail at the OS level (submit throws / the
 * task silently never registers). This class implements the real
 * `BGTaskScheduler` submit calls now so the Phase 6 shell work only needs
 * to add registration + Info.plist entries, not new Kotlin.
 *
 * Phase 7b: `submitTaskRequest`'s failure (the exact "not registered yet"
 * case above) is logged via [DomainLogger] rather than silently swallowed —
 * still non-fatal (a missed background job isn't worth crashing the app
 * over), but now visible instead of invisible.
 */
@OptIn(ExperimentalForeignApi::class)
class BGTaskSchedulerBackgroundTaskScheduler(
    private val logger: DomainLogger
) : BackgroundTaskScheduler {

    override fun scheduleQuestionCacheCleanup() {
        val request = BGAppRefreshTaskRequest(CLEANUP_TASK_ID)
        request.earliestBeginDate = NSDate().dateByAddingTimeInterval(CLEANUP_INTERVAL_SECONDS)
        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        } catch (e: Exception) {
            logger.e(TAG, "submitTaskRequest failed for $CLEANUP_TASK_ID", e)
        }
    }

    override fun scheduleSubmissionArchival() {
        val request = BGProcessingTaskRequest(ARCHIVAL_TASK_ID)
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = true
        request.earliestBeginDate = NSDate().dateByAddingTimeInterval(ARCHIVAL_INITIAL_DELAY_SECONDS)
        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
        } catch (e: Exception) {
            logger.e(TAG, "submitTaskRequest failed for $ARCHIVAL_TASK_ID", e)
        }
    }

    companion object {
        /** Must match an entry in Info.plist's BGTaskSchedulerPermittedIdentifiers (Phase 6). */
        const val CLEANUP_TASK_ID = "com.ssbmax.questioncachecleanup"

        /** Must match an entry in Info.plist's BGTaskSchedulerPermittedIdentifiers (Phase 6). */
        const val ARCHIVAL_TASK_ID = "com.ssbmax.submissionarchival"

        private const val CLEANUP_INTERVAL_SECONDS = 24.0 * 60.0 * 60.0 // 24 hours, matches Android actual
        private const val ARCHIVAL_INITIAL_DELAY_SECONDS = 60.0 * 60.0 // 1 hour, matches Android actual
        private const val TAG = "BGTaskSchedulerBackgroundTaskScheduler"
    }
}
