@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.settings

import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeLogger
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

/**
 * Regression coverage for Phase 7 of the subscription-override plan: `SubscriptionTier`
 * is stored in three independent Firestore locations, and `SettingsViewModel` used to read
 * the one (`SSBMaxUser.subscriptionTier`, off `AuthRepository`) that's hardcoded FREE at
 * account creation and never written again -- so "Current Plan" showed Free forever,
 * regardless of the real tier or the debug override. These tests pin that the tier now
 * comes from [GetSubscriptionTierUseCase] (`SubscriptionRepository`, the same SSOT the
 * debug-override decorator wraps), not from the auth-sourced user object.
 */
class SettingsViewModelTest {

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

    private fun buildViewModel() = SettingsViewModel(
        observeCurrentUser = ObserveCurrentUserUseCase(authRepository),
        getSubscriptionTier = GetSubscriptionTierUseCase(subscriptionRepository),
        logger = FakeLogger()
    )

    @Test
    fun `tier is sourced from SubscriptionRepository not the auth-sourced user object`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PREMIUM)

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SubscriptionTier.PREMIUM, viewModel.uiState.value.subscriptionTier)
    }

    @Test
    fun `unauthenticated user falls back to FREE without an error`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SubscriptionTier.FREE, state.subscriptionTier)
        assertEquals(null, state.error)
    }

    @Test
    fun `tier lookup failure surfaces an error and falls back to FREE`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.failure(Exception("network down"))

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SubscriptionTier.FREE, state.subscriptionTier)
        assertEquals("network down", state.error)
    }

    @Test
    fun `clearError resets the error field`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearError()

        assertEquals(null, viewModel.uiState.value.error)
    }
}
