package com.ssbmax.shared.di

import com.ssbmax.shared.db.DatabaseDriverFactory
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory() }
}
