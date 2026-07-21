package com.ssbmax.ui.settings.theme

import com.ssbmax.shared.platform.settings.AppThemeSettings
import com.ssbmax.shared.domain.model.AppTheme
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
    private val mockAppThemeSettings = mockk<AppThemeSettings>(relaxed = true)
    private val themeFlow = MutableStateFlow(AppTheme.SYSTEM)

    @Before
    fun setup() {
        every { mockAppThemeSettings.themeFlow } returns themeFlow
    }

    @Test
    fun `initial state has SYSTEM theme`() = runTest {
        viewModel = ThemeSettingsViewModel(mockAppThemeSettings)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(AppTheme.SYSTEM, state.appTheme)
        assertNull(state.error)
    }

    @Test
    fun `observes LIGHT theme`() = runTest {
        themeFlow.value = AppTheme.LIGHT

        viewModel = ThemeSettingsViewModel(mockAppThemeSettings)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(AppTheme.LIGHT, state.appTheme)
    }

    @Test
    fun `observes DARK theme`() = runTest {
        themeFlow.value = AppTheme.DARK

        viewModel = ThemeSettingsViewModel(mockAppThemeSettings)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(AppTheme.DARK, state.appTheme)
    }

    @Test
    fun `updateTheme calls themePreferenceManager`() = runTest {
        coEvery { mockAppThemeSettings.setTheme(any()) } returns Unit

        viewModel = ThemeSettingsViewModel(mockAppThemeSettings)
        advanceUntilIdle()

        viewModel.updateTheme(AppTheme.DARK)
        advanceUntilIdle()

        coVerify { mockAppThemeSettings.setTheme(AppTheme.DARK) }
    }

    @Test
    fun `clearError clears error message`() = runTest {
        viewModel = ThemeSettingsViewModel(mockAppThemeSettings)
        advanceUntilIdle()

        viewModel.clearError()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertNull(state.error)
    }
}
