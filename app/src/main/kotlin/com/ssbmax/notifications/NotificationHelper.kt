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
import com.ssbmax.utils.DeepLinkParser
import com.ssbmax.utils.ErrorLogger
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
        private const val CHANNEL_ID_GTO = "gto_results"
        private const val CHANNEL_NAME_GTO = "GTO Test Results"
        private const val NOTIFICATION_ID_INTERVIEW_RESULT = 1001
        private const val NOTIFICATION_ID_GTO_RESULT = 2001
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

            val gtoChannel = NotificationChannel(
                CHANNEL_ID_GTO,
                CHANNEL_NAME_GTO,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when your GTO test results are ready"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(interviewChannel)
            notificationManager?.createNotificationChannel(gtoChannel)
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
            // Build deep link using centralized parser
            val deepLink = DeepLinkParser.buildInterviewResultDeepLink(resultId)
            
            // Create intent to open results screen
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("deepLink", deepLink)
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
                ErrorLogger.log(
                    IllegalStateException("NotificationManager is null"),
                    "Failed to get NotificationManager service"
                )
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
            ErrorLogger.log(e, "Failed to show interview results notification")
        }
    }

    /**
     * Show notification when interview analysis fails
     *
     * @param sessionId The interview session ID
     */
    fun showInterviewAnalysisFailedNotification(sessionId: String) {
        try {
            // Build deep link using centralized parser
            val deepLink = DeepLinkParser.buildInterviewHistoryDeepLink()
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("deepLink", deepLink)
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
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to show interview analysis failed notification")
        }
    }

    /**
     * Show notification when GTO test analysis is complete
     *
     * @param submissionId The GTO submission ID
     * @param testName The test name (e.g., "Group Discussion")
     * @param testType The GTO test type for building correct deep link
     */
    fun showGTOAnalysisCompleteNotification(
        submissionId: String, 
        testName: String,
        testType: com.ssbmax.core.domain.model.gto.GTOTestType
    ) {
        Log.d(TAG, "📢 showGTOAnalysisCompleteNotification called - submissionId: $submissionId, test: $testName, type: ${testType.name}")

        try {
            // Build test-type-specific deep link to GTO result screen
            // Navigation routes: test/gto/{type}/result/{submissionId}
            val deepLink = when (testType) {
                com.ssbmax.core.domain.model.gto.GTOTestType.GROUP_DISCUSSION -> 
                    "ssbmax://test/gto/gd/result/$submissionId"
                com.ssbmax.core.domain.model.gto.GTOTestType.GROUP_PLANNING_EXERCISE -> 
                    "ssbmax://test/gto/gpe/result/$submissionId"
                com.ssbmax.core.domain.model.gto.GTOTestType.LECTURETTE -> 
                    "ssbmax://test/gto/lecturette/result/$submissionId"
                com.ssbmax.core.domain.model.gto.GTOTestType.PROGRESSIVE_GROUP_TASK -> 
                    "ssbmax://test/gto/pgt/result/$submissionId"
                com.ssbmax.core.domain.model.gto.GTOTestType.HALF_GROUP_TASK -> 
                    "ssbmax://test/gto/hgt/result/$submissionId"
                com.ssbmax.core.domain.model.gto.GTOTestType.GROUP_OBSTACLE_RACE -> 
                    "ssbmax://test/gto/gor/result/$submissionId"
                com.ssbmax.core.domain.model.gto.GTOTestType.INDIVIDUAL_OBSTACLES -> 
                    "ssbmax://test/gto/io/result/$submissionId"
                com.ssbmax.core.domain.model.gto.GTOTestType.COMMAND_TASK -> 
                    "ssbmax://test/gto/ct/result/$submissionId"
            }
            
            Log.d(TAG, "📱 Deep link generated: $deepLink")
            
            // Create intent to open results screen
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("deepLink", deepLink)
                putExtra("submissionId", submissionId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                submissionId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_GTO)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("$testName Results Ready")
                .setContentText("Your GTO test has been analyzed. Tap to view your OLQ scores!")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Your $testName has been analyzed by AI. View your performance across all 15 Officer-Like Qualities."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .build()

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val notificationId = NOTIFICATION_ID_GTO_RESULT + submissionId.hashCode()

            if (notificationManager == null) {
                ErrorLogger.log(
                    IllegalStateException("NotificationManager is null"),
                    "Failed to get NotificationManager service"
                )
                return
            }

            // Check if notifications are enabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (!notificationManager.areNotificationsEnabled()) {
                    Log.w(TAG, "⚠️ Notifications are disabled by user")
                }
            }

            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "✅ GTO notification sent successfully! ID: $notificationId")

        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to show GTO analysis complete notification")
        }
    }
}

