package com.ssbmax.shared.ui.permissions

import androidx.compose.runtime.staticCompositionLocalOf
import com.ssbmax.shared.platform.permissions.NotificationPermissionController

/**
 * CompositionLocal for the shared [NotificationPermissionController], scoped
 * to `commonMain/ui` screens (`StudentHomeScreen`).
 *
 * This is a `com.ssbmax.shared.ui.permissions` twin of the pre-existing
 * `com.ssbmax.ui.permissions.LocalNotificationPermissionController` in
 * `app` (Phase 4) — that one only reaches Android-only Compose call sites
 * (still on the old `app/.../navigation` graph); ported `shared` screens
 * need their own instance of the same CompositionLocal since they live in a
 * different Gradle module/package. `MainActivity` provides both instances
 * with the same underlying `AndroidNotificationPermissionController`, so
 * behavior is identical regardless of which graph a screen is reached from.
 *
 * Not Koin-injected, for the same reason as the `app`-side original: the
 * Android actual wraps an `ActivityResultLauncher` that must be registered
 * before `MainActivity` reaches `STARTED`.
 */
val LocalNotificationPermissionController = staticCompositionLocalOf<NotificationPermissionController> {
    error("No NotificationPermissionController provided")
}
