package com.ssbmax.shared.di

import com.ssbmax.shared.domain.usecase.oir.OIRTestScoreCalculator
import com.ssbmax.shared.domain.usecase.oir.SubmitOIRTestUseCase
import com.ssbmax.shared.domain.usecase.ppdt.LoadPPDTTestUseCase
import com.ssbmax.shared.domain.usecase.ppdt.SubmitPPDTTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitSDTTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitSRTTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitTATTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitWATTestUseCase
import com.ssbmax.shared.domain.usecase.subscription.CheckTestEligibilityUseCase
import com.ssbmax.shared.domain.usecase.tat.LoadTATTestUseCase
import com.ssbmax.shared.presentation.oir.OIRTestViewModel
import com.ssbmax.shared.presentation.piq.PIQTestViewModel
import com.ssbmax.shared.presentation.piqresult.PIQSubmissionResultViewModel
import com.ssbmax.shared.presentation.ppdt.PPDTTestViewModel
import com.ssbmax.shared.presentation.ppdtresult.PPDTSubmissionResultViewModel
import com.ssbmax.shared.presentation.sdt.SDTTestViewModel
import com.ssbmax.shared.presentation.sdtresult.SDTSubmissionResultViewModel
import com.ssbmax.shared.presentation.srt.SRTTestViewModel
import com.ssbmax.shared.presentation.srtresult.SRTSubmissionResultViewModel
import com.ssbmax.shared.presentation.tat.TATTestViewModel
import com.ssbmax.shared.presentation.tatresult.TATSubmissionResultViewModel
import com.ssbmax.shared.presentation.wat.WATTestViewModel
import com.ssbmax.shared.presentation.watresult.WATSubmissionResultViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin property key carrying the debug-only subscription-limit bypass into
 * [CheckTestEligibilityUseCase]. Same shape as
 * [com.ssbmax.shared.di.GEMINI_API_KEY_PROPERTY]: Android's
 * `SSBMaxApplication` supplies `BuildConfig.DEBUG && BuildConfig.BYPASS_SUBSCRIPTION_LIMITS`;
 * iOS supplies nothing, so [CheckTestEligibilityUseCase]'s `false` default
 * applies (this bypass was always Android-`BuildConfig`-only, see that
 * class's own doc comment).
 */
const val BYPASS_SUBSCRIPTION_LIMITS_PROPERTY = "BYPASS_SUBSCRIPTION_LIMITS"

/**
 * OIR/PPDT/TAT/WAT/SRT/SDT/PIQ test-taking vertical. Repository dependencies
 * come from [repositoryModule]; async-analysis dispatch uses
 * [coreInfraModule]'s single `SubmissionAnalysisTrigger` binding. Split out of
 * the former monolithic `SharedModule.kt` — see [repositoryModule]'s doc
 * comment for why.
 *
 * WAT/SRT/SDT DO use the async-analysis seam (the Android originals enqueue
 * `WATAnalysisWorker`/`SRTAnalysisWorker`/`SDTAnalysisWorker` via WorkManager
 * after submission) — not synchronous/rule-based scoring; a submission
 * through this vertical persists but is not yet AI-analyzed through this
 * `shared` path (see `SubmissionAnalysisTrigger`'s own doc comment). This
 * vertical is now the live one on both platforms (`MainActivity` has rendered
 * `SSBMaxRoot()`/`SSBMaxNavHost` directly since the KMP-convergence plan's
 * Phase 5 cutover; `app/.../ui/tests/` no longer exists, deleted in Phase 6a).
 */
val testTakingModule = module {
    factory {
        CheckTestEligibilityUseCase(
            subscriptionRepository = get(),
            analyticsTracker = get(),
            bypassSubscriptionLimits = getProperty(BYPASS_SUBSCRIPTION_LIMITS_PROPERTY, false)
        )
    }
    factoryOf(::OIRTestScoreCalculator)
    factoryOf(::SubmitOIRTestUseCase)
    factoryOf(::LoadPPDTTestUseCase)
    factoryOf(::SubmitPPDTTestUseCase)
    factoryOf(::LoadTATTestUseCase)
    factoryOf(::SubmitTATTestUseCase)
    factoryOf(::SubmitWATTestUseCase)
    factoryOf(::SubmitSRTTestUseCase)
    factoryOf(::SubmitSDTTestUseCase)

    viewModelOf(::OIRTestViewModel)
    viewModelOf(::PPDTTestViewModel)
    viewModelOf(::PPDTSubmissionResultViewModel)
    viewModelOf(::TATTestViewModel)
    viewModelOf(::TATSubmissionResultViewModel)
    viewModelOf(::WATTestViewModel)
    viewModelOf(::WATSubmissionResultViewModel)
    viewModelOf(::SRTTestViewModel)
    viewModelOf(::SRTSubmissionResultViewModel)
    viewModelOf(::SDTTestViewModel)
    viewModelOf(::SDTSubmissionResultViewModel)
    viewModelOf(::PIQTestViewModel)
    viewModelOf(::PIQSubmissionResultViewModel)
}
