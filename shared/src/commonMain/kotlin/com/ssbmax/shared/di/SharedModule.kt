package com.ssbmax.shared.di

import com.ssbmax.shared.ai.KtorAIService
import com.ssbmax.shared.ai.KtorGeminiClient
import com.ssbmax.shared.ai.KtorPPDTAnalyzer
import com.ssbmax.shared.ai.KtorTATStoryAnalyzer
import com.ssbmax.shared.data.repository.GitLiveAnalyticsRepository
import com.ssbmax.shared.data.repository.GitLiveAuthRepository
import com.ssbmax.shared.data.repository.GitLiveDifficultyProgressionManager
import com.ssbmax.shared.data.repository.GitLiveGPEImageCacheManager
import com.ssbmax.shared.data.repository.GitLiveGradingQueueRepository
import com.ssbmax.shared.data.repository.GitLiveGTORepository
import com.ssbmax.shared.data.repository.GitLiveGTOTaskCacheManager
import com.ssbmax.shared.data.repository.GitLiveNotificationCacheManager
import com.ssbmax.shared.data.repository.GitLiveNotificationRepository
import com.ssbmax.shared.data.repository.GitLiveOirResultRepository
import com.ssbmax.shared.data.repository.GitLiveOIRQuestionCacheManager
import com.ssbmax.shared.data.repository.GitLiveOIRQuestionSelector
import com.ssbmax.shared.data.repository.GitLivePPDTImageCacheManager
import com.ssbmax.shared.data.repository.GitLiveQuestionCacheRepository
import com.ssbmax.shared.data.repository.GitLiveSRTSituationCacheManager
import com.ssbmax.shared.data.repository.GitLiveStudyContentRepository
import com.ssbmax.shared.data.repository.GitLiveStudyProgressRepository
import com.ssbmax.shared.data.repository.GitLiveSubscriptionRepository
import com.ssbmax.shared.data.repository.GitLiveTATImageCacheManager
import com.ssbmax.shared.data.repository.GitLiveTestContentRepository
import com.ssbmax.shared.data.repository.GitLiveTestProgressRepository
import com.ssbmax.shared.data.repository.GitLiveTestRepository
import com.ssbmax.shared.data.repository.GitLiveUserProfileRepository
import com.ssbmax.shared.data.repository.GitLiveWATWordCacheManager
import com.ssbmax.shared.data.repository.OirResultCache
import com.ssbmax.shared.db.DatabaseDriverFactory
import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.domain.model.interview.QuestionCacheRepository
import com.ssbmax.shared.domain.repository.AnalyticsRepository
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.repository.GradingQueueRepository
import com.ssbmax.shared.domain.repository.GTORepository
import com.ssbmax.shared.domain.repository.NotificationRepository
import com.ssbmax.shared.domain.repository.OirResultRepository
import com.ssbmax.shared.domain.repository.StudyContentRepository
import com.ssbmax.shared.domain.repository.StudyProgressRepository
import com.ssbmax.shared.domain.repository.SubscriptionRepository
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.TestProgressRepository
import com.ssbmax.shared.domain.repository.TestRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.service.AIService
import com.ssbmax.shared.domain.usecase.GetOirResultUseCase
import com.ssbmax.shared.domain.usecase.auth.SignInWithGoogleUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.auth.AuthViewModel
import com.ssbmax.shared.presentation.oirresult.OirResultViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module. Phase 0 wired one vertical slice (auth + OIR result); Phase 2
 * adds UserProfile/Subscription repositories, a real SQLDelight-backed cache,
 * and the Ktor-based Gemini path (full `AIService` via `KtorAIService`,
 * replacing the earlier narrower `InterviewResponseAnalysisService` slice
 * once every AIService method was ported) — still not full DI-graph parity
 * with the app's 6 Hilt modules / 55 ViewModels, which remains Phase 3 scope.
 *
 * DomainLogger is bound to a no-op implementation here — a real cross-platform
 * logger (Android logcat / iOS os_log) is an expect/actual shim deferred to
 * Phase 4's platform-shims item, same tier as WorkManager/TTS/billing.
 *
 * The Gemini API key is read from Koin's property store (`getProperty`, empty
 * default) rather than hardcoded, per this repo's "never hardcode secrets"
 * rule — the app's startKoin() call must supply it via `properties()`/
 * `androidFileProperties()` before this module resolves KtorGeminiClient.
 * (No such startKoin() call exists anywhere yet — inherited from Phase 0,
 * which deferred the live run; see this phase's exit report.)
 */
val sharedModule = module {
    includes(platformModule)

    single<DomainLogger> { NoOpLogger() }

    single { SharedDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single { OirResultCache(get()) }

    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
    single {
        KtorGeminiClient(
            httpClient = get(),
            apiKey = getProperty("GEMINI_API_KEY", "")
        )
    }
    single { KtorPPDTAnalyzer(client = get(), logger = get()) }
    single { KtorTATStoryAnalyzer(client = get(), logger = get()) }

    singleOf(::GitLiveAuthRepository) bind AuthRepository::class
    singleOf(::GitLiveOirResultRepository) bind OirResultRepository::class
    singleOf(::GitLiveUserProfileRepository) bind UserProfileRepository::class
    singleOf(::GitLiveSubscriptionRepository) bind SubscriptionRepository::class
    singleOf(::GitLiveTestProgressRepository) bind TestProgressRepository::class
    singleOf(::GitLiveStudyProgressRepository) bind StudyProgressRepository::class
    singleOf(::GitLiveGradingQueueRepository) bind GradingQueueRepository::class
    singleOf(::GitLiveQuestionCacheRepository) bind QuestionCacheRepository::class
    singleOf(::GitLiveTestRepository) bind TestRepository::class
    singleOf(::GitLiveAnalyticsRepository) bind AnalyticsRepository::class
    singleOf(::GitLiveStudyContentRepository) bind StudyContentRepository::class
    single { GitLiveDifficultyProgressionManager(get()) }
    single { GitLiveGTOTaskCacheManager(get()) }
    single { GitLiveWATWordCacheManager(get()) }
    single { GitLiveGPEImageCacheManager(get()) }
    single { GitLivePPDTImageCacheManager(get()) }
    single { GitLiveSRTSituationCacheManager(get()) }
    single { GitLiveTATImageCacheManager(get()) }
    single { GitLiveOIRQuestionSelector(get()) }
    single { GitLiveOIRQuestionCacheManager(get(), get()) }
    single { GitLiveNotificationCacheManager(get()) }
    singleOf(::GitLiveTestContentRepository) bind TestContentRepository::class
    singleOf(::GitLiveGTORepository) bind GTORepository::class
    singleOf(::GitLiveNotificationRepository) bind NotificationRepository::class
    factoryOf(::KtorAIService) bind AIService::class

    factoryOf(::SignInWithGoogleUseCase)
    factoryOf(::GetOirResultUseCase)

    factoryOf(::AuthViewModel)
    factoryOf(::OirResultViewModel)
}
