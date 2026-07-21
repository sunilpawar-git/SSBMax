package com.ssbmax

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ssbmax.di.appModules
import com.ssbmax.utils.AppConstants
import com.ssbmax.workers.QuestionCacheCleanupWorker
import java.util.concurrent.TimeUnit
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

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
 * - Schedule periodic question cache cleanup (daily)
 */
class SSBMaxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 SSBMax Application starting...")

        startKoin {
            androidLogger()
            androidContext(this@SSBMaxApplication)
            modules(appModules)
        }

        // Schedule periodic cleanup of expired question cache
        scheduleQuestionCacheCleanup()

        // Schedule daily archival of old submissions (6+ months)
        com.ssbmax.workers.ArchivalWorker.schedule(WorkManager.getInstance(this))

        Log.d(TAG, "✅ SSBMax Application initialized")
    }

    /**
     * Schedule daily cleanup of expired question cache entries
     *
     * - Runs once every 24 hours
     * - Requires network connection (to verify Firestore timestamps)
     * - Requires battery not low (to avoid draining user's battery)
     * - Cleans up PIQ-based questions older than 30 days
     */
    private fun scheduleQuestionCacheCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<QuestionCacheCleanupWorker>(
            repeatInterval = AppConstants.WorkManager.CLEANUP_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag("question_cache_cleanup")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AppConstants.WorkManager.CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing schedule if already running
            cleanupRequest
        )

        Log.d(TAG, "📅 Scheduled periodic question cache cleanup (every ${AppConstants.WorkManager.CLEANUP_INTERVAL_HOURS} hours)")
    }
}

