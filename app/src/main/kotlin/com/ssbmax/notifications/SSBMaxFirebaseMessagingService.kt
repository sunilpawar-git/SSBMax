package com.ssbmax.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssbmax.MainActivity
import com.ssbmax.core.domain.model.NotificationType
import kotlin.random.Random

/**
 * Firebase Cloud Messaging Service for SSBMax
 * Handles incoming push notifications from Firebase
 */
class SSBMaxFirebaseMessagingService : FirebaseMessagingService() {
    
    // TODO: Inject NotificationHandler when NotificationRepository is available
    // @Inject lateinit var notificationHandler: NotificationHandler
    
    /**
     * Called when a new FCM token is generated
     * This happens on first app install and when token is refreshed
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // TODO: Send token to NotificationRepository to save in Firestore
        // This will be implemented when NotificationRepository is created
        android.util.Log.d(TAG, "New FCM token: $token")
    }
    
    /**
     * Called when a message is received
     * Parse and display notification based on type
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        android.util.Log.d(TAG, "Message received from: ${message.from}")
        
        // Check if message contains data payload
        if (message.data.isNotEmpty()) {
            android.util.Log.d(TAG, "Message data payload: ${message.data}")
            handleDataPayload(message.data)
        }
        
        // Check if message contains notification payload
        message.notification?.let {
            android.util.Log.d(TAG, "Message notification: ${it.title} - ${it.body}")
            handleNotificationPayload(it.title, it.body, message.data)
        }
    }
    
    /**
     * Handle data payload from FCM
     */
    private fun handleDataPayload(data: Map<String, String>) {
        val type = data["type"]?.let { 
            try { NotificationType.valueOf(it) } 
            catch (e: Exception) { null }
        } ?: NotificationType.GENERAL_ANNOUNCEMENT
        
        val title = data["title"] ?: "SSBMax Notification"
        val message = data["message"] ?: ""
        val actionUrl = data["actionUrl"]
        val notificationId = data["notificationId"]
        
        // Show notification
        showNotification(
            type = type,
            title = title,
            message = message,
            actionUrl = actionUrl,
            notificationId = notificationId
        )
        
        // TODO: Save notification to local database via NotificationRepository
        // This allows showing notification history in NotificationCenterScreen
    }
    
    /**
     * Handle notification payload from FCM
     */
    private fun handleNotificationPayload(
        title: String?,
        body: String?,
        data: Map<String, String>
    ) {
        val type = data["type"]?.let {
            try { NotificationType.valueOf(it) }
            catch (e: Exception) { null }
        } ?: NotificationType.GENERAL_ANNOUNCEMENT
        
        showNotification(
            type = type,
            title = title ?: "SSBMax",
            message = body ?: "",
            actionUrl = data["actionUrl"],
            notificationId = data["notificationId"]
        )
    }
    
    /**
     * Display notification to user
     */
    private fun showNotification(
        type: NotificationType,
        title: String,
        message: String,
        actionUrl: String?,
        notificationId: String?
    ) {
        val channelId = getChannelIdForType(type)
        val channelName = getChannelNameForType(type)
        val importance = getImportanceForType(type)
        
        // Create notification channel (required for Android O+)
        createNotificationChannel(channelId, channelName, importance)
        
        // Create intent for notification tap
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            actionUrl?.let { putExtra("deepLink", it) }
            notificationId?.let { putExtra("notificationId", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(getIconForType(type))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        
        // Add action buttons for specific types
        when (type) {
            NotificationType.GRADING_COMPLETE,
            NotificationType.FEEDBACK_AVAILABLE -> {
                notificationBuilder.addAction(
                    android.R.drawable.ic_menu_view,
                    "View Results",
                    pendingIntent
                )
            }
            NotificationType.BATCH_INVITATION -> {
                notificationBuilder.addAction(
                    android.R.drawable.ic_menu_view,
                    "View Invitation",
                    pendingIntent
                )
            }
            else -> {
                // No action buttons for other types
            }
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Use notification ID from payload or generate random
        val notifId = notificationId?.hashCode() ?: Random.nextInt()
        notificationManager.notify(notifId, notificationBuilder.build())
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel(
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
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Get channel ID based on notification type
     */
    private fun getChannelIdForType(type: NotificationType): String {
        return when (type) {
            NotificationType.GRADING_COMPLETE -> CHANNEL_GRADING
            NotificationType.FEEDBACK_AVAILABLE -> CHANNEL_FEEDBACK
            NotificationType.BATCH_INVITATION -> CHANNEL_BATCH
            NotificationType.GENERAL_ANNOUNCEMENT -> CHANNEL_GENERAL
            NotificationType.STUDY_REMINDER -> CHANNEL_REMINDERS
            NotificationType.TEST_REMINDER -> CHANNEL_REMINDERS
            NotificationType.MARKETPLACE_UPDATE -> CHANNEL_MARKETPLACE
        }
    }
    
    /**
     * Get channel name based on notification type
     */
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
    
    /**
     * Get importance level based on notification type
     */
    private fun getImportanceForType(type: NotificationType): Int {
        return when (type) {
            NotificationType.GRADING_COMPLETE,
            NotificationType.FEEDBACK_AVAILABLE -> NotificationManager.IMPORTANCE_HIGH
            NotificationType.BATCH_INVITATION -> NotificationManager.IMPORTANCE_DEFAULT
            NotificationType.GENERAL_ANNOUNCEMENT -> NotificationManager.IMPORTANCE_DEFAULT
            NotificationType.STUDY_REMINDER,
            NotificationType.TEST_REMINDER -> NotificationManager.IMPORTANCE_DEFAULT
            NotificationType.MARKETPLACE_UPDATE -> NotificationManager.IMPORTANCE_LOW
        }
    }
    
    /**
     * Get icon based on notification type
     */
    private fun getIconForType(type: NotificationType): Int {
        // TODO: Add specific icons for each type
        return android.R.drawable.ic_dialog_info
    }
    
    companion object {
        private const val TAG = "SSBMaxFCM"
        
        // Notification channel IDs
        private const val CHANNEL_GRADING = "grading_channel"
        private const val CHANNEL_FEEDBACK = "feedback_channel"
        private const val CHANNEL_BATCH = "batch_channel"
        private const val CHANNEL_GENERAL = "general_channel"
        private const val CHANNEL_REMINDERS = "reminders_channel"
        private const val CHANNEL_MARKETPLACE = "marketplace_channel"
    }
}

