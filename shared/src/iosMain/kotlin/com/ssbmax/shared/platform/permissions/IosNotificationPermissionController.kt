package com.ssbmax.shared.platform.permissions

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS actual, backed by `UNUserNotificationCenter`. Unlike Android's
 * `POST_NOTIFICATIONS` (a single boolean runtime permission from API 33+),
 * iOS authorization is checked/requested asynchronously at any OS version —
 * no SDK-gating needed, but every call crosses into Foundation's
 * completion-handler API, wrapped here as `suspendCancellableCoroutine`.
 */
class IosNotificationPermissionController : NotificationPermissionController {

    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun isGranted(): Boolean = suspendCancellableCoroutine { cont ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            val granted = status == UNAuthorizationStatusAuthorized ||
                status == UNAuthorizationStatusProvisional
            if (cont.isActive) cont.resume(granted)
        }
    }

    override suspend fun request(): Boolean {
        if (isGranted()) return true
        return suspendCancellableCoroutine { cont ->
            val options = UNAuthorizationOptionAlert or
                UNAuthorizationOptionBadge or
                UNAuthorizationOptionSound
            center.requestAuthorizationWithOptions(options) { granted, _ ->
                if (cont.isActive) cont.resume(granted)
            }
        }
    }
}
