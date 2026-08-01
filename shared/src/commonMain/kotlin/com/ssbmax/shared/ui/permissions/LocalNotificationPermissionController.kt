package com.ssbmax.shared.ui.permissions

import androidx.compose.runtime.staticCompositionLocalOf
import com.ssbmax.shared.platform.permissions.NotificationPermissionController

/**
 * CompositionLocal for the shared [NotificationPermissionController]. The one
 * instance both platforms provide at their Compose root — the pre-Phase-5
 * `app`-only twin (`com.ssbmax.ui.permissions.LocalNotificationPermissionController`)
 * was deleted in the cutover; `MainActivity`/`SSBMaxRoot` callers now provide
 * this instance directly.
 *
 * Not Koin-injected: the Android actual wraps an `ActivityResultLauncher`
 * that must be registered before `MainActivity` reaches `STARTED`.
 */
val LocalNotificationPermissionController = staticCompositionLocalOf<NotificationPermissionController> {
    error("No NotificationPermissionController provided")
}
