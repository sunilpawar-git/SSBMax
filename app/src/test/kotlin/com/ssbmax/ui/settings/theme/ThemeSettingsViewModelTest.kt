package com.ssbmax.ui.settings.theme

import com.ssbmax.core.data.preferences.ThemePreferenceManager
import com.ssbmax.core.domain.model.AppTheme
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSettingsViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: ThemeSettingsViewModel
    private val mockThemePreferenceManager = mockk<ThemePreferenceManager>(relaxed = true)
    private val themeFlow = MutableStateFlow(AppTheme.SYSTEM)

    @Before
    fun setup() {
        every { mockThemePreferenceManager.themeFlow } returns themeFlow
    }

    @Test
    fun `initial state has SYSTEM theme`() = runTest {
        viewModel = ThemeSettingsViewModel(mockThemePreferenceManager)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(AppTheme.SYSTEM, state.appTheme)
        assertNull(state.error)
    }

    @Test
    fun `observes LIGHT theme`() = runTest {
        themeFlow.value = AppTheme.LIGHT

        viewModel = ThemeSettingsViewModel(mockThemePreferenceManager)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(AppTheme.LIGHT, state.appTheme)
    }

    @Test
    fun `observes DARK theme`() = runTest {
        themeFlow.value = AppTheme.DARK

        viewModel = ThemeSettingsViewModel(mockThemePreferenceManager)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(AppTheme.DARK, state.appTheme)
    }

    @Test
    fun `updateTheme calls themePreferenceManager`() = runTest {
        coEvery { mockThemePreferenceManager.setTheme(any()) } returns Unit

        viewModel = ThemeSettingsViewModel(mockThemePreferenceManager)
        advanceUntilIdle()

        viewModel.updateTheme(AppTheme.DARK)
        advanceUntilIdle()

        coVerify { mockThemePreferenceManager.setTheme(AppTheme.DARK) }
    }

    @Test
    fun `clearError clears error message`() = runTest {
        viewModel = ThemeSettingsViewModel(mockThemePreferenceManager)
        advanceUntilIdle()

        viewModel.clearError()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertNull(state.error)
    }
}
