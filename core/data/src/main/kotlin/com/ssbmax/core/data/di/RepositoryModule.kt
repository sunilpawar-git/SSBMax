package com.ssbmax.core.data.di

import com.ssbmax.core.data.repository.AnalyticsRepositoryImpl
import com.ssbmax.core.data.repository.AuthRepositoryImpl
import com.ssbmax.core.data.repository.FirestoreGTORepository
import com.ssbmax.core.data.repository.FirestoreInterviewRepository
import com.ssbmax.core.data.repository.FirestoreQuestionCacheRepository
import com.ssbmax.core.data.repository.GradingQueueRepositoryImpl
import com.ssbmax.core.data.repository.NotificationRepositoryImpl
import com.ssbmax.core.data.repository.SubscriptionManager
import com.ssbmax.core.data.repository.SubscriptionRepositoryImpl
import com.ssbmax.core.data.repository.StudyProgressRepositoryImpl
import com.ssbmax.core.data.repository.TestContentRepositoryImpl
import com.ssbmax.core.data.repository.TestProgressRepositoryImpl
import com.ssbmax.core.data.repository.TestRepositoryImpl
import com.ssbmax.core.data.repository.TestSessionManagerImpl
import com.ssbmax.core.data.repository.TestSubmissionRepositoryImpl
import com.ssbmax.core.data.repository.UnifiedResultRepositoryImpl
import com.ssbmax.core.data.repository.UserProfileRepositoryImpl
import com.ssbmax.core.data.remote.FirestoreSubmissionRepository
import com.ssbmax.shared.domain.model.interview.QuestionCacheRepository
import com.ssbmax.shared.domain.repository.AnalyticsRepository
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.repository.GTORepository
import com.ssbmax.shared.domain.repository.GradingQueueRepository
import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.shared.domain.repository.NotificationRepository
import com.ssbmax.shared.domain.repository.StudyProgressRepository
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.SubscriptionRepository
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.TestProgressRepository
import com.ssbmax.shared.domain.repository.TestRepository
import com.ssbmax.shared.domain.repository.TestSessionRepository
import com.ssbmax.shared.domain.repository.TestSubmissionRepository
import com.ssbmax.shared.domain.repository.TestUsageRecorder
import com.ssbmax.shared.domain.repository.UnifiedResultRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin bindings for repository implementations — one binding per interface,
 * mirroring the former Hilt `RepositoryModule` (`@Binds @Singleton`) in
 * `DataModule.kt`. [SubscriptionManager] binds to both its concrete type
 * (still constructor-injected directly by [com.ssbmax.di.gtoTestModule] and
 * [com.ssbmax.di.debugModule]) and [TestUsageRecorder].
 */
val repositoryModule = module {
    singleOf(::TestRepositoryImpl) bind TestRepository::class
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::FirestoreSubmissionRepository) bind SubmissionRepository::class
    singleOf(::NotificationRepositoryImpl) bind NotificationRepository::class
    singleOf(::TestSubmissionRepositoryImpl) bind TestSubmissionRepository::class
    singleOf(::TestContentRepositoryImpl) bind TestContentRepository::class
    singleOf(::UserProfileRepositoryImpl) bind UserProfileRepository::class
    singleOf(::TestProgressRepositoryImpl) bind TestProgressRepository::class
    singleOf(::GradingQueueRepositoryImpl) bind GradingQueueRepository::class
    singleOf(::AnalyticsRepositoryImpl) bind AnalyticsRepository::class
    singleOf(::SubscriptionRepositoryImpl) bind SubscriptionRepository::class
    singleOf(::StudyProgressRepositoryImpl) bind StudyProgressRepository::class
    singleOf(::FirestoreQuestionCacheRepository) bind QuestionCacheRepository::class
    singleOf(::FirestoreInterviewRepository) bind InterviewRepository::class
    singleOf(::FirestoreGTORepository) bind GTORepository::class
    singleOf(::UnifiedResultRepositoryImpl) bind UnifiedResultRepository::class
    singleOf(::SubscriptionManager) bind TestUsageRecorder::class
    singleOf(::TestSessionManagerImpl) bind TestSessionRepository::class
}
