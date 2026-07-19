package com.ssbmax.shared.di

import com.ssbmax.shared.data.repository.GitLiveAuthRepository
import com.ssbmax.shared.data.repository.GitLiveOirResultRepository
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.repository.OirResultRepository
import com.ssbmax.shared.domain.usecase.GetOirResultUseCase
import com.ssbmax.shared.domain.usecase.SignInWithGoogleUseCase
import com.ssbmax.shared.presentation.auth.AuthViewModel
import com.ssbmax.shared.presentation.oirresult.OirResultViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for the Phase 0 KMP spike. Deliberately small — only wires
 * the one vertical slice (auth + OIR result). Full DI-graph parity with the
 * app's 6 Hilt modules / 55 ViewModels is Phase 3 scope, not Phase 0.
 */
val sharedModule = module {
    singleOf(::GitLiveAuthRepository) bind AuthRepository::class
    singleOf(::GitLiveOirResultRepository) bind OirResultRepository::class

    factoryOf(::SignInWithGoogleUseCase)
    factoryOf(::GetOirResultUseCase)

    factoryOf(::AuthViewModel)
    factoryOf(::OirResultViewModel)
}
