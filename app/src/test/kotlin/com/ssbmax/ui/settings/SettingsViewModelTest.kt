package com.ssbmax.ui.settings

import com.ssbmax.core.data.health.FirebaseHealthCheck
import com.ssbmax.core.domain.usecase.migration.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Tests core health check functionality
 *
 * This is a simplified test suite covering critical flows during God Class refactoring.
 * Theme tests moved to ThemeSettingsViewModelTest.
 * Notification tests moved to NotificationSettingsViewModelTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SettingsViewModel

    // Core dependencies
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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock Android Log
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(android.util.Log::class)
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
