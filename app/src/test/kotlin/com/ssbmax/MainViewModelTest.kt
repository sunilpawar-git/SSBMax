package com.ssbmax

import com.ssbmax.shared.domain.model.AppTheme
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import com.ssbmax.shared.platform.settings.AppThemeSettings
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.Assert.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : BaseViewModelTest() {
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `init sets up theme state`() = runTest {
        // Given
        val mockThemeManager = mockk<AppThemeSettings>()
        val mockAuthRepo = mockk<AuthRepository>()
        val mockUserProfileRepo = mockk<UserProfileRepository>(relaxed = true)

        every { mockThemeManager.getTheme() } returns AppTheme.SYSTEM
        every { mockThemeManager.themeFlow } returns MutableStateFlow(AppTheme.SYSTEM)
        every { mockAuthRepo.currentUser } returns MutableStateFlow(null)
        
        // When
        val viewModel = MainViewModel(mockThemeManager, mockAuthRepo, mockUserProfileRepo)
        advanceUntilIdle()
        
        // Then
        assertNotNull(viewModel.themeState)
    }
}

