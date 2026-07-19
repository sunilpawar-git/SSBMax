package com.ssbmax.di

import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.usecase.auth.GetGoogleSignInIntentUseCase
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.auth.SignInWithGoogleUseCase
import com.ssbmax.shared.domain.usecase.auth.SignOutUseCase
import com.ssbmax.shared.domain.usecase.auth.UpdateUserRoleUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module bridging :shared (KMP) auth use cases into the Android Hilt graph.
 *
 * These use cases live in shared/commonMain and cannot carry `@Inject` constructors
 * there (javax.inject doesn't resolve on the iOS native target), so their Android
 * bindings are provided explicitly here instead. Plain constructor calls only —
 * no behavior change from the previous `@Inject constructor` versions.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthUseCaseModule {

    @Provides
    fun provideGetGoogleSignInIntentUseCase(
        authRepository: AuthRepository
    ): GetGoogleSignInIntentUseCase = GetGoogleSignInIntentUseCase(authRepository)

    @Provides
    fun provideObserveCurrentUserUseCase(
        authRepository: AuthRepository
    ): ObserveCurrentUserUseCase = ObserveCurrentUserUseCase(authRepository)

    @Provides
    fun provideSignInWithGoogleUseCase(
        authRepository: AuthRepository
    ): SignInWithGoogleUseCase = SignInWithGoogleUseCase(authRepository)

    @Provides
    fun provideSignOutUseCase(
        authRepository: AuthRepository
    ): SignOutUseCase = SignOutUseCase(authRepository)

    @Provides
    fun provideUpdateUserRoleUseCase(
        authRepository: AuthRepository
    ): UpdateUserRoleUseCase = UpdateUserRoleUseCase(authRepository)
}
