package com.ssbmax.ui.permissions

import androidx.compose.runtime.staticCompositionLocalOf
import com.ssbmax.shared.platform.permissions.NotificationPermissionController

/**
 * CompositionLocal for the shared [NotificationPermissionController].
 *
 * Not Koin-injected (unlike most `shared` platform singletons) because the
 * Android actual wraps an `ActivityResultLauncher` that must be registered
 * by `MainActivity` before `onCreate` returns (an Android platform
 * constraint — see `AndroidNotificationPermissionController`'s doc).
 * `MainActivity` constructs it and provides it here, at the Compose root,
 * same pattern as `LocalThemeState`.
 */
val LocalNotificationPermissionController = staticCompositionLocalOf<NotificationPermissionController> {
    error("No NotificationPermissionController provided")
}
