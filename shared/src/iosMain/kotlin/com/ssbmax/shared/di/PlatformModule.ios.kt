package com.ssbmax.shared.di

import com.ssbmax.shared.db.DatabaseDriverFactory
import com.ssbmax.shared.domain.util.AnalyticsTracker
import com.ssbmax.shared.domain.util.CrashReporter
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.platform.audio.WhiteNoisePlayer
import com.ssbmax.shared.platform.billing.BillingClient
import com.ssbmax.shared.platform.billing.StoreKitBillingClient
import com.ssbmax.shared.platform.permissions.IosNotificationPermissionController
import com.ssbmax.shared.platform.permissions.NotificationPermissionController
import com.ssbmax.shared.platform.settings.AppThemeSettings
import com.ssbmax.shared.platform.settings.SettingsFactory
import com.ssbmax.shared.platform.tts.IosTTSService
import com.ssbmax.shared.platform.tts.TTSService
import com.ssbmax.shared.platform.util.IosAnalyticsTracker
import com.ssbmax.shared.platform.util.IosCrashReporter
import com.ssbmax.shared.platform.util.IosDomainLogger
import com.ssbmax.shared.platform.worker.BGTaskSchedulerBackgroundTaskScheduler
import com.ssbmax.shared.platform.worker.BackgroundTaskScheduler
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory() }
    single { SettingsFactory().create() }
    single { AppThemeSettings(get()) }
    single<TTSService> { IosTTSService() }
    single { WhiteNoisePlayer() }
    // Unlike Android's AndroidNotificationPermissionController, iOS's
    // UNUserNotificationCenter has no Activity-registration lifecycle
    // constraint, so it can be a plain Application-scoped Koin single.
    single<NotificationPermissionController> { IosNotificationPermissionController() }
    single<BackgroundTaskScheduler> { BGTaskSchedulerBackgroundTaskScheduler(get(), get(), get()) }
    single<BillingClient> { StoreKitBillingClient() }
    // Phase 7a: real observability seams -- closes "iOS has zero logging"/
    // "iOS ships blind to crashes" (this plan's opening complaint for iOS).
    single<DomainLogger> { IosDomainLogger() }
    single<CrashReporter> { IosCrashReporter() }
    single<AnalyticsTracker> { IosAnalyticsTracker() }
}
