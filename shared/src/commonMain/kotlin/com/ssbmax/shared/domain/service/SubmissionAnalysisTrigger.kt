package com.ssbmax.shared.domain.service

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.util.DomainLogger

/**
 * Fires off background AI analysis for a just-submitted test (PPDT/TAT/WAT/
 * SRT/SD/GTO/Interview's respective Android `CoroutineWorker`s).
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
 *   below — it does NOT enqueue any real work by itself. On Android, `app`'s
 *   own Koin module overrides it with a real binding (see below); on iOS,
 *   nothing overrides it yet, so a submission made through the `shared` PPDT
 *   vertical there still persists correctly to Firestore (`submitPPDT`,
 *   verified) but sits at `analysisStatus = PENDING_ANALYSIS` forever. The
 *   result screen's "pending analysis" state handles this correctly (it does
 *   not spin or error), but an iOS-only candidate would never see a result.
 * - Resolved for Android (KMP close-out plan's "Phase D"): `app`'s own Koin
 *   module (`com.ssbmax.di.workManagerModule`) now binds the real
 *   `com.ssbmax.workers.WorkManagerSubmissionAnalysisTrigger`, which
 *   dispatches to the actual `PPDTAnalysisWorker`/`TATStoryAnalysisWorker`+
 *   `TATSynthesisWorker`/`WATAnalysisWorker`/`SRTAnalysisWorker`/
 *   `SDTAnalysisWorker`/`GTOAnalysisWorker`/`InterviewAnalysisWorker`,
 *   overriding this class's binding the same way `core:data`'s
 *   `repositoryModule` already overrides several `shared` bindings on
 *   Android (see `sharedModule`'s doc comment on
 *   `TestUsageRecorder`/`TestSessionRepository`).
 * - iOS still has no real binding: `BGTaskScheduler` gives no execution-time
 *   guarantee even if a real actual were written (Phase 4's own documented
 *   gap), so building one now would still not give candidates a reliable
 *   result without a UX redesign (foreground progress vs. background job) —
 *   an explicit product decision deferred to Phase 6. iOS never loads
 *   `app`'s Koin modules, so it keeps using this class's logging-only
 *   binding automatically, no `expect`/`actual` needed.
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
