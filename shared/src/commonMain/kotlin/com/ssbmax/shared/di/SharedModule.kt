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
import com.ssbmax.shared.data.repository.GitLiveGTOCollections
import com.ssbmax.shared.data.repository.GitLiveGTOProgressDelegate
import com.ssbmax.shared.data.repository.GitLiveGTORepository
import com.ssbmax.shared.data.repository.GitLiveGTOResultsDelegate
import com.ssbmax.shared.data.repository.GitLiveGTOSubmissionDelegate
import com.ssbmax.shared.data.repository.GitLiveGTOTaskCacheManager
import com.ssbmax.shared.data.repository.GitLiveInterviewRepository
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
import com.ssbmax.shared.data.repository.GitLiveSubmissionRepository
import com.ssbmax.shared.data.repository.GitLiveSubscriptionRepository
import com.ssbmax.shared.data.repository.GitLiveTATImageCacheManager
import com.ssbmax.shared.data.repository.GitLiveTestContentRepository
import com.ssbmax.shared.data.repository.GitLiveTestProgressRepository
import com.ssbmax.shared.data.repository.GitLiveTestRepository
import com.ssbmax.shared.data.repository.GitLiveTestSessionRepository
import com.ssbmax.shared.data.repository.GitLiveTestUsageRecorder
import com.ssbmax.shared.data.repository.GitLiveUserProfileRepository
import com.ssbmax.shared.data.repository.GitLiveWATWordCacheManager
import com.ssbmax.shared.data.repository.InterviewQuestionGenerator
import com.ssbmax.shared.data.repository.OirResultCache
import com.ssbmax.shared.db.DatabaseDriverFactory
import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.domain.model.interview.QuestionCacheRepository
import com.ssbmax.shared.domain.repository.AnalyticsRepository
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.repository.GradingQueueRepository
import com.ssbmax.shared.domain.repository.GTORepository
import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.shared.domain.repository.NotificationRepository
import com.ssbmax.shared.domain.repository.OirResultRepository
import com.ssbmax.shared.domain.repository.StudyContentRepository
import com.ssbmax.shared.domain.repository.StudyProgressRepository
import com.ssbmax.shared.domain.repository.SubmissionRepository
import com.ssbmax.shared.domain.repository.SubscriptionRepository
import com.ssbmax.shared.domain.repository.TestContentRepository
import com.ssbmax.shared.domain.repository.TestProgressRepository
import com.ssbmax.shared.domain.repository.TestRepository
import com.ssbmax.shared.domain.repository.TestSessionRepository
import com.ssbmax.shared.domain.repository.TestUsageRecorder
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.domain.service.AIService
import com.ssbmax.shared.domain.service.LoggingSubmissionAnalysisTrigger
import com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger
import com.ssbmax.shared.domain.usecase.GetOirResultUseCase
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.auth.SignInWithGoogleUseCase
import com.ssbmax.shared.domain.usecase.auth.SignOutUseCase
import com.ssbmax.shared.domain.usecase.auth.UpdateUserRoleUseCase
import com.ssbmax.shared.domain.usecase.dashboard.GetOLQDashboardUseCase
import com.ssbmax.shared.domain.usecase.oir.OIRTestScoreCalculator
import com.ssbmax.shared.domain.usecase.oir.SubmitOIRTestUseCase
import com.ssbmax.shared.domain.usecase.ppdt.LoadPPDTTestUseCase
import com.ssbmax.shared.domain.usecase.ppdt.SubmitPPDTTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitSRTTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitTATTestUseCase
import com.ssbmax.shared.domain.usecase.submission.SubmitWATTestUseCase
import com.ssbmax.shared.domain.usecase.subscription.CheckTestEligibilityUseCase
import com.ssbmax.shared.domain.usecase.tat.LoadTATTestUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.auth.AuthViewModel
import com.ssbmax.shared.presentation.home.instructor.InstructorHomeViewModel
import com.ssbmax.shared.presentation.home.student.StudentHomeViewModel
import com.ssbmax.shared.presentation.oir.OIRTestViewModel
import com.ssbmax.shared.presentation.oirresult.OirResultViewModel
import com.ssbmax.shared.presentation.ppdt.PPDTTestViewModel
import com.ssbmax.shared.presentation.ppdtresult.PPDTSubmissionResultViewModel
import com.ssbmax.shared.presentation.srt.SRTTestViewModel
import com.ssbmax.shared.presentation.srtresult.SRTSubmissionResultViewModel
import com.ssbmax.shared.presentation.splash.SplashViewModel
import com.ssbmax.shared.presentation.tat.TATTestViewModel
import com.ssbmax.shared.presentation.tatresult.TATSubmissionResultViewModel
import com.ssbmax.shared.presentation.wat.WATTestViewModel
import com.ssbmax.shared.presentation.watresult.WATSubmissionResultViewModel
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
 *
 * Phase 5 (home-screen vertical): added [GetOLQDashboardUseCase],
 * [StudentHomeViewModel], [InstructorHomeViewModel]. Their constructor
 * dependencies (`UserProfileRepository`, `TestProgressRepository`,
 * `NotificationRepository`, `GradingQueueRepository`) are also bound by
 * `core:data`'s `repositoryModule` (Android-only, included later in `app`'s
 * `appModules` list) — the same shadow-binding pattern already documented on
 * `RepositoryModule.kt` (`core:data`) for `AuthRepository` before it was
 * fixed there. Left unfixed here deliberately: unlike `AuthRepository`'s
 * incompatible `GoogleSignInData.platformData` shapes, these interfaces'
 * GitLive vs. Android-Firestore implementations are behaviorally
 * interchangeable (same method contracts, no crash-causing mismatch) — on
 * Android, Koin's override lets `core:data`'s impl win (unchanged prior
 * behavior); on iOS, `core:data` doesn't exist so `sharedModule`'s GitLive
 * impls are the only binding anyway. Broadly rewiring all double-bound
 * repositories is out of this session's scope (Phase 5 continuation).
 *
 * Phase 5 (OIR test-taking vertical): added [CheckTestEligibilityUseCase],
 * [GitLiveTestUsageRecorder] (`TestUsageRecorder`), [GitLiveTestSessionRepository]
 * (`TestSessionRepository`), [OIRTestScoreCalculator]/[SubmitOIRTestUseCase]
 * factories, and [OIRTestViewModel]. `TestUsageRecorder`/`TestSessionRepository`
 * are the same kind of double-bound interface as `StudentHomeViewModel`'s
 * dependencies above: `core:data`'s `repositoryModule` binds its own
 * `SubscriptionManager`/`TestSessionManagerImpl` to these same interfaces,
 * and (per `app/.../di/KoinModules.kt`'s `appModules` list) `repositoryModule`
 * loads *after* `sharedModule`, so on Android the `core:data` impls still win
 * — this session's Android production OIR flow (the whole `app/.../ui/tests/oir`
 * package, unchanged, still on its own Hilt-era-turned-Koin ViewModel) keeps its
 * existing behavior (debug bypass, Room usage mirror, `SecurityEventLogger`)
 * unaffected. On iOS, `core:data` doesn't exist, so these `GitLive*`/
 * `CheckTestEligibilityUseCase` bindings are the only ones and are what
 * [OIRTestViewModel] actually runs against. Deliberately NOT swapping the
 * Android app's `SharedNavGraph.kt` OIR entries over to the new `shared`
 * screens this session (unlike the auth/home verticals' precedent) — see
 * this phase's exit report for why: `CheckTestEligibilityUseCase` is new,
 * unreviewed-in-production code touching subscription-limit enforcement
 * (monetization-critical), and swapping it into the live Android OIR flow
 * without its own dedicated test coverage first is a bigger risk than this
 * session's scope justifies. The new vertical is reachable today via
 * `SSBMaxNavHost` (iOS + the future full Android switchover), not yet via
 * `app`'s production nav graph.
 *
 * Phase 5 (PPDT test-taking vertical): added [LoadPPDTTestUseCase]/
 * [SubmitPPDTTestUseCase] factories, [PPDTTestViewModel],
 * [PPDTSubmissionResultViewModel], and [LoggingSubmissionAnalysisTrigger]
 * bound to [SubmissionAnalysisTrigger] -- the *only* binding for that
 * interface anywhere in `shared`; see its own doc comment for the real
 * consequence (no background AI analysis is actually triggered yet for a
 * submission made through this vertical). Same "not swapped into the live
 * Android app" precedent as OIR: `app/.../ui/tests/ppdt` is untouched, this
 * vertical is reachable only via `SSBMaxNavHost`.
 *
 * Phase 5 (TAT test-taking vertical): added [LoadTATTestUseCase] (already
 * bound as a plain constructor since Phase 2, wired here for the first
 * time)/[SubmitTATTestUseCase] factories, [TATTestViewModel],
 * [TATSubmissionResultViewModel]. Reuses the exact same
 * [LoggingSubmissionAnalysisTrigger]/[SubmissionAnalysisTrigger] binding
 * PPDT's session introduced -- no new binding needed, same real consequence
 * (a TAT submission through this vertical persists but is not yet
 * AI-analyzed). Unlike PPDT, `SubmitTATTestUseCase` was deliberately kept as
 * its pre-existing thin pass-through (an attempt to widen it to PPDT's
 * build-then-record-usage shape was reverted after it broke
 * `:app:compileDebugKotlin` -- `app/.../ui/tests/tat/TATTestViewModel.kt`, the
 * live Android production TAT flow, already calls this exact signature; see
 * that use case's own doc comment for the full story). [TATTestViewModel]
 * (the KMP port) instead builds the submission and records usage directly,
 * requiring [UserProfileRepository]/[TestUsageRecorder] as constructor
 * dependencies -- both already bound above for other verticals. Same "not
 * swapped into the live Android app" precedent as OIR/PPDT: `app/.../ui/tests/tat`
 * is untouched, this vertical is reachable only via `SSBMaxNavHost`.
 *
 * Phase 5 (WAT test-taking vertical): added [SubmitWATTestUseCase] factory
 * (`GitLiveWATWordCacheManager`/`TestContentRepository.getWATQuestions` were
 * already bound above from an earlier phase), [WATTestViewModel],
 * [WATSubmissionResultViewModel]. Real finding, stated plainly per this
 * session's brief: WAT DOES use the async-analysis seam (the Android
 * original enqueues `WATAnalysisWorker` via WorkManager after submission,
 * same shape as TAT/PPDT) -- it is not synchronous/rule-based scoring.
 * Reuses the same [LoggingSubmissionAnalysisTrigger]/[SubmissionAnalysisTrigger]
 * binding, no new binding needed; same real consequence (a WAT submission
 * through this vertical persists but is not yet AI-analyzed). Unlike TAT,
 * WAT's Android original has no gender/profile-completeness gate before
 * loading, so no `LoadWATTestUseCase` wrapper was introduced --
 * [WATTestViewModel] calls [TestSessionRepository]/[TestContentRepository]
 * directly. Same "not swapped into the live Android app" precedent as
 * OIR/PPDT/TAT: `app/.../ui/tests/wat` is untouched, this vertical is
 * reachable only via `SSBMaxNavHost`.
 *
 * Phase 5 (SRT test-taking vertical): added [SubmitSRTTestUseCase] factory --
 * real finding, matching the WAT session's `SubmitWATTestUseCase` discovery:
 * [SubmitSRTTestUseCase] already existed in `core:domain`/`shared` (it's a
 * thin pass-through to `SubmissionRepository.submitSRT`) but was NEVER bound
 * in this Koin module before this session; without this factory,
 * [SRTTestViewModel] would fail to resolve at runtime. `GitLiveSRTSituationCacheManager`/
 * `TestContentRepository.getSRTQuestions` were already bound above from an
 * earlier phase. Added [SRTTestViewModel], [SRTSubmissionResultViewModel].
 * Real finding, stated plainly per this session's brief: SRT DOES use the
 * async-analysis seam (the Android original enqueues `SRTAnalysisWorker` via
 * WorkManager after submission, same shape as TAT/WAT/PPDT) -- it is not
 * synchronous/rule-based scoring. Reuses the same
 * [LoggingSubmissionAnalysisTrigger]/[SubmissionAnalysisTrigger] binding, no
 * new binding needed; same real consequence (an SRT submission through this
 * vertical persists but is not yet AI-analyzed). Like WAT, SRT's Android
 * original has no gender/profile-completeness gate before loading, so no
 * `LoadSRTTestUseCase` wrapper was introduced -- [SRTTestViewModel] calls
 * [TestSessionRepository]/[TestContentRepository] directly. Same "not
 * swapped into the live Android app" precedent as OIR/PPDT/TAT/WAT:
 * `app/.../ui/tests/srt` is untouched, this vertical is reachable only via
 * `SSBMaxNavHost`.
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
    single { GitLiveGTOCollections() }
    single { GitLiveGTOSubmissionDelegate(get()) }
    single { GitLiveGTOProgressDelegate(get()) }
    single { GitLiveGTOResultsDelegate(get(), get()) }
    singleOf(::GitLiveGTORepository) bind GTORepository::class
    singleOf(::GitLiveNotificationRepository) bind NotificationRepository::class
    factoryOf(::KtorAIService) bind AIService::class
    single { GitLiveSubmissionRepository() } bind SubmissionRepository::class
    single { InterviewQuestionGenerator(get(), get(), get()) }
    singleOf(::GitLiveInterviewRepository) bind InterviewRepository::class
    singleOf(::GitLiveTestSessionRepository) bind TestSessionRepository::class
    singleOf(::GitLiveTestUsageRecorder) bind TestUsageRecorder::class

    factoryOf(::SignInWithGoogleUseCase)
    factoryOf(::UpdateUserRoleUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::ObserveCurrentUserUseCase)
    factoryOf(::GetOirResultUseCase)
    factoryOf(::GetOLQDashboardUseCase)
    factoryOf(::CheckTestEligibilityUseCase)
    factoryOf(::OIRTestScoreCalculator)
    factoryOf(::SubmitOIRTestUseCase)
    factoryOf(::LoadPPDTTestUseCase)
    factoryOf(::SubmitPPDTTestUseCase)
    factoryOf(::LoadTATTestUseCase)
    factoryOf(::SubmitTATTestUseCase)
    factoryOf(::SubmitWATTestUseCase)
    factoryOf(::SubmitSRTTestUseCase)

    singleOf(::LoggingSubmissionAnalysisTrigger) bind SubmissionAnalysisTrigger::class

    factoryOf(::AuthViewModel)
    factoryOf(::OirResultViewModel)
    factoryOf(::SplashViewModel)
    factoryOf(::StudentHomeViewModel)
    factoryOf(::InstructorHomeViewModel)
    factoryOf(::OIRTestViewModel)
    factoryOf(::PPDTTestViewModel)
    factoryOf(::PPDTSubmissionResultViewModel)
    factoryOf(::TATTestViewModel)
    factoryOf(::TATSubmissionResultViewModel)
    factoryOf(::WATTestViewModel)
    factoryOf(::WATSubmissionResultViewModel)
    factoryOf(::SRTTestViewModel)
    factoryOf(::SRTSubmissionResultViewModel)
}
