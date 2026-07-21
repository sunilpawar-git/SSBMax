package com.ssbmax.di

import com.ssbmax.shared.domain.usecase.study.GetStudyMaterialDetailUseCase
import com.ssbmax.shared.domain.usecase.study.GetStudyProgressUseCase
import com.ssbmax.shared.domain.usecase.study.SaveStudyProgressUseCase
import com.ssbmax.shared.domain.usecase.study.TrackStudySessionUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Koin module bridging :shared (KMP) study use cases into the app's Koin graph.
 *
 * See [authUseCaseModule] for why these bindings are explicit rather than
 * relying on annotation processing.
 */
val studyUseCaseModule = module {
    factoryOf(::GetStudyMaterialDetailUseCase)
    factoryOf(::GetStudyProgressUseCase)
    factoryOf(::SaveStudyProgressUseCase)
    factoryOf(::TrackStudySessionUseCase)
}
