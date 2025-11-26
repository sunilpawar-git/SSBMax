package com.ssbmax.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ssbmax.MainActivity
import com.ssbmax.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for sending local notifications
 *
 * Used by background workers to notify users when async operations complete.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "NotificationHelper"
        private const val CHANNEL_ID_INTERVIEW = "interview_results"
        private const val CHANNEL_NAME_INTERVIEW = "Interview Results"
        private const val NOTIFICATION_ID_INTERVIEW_RESULT = 1001
    }

    init {
        Log.d(TAG, "📢 NotificationHelper initialized")
        createNotificationChannels()
    }

    /**
     * Create notification channels (required for Android O+)
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val interviewChannel = NotificationChannel(
                CHANNEL_ID_INTERVIEW,
                CHANNEL_NAME_INTERVIEW,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when your interview results are ready"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(interviewChannel)
        }
    }

    /**
     * Show notification when interview results are ready
     *
     * @param sessionId The interview session ID
     * @param resultId The result ID to navigate to
     */
    fun showInterviewResultsReadyNotification(sessionId: String, resultId: String) {
        Log.d(TAG, "📢 showInterviewResultsReadyNotification called - sessionId: $sessionId, resultId: $resultId")

        try {
            // Create intent to open results screen
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("deepLink", "ssbmax://interview/result/$resultId")
                putExtra("sessionId", sessionId)
                putExtra("resultId", resultId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                sessionId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_INTERVIEW)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.notification_interview_results_title))
                .setContentText(context.getString(R.string.notification_interview_results_body))
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_interview_results_body)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .build()

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val notificationId = NOTIFICATION_ID_INTERVIEW_RESULT + sessionId.hashCode()

            if (notificationManager == null) {
                Log.e(TAG, "❌ NotificationManager is null!")
                return
            }

            // Check if notifications are enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (!notificationManager.areNotificationsEnabled()) {
                    Log.w(TAG, "⚠️ Notifications are disabled by user")
                }
            }

            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "✅ Notification sent successfully! ID: $notificationId")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to show notification", e)
        }
    }

    /**
     * Show notification when interview analysis fails
     *
     * @param sessionId The interview session ID
     */
    fun showInterviewAnalysisFailedNotification(sessionId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deepLink", "ssbmax://interview/history")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_INTERVIEW)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.notification_interview_failed_title))
            .setContentText(context.getString(R.string.notification_interview_failed_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.notify(
            NOTIFICATION_ID_INTERVIEW_RESULT + sessionId.hashCode(),
            notification
        )
    }
}

