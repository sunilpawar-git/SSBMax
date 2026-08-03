package com.ssbmax.shared.di

import com.ssbmax.shared.ai.KtorAIService
import com.ssbmax.shared.ai.KtorGeminiClient
import com.ssbmax.shared.ai.KtorPPDTAnalyzer
import com.ssbmax.shared.ai.KtorTATStoryAnalyzer
import com.ssbmax.shared.analysis.GTOAnalysisOrchestrator
import com.ssbmax.shared.analysis.InterviewAnalysisOrchestrator
import com.ssbmax.shared.analysis.KtorSubmissionAnalysisTrigger
import com.ssbmax.shared.analysis.PPDTAnalysisOrchestrator
import com.ssbmax.shared.analysis.SDAnalysisOrchestrator
import com.ssbmax.shared.analysis.SRTAnalysisOrchestrator
import com.ssbmax.shared.analysis.TATAnalysisOrchestrator
import com.ssbmax.shared.analysis.WATAnalysisOrchestrator
import com.ssbmax.navigation.DeepLinkGateway
import com.ssbmax.shared.data.repository.OirResultCache
import com.ssbmax.shared.db.DatabaseDriverFactory
import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.domain.service.AIService
import com.ssbmax.shared.domain.service.SubmissionAnalysisTrigger
import com.ssbmax.shared.domain.util.ObservabilitySeam
import com.ssbmax.shared.presentation.root.AppRootViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin property key carrying the Gemini API key into [coreInfraModule].
 *
 * SSOT for the literal: both entry points that supply it (Android's
 * `SSBMaxApplication.onCreate`, from `BuildConfig`; iOS's `ensureKoinStarted`,
 * from `Info.plist`) and the [KtorGeminiClient] binding that reads it use this
 * constant, so a typo can't silently degrade to an empty key on one platform.
 */
const val GEMINI_API_KEY_PROPERTY = "GEMINI_API_KEY"

/**
 * Cross-cutting infrastructure: platform shims, local DB, HTTP/Gemini client,
 * and the one [SubmissionAnalysisTrigger] binding shared by every async-analyzed
 * test vertical (PPDT/TAT/WAT/SRT/SDT/GTO/Interview) — see that interface's own
 * doc comment for the still-open real consequence (submissions persist but
 * aren't yet AI-analyzed through this `shared` path).
 *
 * DomainLogger/CrashReporter/AnalyticsTracker are bound per-platform in
 * [platformModule] (Phase 7a) — real logcat/NSLog, Crashlytics, and
 * Analytics implementations, not a shared no-op default. `NoOpLogger` still
 * exists as a test double (see `RepositoryFakes.kt`), just no longer as the
 * production binding here.
 *
 * The Gemini API key is read from Koin's property store
 * ([GEMINI_API_KEY_PROPERTY], empty default) rather than hardcoded, per this
 * repo's "never hardcode secrets" rule — the app's `startKoin()` call must
 * supply it via `properties()` before this module resolves [KtorGeminiClient].
 * Both platforms do (Phase 9.0); this is now the only `AIService` binding in
 * the app, so an empty key here means AI evaluation fails everywhere.
 */
val coreInfraModule = module {
    includes(platformModule)

    // See ObservabilitySeam's own doc for why this exists alongside the
    // plain DomainLogger/AnalyticsTracker bindings platformModule provides.
    single { ObservabilitySeam(get(), get()) }

    // Root ViewModel for every entry point rendering SSBMaxRoot (Android's
    // MainActivity, iOS's MainViewController).
    viewModelOf(::AppRootViewModel)

    // Deep-link seam (Phase 4): one Koin singleton both platforms' entry
    // points submit into, and SSBMaxRoot's DeepLinkEffect drains.
    single { DeepLinkGateway() }

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
            apiKey = getProperty(GEMINI_API_KEY_PROPERTY, "")
        )
    }
    single { KtorPPDTAnalyzer(client = get(), logger = get()) }
    single { KtorTATStoryAnalyzer(client = get(), logger = get()) }
    factoryOf(::KtorAIService) bind AIService::class

    // No app-wide CoroutineScope singleton existed before this -- see
    // GitLiveOIRQuestionCacheManager's doc comment for the same precedent this follows.
    // Now shared by the fire-and-forget SubmissionAnalysisTrigger dispatch below.
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    singleOf(::PPDTAnalysisOrchestrator)
    singleOf(::TATAnalysisOrchestrator)
    singleOf(::WATAnalysisOrchestrator)
    singleOf(::SRTAnalysisOrchestrator)
    singleOf(::SDAnalysisOrchestrator)
    singleOf(::GTOAnalysisOrchestrator)
    singleOf(::InterviewAnalysisOrchestrator)
    singleOf(::KtorSubmissionAnalysisTrigger) bind SubmissionAnalysisTrigger::class
}
