package com.ssbmax.shared.platform.settings

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual class SettingsFactory {
    actual fun create(): Settings {
        return NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
}
