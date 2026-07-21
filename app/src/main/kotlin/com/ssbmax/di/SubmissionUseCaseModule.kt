package com.ssbmax.di

import com.ssbmax.shared.domain.usecase.submission.GetUserSubmissionsUseCase
import com.ssbmax.shared.domain.usecase.submission.ObserveSubmissionUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitSDTTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitSRTTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitTATTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitWATTestUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Koin module bridging :shared (KMP) submission use cases into the app's Koin graph.
 *
 * See [authUseCaseModule] for why these bindings are explicit rather than
 * relying on annotation processing.
 */
val submissionUseCaseModule = module {
    factoryOf(::GetUserSubmissionsUseCase)
    factoryOf(::ObserveSubmissionUseCase)
    factoryOf(::SubmitSDTTestUseCase)
    factoryOf(::SubmitSRTTestUseCase)
    factoryOf(::SubmitTATTestUseCase)
    factoryOf(::SubmitWATTestUseCase)
}
