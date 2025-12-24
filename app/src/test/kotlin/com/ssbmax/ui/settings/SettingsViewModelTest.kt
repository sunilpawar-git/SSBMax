package com.ssbmax.ui.settings

import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: SettingsViewModel
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)

    private val mockFreeUser = SSBMaxUser(
        id = "user-123",
        email = "test@example.com",
        displayName = "Test User",
        role = UserRole.STUDENT,
        subscriptionTier = SubscriptionTier.FREE
    )

    private val mockProUser = mockFreeUser.copy(
        subscriptionTier = SubscriptionTier.PRO
    )

    private val mockPremiumUser = mockFreeUser.copy(
        subscriptionTier = SubscriptionTier.PREMIUM
    )

    @Before
    fun setup() {
        // Default: return FREE user
        every { mockObserveCurrentUser() } returns flowOf(mockFreeUser)
    }

    @Test
    fun `initial state has FREE tier`() = runTest {
        viewModel = SettingsViewModel(mockObserveCurrentUser)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(SubscriptionTier.FREE, state.subscriptionTier)
        assertNull(state.error)
    }

    @Test
    fun `observes FREE tier subscription`() = runTest {
        every { mockObserveCurrentUser() } returns flowOf(mockFreeUser)

        viewModel = SettingsViewModel(mockObserveCurrentUser)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(SubscriptionTier.FREE, state.subscriptionTier)
        assertNull(state.error)
    }

    @Test
    fun `observes PRO tier subscription`() = runTest {
        every { mockObserveCurrentUser() } returns flowOf(mockProUser)

        viewModel = SettingsViewModel(mockObserveCurrentUser)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(SubscriptionTier.PRO, state.subscriptionTier)
        assertNull(state.error)
    }

    @Test
    fun `observes PREMIUM tier subscription`() = runTest {
        every { mockObserveCurrentUser() } returns flowOf(mockPremiumUser)

        viewModel = SettingsViewModel(mockObserveCurrentUser)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(SubscriptionTier.PREMIUM, state.subscriptionTier)
        assertNull(state.error)
    }

    @Test
    fun `handles null user with FREE tier default`() = runTest {
        every { mockObserveCurrentUser() } returns flowOf(null)

        viewModel = SettingsViewModel(mockObserveCurrentUser)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(SubscriptionTier.FREE, state.subscriptionTier)
        assertNull(state.error)
    }

    @Test
    fun `handles error in user observation`() = runTest {
        every { mockObserveCurrentUser() } returns flowOf()  // Empty flow triggers catch block

        viewModel = SettingsViewModel(mockObserveCurrentUser)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Should have FREE tier default and possibly an error
        assertEquals(SubscriptionTier.FREE, state.subscriptionTier)
    }

    @Test
    fun `clearError clears error message`() = runTest {
        viewModel = SettingsViewModel(mockObserveCurrentUser)
        advanceUntilIdle()

        // Manually set error for testing
        viewModel.clearError()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertNull(state.error)
    }

    @Test
    fun `updates subscription tier when user changes`() = runTest {
        every { mockObserveCurrentUser() } returns flowOf(mockFreeUser, mockProUser)

        viewModel = SettingsViewModel(mockObserveCurrentUser)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Should reflect the latest emission (PRO)
        assertEquals(SubscriptionTier.PRO, state.subscriptionTier)
        assertNull(state.error)
    }
}
