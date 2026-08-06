@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.profile

import com.ssbmax.shared.domain.model.SubscriptionOverride
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.shared.platform.settings.DeveloperSettings
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeLogger
import com.ssbmax.shared.presentation.testing.FakeSettings
import com.ssbmax.shared.presentation.testing.FakeSubscriptionRepository
import com.ssbmax.shared.presentation.testing.FakeTestProgressRepository
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
 * Phase 7 (tier-storage SSOT) regression coverage: `isPremium` used to derive from
 * `UserProfile.subscriptionType` (`userProfile?.subscriptionType?.name == "PREMIUM"`), a
 * Firestore field only ever written as a hardcoded FREE default -- never a live value -- so
 * this screen could never actually show a premium user as premium. Pins that `isPremium` now
 * comes from [GetSubscriptionTierUseCase] instead.
 */
class StudentProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userProfileRepository: FakeUserProfileRepository
    private lateinit var testProgressRepository: FakeTestProgressRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var subscriptionRepository: FakeSubscriptionRepository
    private lateinit var developerSettings: DeveloperSettings

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userProfileRepository = FakeUserProfileRepository()
        testProgressRepository = FakeTestProgressRepository()
        authRepository = FakeAuthRepository(initialUser = testUser())
        subscriptionRepository = FakeSubscriptionRepository()
        developerSettings = DeveloperSettings(FakeSettings())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = StudentProfileViewModel(
        userProfileRepository = userProfileRepository,
        testProgressRepository = testProgressRepository,
        observeCurrentUser = ObserveCurrentUserUseCase(authRepository),
        getSubscriptionTier = GetSubscriptionTierUseCase(subscriptionRepository),
        developerSettings = developerSettings,
        logger = FakeLogger()
    )

    @Test
    fun `isPremium is true when SubscriptionRepository reports PREMIUM`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PREMIUM)

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isPremium)
    }

    @Test
    fun `isPremium is false for FREE and PRO tiers`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PRO)

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isPremium)
    }

    @Test
    fun `override change alone, with no auth change, triggers a refetch`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.FREE)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isPremium)

        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PREMIUM)
        developerSettings.setOverride(SubscriptionOverride.FORCE_PREMIUM)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isPremium)
    }

    @Test
    fun `auth-state change alone, with no override, triggers a refetch`() = runTest(testDispatcher) {
        subscriptionRepository.tierResult = Result.success(SubscriptionTier.FREE)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isPremium)

        subscriptionRepository.tierResult = Result.success(SubscriptionTier.PREMIUM)
        authRepository.userFlow.value = testUser(id = "test-user-2")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isPremium)
    }
}
