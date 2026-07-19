package com.ssbmax.di

import com.ssbmax.shared.domain.repository.StudyContentRepository
import com.ssbmax.shared.domain.repository.StudyProgressRepository
import com.ssbmax.shared.domain.usecase.study.GetStudyMaterialDetailUseCase
import com.ssbmax.shared.domain.usecase.study.GetStudyProgressUseCase
import com.ssbmax.shared.domain.usecase.study.SaveStudyProgressUseCase
import com.ssbmax.shared.domain.usecase.study.TrackStudySessionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module bridging :shared (KMP) study use cases into the Android Hilt graph.
 *
 * See [AuthUseCaseModule] for why these bindings are explicit rather than
 * `@Inject`-constructor based.
 */
@Module
@InstallIn(SingletonComponent::class)
object StudyUseCaseModule {

    @Provides
    fun provideGetStudyMaterialDetailUseCase(
        studyContentRepository: StudyContentRepository
    ): GetStudyMaterialDetailUseCase = GetStudyMaterialDetailUseCase(studyContentRepository)

    @Provides
    fun provideGetStudyProgressUseCase(
        studyProgressRepository: StudyProgressRepository
    ): GetStudyProgressUseCase = GetStudyProgressUseCase(studyProgressRepository)

    @Provides
    fun provideSaveStudyProgressUseCase(
        studyProgressRepository: StudyProgressRepository
    ): SaveStudyProgressUseCase = SaveStudyProgressUseCase(studyProgressRepository)

    @Provides
    fun provideTrackStudySessionUseCase(
        studyProgressRepository: StudyProgressRepository
    ): TrackStudySessionUseCase = TrackStudySessionUseCase(studyProgressRepository)
}
