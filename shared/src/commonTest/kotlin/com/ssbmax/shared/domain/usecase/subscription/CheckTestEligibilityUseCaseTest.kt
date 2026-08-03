package com.ssbmax.shared.domain.usecase.subscription

import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.TestEligibility
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.UsageInfo
import com.ssbmax.shared.domain.util.SecurityEvents
import com.ssbmax.shared.presentation.testing.FakeSubscriptionRepository
import com.ssbmax.shared.presentation.testing.RecordingAnalyticsTracker
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 7a (KMP-convergence plan): pins the restored `sec_limit_reached`
 * security-event telemetry — the one `core:data` `SecurityEventLogger` call
 * this use case's own doc comment flagged as dropped during the KMP port.
 */
class CheckTestEligibilityUseCaseTest {

    private val subscriptionRepository = FakeSubscriptionRepository()
    private val analyticsTracker = RecordingAnalyticsTracker()
    private val useCase = CheckTestEligibilityUseCase(subscriptionRepository, analyticsTracker)

    @Test
    fun `eligible user fires no security event`() = runTest {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PRO)
        subscriptionRepository.monthlyUsageResult = Result.success(
            mapOf("TAT Tests" to UsageInfo(used = 0, limit = 3))
        )

        val result = useCase(TestType.TAT, "user-1")

        assertTrue(result is TestEligibility.Eligible)
        assertTrue(analyticsTracker.events.isEmpty())
    }

    @Test
    fun `limit reached fires sec_limit_reached with test type and tier`() = runTest {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.FREE)
        subscriptionRepository.monthlyUsageResult = Result.success(
            mapOf("OIR Tests" to UsageInfo(used = 1, limit = 1))
        )

        val result = useCase(TestType.OIR, "user-1")

        assertTrue(result is TestEligibility.LimitReached)
        assertEquals(1, analyticsTracker.events.size)
        val event = analyticsTracker.events.single()
        assertEquals(SecurityEvents.LIMIT_REACHED, event.name)
        assertEquals("OIR", event.params["test_type"])
        assertEquals("FREE", event.params["tier"])
    }

    @Test
    fun `network error fires no security event`() = runTest {
        subscriptionRepository.tierResult = Result.failure(Exception("offline"))

        val result = useCase(TestType.OIR, "user-1")

        assertTrue(result is TestEligibility.NetworkError)
        assertTrue(analyticsTracker.events.isEmpty())
    }

    @Test
    fun `bypass enabled returns eligible without touching the repository`() = runTest {
        val bypassingUseCase = CheckTestEligibilityUseCase(
            subscriptionRepository = subscriptionRepository,
            analyticsTracker = analyticsTracker,
            bypassSubscriptionLimits = true
        )
        // subscriptionRepository is left at its default (FREE tier, no usage) —
        // a real lookup would return Eligible(remainingTests = 1), not 999, so
        // asserting 999 below proves the bypass short-circuits before any
        // repository read, matching the Android original's `canTakeTest`
        // debug-bypass early return (`999 remaining tests`).

        val result = bypassingUseCase(TestType.OIR, "user-1")

        assertTrue(result is TestEligibility.Eligible)
        assertEquals(999, (result as TestEligibility.Eligible).remainingTests)
        assertTrue(analyticsTracker.events.isEmpty())
    }

    @Test
    fun `bypass disabled by default enforces real limits`() = runTest {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.FREE)
        subscriptionRepository.monthlyUsageResult = Result.success(
            mapOf("OIR Tests" to UsageInfo(used = 1, limit = 1))
        )

        val result = useCase(TestType.OIR, "user-1")

        assertTrue(result is TestEligibility.LimitReached)
    }
}
