package com.ssbmax.shared.di

import com.ssbmax.shared.db.DatabaseDriverFactory
import com.ssbmax.shared.platform.settings.AppThemeSettings
import com.ssbmax.shared.platform.settings.SettingsFactory
import com.ssbmax.shared.platform.tts.AndroidTTSService
import com.ssbmax.shared.platform.tts.TTSService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { SettingsFactory(androidContext()).create() }
    single { AppThemeSettings(get()) }
    single<TTSService> { AndroidTTSService(androidContext()) }
}
