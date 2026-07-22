package com.ssbmax.shared.domain.service

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.util.DomainLogger

/**
 * Fires off background AI analysis for a just-submitted test (PPDT's
 * `PPDTAnalysisWorker`, and eventually TAT/WAT/SRT/SD's equivalents).
 *
 * This interface is the seam the KMP migration plan's Phase 5 PPDT session
 * was told to introduce: the Android original (`PPDTTestViewModel.
 * enqueuePPDTAnalysisWorker`, via `BaseTestViewModel.enqueueAnalysisWork`)
 * calls `androidx.work.WorkManager` directly with a concrete
 * `OneTimeWorkRequestBuilder<PPDTAnalysisWorker>()` — and `PPDTAnalysisWorker`
 * is an `app`-module class. `shared` cannot depend on `app` (the dependency
 * arrow only ever points the other way), so no real `expect`/`actual` in
 * `shared/androidMain` can reference it. Unlike Phase 4's
 * [com.ssbmax.shared.platform.worker.BackgroundTaskScheduler] (periodic,
 * app-level jobs whose Android `actual` owns its own worker class), a
 * one-off *per-submission* trigger's real implementation can only live where
 * the concrete worker class already lives: `app` itself.
 *
 * Consequence, stated plainly rather than silently worked around:
 * - [PPDTTestViewModel][com.ssbmax.shared.presentation.ppdt.PPDTTestViewModel]
 *   depends on this interface and calls it after a successful submission.
 * - The only binding registered in `shared`'s own Koin module
 *   ([com.ssbmax.shared.di.sharedModule]) is [LoggingSubmissionAnalysisTrigger]
 *   below — it does NOT enqueue any real work. A submission made through the
 *   `shared` PPDT vertical today persists correctly to Firestore
 *   (`submitPPDT`, verified) but will sit at `analysisStatus =
 *   PENDING_ANALYSIS` forever unless something else (the real Android app's
 *   still-untouched `app/.../ui/tests/ppdt` flow, or a manual trigger)
 *   completes the analysis. The result screen's "pending analysis" state
 *   handles this correctly (it does not spin or error), but a candidate
 *   using only the `shared` vertical would never see a result.
 * - A real Android binding (e.g. `WorkManagerSubmissionAnalysisTrigger`,
 *   constructed with a `WorkManager` and enqueuing the real
 *   `PPDTAnalysisWorker`) belongs in `app`'s own Koin module, overriding this
 *   one, the same way `core:data`'s `repositoryModule` already overrides
 *   several `shared` bindings on Android (see `sharedModule`'s doc comment on
 *   `TestUsageRecorder`/`TestSessionRepository`). It was NOT added this
 *   session because doing so is exactly the kind of "swap the live Android
 *   flow onto new, unreviewed shared code" this plan's own precedent (OIR)
 *   says not to do without dedicated review/tests first — and this session's
 *   scope is the `shared` UI port, not touching `app`'s production DI graph.
 * - On iOS, `BGTaskScheduler` gives no execution-time guarantee even if a
 *   real actual were written (Phase 4's own documented gap), so building one
 *   now would still not give candidates a reliable result without a UX
 *   redesign (foreground progress vs. background job) — an explicit product
 *   decision this session does not have standing to make. Tracked in the KMP
 *   migration plan's "Open items" table rather than silently resolved here.
 */
interface SubmissionAnalysisTrigger {
    fun trigger(testType: TestType, submissionId: String)
}

/**
 * Default binding — logs that analysis was NOT actually scheduled, rather
 * than silently doing nothing. See the interface doc above for why no real
 * implementation is wired into `shared`'s own Koin module yet.
 */
class LoggingSubmissionAnalysisTrigger(
    private val logger: DomainLogger
) : SubmissionAnalysisTrigger {
    override fun trigger(testType: TestType, submissionId: String) {
        logger.w(
            "SubmissionAnalysisTrigger",
            "No real analysis scheduler bound for $testType submission $submissionId " +
                "-- it will remain PENDING_ANALYSIS until a platform-specific trigger is wired in."
        )
    }
}
