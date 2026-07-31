package com.ssbmax.shared.di

import com.ssbmax.shared.domain.usecase.results.GetHistoricResultsUseCase
import com.ssbmax.shared.domain.usecase.submission.GetUserSubmissionsUseCase
import com.ssbmax.shared.domain.usecase.submission.ObserveSubmissionUseCase
import com.ssbmax.shared.presentation.analytics.AnalyticsViewModel
import com.ssbmax.shared.presentation.grading.TestDetailGradingViewModel
import com.ssbmax.shared.presentation.instructorgrading.InstructorGradingViewModel
import com.ssbmax.shared.presentation.results.HistoricResultsViewModel
import com.ssbmax.shared.presentation.submissions.SubmissionDetailViewModel
import com.ssbmax.shared.presentation.submissions.SubmissionsListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Results/Submissions/Grading/Analytics/Instructor-Grading-Queue vertical.
 * `TestSubmissionRepository`/`AnalyticsRepository`/`NotificationRepository`/
 * `UserProfileRepository`/`AuthRepository`/`GradingQueueRepository` come from
 * [repositoryModule]. Split out of the former monolithic `SharedModule.kt` —
 * see [repositoryModule]'s doc comment for why.
 *
 * [GetHistoricResultsUseCase] (only ever queries TAT submissions per its own
 * doc comment — a pre-existing limitation, not introduced here) and
 * [GetUserSubmissionsUseCase]/[ObserveSubmissionUseCase] existed in `shared`
 * before Phase 5's session but were never bound in this Koin graph until
 * then. `AnalyticsScreen` depends only on the already-KMP-safe
 * `AnalyticsRepository` interface, not the Android-only `AnalyticsManager` —
 * no platform shim was needed. Not swapped into the live Android app
 * (`app/.../ui/{results,submissions,grading,analytics}` is untouched).
 */
val resultsSubmissionsModule = module {
    factoryOf(::GetHistoricResultsUseCase)
    factoryOf(::GetUserSubmissionsUseCase)
    factoryOf(::ObserveSubmissionUseCase)
    viewModelOf(::HistoricResultsViewModel)
    viewModelOf(::SubmissionsListViewModel)
    viewModelOf(::SubmissionDetailViewModel)
    viewModelOf(::TestDetailGradingViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::InstructorGradingViewModel)
}
