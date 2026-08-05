package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.SubscriptionOverride
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.platform.settings.DeveloperSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the Phase 3 (KMP-convergence plan) no-op guarantee: every submit path calls
 * [com.ssbmax.shared.domain.repository.TestUsageRecorder.recordTestUsage] unconditionally, so
 * without this no-op a dev session under an override would permanently inflate real monthly
 * counters with no in-app reset.
 */
class DebugOverrideTestUsageRecorderTest {

    private val delegate = FakeTestUsageRecorder()
    private val developerSettings = DeveloperSettings(FakeInMemorySettings())
    private val recorder = DebugOverrideTestUsageRecorder(delegate, developerSettings)

    @Test
    fun `FOLLOW_REAL delegates recordTestUsage`() = runTest {
        recorder.recordTestUsage(TestType.OIR, "user-1", "submission-1")

        assertEquals(1, delegate.calls.size)
        assertEquals(TestType.OIR, delegate.calls.single().testType)
        assertEquals("submission-1", delegate.calls.single().submissionId)
    }

    @Test
    fun `FORCE_FREE no-ops instead of delegating`() = runTest {
        developerSettings.setOverride(SubscriptionOverride.FORCE_FREE)

        recorder.recordTestUsage(TestType.OIR, "user-1", "submission-1")

        assertTrue(delegate.calls.isEmpty())
    }

    @Test
    fun `FORCE_PREMIUM no-ops instead of delegating`() = runTest {
        developerSettings.setOverride(SubscriptionOverride.FORCE_PREMIUM)

        recorder.recordTestUsage(TestType.IO, "user-1")

        assertTrue(delegate.calls.isEmpty())
    }

    @Test
    fun `toggling the setting changes behavior without reconstruction`() = runTest {
        recorder.recordTestUsage(TestType.OIR, "user-1")
        assertEquals(1, delegate.calls.size)

        developerSettings.setOverride(SubscriptionOverride.FORCE_FREE)
        recorder.recordTestUsage(TestType.TAT, "user-1")
        assertEquals(1, delegate.calls.size)

        developerSettings.setOverride(SubscriptionOverride.FOLLOW_REAL)
        recorder.recordTestUsage(TestType.WAT, "user-1")
        assertEquals(2, delegate.calls.size)
    }
}
