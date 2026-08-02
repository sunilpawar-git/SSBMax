package com.ssbmax.shared.platform.notifications

import com.ssbmax.shared.domain.model.FCMToken
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.repository.NotificationRepository
import com.ssbmax.shared.platform.ensureKoinStarted
import com.ssbmax.shared.platform.sharedKoin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.UIKit.UIDevice

/**
 * Swift-callable bridge for the APNs device-token hand-off (Phase 6, KMP
 * migration plan item 4: "push notifications via APNs"). The actual
 * `UIApplication.registerForRemoteNotifications()` call and the
 * `UNUserNotificationCenterDelegate` live in `iosApp/iosApp/AppDelegate.swift`
 * -- not portable to `commonMain`/`iosMain` since they're pure
 * UIKit/Swift-async APIs with no Kotlin/Native cinterop coverage worth the
 * ceremony for a single call site. This file only bridges the *result*
 * (the raw APNs device token) into the same repository Android's FCM path
 * uses.
 *
 * Android's `SSBMaxFirebaseMessagingService.onNewToken` (in `app`) already
 * calls `NotificationRepository.saveFCMToken` for real (fixed pre-Phase-7c,
 * not a stub) -- this bridge writes into the same repository/collection,
 * not a first-of-its-kind path.
 *
 * **Read before assuming an iOS token round-trips through FCM send:** the
 * token saved here is the raw APNs device token, not an FCM registration
 * token. GitLive's Firebase Kotlin SDK has no Messaging module (confirmed
 * absent -- see the plan's Risk #1 on thin GitLive coverage for
 * Messaging/Analytics/Crashlytics), so there is no KMP-portable way to run
 * this token through Firebase Cloud Messaging's token-exchange short of
 * linking `FirebaseMessaging` as a new iOS SPM product (an Xcode-project
 * change, not reachable from Gradle/Kotlin -- deliberately not attempted in
 * Phase 7c, same call as the Crashlytics/Analytics SPM gap Phase 7a named).
 * A server that wants to push to `platform == "ios"` rows in this
 * collection must call Apple's APNs HTTP/2 API directly with the raw
 * token, not FCM's send API -- flagging this for whoever wires server push
 * (nothing in `functions/` sends pushes yet either way), not silently
 * implying FCM send would work for these rows.
 */
private val bridgeScope = CoroutineScope(Dispatchers.Main)

/**
 * Called from `AppDelegate.application(_:didRegisterForRemoteNotificationsWithDeviceToken:)`
 * once the hex-encoded APNs token is available. No-ops (does not throw) if
 * no user is signed in yet -- iOS re-delivers the device-token callback on
 * every launch, so the token is captured again once the user signs in.
 */
@OptIn(ExperimentalForeignApi::class)
fun onApnsDeviceTokenReceived(tokenHex: String) {
    ensureKoinStarted()
    val koin = sharedKoin()
    val userId = koin.get<AuthRepository>().currentUser.value?.id ?: return
    val notificationRepository = koin.get<NotificationRepository>()
    val deviceId = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: tokenHex.take(DEVICE_ID_FALLBACK_LENGTH)

    bridgeScope.launch {
        notificationRepository.saveFCMToken(
            FCMToken(
                userId = userId,
                token = tokenHex,
                deviceId = deviceId,
                platform = PLATFORM_IOS
            )
        )
    }
}

private const val PLATFORM_IOS = "ios"

// Only used if identifierForVendor is unavailable (rare -- e.g. right after
// a fresh install before the OS assigns one); keeps deviceId non-blank
// rather than failing the save.
private const val DEVICE_ID_FALLBACK_LENGTH = 16
