package com.ssbmax.core.domain.model

/**
 * Notification types for SSBMax app
 */
enum class NotificationType {
    GRADING_COMPLETE,       // Assessor finished grading a test
    FEEDBACK_AVAILABLE,     // New remarks/feedback added
    BATCH_INVITATION,       // Invited to join a batch
    GENERAL_ANNOUNCEMENT,   // General app announcements
    STUDY_REMINDER,         // Reminder to study
    TEST_REMINDER,          // Reminder to take a test
    MARKETPLACE_UPDATE      // Updates about marketplace/classes
}

/**
 * Notification priority levels
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * SSBMax Notification model
 */
data class SSBMaxNotification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val title: String,
    val message: String,
    val imageUrl: String? = null,
    val actionUrl: String? = null,          // Deep link to relevant screen
    val actionData: Map<String, String>? = null, // Additional data for navigation
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
) {
    /**
     * Check if notification is expired
     */
    val isExpired: Boolean
        get() = expiresAt?.let { it < System.currentTimeMillis() } ?: false
    
    /**
     * Get formatted time ago string
     */
    fun getTimeAgo(): String {
        val diff = System.currentTimeMillis() - createdAt
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }
}

/**
 * Notification settings/preferences
 */
data class NotificationPreferences(
    val userId: String,
    val enablePushNotifications: Boolean = true,
    val enableGradingNotifications: Boolean = true,
    val enableFeedbackNotifications: Boolean = true,
    val enableBatchInvitations: Boolean = true,
    val enableGeneralAnnouncements: Boolean = true,
    val enableStudyReminders: Boolean = true,
    val enableTestReminders: Boolean = true,
    val enableMarketplaceUpdates: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22, // 10 PM
    val quietHoursEnd: Int = 8,    // 8 AM
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if notification should be shown based on type and preferences
     */
    fun shouldShowNotification(type: NotificationType): Boolean {
        if (!enablePushNotifications) return false
        
        return when (type) {
            NotificationType.GRADING_COMPLETE -> enableGradingNotifications
            NotificationType.FEEDBACK_AVAILABLE -> enableFeedbackNotifications
            NotificationType.BATCH_INVITATION -> enableBatchInvitations
            NotificationType.GENERAL_ANNOUNCEMENT -> enableGeneralAnnouncements
            NotificationType.STUDY_REMINDER -> enableStudyReminders
            NotificationType.TEST_REMINDER -> enableTestReminders
            NotificationType.MARKETPLACE_UPDATE -> enableMarketplaceUpdates
        }
    }
    
    /**
     * Check if currently in quiet hours
     */
    fun isInQuietHours(): Boolean {
        if (!quietHoursEnabled) return false
        
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        return if (quietHoursStart < quietHoursEnd) {
            currentHour in quietHoursStart until quietHoursEnd
        } else {
            currentHour >= quietHoursStart || currentHour < quietHoursEnd
        }
    }
}

/**
 * FCM token information
 */
data class FCMToken(
    val userId: String,
    val token: String,
    val deviceId: String,
    val platform: String = "android",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Notification action data for grading complete
 */
data class GradingCompleteData(
    val submissionId: String,
    val testType: String,
    val score: Float?,
    val assessorName: String
)

/**
 * Notification action data for feedback available
 */
data class FeedbackAvailableData(
    val submissionId: String,
    val testType: String,
    val assessorName: String,
    val feedbackCount: Int
)

/**
 * Notification action data for batch invitation
 */
data class BatchInvitationData(
    val batchId: String,
    val batchName: String,
    val assessorName: String,
    val startDate: Long
)

