package com.ssbmax.di

import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.usecase.submission.GetUserSubmissionsUseCase
import com.ssbmax.shared.domain.usecase.submission.ObserveSubmissionUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitSDTTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitSRTTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitTATTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitWATTestUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module bridging :shared (KMP) submission use cases into the Android Hilt graph.
 *
 * See [AuthUseCaseModule] for why these bindings are explicit rather than
 * `@Inject`-constructor based.
 */
@Module
@InstallIn(SingletonComponent::class)
object SubmissionUseCaseModule {

    @Provides
    fun provideGetUserSubmissionsUseCase(
        submissionRepository: SubmissionRepository
    ): GetUserSubmissionsUseCase = GetUserSubmissionsUseCase(submissionRepository)

    @Provides
    fun provideObserveSubmissionUseCase(
        submissionRepository: SubmissionRepository
    ): ObserveSubmissionUseCase = ObserveSubmissionUseCase(submissionRepository)

    @Provides
    fun provideSubmitSDTTestUseCase(
        submissionRepository: SubmissionRepository
    ): SubmitSDTTestUseCase = SubmitSDTTestUseCase(submissionRepository)

    @Provides
    fun provideSubmitSRTTestUseCase(
        submissionRepository: SubmissionRepository
    ): SubmitSRTTestUseCase = SubmitSRTTestUseCase(submissionRepository)

    @Provides
    fun provideSubmitTATTestUseCase(
        submissionRepository: SubmissionRepository
    ): SubmitTATTestUseCase = SubmitTATTestUseCase(submissionRepository)

    @Provides
    fun provideSubmitWATTestUseCase(
        submissionRepository: SubmissionRepository
    ): SubmitWATTestUseCase = SubmitWATTestUseCase(submissionRepository)
}
