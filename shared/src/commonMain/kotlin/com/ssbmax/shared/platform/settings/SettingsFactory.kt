package com.ssbmax.shared.platform.settings

import com.russhwolf.settings.Settings

/**
 * Provides a platform-backed [Settings] instance (multiplatform-settings).
 *
 * Android: wraps `SharedPreferences` (the same storage `ThemePreferenceManager`
 * used directly before this shim existed).
 * iOS: wraps `NSUserDefaults`.
 *
 * Construction differs per platform (Android needs a `Context`, iOS doesn't),
 * mirroring the `DatabaseDriverFactory` expect/actual pattern already
 * established in this module.
 */
expect class SettingsFactory {
    fun create(): Settings
}
