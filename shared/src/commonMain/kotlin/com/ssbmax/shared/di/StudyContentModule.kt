package com.ssbmax.shared.di

import com.ssbmax.shared.domain.usecase.study.GetStudyMaterialDetailUseCase
import com.ssbmax.shared.domain.usecase.study.GetStudyMaterialsUseCase
import com.ssbmax.shared.domain.usecase.study.GetStudyProgressUseCase
import com.ssbmax.shared.domain.usecase.study.SaveStudyProgressUseCase
import com.ssbmax.shared.domain.usecase.study.TrackStudySessionUseCase
import com.ssbmax.shared.presentation.notifications.NotificationCenterViewModel
import com.ssbmax.shared.presentation.phase1detail.Phase1DetailViewModel
import com.ssbmax.shared.presentation.phase2detail.Phase2DetailViewModel
import com.ssbmax.shared.presentation.ssboverview.SSBOverviewViewModel
import com.ssbmax.shared.presentation.studenttests.StudentTestsViewModel
import com.ssbmax.shared.presentation.study.StudyMaterialDetailViewModel
import com.ssbmax.shared.presentation.study.StudyMaterialsViewModel
import com.ssbmax.shared.presentation.topic.TopicViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * SSB Overview / Phase1-2 Detail / Notification Center / Student "All Tests" /
 * Study Materials / Topic vertical. `TestProgressRepository`/
 * `StudyContentRepository`/`StudyProgressRepository`/`NotificationRepository`/
 * `InterviewRepository` and `ObserveCurrentUserUseCase` come from
 * [repositoryModule]/[authHomeModule]; [DomainLogger] from [coreInfraModule].
 * Split out of the former monolithic `SharedModule.kt` — see
 * [repositoryModule]'s doc comment for why.
 *
 * [GetStudyMaterialsUseCase]/[GetStudyMaterialDetailUseCase]/
 * [SaveStudyProgressUseCase]/[TrackStudySessionUseCase]/
 * [GetStudyProgressUseCase] existed in `shared` before Phase 5's Study
 * Materials session but were never bound in this Koin graph until then.
 */
val studyContentModule = module {
    factoryOf(::SSBOverviewViewModel)
    factoryOf(::Phase1DetailViewModel)
    factoryOf(::Phase2DetailViewModel)
    factoryOf(::NotificationCenterViewModel)
    factoryOf(::StudentTestsViewModel)

    factoryOf(::GetStudyMaterialsUseCase)
    factoryOf(::GetStudyMaterialDetailUseCase)
    factoryOf(::SaveStudyProgressUseCase)
    factoryOf(::TrackStudySessionUseCase)
    factoryOf(::GetStudyProgressUseCase)
    factoryOf(::StudyMaterialsViewModel)
    factoryOf(::StudyMaterialDetailViewModel)

    factoryOf(::TopicViewModel)
}
