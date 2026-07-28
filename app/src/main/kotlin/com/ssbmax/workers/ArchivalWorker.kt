package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.shared.domain.repository.SubmissionRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.temporal.ChronoUnit

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
        private const val MAX_RETRIES = 3
        // Scheduling (periodic request + constraints) moved to
        // com.ssbmax.shared.platform.worker.WorkManagerBackgroundTaskScheduler
        // (Phase 4 platform shim) -- same constraints/interval, unchanged.
        // This class now only implements doWork(); it no longer schedules itself.
    }
}
