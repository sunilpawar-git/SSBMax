package com.ssbmax.shared.platform.worker

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Android actual, backed by `androidx.work.WorkManager`. Real behavior moved
 * from `SSBMaxApplication.onCreate` (cleanup) and `ArchivalWorker.schedule`
 * (archival) — same intervals/constraints, unchanged.
 *
 * The concrete `CoroutineWorker` subclasses (`QuestionCacheCleanupWorker`,
 * `ArchivalWorker`) stay in `app` (they're Android-only, KoinComponent-based
 * classes with business logic that reaches into `shared` repositories) —
 * `shared` can't depend on `app` in the other direction, so this class is
 * parameterized by [KClass] reference instead of hardcoding them, and
 * `app`'s Koin module supplies the concrete classes when constructing this.
 */
class WorkManagerBackgroundTaskScheduler(
    private val workManager: WorkManager,
    private val cleanupWorker: KClass<out ListenableWorker>,
    private val archivalWorker: KClass<out ListenableWorker>
) : BackgroundTaskScheduler {

    override fun scheduleQuestionCacheCleanup() {
        val request = PeriodicWorkRequest.Builder(
            cleanupWorker.java,
            CLEANUP_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag(CLEANUP_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun scheduleSubmissionArchival() {
        val request = PeriodicWorkRequest.Builder(
            archivalWorker.java,
            1,
            TimeUnit.DAYS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(true)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setInitialDelay(1, TimeUnit.HOURS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            ARCHIVAL_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private companion object {
        const val CLEANUP_INTERVAL_HOURS = 24L
        const val CLEANUP_TAG = "question_cache_cleanup"
        // Same literal AppConstants.WorkManager.CLEANUP_WORK_NAME used pre-shim --
        // keeps WorkManager's uniqueWork identity stable across this migration
        // (a changed name would silently duplicate the schedule on existing installs).
        const val CLEANUP_WORK_NAME = "question_cache_cleanup_periodic"
        const val ARCHIVAL_WORK_NAME = "archival_worker"
    }
}
