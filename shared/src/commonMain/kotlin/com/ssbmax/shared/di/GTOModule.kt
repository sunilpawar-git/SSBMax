package com.ssbmax.shared.di

import com.ssbmax.shared.presentation.gd.GDTestViewModel
import com.ssbmax.shared.presentation.gdresult.GDResultViewModel
import com.ssbmax.shared.presentation.gpe.GPETestViewModel
import com.ssbmax.shared.presentation.gperesult.GPEResultViewModel
import com.ssbmax.shared.presentation.gto.common.GTOEligibilityChecker
import com.ssbmax.shared.presentation.gto.common.GTOSubmissionCoordinator
import com.ssbmax.shared.presentation.lecturette.LecturetteTestViewModel
import com.ssbmax.shared.presentation.lecturetteresult.LecturetteResultViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * GTO (GD/Lecturette/GPE) vertical. `GTORepository`/`TestContentRepository`/
 * `SubmissionRepository` come from [repositoryModule]. [GTOEligibilityChecker]
 * and [GTOSubmissionCoordinator] are shared factories (not per-sub-test) —
 * see those classes' own doc comments for why a shared class earns its keep
 * here unlike other verticals' inlined single-call eligibility checks. Split
 * out of the former monolithic `SharedModule.kt` — see [repositoryModule]'s
 * doc comment for why.
 *
 * All three sub-tests (GD, Lecturette, GPE) DO use the async-analysis seam
 * (the Android original enqueues `GTOAnalysisWorker` via WorkManager after
 * submission), fired via [GTOSubmissionCoordinator] using
 * [coreInfraModule]'s single `SubmissionAnalysisTrigger` binding — same real
 * consequence as every other test vertical (a submission persists but is not
 * yet AI-analyzed through this path). Not swapped into the live Android app
 * (`app/.../ui/tests/gto/{gd,lecturette}` and `.../gpe` are untouched).
 */
val gtoModule = module {
    factoryOf(::GTOEligibilityChecker)
    factoryOf(::GTOSubmissionCoordinator)
    viewModelOf(::GDTestViewModel)
    viewModelOf(::GDResultViewModel)
    viewModelOf(::LecturetteTestViewModel)
    viewModelOf(::LecturetteResultViewModel)
    viewModelOf(::GPETestViewModel)
    viewModelOf(::GPEResultViewModel)
}
