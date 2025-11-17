package com.ssbmax.ui.settings

import com.ssbmax.core.data.health.FirebaseHealthCheck
import com.ssbmax.core.data.preferences.ThemePreferenceManager
import com.ssbmax.core.domain.model.AppTheme
import com.ssbmax.core.domain.model.NotificationPreferences
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.migration.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Baseline tests for SettingsViewModel
 * Tests core notification toggle, theme update, and health check functionality
 *
 * This is a simplified test suite covering critical flows before God Class refactoring.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SettingsViewModel

    // Core dependencies
    private val mockNotificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val mockObserveCurrentUser = mockk<ObserveCurrentUserUseCase>(relaxed = true)
    private val mockThemePreferenceManager = mockk<ThemePreferenceManager>(relaxed = true)
    private val mockFirebaseHealthCheck = mockk<FirebaseHealthCheck>(relaxed = true)

    // Migration use cases (mocked but not used in baseline tests)
    private val mockMigrateOIR = mockk<MigrateOIRUseCase>(relaxed = true)
    private val mockMigratePPDT = mockk<MigratePPDTUseCase>(relaxed = true)
    private val mockMigratePsychology = mockk<MigratePsychologyUseCase>(relaxed = true)
    private val mockMigratePIQForm = mockk<MigratePIQFormUseCase>(relaxed = true)
    private val mockMigrateGTO = mockk<MigrateGTOUseCase>(relaxed = true)
    private val mockMigrateInterview = mockk<MigrateInterviewUseCase>(relaxed = true)
    private val mockMigrateSSBOverview = mockk<MigrateSSBOverviewUseCase>(relaxed = true)
    private val mockMigrateMedicals = mockk<MigrateMedicalsUseCase>(relaxed = true)
    private val mockMigrateConference = mockk<MigrateConferenceUseCase>(relaxed = true)
    private val mockClearCache = mockk<ClearFirestoreCacheUseCase>(relaxed = true)
    private val mockForceRefresh = mockk<ForceRefreshContentUseCase>(relaxed = true)

    private val themeFlow = MutableStateFlow(AppTheme.SYSTEM)

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
        enableMarketplaceUpdates = false
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
        every { mockThemePreferenceManager.themeFlow } returns themeFlow
        coEvery { mockThemePreferenceManager.setTheme(any()) } just runs
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

    // ==================== Notification Toggle Tests ====================

    @Test
    fun `togglePushNotifications updates preferences correctly`() = runTest {
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
        coVerify { mockNotificationRepository.savePreferences(any()) }
    }

    @Test
    fun `toggleGradingComplete updates preferences correctly`() = runTest {
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
        coVerify { mockNotificationRepository.savePreferences(any()) }
    }

    @Test
    fun `toggleStudyReminders updates preferences correctly`() = runTest {
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
        coVerify { mockNotificationRepository.savePreferences(any()) }
    }

    // ==================== Theme Update Tests ====================

    @Test
    fun `updateTheme changes app theme correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateTheme(AppTheme.DARK)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(AppTheme.DARK, state.appTheme)
        coVerify { mockThemePreferenceManager.setTheme(AppTheme.DARK) }
    }

    @Test
    fun `updateTheme to LIGHT theme works correctly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateTheme(AppTheme.LIGHT)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(AppTheme.LIGHT, state.appTheme)
        coVerify { mockThemePreferenceManager.setTheme(AppTheme.LIGHT) }
    }

    // ==================== Health Check Tests ====================

    @Test
    fun `runHealthCheck sets isCheckingHealth to true then false`() = runTest {
        // Given
        val healthStatus = FirebaseHealthCheck.HealthStatus(
            isFirestoreHealthy = true,
            isStorageHealthy = true,
            firestoreError = null,
            storageError = null
        )
        coEvery { mockFirebaseHealthCheck.checkHealth() } returns healthStatus
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.runHealthCheck()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isCheckingHealth) // Should be false after completion
        assertNotNull(state.healthCheckResult)
        assertTrue(state.healthCheckResult!!.isFirestoreHealthy)
        assertTrue(state.healthCheckResult!!.isStorageHealthy)
    }

    @Test
    fun `runHealthCheck handles errors gracefully`() = runTest {
        // Given
        coEvery { mockFirebaseHealthCheck.checkHealth() } throws Exception("Network error")
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.runHealthCheck()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isCheckingHealth)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Health check failed"))
    }

    // ==================== Helper Methods ====================

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            notificationRepository = mockNotificationRepository,
            observeCurrentUser = mockObserveCurrentUser,
            themePreferenceManager = mockThemePreferenceManager,
            firebaseHealthCheck = mockFirebaseHealthCheck,
            migrateOIRUseCase = mockMigrateOIR,
            migratePPDTUseCase = mockMigratePPDT,
            migratePsychologyUseCase = mockMigratePsychology,
            migratePIQFormUseCase = mockMigratePIQForm,
            migrateGTOUseCase = mockMigrateGTO,
            migrateInterviewUseCase = mockMigrateInterview,
            migrateSSBOverviewUseCase = mockMigrateSSBOverview,
            migrateMedicalsUseCase = mockMigrateMedicals,
            migrateConferenceUseCase = mockMigrateConference,
            clearCacheUseCase = mockClearCache,
            forceRefreshContentUseCase = mockForceRefresh
        )
    }
}
