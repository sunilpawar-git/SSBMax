package com.ssbmax.shared.di

import com.ssbmax.shared.db.DatabaseDriverFactory
import com.ssbmax.shared.platform.settings.AppThemeSettings
import com.ssbmax.shared.platform.settings.SettingsFactory
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory() }
    single { SettingsFactory().create() }
    single { AppThemeSettings(get()) }
}
