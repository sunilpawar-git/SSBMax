package com.ssbmax.ui.components.drawer

import androidx.compose.ui.test.*
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.testing.BaseComposeTest
import com.ssbmax.testing.TestDataFactory
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

/**
 * UI tests for DrawerHeader component with subscription badges
 */
@HiltAndroidTest
class DrawerHeaderTest : BaseComposeTest() {

    @Test
    fun drawerHeader_showsBasicBadge_forFreeUser() {
        // Given: User with FREE subscription
        val userProfile = TestDataFactory.createFreeUser(
            fullName = "John Doe",
            age = 25
        )

        composeTestRule.setContent {
            DrawerHeader(
                userProfile = userProfile,
                isLoading = false,
                onEditProfile = {}
            )
        }

        // Then: User info should be displayed
        composeTestRule
            .onNodeWithText("John Doe")
            .assertIsDisplayed()

        // And: "Basic" badge should be displayed
        composeTestRule
            .onNodeWithText("Basic")
            .assertIsDisplayed()
    }

    @Test
    fun drawerHeader_showsProBadge_forPremiumAssessor() {
        // Given: User with PREMIUM_ASSESSOR subscription
        val userProfile = TestDataFactory.createProUser(
            fullName = "Jane Smith",
            age = 28
        )

        composeTestRule.setContent {
            DrawerHeader(
                userProfile = userProfile,
                isLoading = false,
                onEditProfile = {}
            )
        }

        // Then: User info should be displayed
        composeTestRule
            .onNodeWithText("Jane Smith")
            .assertIsDisplayed()

        // And: "Pro" badge should be displayed
        composeTestRule
            .onNodeWithText("Pro")
            .assertIsDisplayed()
    }

    @Test
    fun drawerHeader_showsAIBadge_forPremiumAI() {
        // Given: User with PREMIUM_AI subscription
        val userProfile = TestDataFactory.createAIPremiumUser(
            fullName = "Alex Kumar",
            age = 26
        )

        composeTestRule.setContent {
            DrawerHeader(
                userProfile = userProfile,
                isLoading = false,
                onEditProfile = {}
            )
        }

        // Then: User info should be displayed
        composeTestRule
            .onNodeWithText("Alex Kumar")
            .assertIsDisplayed()

        // And: "AI" badge should be displayed
        composeTestRule
            .onNodeWithText("AI")
            .assertIsDisplayed()
    }

    @Test
    fun drawerHeader_noBadge_whenNoProfile() {
        // Given: No user profile
        composeTestRule.setContent {
            DrawerHeader(
                userProfile = null,
                isLoading = false,
                onEditProfile = {}
            )
        }

        // Then: No badges should be displayed
        composeTestRule
            .onNodeWithText("Basic")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("Pro")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("AI")
            .assertDoesNotExist()

        // And: Prompt to complete profile should be shown
        composeTestRule
            .onNodeWithText("Complete Your Profile")
            .assertIsDisplayed()
    }

    @Test
    fun drawerHeader_noBadge_whenLoading() {
        // Given: Loading state
        composeTestRule.setContent {
            DrawerHeader(
                userProfile = null,
                isLoading = true,
                onEditProfile = {}
            )
        }

        // Then: Loading message should be displayed
        composeTestRule
            .onNodeWithText("Loading profile...")
            .assertIsDisplayed()

        // And: No badges should be displayed
        composeTestRule
            .onNodeWithText("Basic")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("Pro")
            .assertDoesNotExist()
    }

    @Test
    fun drawerHeader_showsUserInfo_withBadge() {
        // Given: Complete user profile with subscription
        val userProfile = TestDataFactory.createTestUserProfile(
            fullName = "Sarah Connor",
            age = 30,
            subscriptionType = SubscriptionType.PREMIUM
        )

        composeTestRule.setContent {
            DrawerHeader(
                userProfile = userProfile,
                isLoading = false,
                onEditProfile = {}
            )
        }

        // Then: All user information should be displayed
        composeTestRule
            .onNodeWithText("Sarah Connor")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("30 years", substring = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Male", substring = true)
            .assertIsDisplayed()

        // And: Subscription badge should be displayed
        composeTestRule
            .onNodeWithText("AI")
            .assertIsDisplayed()

        // And: Edit button should be present
        composeTestRule
            .onNodeWithText("Edit Profile")
            .assertIsDisplayed()
    }

    @Test
    fun drawerHeader_editButton_isClickable() {
        // Given: User profile with edit callback
        var editClicked = false
        val userProfile = TestDataFactory.createProUser(
            fullName = "Test User"
        )

        composeTestRule.setContent {
            DrawerHeader(
                userProfile = userProfile,
                isLoading = false,
                onEditProfile = { editClicked = true }
            )
        }

        // When: Click edit button
        composeTestRule
            .onNodeWithText("Edit Profile")
            .performClick()

        // Then: Callback should be invoked
        composeTestRule.waitForIdle()
        assert(editClicked) { "Edit profile callback should be called" }
    }

    @Test
    fun drawerHeader_showsInitials_whenProfileExists() {
        // Given: User profile
        val userProfile = TestDataFactory.createFreeUser(
            fullName = "Bob Anderson"
        )

        composeTestRule.setContent {
            DrawerHeader(
                userProfile = userProfile,
                isLoading = false,
                onEditProfile = {}
            )
        }

        // Then: User initials should be displayed
        composeTestRule
            .onNodeWithText("BA")
            .assertIsDisplayed()

        // And: Badge should be overlaid on avatar
        composeTestRule
            .onNodeWithText("Basic")
            .assertIsDisplayed()
    }
}

