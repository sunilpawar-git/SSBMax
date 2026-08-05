package com.ssbmax.shared.platform.settings

import com.ssbmax.shared.domain.model.SubscriptionOverride
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.presentation.testing.FakeSettings
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
