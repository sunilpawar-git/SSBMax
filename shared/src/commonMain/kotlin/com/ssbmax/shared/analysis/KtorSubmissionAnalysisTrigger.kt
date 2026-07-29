package com.ssbmax.shared.analysis

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger
import com.ssbmax.shared.domain.util.DomainLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Real cross-platform [SubmissionAnalysisTrigger] binding, replacing the
 * logging-only default (see that interface's own doc comment). On Android, `app`'s
 * `workManagerModule` still loads after `sharedModule` and overrides this binding with
 * the real `WorkManagerSubmissionAnalysisTrigger` (unchanged Koin last-wins pattern) --
 * so this class is the trigger that actually runs on iOS, which never loads `app`'s Koin
 * modules. `trigger()` is non-suspend (interface constraint), so each dispatch is
 * launched fire-and-forget on [scope], matching the Android original's own handling of
 * TAT (the one branch that similarly can't build a synchronous WorkRequest).
 *
 * Deliberately dispatches immediately in the foreground rather than deferring to
 * `BGTaskScheduler` (see [com.ssbmax.shared.platform.worker.BGTaskSchedulerBackgroundTaskScheduler]'s
 * doc): `BGTaskScheduler` gives no execution-time guarantee, so an immediate in-process
 * run is strictly more reliable for a just-submitted test than hoping iOS schedules a
 * background task soon. Does not send a push notification on completion (no push-capable
 * abstraction exists in `shared` yet -- real APNs wiring is a separate, already-tracked
 * gap); the UI already observes `analysisStatus` reactively via Firestore, so results
 * still surface without one.
 */
class KtorSubmissionAnalysisTrigger(
    private val scope: CoroutineScope,
    private val logger: DomainLogger,
    private val ppdt: PPDTAnalysisOrchestrator,
    private val tat: TATAnalysisOrchestrator,
    private val wat: WATAnalysisOrchestrator,
    private val srt: SRTAnalysisOrchestrator,
    private val sd: SDAnalysisOrchestrator,
    private val gto: GTOAnalysisOrchestrator,
    private val interview: InterviewAnalysisOrchestrator
) : SubmissionAnalysisTrigger {

    override fun trigger(testType: TestType, submissionId: String) {
        scope.launch {
            when (testType) {
                TestType.PPDT -> ppdt.analyze(submissionId)
                TestType.TAT -> tat.analyze(submissionId)
                TestType.WAT -> wat.analyze(submissionId)
                TestType.SRT -> srt.analyze(submissionId)
                TestType.SD -> sd.analyze(submissionId)
                TestType.GTO_GD, TestType.GTO_LECTURETTE, TestType.GTO_GPE -> gto.analyze(submissionId)
                TestType.IO -> interview.analyze(submissionId)
                else -> logger.w(
                    TAG,
                    "No analysis orchestrator bound for $testType submission $submissionId " +
                        "-- it will remain PENDING_ANALYSIS."
                )
            }
        }
    }

    private companion object {
        const val TAG = "KtorSubmissionAnalysisTrigger"
    }
}
