package com.ssbmax.ui.tests.ppdt

import com.ssbmax.core.domain.model.PPDTPhase
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.TestType
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for PPDT ViewModel bugs fixed in June 2026:
 *   Bug 1 — loadTest() was called in init{}, causing double-fetch with LaunchedEffect
 *   Bug 2 — 30s timer finally{} raced against 240s timer, setting isTimerActive=false prematurely
 *
 * Also covers subscription-limit gating (free-tier limit reached / eligible paths).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PPDTTestViewModelRegressionTest : PPDTTestViewModelTestBase() {

    // ==================== Bug 1: No loadTest() in init {} ====================

    @Test
    fun `on ViewModel construction, getPPDTQuestion is not called`() = runTest {
        // WHY: init{} calling loadTest() causes a double-fetch whenever LaunchedEffect(testId)
        // in PPDTTestScreen also calls loadTest() — two concurrent fetches race for the same slot.
        // SSOT rule: LaunchedEffect(testId) in the screen is the single call site.
        val viewModel = buildViewModel()
        advanceUntilIdle()
        coVerify(exactly = 0) { mockTestContentRepo.getPPDTQuestion(genderTag = any()) }
    }

    @Test
    fun `after one loadTest call, getPPDTQuestion is called exactly once`() = runTest {
        // WHY: With init{} bug, construction + one explicit loadTest() = two getPPDTQuestion
        // calls. That wastes cache reads and can serve a different image than the one shown.
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        coVerify(exactly = 1) { mockTestContentRepo.getPPDTQuestion(genderTag = any()) }
    }

    // ==================== Bug 2: Timer generation token ====================

    @Test
    fun `after IMAGE_VIEWING timer expires, writing phase timer is still active`() = runTest {
        // WHY: The dying 30s coroutine's finally{} block unconditionally sets isTimerActive=false,
        // which can fire AFTER startTimer(240) has already set it true — killing the writing timer
        // before it starts. The generation token (timerStartTime) makes finally{} a no-op when a
        // newer timer has taken ownership.
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()
        viewModel.startTest()

        // Viewing timer fires at t=30000; break exits loop immediately;
        // finally{} fires within advanceTimeBy — generation token makes it a no-op.
        // No advanceUntilIdle() here: it would drain the 240s writing timer to completion,
        // causing its own finally{} to set isTimerActive=false and defeat the assertion below.
        advanceTimeBy(30_001)

        val state = viewModel.uiState.value
        assertEquals("Should auto-advance to WRITING phase", PPDTPhase.WRITING, state.currentPhase)
        assertTrue("Writing timer must still be active — finally{} must not kill it", state.isTimerActive)
        assertEquals("Writing timer must start at 240s", 240, state.timeRemainingSeconds)
    }

    // ==================== Subscription Limit Tests ====================

    @Test
    fun `loadTest shows limit reached when FREE tier exhausted`() = runTest {
        coEvery { mockSubscriptionManager.canTakeTest(any(), any()) } returns
            com.ssbmax.core.data.repository.TestEligibility.LimitReached(
                tier = com.ssbmax.core.domain.model.SubscriptionTier.FREE,
                limit = 1,
                usedCount = 1,
                resetsAt = "01 Dec 2025"
            )

        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Should show limit reached", state.isLimitReached)
        assertEquals("Should show FREE tier", SubscriptionTier.FREE, state.subscriptionTier)
        assertEquals("Should show 1 test limit", 1, state.testsLimit)
        assertEquals("Should show 1 test used", 1, state.testsUsed)
        assertEquals("Should show reset date", "01 Dec 2025", state.resetsAt)
        assertFalse("Should not be loading", state.isLoading)
        assertEquals("Should have empty image URL when limited", "", state.imageUrl)
    }

    @Test
    fun `loadTest proceeds when user is eligible`() = runTest {
        coEvery { mockSubscriptionManager.canTakeTest(any(), any()) } returns
            com.ssbmax.core.data.repository.TestEligibility.Eligible(remainingTests = 5)

        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Should NOT show limit reached", state.isLimitReached)
        assertTrue("Should have loaded image URL", state.imageUrl.isNotEmpty())
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have error", state.error)
    }

    @Test
    fun `loadTest calls canTakeTest with correct test type`() = runTest {
        val viewModel = buildViewModel()
        viewModel.loadTest("ppdt_standard")
        advanceUntilIdle()

        coVerify(atLeast = 1) { mockSubscriptionManager.canTakeTest(TestType.PPDT, any()) }
    }
}
