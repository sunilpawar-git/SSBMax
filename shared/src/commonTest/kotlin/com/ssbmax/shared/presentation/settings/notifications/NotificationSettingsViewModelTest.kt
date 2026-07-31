@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.settings.notifications

import com.ssbmax.shared.domain.model.NotificationPreferences
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.presentation.testing.FakeAuthRepository
import com.ssbmax.shared.presentation.testing.FakeNotificationRepository
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Characterization test for [NotificationSettingsViewModel], written after its
 * mechanical conversion to `androidx.lifecycle.ViewModel` (Phase 1 of the
 * KMP-convergence plan) to close a test-coverage gap flagged during that
 * phase's tech-debt sweep. Pins load-preferences and optimistic-update-with-
 * rollback toggle behaviour.
 */
class NotificationSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var notificationRepository: FakeNotificationRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository(initialUser = testUser())
        notificationRepository = FakeNotificationRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = NotificationSettingsViewModel(
        notificationRepository = notificationRepository,
        observeCurrentUser = ObserveCurrentUserUseCase(authRepository)
    )

    @Test
    fun `loads preferences successfully on init`() = runTest(testDispatcher) {
        val prefs = NotificationPreferences(userId = "test-user-1", enablePushNotifications = false)
        notificationRepository.preferencesResult = Result.success(prefs)

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(prefs, state.notificationPreferences)
        assertNull(state.error)
    }

    @Test
    fun `permission-denied failure surfaces default preferences without an error`() = runTest(testDispatcher) {
        notificationRepository.preferencesResult = Result.failure(Exception("PERMISSION_DENIED: no access"))

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.notificationPreferences)
        assertNull(state.error)
    }

    @Test
    fun `other failure surfaces the error message`() = runTest(testDispatcher) {
        notificationRepository.preferencesResult = Result.failure(Exception("network down"))

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("network down", state.error)
    }

    @Test
    fun `unauthenticated user clears preferences without an error`() = runTest(testDispatcher) {
        authRepository.userFlow.value = null

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNull(state.notificationPreferences)
        assertNull(state.error)
    }

    @Test
    fun `togglePushNotifications updates and saves preferences`() = runTest(testDispatcher) {
        val prefs = NotificationPreferences(userId = "test-user-1", enablePushNotifications = true)
        notificationRepository.preferencesResult = Result.success(prefs)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.togglePushNotifications(false)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.notificationPreferences?.enablePushNotifications)
        assertEquals(1, notificationRepository.savedPreferences.size)
        assertEquals(false, notificationRepository.savedPreferences.first().enablePushNotifications)
    }

    @Test
    fun `toggle failure rolls back the optimistic update and surfaces an error`() = runTest(testDispatcher) {
        val prefs = NotificationPreferences(userId = "test-user-1", enableTestReminders = true)
        notificationRepository.preferencesResult = Result.success(prefs)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        notificationRepository.savePreferencesResult = Result.failure(Exception("save failed"))

        viewModel.toggleTestReminders(false)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(true, state.notificationPreferences?.enableTestReminders)
        assertEquals("save failed", state.error)
    }

    @Test
    fun `clearError resets the error field`() = runTest(testDispatcher) {
        notificationRepository.preferencesResult = Result.failure(Exception("boom"))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }
}
