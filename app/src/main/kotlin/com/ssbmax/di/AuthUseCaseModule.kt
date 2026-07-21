package com.ssbmax.di

import com.ssbmax.shared.domain.usecase.auth.GetGoogleSignInIntentUseCase
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.auth.SignInWithGoogleUseCase
import com.ssbmax.shared.domain.usecase.auth.SignOutUseCase
import com.ssbmax.shared.domain.usecase.auth.UpdateUserRoleUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Koin module bridging :shared (KMP) auth use cases into the app's Koin graph.
 *
 * `factoryOf` resolves each use case's constructor via reflection (an
 * `AuthRepository` binding already lives in :shared's own Koin module) —
 * matches Hilt's previous unscoped (non-`@Singleton`) `@Provides` behavior.
 */
val authUseCaseModule = module {
    factoryOf(::GetGoogleSignInIntentUseCase)
    factoryOf(::ObserveCurrentUserUseCase)
    factoryOf(::SignInWithGoogleUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::UpdateUserRoleUseCase)
}
