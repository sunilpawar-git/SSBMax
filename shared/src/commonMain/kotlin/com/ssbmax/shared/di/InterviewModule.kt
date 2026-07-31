package com.ssbmax.shared.di

import com.ssbmax.shared.domain.usecase.CheckInterviewPrerequisitesUseCase
import com.ssbmax.shared.presentation.interviewresult.InterviewResultViewModel
import com.ssbmax.shared.presentation.interviewsession.InterviewSessionViewModel
import com.ssbmax.shared.presentation.interviewstart.StartInterviewViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Interview vertical (start/session/result). `GitLiveInterviewRepository`/
 * `GitLiveQuestionCacheRepository`/`InterviewQuestionGenerator` come from
 * [repositoryModule]. Split out of the former monolithic `SharedModule.kt` —
 * see [repositoryModule]'s doc comment for why.
 *
 * `InterviewCompleter` uses the same `SubmissionAnalysisTrigger` seam as
 * every other test vertical (the Android original enqueues
 * `InterviewAnalysisWorker` via WorkManager after session completion) — a
 * completed session persists but is not yet AI-analyzed through this path.
 * The session screen's own investigation found no audio recording anywhere in
 * the Android original — it uses a plain text field plus the platform
 * keyboard's own voice-to-text, so no platform `expect`/`actual` shim was
 * needed. Not swapped into the live Android app (`app/.../ui/interview` is
 * untouched).
 */
val interviewModule = module {
    factoryOf(::CheckInterviewPrerequisitesUseCase)
    viewModelOf(::StartInterviewViewModel)
    viewModelOf(::InterviewSessionViewModel)
    viewModelOf(::InterviewResultViewModel)
}
