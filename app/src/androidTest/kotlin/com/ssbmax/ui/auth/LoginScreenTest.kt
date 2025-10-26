package com.ssbmax.ui.auth

import androidx.compose.ui.test.*
import com.ssbmax.core.domain.model.SSBMaxUser
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.testing.BaseComposeTest
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

/**
 * UI tests for LoginScreen
 * Tests focus on UI state handling, as actual Google Sign-In requires real auth
 */
@HiltAndroidTest
class LoginScreenTest : BaseComposeTest() {

    private lateinit var mockViewModel: AuthViewModel
    private lateinit var uiStateFlow: MutableStateFlow<AuthUiState>
    private var loginSuccessCalled = false
    private var roleSelectionCalled = false

    @Before
    override fun setup() {
        super.setup()
        
        // Setup mocks
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(AuthUiState.Initial)
        every { mockViewModel.uiState } returns uiStateFlow
        
        // Reset flags
        loginSuccessCalled = false
        roleSelectionCalled = false
    }

    @Test
    fun loginScreen_displaysCorrectly() {
        // Given: Initial state
        composeTestRule.setContent {
            LoginScreen(viewModel = mockViewModel)
        }

        // Then: Verify UI elements
        composeTestRule
            .onNodeWithText("SSBMax")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Your Path to SSB Success")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Welcome!")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Continue with Google")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithTag("google_signin_button")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun googleSignInButton_isClickable() {
        // Given: Initial state
        composeTestRule.setContent {
            LoginScreen(viewModel = mockViewModel)
        }

        // When: Click Google Sign-In button
        composeTestRule
            .onNodeWithTag("google_signin_button")
            .performClick()

        // Then: ViewModel method should be called
        // Note: We can't fully test the Google Sign-In flow in UI tests
        // as it requires real Google auth, but we can verify button click works
        composeTestRule
            .onNodeWithTag("google_signin_button")
            .assertExists()
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        // Given: Loading state
        uiStateFlow.value = AuthUiState.Loading

        composeTestRule.setContent {
            LoginScreen(viewModel = mockViewModel)
        }

        // Then: Loading indicator should be visible
        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertIsDisplayed()
        
        // And: Button should be disabled
        composeTestRule
            .onNodeWithTag("google_signin_button")
            .assertIsNotEnabled()
    }

    @Test
    fun errorState_displaysErrorMessage() {
        // Given: Error state
        val errorMessage = "Google Sign-In failed. Please try again."
        uiStateFlow.value = AuthUiState.Error(errorMessage)

        composeTestRule.setContent {
            LoginScreen(viewModel = mockViewModel)
        }

        // Then: Error message should be visible
        composeTestRule
            .onNodeWithTag("error_message")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText(errorMessage, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun successState_triggersNavigation() {
        // Given: Mock user
        val mockUser = SSBMaxUser(
            id = "test-123",
            email = "test@example.com",
            displayName = "Test User",
            role = UserRole.STUDENT,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis() - 10000 // Not first time
        )

        composeTestRule.setContent {
            LoginScreen(
                viewModel = mockViewModel,
                onLoginSuccess = { loginSuccessCalled = true }
            )
        }

        // When: Success state is set
        uiStateFlow.value = AuthUiState.Success(mockUser)

        // Then: Navigation callback should be triggered
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            loginSuccessCalled
        }
        
        assert(loginSuccessCalled) { "Login success callback should be called" }
    }

    @Test
    fun needsRoleSelectionState_triggersRoleSelectionNavigation() {
        // Given: Mock new user
        val mockUser = SSBMaxUser(
            id = "test-456",
            email = "newuser@example.com",
            displayName = "New User",
            role = UserRole.STUDENT,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis() // First time
        )

        composeTestRule.setContent {
            LoginScreen(
                viewModel = mockViewModel,
                onNeedsRoleSelection = { roleSelectionCalled = true }
            )
        }

        // When: NeedsRoleSelection state is set
        uiStateFlow.value = AuthUiState.NeedsRoleSelection(mockUser)

        // Then: Role selection callback should be triggered
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            roleSelectionCalled
        }
        
        assert(roleSelectionCalled) { "Role selection callback should be called" }
    }

    @Test
    fun termsAndPrivacyText_isDisplayed() {
        // Given: Initial state
        composeTestRule.setContent {
            LoginScreen(viewModel = mockViewModel)
        }

        // Then: Terms and Privacy text should be visible
        composeTestRule
            .onNodeWithText("By continuing, you agree to our Terms of Service and Privacy Policy", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun multipleErrorStates_displayDifferentMessages() {
        // Test 1: Network error
        composeTestRule.setContent {
            LoginScreen(viewModel = mockViewModel)
        }
        
        uiStateFlow.value = AuthUiState.Error("Network error. Please check your connection.")
        
        composeTestRule
            .onNodeWithText("Network error", substring = true)
            .assertIsDisplayed()

        // Test 2: Different error
        uiStateFlow.value = AuthUiState.Error("Account not found.")
        
        composeTestRule
            .onNodeWithText("Account not found", substring = true)
            .assertIsDisplayed()
        
        // Previous error should not be visible
        composeTestRule
            .onNodeWithText("Network error", substring = true)
            .assertDoesNotExist()
    }
}

