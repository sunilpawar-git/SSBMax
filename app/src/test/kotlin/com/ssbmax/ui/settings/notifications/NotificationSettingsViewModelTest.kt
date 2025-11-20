package com.ssbmax.ui.settings.notifications

import com.ssbmax.core.domain.model.NotificationPreferences
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
 * Tests for NotificationSettingsViewModel
 * Covers all toggle methods, loadPreferences, updateQuietHours, and error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: NotificationSettingsViewModel

    // Dependencies
    private val mockNotificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)

    private val testUser = SSBMaxUser(
        id = "test-user-123",
        email = "test@example.com",
        role = UserRole.STUDENT,
        displayName = "Test User"
    )

    private val testPreferences = NotificationPreferences(
        userId = testUser.id,
        enablePushNotifications = true,
        enableGradingNotifications = true,
        enableFeedbackNotifications = true,
        enableBatchInvitations = true,
        enableGeneralAnnouncements = true,
        enableStudyReminders = false,
        enableTestReminders = false,
        enableMarketplaceUpdates = false,
        quietHoursStart = 22,
        quietHoursEnd = 7
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock Android Log
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any()) } returns 0

        // Setup default mocks
        coEvery { mockObserveCurrentUser() } returns flowOf(testUser)
        coEvery { mockNotificationRepository.getPreferences(testUser.id) } returns
            Result.success(testPreferences)
        coEvery { mockNotificationRepository.savePreferences(any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(android.util.Log::class)
    }

    // ==================== Toggle Methods Tests (Success Cases) ====================

    @Test
    fun `togglePushNotifications success updates preferences correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.togglePushNotifications(false)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertFalse(state.notificationPreferences!!.enablePushNotifications)
        assertNull(state.error)
        coVerify { mockNotificationRepository.savePreferences(match { !it.enablePushNotifications }) }
    }

    @Test
    fun `toggleGradingComplete success updates preferences correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleGradingComplete(false)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertFalse(state.notificationPreferences!!.enableGradingNotifications)
        assertNull(state.error)
        coVerify { mockNotificationRepository.savePreferences(match { !it.enableGradingNotifications }) }
    }

    @Test
    fun `toggleFeedbackAvailable success updates preferences correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleFeedbackAvailable(false)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertFalse(state.notificationPreferences!!.enableFeedbackNotifications)
        assertNull(state.error)
        coVerify { mockNotificationRepository.savePreferences(match { !it.enableFeedbackNotifications }) }
    }

    @Test
    fun `toggleBatchInvitation success updates preferences correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleBatchInvitation(false)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertFalse(state.notificationPreferences!!.enableBatchInvitations)
        assertNull(state.error)
        coVerify { mockNotificationRepository.savePreferences(match { !it.enableBatchInvitations }) }
    }

    @Test
    fun `toggleGeneralAnnouncement success updates preferences correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleGeneralAnnouncement(false)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertFalse(state.notificationPreferences!!.enableGeneralAnnouncements)
        assertNull(state.error)
        coVerify { mockNotificationRepository.savePreferences(match { !it.enableGeneralAnnouncements }) }
    }

    @Test
    fun `toggleStudyReminders success updates preferences correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleStudyReminders(true)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertTrue(state.notificationPreferences!!.enableStudyReminders)
        assertNull(state.error)
        coVerify { mockNotificationRepository.savePreferences(match { it.enableStudyReminders }) }
    }

    @Test
    fun `toggleTestReminders success updates preferences correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleTestReminders(true)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertTrue(state.notificationPreferences!!.enableTestReminders)
        assertNull(state.error)
        coVerify { mockNotificationRepository.savePreferences(match { it.enableTestReminders }) }
    }

    @Test
    fun `toggleMarketplaceUpdates success updates preferences correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleMarketplaceUpdates(true)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertTrue(state.notificationPreferences!!.enableMarketplaceUpdates)
        assertNull(state.error)
        coVerify { mockNotificationRepository.savePreferences(match { it.enableMarketplaceUpdates }) }
    }

    // ==================== Toggle Methods Tests (Failure Cases) ====================

    @Test
    fun `togglePushNotifications failure reverts preferences and shows error`() = runTest {
        // Given
        coEvery { mockNotificationRepository.savePreferences(any()) } returns
            Result.failure(Exception("Network error"))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.togglePushNotifications(false)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertTrue(state.notificationPreferences!!.enablePushNotifications) // Reverted
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Network error"))
    }

    @Test
    fun `toggleGradingComplete failure reverts preferences and shows error`() = runTest {
        // Given
        coEvery { mockNotificationRepository.savePreferences(any()) } returns
            Result.failure(Exception("Save failed"))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleGradingComplete(false)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertTrue(state.notificationPreferences!!.enableGradingNotifications) // Reverted
        assertNotNull(state.error)
    }

    @Test
    fun `toggleFeedbackAvailable failure reverts preferences and shows error`() = runTest {
        // Given
        coEvery { mockNotificationRepository.savePreferences(any()) } returns
            Result.failure(Exception("Save failed"))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleFeedbackAvailable(false)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertTrue(state.notificationPreferences!!.enableFeedbackNotifications) // Reverted
        assertNotNull(state.error)
    }

    @Test
    fun `toggleBatchInvitation failure reverts preferences and shows error`() = runTest {
        // Given
        coEvery { mockNotificationRepository.savePreferences(any()) } returns
            Result.failure(Exception("Save failed"))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleBatchInvitation(false)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertTrue(state.notificationPreferences!!.enableBatchInvitations) // Reverted
        assertNotNull(state.error)
    }

    @Test
    fun `toggleGeneralAnnouncement failure reverts preferences and shows error`() = runTest {
        // Given
        coEvery { mockNotificationRepository.savePreferences(any()) } returns
            Result.failure(Exception("Save failed"))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleGeneralAnnouncement(false)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertTrue(state.notificationPreferences!!.enableGeneralAnnouncements) // Reverted
        assertNotNull(state.error)
    }

    @Test
    fun `toggleStudyReminders failure reverts preferences and shows error`() = runTest {
        // Given
        coEvery { mockNotificationRepository.savePreferences(any()) } returns
            Result.failure(Exception("Save failed"))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleStudyReminders(true)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertFalse(state.notificationPreferences!!.enableStudyReminders) // Reverted
        assertNotNull(state.error)
    }

    @Test
    fun `toggleTestReminders failure reverts preferences and shows error`() = runTest {
        // Given
        coEvery { mockNotificationRepository.savePreferences(any()) } returns
            Result.failure(Exception("Save failed"))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleTestReminders(true)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertFalse(state.notificationPreferences!!.enableTestReminders) // Reverted
        assertNotNull(state.error)
    }

    @Test
    fun `toggleMarketplaceUpdates failure reverts preferences and shows error`() = runTest {
        // Given
        coEvery { mockNotificationRepository.savePreferences(any()) } returns
            Result.failure(Exception("Save failed"))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleMarketplaceUpdates(true)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertFalse(state.notificationPreferences!!.enableMarketplaceUpdates) // Reverted
        assertNotNull(state.error)
    }

    // ==================== loadPreferences Tests ====================

    @Test
    fun `loadPreferences shows loading state then success`() = runTest {
        // Given & When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then (after load)
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.notificationPreferences)
        assertEquals(testUser.id, state.notificationPreferences!!.userId)
        assertNull(state.error)
    }

    @Test
    fun `loadPreferences handles error correctly`() = runTest {
        // Given
        coEvery { mockNotificationRepository.getPreferences(testUser.id) } returns
            Result.failure(Exception("Network error"))
        viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.notificationPreferences) // Uses default preferences
        assertEquals(testUser.id, state.notificationPreferences!!.userId)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Network error"))
    }

    @Test
    fun `loadPreferences handles permission denied gracefully`() = runTest {
        // Given
        coEvery { mockNotificationRepository.getPreferences(testUser.id) } returns
            Result.failure(Exception("PERMISSION_DENIED"))
        viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.notificationPreferences) // Uses default preferences
        assertEquals(testUser.id, state.notificationPreferences!!.userId)
        assertNull(state.error) // No error shown for permission denied
    }

    @Test
    fun `loadPreferences handles no user gracefully`() = runTest {
        // Given
        coEvery { mockObserveCurrentUser() } returns flowOf(null)
        viewModel = createViewModel()

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.notificationPreferences) // No preferences without user
        assertNull(state.error)
    }

    // ==================== updateQuietHours Tests ====================

    @Test
    fun `updateQuietHours success updates preferences correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateQuietHours(startHour = 23, endHour = 6)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertEquals(23, state.notificationPreferences!!.quietHoursStart)
        assertEquals(6, state.notificationPreferences!!.quietHoursEnd)
        assertNull(state.error)
        coVerify {
            mockNotificationRepository.savePreferences(
                match { it.quietHoursStart == 23 && it.quietHoursEnd == 6 }
            )
        }
    }

    @Test
    fun `updateQuietHours failure reverts preferences and shows error`() = runTest {
        // Given
        coEvery { mockNotificationRepository.savePreferences(any()) } returns
            Result.failure(Exception("Update failed"))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateQuietHours(startHour = 23, endHour = 6)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.notificationPreferences)
        assertEquals(22, state.notificationPreferences!!.quietHoursStart) // Reverted
        assertEquals(7, state.notificationPreferences!!.quietHoursEnd) // Reverted
        assertNotNull(state.error)
    }

    // ==================== clearError Tests ====================

    @Test
    fun `clearError removes error message`() = runTest {
        // Given
        coEvery { mockNotificationRepository.savePreferences(any()) } returns
            Result.failure(Exception("Test error"))
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.togglePushNotifications(false)
        advanceUntilIdle()

        // Verify error exists
        assertNotNull(viewModel.uiState.value.error)

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    // ==================== Helper Methods ====================

    private fun createViewModel(): NotificationSettingsViewModel {
        return NotificationSettingsViewModel(
            notificationRepository = mockNotificationRepository,
            observeCurrentUser = mockObserveCurrentUser
        )
    }
}
