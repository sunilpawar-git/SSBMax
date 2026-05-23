package com.ssbmax.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ssbmax.MainActivity
import com.ssbmax.core.domain.model.NotificationType
import kotlin.random.Random

/**
 * Helper class to encapsulate system push notification builder and channels configuration.
 */
object FCMNotificationHelper {

    private const val CHANNEL_GRADING = "grading_channel"
    private const val CHANNEL_FEEDBACK = "feedback_channel"
    private const val CHANNEL_BATCH = "batch_channel"
    private const val CHANNEL_GENERAL = "general_channel"
    private const val CHANNEL_REMINDERS = "reminders_channel"
    private const val CHANNEL_MARKETPLACE = "marketplace_channel"

    fun showNotification(
        context: Context,
        type: NotificationType,
        title: String,
        message: String,
        actionUrl: String?,
        notificationId: String?
    ) {
        val channelId = getChannelIdForType(type)
        val channelName = getChannelNameForType(type)
        val importance = getImportanceForType(type)

        createNotificationChannel(context, channelId, channelName, importance)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            actionUrl?.let { putExtra("deepLink", it) }
            notificationId?.let { putExtra("notificationId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        addActionButtons(builder, type, pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = notificationId?.hashCode() ?: Random.nextInt()
        notificationManager.notify(notifId, builder.build())
    }

    private fun addActionButtons(
        builder: NotificationCompat.Builder,
        type: NotificationType,
        pendingIntent: PendingIntent
    ) {
        when (type) {
            NotificationType.GRADING_COMPLETE,
            NotificationType.FEEDBACK_AVAILABLE -> {
                builder.addAction(android.R.drawable.ic_menu_view, "View Results", pendingIntent)
            }
            NotificationType.BATCH_INVITATION -> {
                builder.addAction(android.R.drawable.ic_menu_view, "View Invitation", pendingIntent)
            }
            else -> {}
        }
    }

    private fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "SSBMax notifications for $channelName"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getChannelIdForType(type: NotificationType): String {
        return when (type) {
            NotificationType.GRADING_COMPLETE -> CHANNEL_GRADING
            NotificationType.FEEDBACK_AVAILABLE -> CHANNEL_FEEDBACK
            NotificationType.BATCH_INVITATION -> CHANNEL_BATCH
            NotificationType.GENERAL_ANNOUNCEMENT -> CHANNEL_GENERAL
            NotificationType.STUDY_REMINDER,
            NotificationType.TEST_REMINDER -> CHANNEL_REMINDERS
            NotificationType.MARKETPLACE_UPDATE -> CHANNEL_MARKETPLACE
        }
    }

    private fun getChannelNameForType(type: NotificationType): String {
        return when (type) {
            NotificationType.GRADING_COMPLETE -> "Grading Complete"
            NotificationType.FEEDBACK_AVAILABLE -> "New Feedback"
            NotificationType.BATCH_INVITATION -> "Batch Invitations"
            NotificationType.GENERAL_ANNOUNCEMENT -> "Announcements"
            NotificationType.STUDY_REMINDER -> "Study Reminders"
            NotificationType.TEST_REMINDER -> "Test Reminders"
            NotificationType.MARKETPLACE_UPDATE -> "Marketplace Updates"
        }
    }

    private fun getImportanceForType(type: NotificationType): Int {
        return when (type) {
            NotificationType.GRADING_COMPLETE,
            NotificationType.FEEDBACK_AVAILABLE -> NotificationManager.IMPORTANCE_HIGH
            NotificationType.BATCH_INVITATION,
            NotificationType.GENERAL_ANNOUNCEMENT,
            NotificationType.STUDY_REMINDER,
            NotificationType.TEST_REMINDER -> NotificationManager.IMPORTANCE_DEFAULT
            NotificationType.MARKETPLACE_UPDATE -> NotificationManager.IMPORTANCE_LOW
        }
    }
}
