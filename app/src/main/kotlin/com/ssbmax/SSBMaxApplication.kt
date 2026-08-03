package com.ssbmax

import android.app.Application
import android.util.Log
import com.ssbmax.di.appModules
import com.ssbmax.shared.di.BYPASS_SUBSCRIPTION_LIMITS_PROPERTY
import com.ssbmax.shared.di.GEMINI_API_KEY_PROPERTY
import com.ssbmax.shared.platform.worker.BackgroundTaskScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.get

private const val TAG = "SSBMaxApplication"

/**
 * SSBMax Application class
 * Koin entry point for dependency injection.
 *
 * Responsibilities:
 * - Start Koin with all app/core:data/:shared modules
 * - Initialize WorkManager for background jobs (default WorkManager worker
 *   factory now that workers resolve their own dependencies via
 *   `KoinComponent`/`inject()` instead of Hilt's assisted-injection worker
 *   factory — no custom `android.app.Configuration` provider override
 *   needed, so default WorkManager initialization is used again)
 * - Schedule periodic question cache cleanup + submission archival via the
 *   shared BackgroundTaskScheduler (Phase 4 platform shim) instead of
 *   calling WorkManager directly — same underlying WorkManager behavior on
 *   Android, but now behind the interface iOS's BGTaskScheduler actual also
 *   implements.
 * - Supply the Gemini API key as a Koin property (Phase 9.0), the way
 *   iOS's `ensureKoinStarted()` already does from its Info.plist. `shared`'s
 *   `coreInfraModule` reads it via `getProperty("GEMINI_API_KEY", "")` to
 *   build `KtorGeminiClient`; without this the key would resolve to "" and
 *   every AI evaluation would fail silently (no compile error) now that
 *   `core:data`'s `BuildConfig`-reading `aiModule` is gone.
 * - Supply the debug subscription-limit bypass as a Koin property (Phase 9d),
 *   same shape as the Gemini key above. `shared`'s `CheckTestEligibilityUseCase`
 *   reads it via `getProperty("BYPASS_SUBSCRIPTION_LIMITS", false)`; without
 *   this, local debug builds silently enforce real subscription limits
 *   instead of the `BYPASS_SUBSCRIPTION_LIMITS` convenience documented in
 *   `CLAUDE.local.md` (this restores it — it had gone dead once every test
 *   ViewModel migrated off `core:data`'s Android-only `SubscriptionManager`).
 */
class SSBMaxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 SSBMax Application starting...")

        startKoin {
            androidLogger()
            androidContext(this@SSBMaxApplication)
            properties(
                mapOf(
                    GEMINI_API_KEY_PROPERTY to BuildConfig.GEMINI_API_KEY,
                    BYPASS_SUBSCRIPTION_LIMITS_PROPERTY to (BuildConfig.DEBUG && BuildConfig.BYPASS_SUBSCRIPTION_LIMITS)
                )
            )
            modules(appModules)
        }

        val scheduler = get<BackgroundTaskScheduler>(BackgroundTaskScheduler::class.java)
        scheduler.scheduleQuestionCacheCleanup()
        scheduler.scheduleSubmissionArchival()

        Log.d(TAG, "✅ SSBMax Application initialized")
    }
}

