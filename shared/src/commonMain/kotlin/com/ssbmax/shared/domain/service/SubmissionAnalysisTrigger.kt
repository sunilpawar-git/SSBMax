package com.ssbmax.shared.domain.service

import com.ssbmax.shared.domain.model.TestType

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
 * - Resolved for Android (KMP close-out plan's "Phase D"): `app`'s own Koin
 *   module (`com.ssbmax.di.workManagerModule`) binds the real
 *   `com.ssbmax.workers.WorkManagerSubmissionAnalysisTrigger`, which
 *   dispatches to the actual `PPDTAnalysisWorker`/`TATStoryAnalysisWorker`+
 *   `TATSynthesisWorker`/`WATAnalysisWorker`/`SRTAnalysisWorker`/
 *   `SDTAnalysisWorker`/`GTOAnalysisWorker`/`InterviewAnalysisWorker`,
 *   overriding `shared`'s own binding (Koin last-wins, the same pattern
 *   `core:data`'s `repositoryModule` used to shadow-override
 *   `TestUsageRecorder`/`TestSessionRepository` before both were single-sourced
 *   into `shared` in Phases 9d/9e and `core:data` itself was deleted in 9f).
 * - Resolved for iOS: `shared`'s own binding
 *   ([com.ssbmax.shared.analysis.KtorSubmissionAnalysisTrigger], registered in
 *   `coreInfraModule`) dispatches immediately in the foreground (not via
 *   `BGTaskScheduler`, which gives no execution-time guarantee -- see
 *   `BGTaskSchedulerBackgroundTaskScheduler`'s doc) to a real per-test-type
 *   orchestrator under `com.ssbmax.shared.analysis` for PPDT/TAT/WAT/SRT/SD/
 *   GTO/Interview. iOS never loads `app`'s Koin modules, so this binding is
 *   the one that actually runs there. It does not send a push notification on
 *   completion (no push-capable abstraction exists in `shared` yet); the
 *   result screen's reactive `analysisStatus` observation still surfaces the
 *   result once it lands.
 */
interface SubmissionAnalysisTrigger {
    fun trigger(testType: TestType, submissionId: String)
}
