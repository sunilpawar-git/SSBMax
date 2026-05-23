package com.ssbmax.notifications

import android.provider.Settings
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssbmax.core.domain.model.FCMToken
import com.ssbmax.core.domain.model.NotificationPriority
import com.ssbmax.core.domain.model.NotificationType
import com.ssbmax.core.domain.model.SSBMaxNotification
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Firebase Cloud Messaging Service for SSBMax.
 * Automatically processes push notifications, persists history, and updates device tokens under Hilt.
 */
@AndroidEntryPoint
class SSBMaxFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var observeCurrentUser: ObserveCurrentUserUseCase

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val deviceId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = observeCurrentUser().first()
                if (currentUser != null) {
                    val fcmToken = FCMToken(
                        userId = currentUser.id,
                        token = token,
                        deviceId = deviceId,
                        platform = "android"
                    )
                    notificationRepository.saveFCMToken(fcmToken)
                }
            } catch (e: Exception) {
                android.util.Log.e("SSBMaxFCM", "Failed to save refreshed token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        var title = "SSBMax Notification"
        var body = ""
        var actionUrl: String? = null
        var notificationId: String? = null
        var type = NotificationType.GENERAL_ANNOUNCEMENT

        if (message.data.isNotEmpty()) {
            title = message.data["title"] ?: title
            body = message.data["message"] ?: body
            actionUrl = message.data["actionUrl"]
            notificationId = message.data["notificationId"]
            message.data["type"]?.let {
                try { type = NotificationType.valueOf(it) } catch (e: Exception) { }
            }
        }

        message.notification?.let {
            title = it.title ?: title
            body = it.body ?: body
        }

        saveAndShowNotification(
            type = type,
            title = title,
            message = body,
            actionUrl = actionUrl,
            notificationId = notificationId
        )
    }

    private fun saveAndShowNotification(
        type: NotificationType,
        title: String,
        message: String,
        actionUrl: String?,
        notificationId: String?
    ) {
        val notifId = notificationId ?: UUID.randomUUID().toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = observeCurrentUser().first()
                if (currentUser != null) {
                    val notification = SSBMaxNotification(
                        id = notifId,
                        userId = currentUser.id,
                        type = type,
                        priority = NotificationPriority.NORMAL,
                        title = title,
                        message = message,
                        actionUrl = actionUrl
                    )
                    notificationRepository.saveNotification(notification)
                }
            } catch (e: Exception) {
                android.util.Log.e("SSBMaxFCM", "Failed to persist notification history", e)
            }
        }

        FCMNotificationHelper.showNotification(
            context = this,
            type = type,
            title = title,
            message = message,
            actionUrl = actionUrl,
            notificationId = notifId
        )
    }
}
