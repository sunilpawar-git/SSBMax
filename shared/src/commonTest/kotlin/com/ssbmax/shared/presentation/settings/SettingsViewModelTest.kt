@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.settings

import com.ssbmax.shared.domain.model.SubscriptionOverride
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.shared.platform.settings.DeveloperSettings
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeLogger
import com.ssbmax.shared.presentation.testing.FakeSettings
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
    private lateinit var developerSettings: DeveloperSettings

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository(initialUser = testUser())
        subscriptionRepository = FakeSubscriptionRepository()
        developerSettings = DeveloperSettings(FakeSettings())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = SettingsViewModel(
        observeCurrentUser = ObserveCurrentUserUseCase(authRepository),
        getSubscriptionTier = GetSubscriptionTierUseCase(subscriptionRepository),
        developerSettings = developerSettings,
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

    @Test
    fun `override change alone, with no auth change, triggers a tier refetch`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.FREE)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SubscriptionTier.FREE, viewModel.uiState.value.subscriptionTier)

        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PRO)
        developerSettings.setOverride(SubscriptionOverride.FORCE_PRO)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.subscriptionTier)
    }

    @Test
    fun `logged-out user still shows FREE even with an active override`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PREMIUM)
        developerSettings.setOverride(SubscriptionOverride.FORCE_PREMIUM)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SubscriptionTier.FREE, state.subscriptionTier)
        assertEquals(null, state.error)
    }

    @Test
    fun `rapid FOLLOW_REAL to FORCE_FREE to FORCE_PRO override toggles reflect only the latest value`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            subscriptionRepository.tierResult = Result.success(SubscriptionTier.FREE)
            developerSettings.setOverride(SubscriptionOverride.FORCE_FREE)
            subscriptionRepository.tierResult = Result.success(SubscriptionTier.PRO)
            developerSettings.setOverride(SubscriptionOverride.FORCE_PRO)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.subscriptionTier)
        }

    @Test
    fun `existing auth-change-triggers-refetch behavior still holds after the combine restructure`() =
        runTest(testDispatcher) {
            subscriptionRepository.tierResult = Result.success(SubscriptionTier.FREE)
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(SubscriptionTier.FREE, viewModel.uiState.value.subscriptionTier)

            subscriptionRepository.tierResult = Result.success(SubscriptionTier.PREMIUM)
            authRepository.userFlow.value = testUser(id = "test-user-2")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(SubscriptionTier.PREMIUM, viewModel.uiState.value.subscriptionTier)
        }

    @Test
    fun `one override toggle produces exactly one tier fetch, not a duplicate or race`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val callsAfterInit = subscriptionRepository.getSubscriptionTierCallCount

        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PRO)
        developerSettings.setOverride(SubscriptionOverride.FORCE_PRO)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(callsAfterInit + 1, subscriptionRepository.getSubscriptionTierCallCount)
        assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.subscriptionTier)
    }
}
