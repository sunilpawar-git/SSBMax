package com.ssbmax.di

import androidx.work.WorkManager
import com.ssbmax.shared.platform.worker.BackgroundTaskScheduler
import com.ssbmax.shared.platform.worker.WorkManagerBackgroundTaskScheduler
import com.ssbmax.workers.ArchivalWorker
import com.ssbmax.workers.QuestionCacheCleanupWorker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for providing WorkManager and the shared-module
 * BackgroundTaskScheduler (Phase 4 platform shim). The scheduler is
 * bound here, in `app`, rather than in shared's own platformModule --
 * unlike iOS's BGTaskScheduler actual, the Android actual is generic over
 * which `ListenableWorker` classes to run (`shared` can't depend on
 * `app`'s concrete Worker classes), so `app` supplies them.
 */
val workManagerModule = module {
    single { WorkManager.getInstance(androidContext()) }
    single<BackgroundTaskScheduler> {
        WorkManagerBackgroundTaskScheduler(
            workManager = get(),
            cleanupWorker = QuestionCacheCleanupWorker::class,
            archivalWorker = ArchivalWorker::class
        )
    }
}
