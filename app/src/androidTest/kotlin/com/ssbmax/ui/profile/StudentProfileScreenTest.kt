package com.ssbmax.ui.profile

import androidx.compose.ui.test.*
import com.ssbmax.core.domain.model.*
import com.ssbmax.testing.BaseComposeTest
import com.ssbmax.testing.TestDataFactory
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

/**
 * UI tests for StudentProfileScreen
 */
@HiltAndroidTest
class StudentProfileScreenTest : BaseComposeTest() {

    private lateinit var mockViewModel: StudentProfileViewModel
    private lateinit var uiStateFlow: MutableStateFlow<StudentProfileUiState>
    private lateinit var testProfile: UserProfile

    @Before
    override fun setup() {
        super.setup()
        
        // Setup test data
        testProfile = TestDataFactory.createTestUserProfile(
            userId = "test-123",
            fullName = "John Doe",
            age = 25,
            gender = Gender.MALE
        )
        
        // Setup mocks
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(
            StudentProfileUiState(
                userName = "John Doe",
                userEmail = "john@example.com",
                isLoading = false
            )
        )
        every { mockViewModel.uiState } returns uiStateFlow
    }

    @Test
    fun profileScreen_displaysUserName() {
        // Given: Profile loaded
        composeTestRule.setContent {
            StudentProfileScreen(viewModel = mockViewModel)
        }

        // Then: User name should be displayed
        composeTestRule
            .onNodeWithText("John Doe")
            .assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysUserInfo() {
        // Given: Profile loaded with stats
        uiStateFlow.value = uiStateFlow.value.copy(
            totalTestsAttempted = 10,
            streakDays = 5
        )
        
        composeTestRule.setContent {
            StudentProfileScreen(viewModel = mockViewModel)
        }

        // Then: User info should be displayed
        composeTestRule
            .onNodeWithText("10", substring = true)
            .assertExists()
        
        composeTestRule
            .onNodeWithText("5", substring = true)
            .assertExists()
    }

    @Test
    fun profileScreen_showsLoadingState() {
        // Given: Loading state
        uiStateFlow.value = uiStateFlow.value.copy(
            isLoading = true,
            userName = ""
        )

        composeTestRule.setContent {
            StudentProfileScreen(viewModel = mockViewModel)
        }

        // Then: Loading indicator should be visible
        composeTestRule
            .onNodeWithText("Loading", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysEmail() {
        // Given: Profile loaded
        composeTestRule.setContent {
            StudentProfileScreen(viewModel = mockViewModel)
        }

        // Then: Email should be displayed
        composeTestRule
            .onNodeWithText("john@example.com", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileScreen_logoutButton_isVisible() {
        // Given: Profile loaded
        composeTestRule.setContent {
            StudentProfileScreen(viewModel = mockViewModel)
        }

        // Then: Logout button should be visible
        composeTestRule
            .onNodeWithText("Logout", substring = true)
            .assertIsDisplayed()
    }
}

