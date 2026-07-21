package com.ssbmax.shared.platform.settings

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings

actual class SettingsFactory(private val context: Context) {
    actual fun create(): Settings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return SharedPreferencesSettings(prefs)
    }

    private companion object {
        // Same SharedPreferences file name the pre-shim ThemePreferenceManager used —
        // keeps existing installs' saved theme choice intact across this migration.
        const val PREFS_NAME = "app_theme"
    }
}
