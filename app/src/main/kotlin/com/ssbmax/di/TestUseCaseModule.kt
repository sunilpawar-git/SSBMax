package com.ssbmax.di

import com.ssbmax.shared.domain.repository.GTORepository
import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.SubscriptionRepository
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.TestSessionRepository
import com.ssbmax.shared.domain.repository.TestUsageRecorder
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.usecase.CheckInterviewPrerequisitesUseCase
import com.ssbmax.shared.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.shared.domain.usecase.oir.OIRTestScoreCalculator
import com.ssbmax.shared.domain.usecase.oir.SubmitOIRTestUseCase
import com.ssbmax.shared.domain.usecase.ppdt.LoadPPDTTestUseCase
import com.ssbmax.shared.domain.usecase.ppdt.SubmitPPDTTestUseCase
import com.ssbmax.shared.domain.usecase.results.GetHistoricResultsUseCase
import com.ssbmax.shared.domain.usecase.tat.LoadTATTestUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module bridging :shared (KMP) test-taking use cases (OIR/PPDT/TAT scoring
 * and submission, interview prerequisites, the OLQ dashboard, and historic results)
 * into the Android Hilt graph.
 *
 * See [AuthUseCaseModule] for why these bindings are explicit rather than
 * `@Inject`-constructor based. `@Singleton` scope is preserved for
 * [GetOLQDashboardUseCase] and [GetHistoricResultsUseCase], matching their
 * scope before the move to :shared.
 */
@Module
@InstallIn(SingletonComponent::class)
object TestUseCaseModule {

    @Provides
    fun provideCheckInterviewPrerequisitesUseCase(
        submissionRepository: SubmissionRepository,
        subscriptionRepository: SubscriptionRepository,
        interviewRepository: InterviewRepository
    ): CheckInterviewPrerequisitesUseCase = CheckInterviewPrerequisitesUseCase(
        submissionRepository,
        subscriptionRepository,
        interviewRepository
    )

    @Provides
    @Singleton
    fun provideGetOLQDashboardUseCase(
        submissionRepository: SubmissionRepository,
        gtoRepository: GTORepository,
        interviewRepository: InterviewRepository,
        logger: DomainLogger
    ): GetOLQDashboardUseCase = GetOLQDashboardUseCase(
        submissionRepository,
        gtoRepository,
        interviewRepository,
        logger
    )

    @Provides
    @Singleton
    fun provideGetHistoricResultsUseCase(
        submissionRepository: SubmissionRepository
    ): GetHistoricResultsUseCase = GetHistoricResultsUseCase(submissionRepository)

    @Provides
    fun provideOIRTestScoreCalculator(
        logger: DomainLogger
    ): OIRTestScoreCalculator = OIRTestScoreCalculator(logger)

    @Provides
    fun provideSubmitOIRTestUseCase(
        scoreCalculator: OIRTestScoreCalculator,
        usageRecorder: TestUsageRecorder,
        dashboardUseCase: GetOLQDashboardUseCase,
        submissionRepository: SubmissionRepository,
        testSessionRepository: TestSessionRepository,
        testContentRepository: TestContentRepository
    ): SubmitOIRTestUseCase = SubmitOIRTestUseCase(
        scoreCalculator,
        usageRecorder,
        dashboardUseCase,
        submissionRepository,
        testSessionRepository,
        testContentRepository
    )

    @Provides
    fun provideLoadPPDTTestUseCase(
        testContentRepository: TestContentRepository,
        testSessionRepository: TestSessionRepository,
        userProfileRepository: UserProfileRepository
    ): LoadPPDTTestUseCase = LoadPPDTTestUseCase(
        testContentRepository,
        testSessionRepository,
        userProfileRepository
    )

    @Provides
    fun provideSubmitPPDTTestUseCase(
        submissionRepository: SubmissionRepository,
        userProfileRepository: UserProfileRepository,
        usageRecorder: TestUsageRecorder
    ): SubmitPPDTTestUseCase = SubmitPPDTTestUseCase(
        submissionRepository,
        userProfileRepository,
        usageRecorder
    )

    @Provides
    fun provideLoadTATTestUseCase(
        testContentRepository: TestContentRepository,
        testSessionRepository: TestSessionRepository,
        userProfileRepository: UserProfileRepository
    ): LoadTATTestUseCase = LoadTATTestUseCase(
        testContentRepository,
        testSessionRepository,
        userProfileRepository
    )
}
