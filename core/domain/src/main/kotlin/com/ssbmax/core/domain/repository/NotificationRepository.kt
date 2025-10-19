package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.FCMToken
import com.ssbmax.core.domain.model.NotificationPreferences
import com.ssbmax.core.domain.model.SSBMaxNotification
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing notifications and FCM tokens
 */
interface NotificationRepository {
    
    /**
     * Save or update FCM token for the current device
     */
    suspend fun saveFCMToken(token: FCMToken): Result<Unit>
    
    /**
     * Get FCM token for current device
     */
    suspend fun getFCMToken(userId: String, deviceId: String): Result<FCMToken?>
    
    /**
     * Delete FCM token (on logout)
     */
    suspend fun deleteFCMToken(userId: String, deviceId: String): Result<Unit>
    
    /**
     * Save notification to Firestore and local database
     */
    suspend fun saveNotification(notification: SSBMaxNotification): Result<Unit>
    
    /**
     * Get all notifications for a user
     */
    fun getNotifications(userId: String): Flow<List<SSBMaxNotification>>
    
    /**
     * Get unread notification count
     */
    fun getUnreadCount(userId: String): Flow<Int>
    
    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit>
    
    /**
     * Mark all notifications as read
     */
    suspend fun markAllAsRead(userId: String): Result<Unit>
    
    /**
     * Delete notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    
    /**
     * Get notification preferences
     */
    suspend fun getPreferences(userId: String): Result<NotificationPreferences>
    
    /**
     * Save notification preferences
     */
    suspend fun savePreferences(preferences: NotificationPreferences): Result<Unit>
}

