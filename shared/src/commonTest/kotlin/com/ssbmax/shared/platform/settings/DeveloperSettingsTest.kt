package com.ssbmax.shared.platform.settings

import com.russhwolf.settings.Settings
import com.ssbmax.shared.domain.model.SubscriptionOverride
import com.ssbmax.shared.domain.model.SubscriptionTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class DeveloperSettingsTest {

    @Test
    fun `defaults are FOLLOW_REAL and bypass false`() {
        val settings = DeveloperSettings(FakeSettings())

        assertEquals(SubscriptionOverride.FOLLOW_REAL, settings.getOverride())
        assertFalse(settings.getBypassInterviewPrerequisites())
        assertEquals(SubscriptionOverride.FOLLOW_REAL, settings.overrideFlow.value)
        assertFalse(settings.bypassInterviewPrerequisitesFlow.value)
    }

    @Test
    fun `setOverride round-trips through get and the flow`() {
        val settings = DeveloperSettings(FakeSettings())

        settings.setOverride(SubscriptionOverride.FORCE_PRO)

        assertEquals(SubscriptionOverride.FORCE_PRO, settings.getOverride())
        assertEquals(SubscriptionOverride.FORCE_PRO, settings.overrideFlow.value)
    }

    @Test
    fun `setBypassInterviewPrerequisites round-trips through get and the flow`() {
        val settings = DeveloperSettings(FakeSettings())

        settings.setBypassInterviewPrerequisites(true)

        assertEquals(true, settings.getBypassInterviewPrerequisites())
        assertEquals(true, settings.bypassInterviewPrerequisitesFlow.value)
    }

    @Test
    fun `malformed stored override falls back to FOLLOW_REAL instead of crashing`() {
        val fakeSettings = FakeSettings()
        fakeSettings.putString("developer_subscription_override", "NOT_A_REAL_VALUE")
        val settings = DeveloperSettings(fakeSettings)

        assertEquals(SubscriptionOverride.FOLLOW_REAL, settings.getOverride())
    }

    @Test
    fun `currentOverrideTierOrNull maps every override to its tier and FOLLOW_REAL to null`() {
        val fakeSettings = FakeSettings()
        val settings = DeveloperSettings(fakeSettings)

        assertNull(settings.currentOverrideTierOrNull())

        settings.setOverride(SubscriptionOverride.FORCE_FREE)
        assertEquals(SubscriptionTier.FREE, settings.currentOverrideTierOrNull())

        settings.setOverride(SubscriptionOverride.FORCE_PRO)
        assertEquals(SubscriptionTier.PRO, settings.currentOverrideTierOrNull())

        settings.setOverride(SubscriptionOverride.FORCE_PREMIUM)
        assertEquals(SubscriptionTier.PREMIUM, settings.currentOverrideTierOrNull())

        settings.setOverride(SubscriptionOverride.FOLLOW_REAL)
        assertNull(settings.currentOverrideTierOrNull())
    }
}

/**
 * Hand-rolled in-memory [Settings]: this dependency's pinned version (see
 * gradle/libs.versions.toml) doesn't ship a `MapSettings`/test artifact, and
 * the rest of this test suite already hand-rolls fakes rather than pull in
 * a mocking library with no Kotlin/Native target (see RepositoryFakes.kt).
 */
private class FakeSettings : Settings {
    private val values = mutableMapOf<String, Any>()

    override val keys: Set<String> get() = values.keys
    override val size: Int get() = values.size

    override fun clear() = values.clear()
    override fun remove(key: String) { values.remove(key) }
    override fun hasKey(key: String): Boolean = values.containsKey(key)

    override fun putInt(key: String, value: Int) { values[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = values[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String): Int? = values[key] as? Int

    override fun putLong(key: String, value: Long) { values[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = values[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String): Long? = values[key] as? Long

    override fun putString(key: String, value: String) { values[key] = value }
    override fun getString(key: String, defaultValue: String): String = values[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String): String? = values[key] as? String

    override fun putFloat(key: String, value: Float) { values[key] = value }
    override fun getFloat(key: String, defaultValue: Float): Float = values[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = values[key] as? Float

    override fun putDouble(key: String, value: Double) { values[key] = value }
    override fun getDouble(key: String, defaultValue: Double): Double = values[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = values[key] as? Double

    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean
}
