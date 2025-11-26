package com.ssbmax.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import com.ssbmax.utils.ErrorLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic worker for cleaning up expired question cache entries
 *
 * Runs daily to:
 * - Remove PIQ-based questions older than 30 days
 * - Free up Firestore storage
 * - Maintain cache efficiency
 *
 * Scheduled in SSBMaxApplication.onCreate()
 */
@HiltWorker
class QuestionCacheCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val questionCacheRepository: QuestionCacheRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "QuestionCacheCleanup"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "🗑️ Starting periodic question cache cleanup...")

            val deletedCount = questionCacheRepository.cleanupExpired()
                .getOrElse { error ->
                    ErrorLogger.log(error, "Failed to cleanup expired question cache")
                    return if (runAttemptCount < 2) {
                        Log.w(TAG, "   ⚠️ Retry attempt ${runAttemptCount + 1}/2")
                        Result.retry()
                    } else {
                        Log.e(TAG, "   ❌ Max retries reached")
                        Result.failure()
                    }
                }

            if (deletedCount > 0) {
                Log.d(TAG, "✅ Cleaned up $deletedCount expired cache entries")
            } else {
                Log.d(TAG, "✅ No expired entries to clean up")
            }

            Result.success()
        } catch (e: Exception) {
            ErrorLogger.log(e, "Cache cleanup failed unexpectedly")
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
