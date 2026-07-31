@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.premium

import com.ssbmax.shared.domain.model.BillingCycle
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeSubscriptionRepository
import com.ssbmax.shared.presentation.testing.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterization test for [UpgradeViewModel], written retroactively (13-VM
 * gap-closing pass, see the KMP-convergence plan's Phase 1). Pins the
 * current-tier load + static plan list + "visual only" upgrade-dialog flow
 * (no payment gateway wired, see the ViewModel's own doc comment).
 */
class UpgradeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var subscriptionRepository: FakeSubscriptionRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository(initialUser = testUser())
        subscriptionRepository = FakeSubscriptionRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = UpgradeViewModel(
        observeCurrentUser = ObserveCurrentUserUseCase(authRepository),
        getSubscriptionTier = GetSubscriptionTierUseCase(subscriptionRepository),
        logger = NoOpLogger()
    )

    @Test
    fun `loads current subscription tier and available plans on init`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PRO)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(SubscriptionTier.PRO, state.currentTier)
        assertEquals(4, state.availablePlans.size)
    }

    @Test
    fun `defaults to FREE tier when no user is logged in`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SubscriptionTier.FREE, state.currentTier)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `selectBillingCycle updates the selected cycle`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectBillingCycle(BillingCycle.ANNUALLY)

        assertEquals(BillingCycle.ANNUALLY, viewModel.uiState.value.selectedBillingCycle)
    }

    @Test
    fun `upgradeToPlan shows the coming-soon dialog without a real purchase flow`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.upgradeToPlan(SubscriptionTier.PREMIUM)

        val state = viewModel.uiState.value
        assertTrue(state.showComingSoonDialog)
        assertEquals(SubscriptionTier.PREMIUM, state.selectedPlanForUpgrade)
    }

    @Test
    fun `dismissComingSoonDialog clears the dialog state`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.upgradeToPlan(SubscriptionTier.PRO)

        viewModel.dismissComingSoonDialog()

        val state = viewModel.uiState.value
        assertEquals(false, state.showComingSoonDialog)
        assertEquals(null, state.selectedPlanForUpgrade)
    }
}
