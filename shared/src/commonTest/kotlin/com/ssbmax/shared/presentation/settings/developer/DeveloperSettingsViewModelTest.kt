@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.settings.developer

import com.ssbmax.shared.domain.model.SubscriptionOverride
import com.ssbmax.shared.platform.isDebugBuild
import com.ssbmax.shared.platform.settings.DeveloperSettings
import com.ssbmax.shared.presentation.testing.FakeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Phase 5 (dev-subscription-override plan): pins that [DeveloperSettingsViewModel] surfaces
 * [DeveloperSettings]' persisted state, reacts to changes made through the settings object
 * directly (matching [DeveloperSettingsUiState.isVisible]'s doc comment note that toggling doesn't
 * require re-construction), and that its own setters round-trip through the same store.
 */
class DeveloperSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var developerSettings: DeveloperSettings

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        developerSettings = DeveloperSettings(FakeSettings())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = DeveloperSettingsViewModel(developerSettings)

    @Test
    fun `isVisible matches isDebugBuild`() {
        val viewModel = buildViewModel()

        assertEquals(isDebugBuild(), viewModel.uiState.value.isVisible)
    }

    @Test
    fun `defaults are FOLLOW_REAL and bypass false`() {
        val viewModel = buildViewModel()

        assertEquals(SubscriptionOverride.FOLLOW_REAL, viewModel.uiState.value.override)
        assertFalse(viewModel.uiState.value.bypassInterviewPrerequisites)
    }

    @Test
    fun `updateOverride persists through DeveloperSettings and updates state immediately`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.updateOverride(SubscriptionOverride.FORCE_PREMIUM)

        assertEquals(SubscriptionOverride.FORCE_PREMIUM, viewModel.uiState.value.override)
        assertEquals(SubscriptionOverride.FORCE_PREMIUM, developerSettings.getOverride())
    }

    @Test
    fun `updateBypassInterviewPrerequisites persists and updates state immediately`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.updateBypassInterviewPrerequisites(true)

        assertTrue(viewModel.uiState.value.bypassInterviewPrerequisites)
        assertTrue(developerSettings.getBypassInterviewPrerequisites())
    }

    @Test
    fun `external change to DeveloperSettings is reflected without reconstruction`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        developerSettings.setOverride(SubscriptionOverride.FORCE_FREE)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SubscriptionOverride.FORCE_FREE, viewModel.uiState.value.override)
    }
}
