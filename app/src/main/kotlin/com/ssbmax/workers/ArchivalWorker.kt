package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.ssbmax.shared.domain.repository.SubmissionRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Background worker to archive submissions older than 6 months
 * Runs daily when device is charging and connected to network
 * 
 * Archives old data to keep main database performant while
 * preserving historical data in archive collection
 */
class ArchivalWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val submissionRepository: SubmissionRepository by inject()

    override suspend fun doWork(): Result {
        Log.d(TAG, "📦 Starting archival worker")

        return try {
            // Calculate timestamp for 6 months ago
            val sixMonthsAgo = Instant.now()
                .minus(180, ChronoUnit.DAYS)
                .toEpochMilli()

            Log.d(TAG, "Archiving submissions older than: ${Instant.ofEpochMilli(sixMonthsAgo)}")

            // Archive submissions older than 6 months
            val archivedCount = submissionRepository.archiveOldSubmissions(sixMonthsAgo)
                .getOrElse { errorCount ->
                    Log.e(TAG, "Failed to archive submissions: $errorCount")
                    0
                }

            Log.d(TAG, "✅ Archived $archivedCount submissions")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Archival worker failed", e)
            
            // Retry on failure
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "ArchivalWorker"
        private const val WORK_NAME = "archival_worker"
        private const val MAX_RETRIES = 3

        /**
         * Schedule periodic archival worker
         * Runs daily at night when device is charging
         */
        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(true)  // Only run when charging
                .setRequiresBatteryNotLow(true)  // Don't drain battery
                .build()

            val archivalRequest = PeriodicWorkRequestBuilder<ArchivalWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)  // Delay first run by 1 hour
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // Keep existing schedule
                archivalRequest
            )
            
            Log.d(TAG, "📅 Archival worker scheduled (daily)")
        }
        
        /**
         * Cancel archival worker
         */
        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "🚫 Archival worker cancelled")
        }
    }
}
