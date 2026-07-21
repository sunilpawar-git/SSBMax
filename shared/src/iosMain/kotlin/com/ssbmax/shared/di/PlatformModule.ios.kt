package com.ssbmax.shared.di

import com.ssbmax.shared.db.DatabaseDriverFactory
import com.ssbmax.shared.platform.audio.WhiteNoisePlayer
import com.ssbmax.shared.platform.permissions.IosNotificationPermissionController
import com.ssbmax.shared.platform.permissions.NotificationPermissionController
import com.ssbmax.shared.platform.settings.AppThemeSettings
import com.ssbmax.shared.platform.settings.SettingsFactory
import com.ssbmax.shared.platform.tts.IosTTSService
import com.ssbmax.shared.platform.tts.TTSService
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
    single<BackgroundTaskScheduler> { BGTaskSchedulerBackgroundTaskScheduler() }
}
