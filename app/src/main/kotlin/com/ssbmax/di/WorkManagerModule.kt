package com.ssbmax.di

import androidx.work.WorkManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for providing WorkManager.
 */
val workManagerModule = module {
    single { WorkManager.getInstance(androidContext()) }
}
