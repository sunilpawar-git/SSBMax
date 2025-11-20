package com.ssbmax.ui.settings

import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for SettingsViewModel
 * Tests subscription tier observation and error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var observeCurrentUser: ObserveCurrentUserUseCase
    private lateinit var viewModel: SettingsViewModel
    private lateinit var userFlow: MutableStateFlow<SSBMaxUser?>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock Android Log
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any()) } returns 0

        // Setup mocks
        observeCurrentUser = mockk()
        userFlow = MutableStateFlow(null)
        every { observeCurrentUser.invoke() } returns userFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(android.util.Log::class)
    }

    @Test
    fun `viewModel initializes with FREE tier by default`() = runTest {
        // When
        viewModel = SettingsViewModel(observeCurrentUser)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals(SubscriptionTier.FREE, state.subscriptionTier)
        assertNull(state.error)
    }

    @Test
    fun `subscription tier updates when user changes`() = runTest {
        // Given
        viewModel = SettingsViewModel(observeCurrentUser)
        advanceUntilIdle()

        // Initially FREE (no user)
        assertEquals(SubscriptionTier.FREE, viewModel.uiState.value.subscriptionTier)

        // When - user with PRO subscription logs in
        val proUser = SSBMaxUser(
            id = "user123",
            email = "pro@ssbmax.com",
            displayName = "Pro User",
            role = UserRole.STUDENT,
            subscriptionTier = SubscriptionTier.PRO,
            subscription = null
        )
        userFlow.value = proUser
        advanceUntilIdle()

        // Then
        assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.subscriptionTier)

        // When - user upgrades to PREMIUM
        val premiumUser = proUser.copy(subscriptionTier = SubscriptionTier.PREMIUM)
        userFlow.value = premiumUser
        advanceUntilIdle()

        // Then
        assertEquals(SubscriptionTier.PREMIUM, viewModel.uiState.value.subscriptionTier)
    }

    @Test
    fun `subscription tier defaults to FREE when user is null`() = runTest {
        // Given - start with a PRO user
        val proUser = SSBMaxUser(
            id = "user123",
            email = "pro@ssbmax.com",
            displayName = "Pro User",
            role = UserRole.STUDENT,
            subscriptionTier = SubscriptionTier.PRO,
            subscription = null
        )
        userFlow.value = proUser
        viewModel = SettingsViewModel(observeCurrentUser)
        advanceUntilIdle()

        assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.subscriptionTier)

        // When - user logs out (becomes null)
        userFlow.value = null
        advanceUntilIdle()

        // Then - defaults back to FREE
        assertEquals(SubscriptionTier.FREE, viewModel.uiState.value.subscriptionTier)
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        // Given
        viewModel = SettingsViewModel(observeCurrentUser)
        advanceUntilIdle()

        // When
        viewModel.clearError()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
    }
}
