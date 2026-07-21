package com.ssbmax.shared.ui.auth

import androidx.compose.runtime.staticCompositionLocalOf
import com.ssbmax.shared.platform.auth.GoogleSignInLauncher

/**
 * CompositionLocal for the shared [GoogleSignInLauncher].
 *
 * Not Koin-injected (see [GoogleSignInLauncher]'s class doc for why) — the
 * Android actual wraps an `ActivityResultLauncher` that must be registered
 * by `MainActivity` before it reaches `STARTED`. `MainActivity` constructs
 * it and provides it here, at the Compose root, same pattern as
 * `LocalNotificationPermissionController` (Phase 4) and `LocalThemeState`.
 * The iOS entry point (`MainViewController`) provides the (stub) iOS actual
 * directly, since it has no equivalent lifecycle-registration constraint.
 */
val LocalGoogleSignInLauncher = staticCompositionLocalOf<GoogleSignInLauncher> {
    error("No GoogleSignInLauncher provided")
}
