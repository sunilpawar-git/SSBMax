package com.ssbmax.core.data.repository

import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.ssbmax.core.data.local.dao.NotificationDao
import com.ssbmax.core.data.local.entity.toDomain
import com.ssbmax.core.data.local.entity.toEntity
import com.ssbmax.core.domain.model.FCMToken
import com.ssbmax.core.domain.model.NotificationPreferences
import com.ssbmax.core.domain.model.SSBMaxNotification
import com.ssbmax.core.domain.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NotificationRepository
 * Manages FCM tokens and notifications using Firestore and Room
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val firebaseMessaging: FirebaseMessaging,
    private val notificationDao: NotificationDao
) : NotificationRepository {
    
    private val tokensCollection = firestore.collection("fcm_tokens")
    private val notificationsCollection = firestore.collection("notifications")
    private val preferencesCollection = firestore.collection("notification_preferences")
    
    /**
     * Get unique device ID
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }
    
    override suspend fun saveFCMToken(token: FCMToken): Result<Unit> {
        return try {
            // Save to Firestore
            tokensCollection
                .document("${token.userId}_${token.deviceId}")
                .set(
                    mapOf(
                        "userId" to token.userId,
                        "token" to token.token,
                        "deviceId" to token.deviceId,
                        "platform" to token.platform,
                        "createdAt" to token.createdAt,
                        "updatedAt" to token.updatedAt
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save FCM token: ${e.message}", e))
        }
    }
    
    override suspend fun getFCMToken(userId: String, deviceId: String): Result<FCMToken?> {
        return try {
            val doc = tokensCollection
                .document("${userId}_$deviceId")
                .get()
                .await()
            
            if (!doc.exists()) {
                return Result.success(null)
            }
            
            val token = FCMToken(
                userId = doc.getString("userId") ?: userId,
                token = doc.getString("token") ?: "",
                deviceId = doc.getString("deviceId") ?: deviceId,
                platform = doc.getString("platform") ?: "android",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            )
            
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get FCM token: ${e.message}", e))
        }
    }
    
    override suspend fun deleteFCMToken(userId: String, deviceId: String): Result<Unit> {
        return try {
            tokensCollection
                .document("${userId}_$deviceId")
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete FCM token: ${e.message}", e))
        }
    }
    
    override suspend fun saveNotification(notification: SSBMaxNotification): Result<Unit> {
        return try {
            // Save to Firestore
            notificationsCollection
                .document(notification.id)
                .set(
                    mapOf(
                        "id" to notification.id,
                        "userId" to notification.userId,
                        "type" to notification.type.name,
                        "priority" to notification.priority.name,
                        "title" to notification.title,
                        "message" to notification.message,
                        "imageUrl" to notification.imageUrl,
                        "actionUrl" to notification.actionUrl,
                        "actionData" to notification.actionData,
                        "isRead" to notification.isRead,
                        "createdAt" to notification.createdAt,
                        "expiresAt" to notification.expiresAt
                    )
                )
                .await()
            
            // Save to local database
            notificationDao.insert(notification.toEntity())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save notification: ${e.message}", e))
        }
    }
    
    override fun getNotifications(userId: String): Flow<List<SSBMaxNotification>> {
        return notificationDao.getNotifications(userId)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override fun getUnreadCount(userId: String): Flow<Int> {
        return notificationDao.getUnreadCount(userId)
    }
    
    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            // Update in Firestore
            notificationsCollection
                .document(notificationId)
                .update("isRead", true)
                .await()
            
            // Update in local database
            notificationDao.markAsRead(notificationId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to mark notification as read: ${e.message}", e))
        }
    }
    
    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            // Update in Firestore
            val batch = firestore.batch()
            notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
                .documents
                .forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
            batch.commit().await()
            
            // Update in local database
            notificationDao.markAllAsRead(userId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to mark all notifications as read: ${e.message}", e))
        }
    }
    
    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            // Delete from Firestore
            notificationsCollection
                .document(notificationId)
                .delete()
                .await()
            
            // Delete from local database
            notificationDao.delete(notificationId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete notification: ${e.message}", e))
        }
    }
    
    override suspend fun getPreferences(userId: String): Result<NotificationPreferences> {
        return try {
            val doc = preferencesCollection
                .document(userId)
                .get()
                .await()
            
            if (!doc.exists()) {
                // Return default preferences
                return Result.success(NotificationPreferences(userId = userId))
            }
            
            val prefs = NotificationPreferences(
                userId = userId,
                enablePushNotifications = doc.getBoolean("enablePushNotifications") ?: true,
                enableGradingNotifications = doc.getBoolean("enableGradingNotifications") ?: true,
                enableFeedbackNotifications = doc.getBoolean("enableFeedbackNotifications") ?: true,
                enableBatchInvitations = doc.getBoolean("enableBatchInvitations") ?: true,
                enableGeneralAnnouncements = doc.getBoolean("enableGeneralAnnouncements") ?: true,
                enableStudyReminders = doc.getBoolean("enableStudyReminders") ?: true,
                enableTestReminders = doc.getBoolean("enableTestReminders") ?: true,
                enableMarketplaceUpdates = doc.getBoolean("enableMarketplaceUpdates") ?: true,
                quietHoursEnabled = doc.getBoolean("quietHoursEnabled") ?: false,
                quietHoursStart = (doc.getLong("quietHoursStart") ?: 22).toInt(),
                quietHoursEnd = (doc.getLong("quietHoursEnd") ?: 8).toInt(),
                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            )
            
            Result.success(prefs)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get notification preferences: ${e.message}", e))
        }
    }
    
    override suspend fun savePreferences(preferences: NotificationPreferences): Result<Unit> {
        return try {
            preferencesCollection
                .document(preferences.userId)
                .set(
                    mapOf(
                        "userId" to preferences.userId,
                        "enablePushNotifications" to preferences.enablePushNotifications,
                        "enableGradingNotifications" to preferences.enableGradingNotifications,
                        "enableFeedbackNotifications" to preferences.enableFeedbackNotifications,
                        "enableBatchInvitations" to preferences.enableBatchInvitations,
                        "enableGeneralAnnouncements" to preferences.enableGeneralAnnouncements,
                        "enableStudyReminders" to preferences.enableStudyReminders,
                        "enableTestReminders" to preferences.enableTestReminders,
                        "enableMarketplaceUpdates" to preferences.enableMarketplaceUpdates,
                        "quietHoursEnabled" to preferences.quietHoursEnabled,
                        "quietHoursStart" to preferences.quietHoursStart,
                        "quietHoursEnd" to preferences.quietHoursEnd,
                        "updatedAt" to preferences.updatedAt
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save notification preferences: ${e.message}", e))
        }
    }
    
    /**
     * Get current FCM token from Firebase SDK
     */
    suspend fun getCurrentFCMToken(): Result<String> {
        return try {
            val token = firebaseMessaging.token.await()
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get FCM token: ${e.message}", e))
        }
    }
    
    /**
     * Subscribe to topic
     */
    suspend fun subscribeToTopic(topic: String): Result<Unit> {
        return try {
            firebaseMessaging.subscribeToTopic(topic).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to subscribe to topic: ${e.message}", e))
        }
    }
    
    /**
     * Unsubscribe from topic
     */
    suspend fun unsubscribeFromTopic(topic: String): Result<Unit> {
        return try {
            firebaseMessaging.unsubscribeFromTopic(topic).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to unsubscribe from topic: ${e.message}", e))
        }
    }
}

