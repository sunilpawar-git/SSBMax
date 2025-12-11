package com.ssbmax.core.data.analytics

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerTest {

    private val firebaseAnalytics: FirebaseAnalytics = mockk(relaxed = true)
    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true)

    @Test
    fun trackTestStarted_populatesExpectedParams() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        val bundleSlot = slot<Bundle>()
        every { firebaseAnalytics.logEvent(eq("test_started"), capture(bundleSlot)) } answers { }

        val manager = AnalyticsManager(firebaseAnalytics, firebaseAuth)

        manager.trackTestStarted(testType = "WAT", testId = "wat-1")

        val bundle = bundleSlot.captured
        assertEquals("WAT", bundle.getString("test_type"))
        assertEquals("wat-1", bundle.getString("test_id"))
        assertEquals("FREE", bundle.getString("current_tier"))

        unmockkAll()
    }

    @Test
    fun trackUpgradeCompleted_populatesValueAndCurrency() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        val captured = mutableListOf<Bundle>()
        every { firebaseAnalytics.logEvent(eq("upgrade_completed"), any<Bundle>()) } answers {
            val b = secondArg<Bundle>()
            captured += b
        }

        val manager = AnalyticsManager(firebaseAnalytics, firebaseAuth)

        manager.trackUpgradeCompleted(fromTier = "FREE", toTier = "PRO", priceInRupees = 499)

        val bundle = captured.single()
        assertEquals("FREE", bundle.getString("from_tier"))
        assertEquals("PRO", bundle.getString("to_tier"))
        assertEquals(499.0, bundle.getDouble(FirebaseAnalytics.Param.VALUE), 0.0)
        assertEquals("INR", bundle.getString(FirebaseAnalytics.Param.CURRENCY))

        unmockkAll()
    }

    @Test
    fun trackUpgradeInitiated_populatesFromAndToAndSource() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        val captured = mutableListOf<Bundle>()
        every { firebaseAnalytics.logEvent(eq("upgrade_initiated"), any<Bundle>()) } answers {
            captured += secondArg<Bundle>()
        }

        val manager = AnalyticsManager(firebaseAnalytics, firebaseAuth)

        manager.trackUpgradeInitiated(fromTier = "FREE", toTier = "PRO", source = "banner")

        val bundle = captured.single()
        assertEquals("FREE", bundle.getString("from_tier"))
        assertEquals("PRO", bundle.getString("to_tier"))
        assertEquals("banner", bundle.getString("source"))

        unmockkAll()
    }

    @Test
    fun trackTestCompleted_populatesMetrics() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        val captured = mutableListOf<Bundle>()
        every { firebaseAnalytics.logEvent(eq("test_completed"), any<Bundle>()) } answers {
            captured += secondArg<Bundle>()
        }

        val manager = AnalyticsManager(firebaseAnalytics, firebaseAuth)

        manager.trackTestCompleted(
            testType = "WAT",
            testId = "wat-1",
            score = 87.5f,
            timeSpentSeconds = 120L,
            questionsAnswered = 12
        )

        val bundle = captured.single()
        assertEquals("WAT", bundle.getString("test_type"))
        assertEquals("wat-1", bundle.getString("test_id"))
        assertEquals(87.5, bundle.getDouble("score"), 0.0)
        assertEquals(120L, bundle.getLong("time_spent_seconds"))
        assertEquals(12L, bundle.getLong("questions_answered"))
        assertEquals("FREE", bundle.getString("current_tier"))

        unmockkAll()
    }

    @Test
    fun trackTestLimitReached_populatesUsage() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        val captured = mutableListOf<Bundle>()
        every { firebaseAnalytics.logEvent(eq("test_limit_reached"), any<Bundle>()) } answers {
            captured += secondArg<Bundle>()
        }

        val manager = AnalyticsManager(firebaseAnalytics, firebaseAuth)

        manager.trackTestLimitReached(testType = "WAT", currentTier = "FREE", currentUsage = 1, limit = 3)

        val bundle = captured.single()
        assertEquals("WAT", bundle.getString("test_type"))
        assertEquals("FREE", bundle.getString("current_tier"))
        assertEquals(1L, bundle.getLong("current_usage"))
        assertEquals(3L, bundle.getLong("limit"))

        unmockkAll()
    }

    @Test
    fun trackSubscriptionView_populatesSourceAndTier() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        val captured = mutableListOf<Bundle>()
        every { firebaseAnalytics.logEvent(eq("subscription_view"), any<Bundle>()) } answers {
            captured += secondArg<Bundle>()
        }

        val manager = AnalyticsManager(firebaseAnalytics, firebaseAuth)

        manager.trackSubscriptionView(source = "settings")

        val bundle = captured.single()
        assertEquals("settings", bundle.getString("source"))
        assertEquals("FREE", bundle.getString("current_tier"))

        unmockkAll()
    }

    @Test
    fun trackFeatureUsed_populatesNameAndTier() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        val captured = mutableListOf<Bundle>()
        every { firebaseAnalytics.logEvent(eq("feature_used"), any<Bundle>()) } answers {
            captured += secondArg<Bundle>()
        }

        val manager = AnalyticsManager(firebaseAnalytics, firebaseAuth)

        manager.trackFeatureUsed(featureName = "tts", parameters = mapOf("quality" to "hd"))

        val bundle = captured.single()
        assertEquals("tts", bundle.getString("feature_name"))
        assertEquals("FREE", bundle.getString("current_tier"))
        assertEquals("hd", bundle.getString("quality"))

        unmockkAll()
    }
}
