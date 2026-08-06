@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.profile

import com.ssbmax.shared.domain.model.EntryType
import com.ssbmax.shared.domain.model.Gender
import com.ssbmax.shared.domain.model.SubscriptionOverride
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.model.UserProfile
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.shared.platform.settings.DeveloperSettings
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeLogger
import com.ssbmax.shared.presentation.testing.FakeSettings
import com.ssbmax.shared.presentation.testing.FakeSubscriptionRepository
import com.ssbmax.shared.presentation.testing.FakeUserProfileRepository
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
 * Phase 7 (tier-storage SSOT) regression coverage: the drawer's tier badge used to derive
 * from `UserProfile.subscriptionType`, a Firestore field only ever written as a hardcoded
 * FREE default at profile creation and never updated -- so the badge showed "Free" forever
 * regardless of the real tier or the debug override. These tests pin that
 * [UserProfileUiState.subscriptionTier] now comes from [GetSubscriptionTierUseCase]
 * (`SubscriptionRepository`, the same SSOT [SettingsViewModel][com.ssbmax.shared.presentation.settings.SettingsViewModel]'s
 * "Current Plan" fix uses), independent of the profile document.
 */
class UserProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userProfileRepository: FakeUserProfileRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var subscriptionRepository: FakeSubscriptionRepository
    private lateinit var developerSettings: DeveloperSettings

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userProfileRepository = FakeUserProfileRepository(initialProfile = profile())
        authRepository = FakeAuthRepository(initialUser = testUser())
        subscriptionRepository = FakeSubscriptionRepository()
        developerSettings = DeveloperSettings(FakeSettings())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun profile() = UserProfile(
        userId = "test-user-1", fullName = "Test User", age = 24,
        gender = Gender.MALE, entryType = EntryType.GRADUATE
    )

    private fun buildViewModel() = UserProfileViewModel(
        userProfileRepository = userProfileRepository,
        authRepository = authRepository,
        getSubscriptionTier = GetSubscriptionTierUseCase(subscriptionRepository),
        developerSettings = developerSettings,
        logger = FakeLogger()
    )

    @Test
    fun `subscription tier is sourced from SubscriptionRepository not the profile document`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PREMIUM)

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SubscriptionTier.PREMIUM, viewModel.uiState.value.subscriptionTier)
    }

    @Test
    fun `unauthenticated user clears the subscription tier`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PREMIUM)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SubscriptionTier.PREMIUM, viewModel.uiState.value.subscriptionTier)

        authRepository.userFlow.value = null
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.subscriptionTier)
    }

    @Test
    fun `override change alone updates subscriptionTier with no auth-state change`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.FREE)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SubscriptionTier.FREE, viewModel.uiState.value.subscriptionTier)

        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PREMIUM)
        developerSettings.setOverride(SubscriptionOverride.FORCE_PREMIUM)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SubscriptionTier.PREMIUM, viewModel.uiState.value.subscriptionTier)
    }
}
