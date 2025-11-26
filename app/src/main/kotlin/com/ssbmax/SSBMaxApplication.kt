package com.ssbmax

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ssbmax.utils.AppConstants
import com.ssbmax.workers.QuestionCacheCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "SSBMaxApplication"

/**
 * SSBMax Application class
 * Hilt entry point for dependency injection
 *
 * Responsibilities:
 * - Initialize WorkManager for background jobs
 * - Configure custom HiltWorkerFactory for dependency injection in workers
 * - Schedule periodic question cache cleanup (daily)
 */
@HiltAndroidApp
class SSBMaxApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 SSBMax Application starting...")

        // Schedule periodic cleanup of expired question cache
        scheduleQuestionCacheCleanup()

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

