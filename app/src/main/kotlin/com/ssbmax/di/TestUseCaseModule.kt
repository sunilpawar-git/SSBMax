package com.ssbmax.di

import com.ssbmax.shared.domain.usecase.CheckInterviewPrerequisitesUseCase
import com.ssbmax.shared.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.shared.domain.usecase.oir.OIRTestScoreCalculator
import com.ssbmax.shared.domain.usecase.oir.SubmitOIRTestUseCase
import com.ssbmax.shared.domain.usecase.ppdt.LoadPPDTTestUseCase
import com.ssbmax.shared.domain.usecase.ppdt.SubmitPPDTTestUseCase
import com.ssbmax.shared.domain.usecase.results.GetHistoricResultsUseCase
import com.ssbmax.shared.domain.usecase.tat.LoadTATTestUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin module bridging :shared (KMP) test-taking use cases (OIR/PPDT/TAT scoring
 * and submission, interview prerequisites, the OLQ dashboard, and historic results)
 * into the app's Koin graph.
 *
 * `@Singleton` scope is preserved for [GetOLQDashboardUseCase] and
 * [GetHistoricResultsUseCase] (`singleOf`), matching their scope before the
 * move to :shared. Everything else was an unscoped `@Provides` (`factoryOf`).
 */
val testUseCaseModule = module {
    factoryOf(::CheckInterviewPrerequisitesUseCase)
    singleOf(::GetOLQDashboardUseCase)
    singleOf(::GetHistoricResultsUseCase)
    factoryOf(::OIRTestScoreCalculator)
    factoryOf(::SubmitOIRTestUseCase)
    factoryOf(::LoadPPDTTestUseCase)
    factoryOf(::SubmitPPDTTestUseCase)
    factoryOf(::LoadTATTestUseCase)
}
