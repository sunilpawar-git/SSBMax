package com.ssbmax.ui.components

import androidx.compose.ui.test.*
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.testing.BaseComposeTest
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

/**
 * UI tests for SubscriptionBadge and ProfileAvatarWithBadge components
 */
@HiltAndroidTest
class SubscriptionBadgeTest : BaseComposeTest() {

    @Test
    fun subscriptionBadge_displaysBasicTier() {
        // Given: FREE subscription type
        composeTestRule.setContent {
            SubscriptionBadge(subscriptionType = SubscriptionType.FREE)
        }

        // Then: "Basic" badge should be displayed
        composeTestRule
            .onNodeWithText("Basic")
            .assertIsDisplayed()
    }

    @Test
    fun subscriptionBadge_displaysProTier() {
        // Given: PREMIUM_ASSESSOR subscription type
        composeTestRule.setContent {
            SubscriptionBadge(subscriptionType = SubscriptionType.PREMIUM_ASSESSOR)
        }

        // Then: "Pro" badge should be displayed
        composeTestRule
            .onNodeWithText("Pro")
            .assertIsDisplayed()
    }

    @Test
    fun subscriptionBadge_displaysAIPremiumTier() {
        // Given: PREMIUM_AI subscription type
        composeTestRule.setContent {
            SubscriptionBadge(subscriptionType = SubscriptionType.PREMIUM_AI)
        }

        // Then: "AI" badge should be displayed
        composeTestRule
            .onNodeWithText("AI")
            .assertIsDisplayed()
    }

    @Test
    fun profileAvatarWithBadge_showsBadgeWhenSubscriptionExists() {
        // Given: Profile avatar with FREE subscription
        composeTestRule.setContent {
            ProfileAvatarWithBadge(
                initials = "TU",
                subscriptionType = SubscriptionType.FREE
            )
        }

        // Then: Badge should be displayed
        composeTestRule
            .onNodeWithText("Basic")
            .assertIsDisplayed()
    }

    @Test
    fun profileAvatarWithBadge_hidesBadgeWhenSubscriptionNull() {
        // Given: Profile avatar with null subscription
        composeTestRule.setContent {
            ProfileAvatarWithBadge(
                initials = "TU",
                subscriptionType = null
            )
        }

        // Then: No badge should be displayed
        composeTestRule
            .onNodeWithText("Basic")
            .assertDoesNotExist()
        
        composeTestRule
            .onNodeWithText("Pro")
            .assertDoesNotExist()
        
        composeTestRule
            .onNodeWithText("AI")
            .assertDoesNotExist()
    }

    @Test
    fun profileAvatarWithBadge_displaysInitials() {
        // Given: Profile avatar with initials "JD"
        composeTestRule.setContent {
            ProfileAvatarWithBadge(
                initials = "JD",
                subscriptionType = SubscriptionType.PREMIUM_ASSESSOR
            )
        }

        // Then: Initials should be displayed
        composeTestRule
            .onNodeWithText("JD")
            .assertIsDisplayed()
    }

    @Test
    fun profileAvatarWithBadge_displaysBothInitialsAndBadge() {
        // Given: Profile avatar with initials and subscription
        composeTestRule.setContent {
            ProfileAvatarWithBadge(
                initials = "AP",
                subscriptionType = SubscriptionType.PREMIUM_AI
            )
        }

        // Then: Both initials and badge should be displayed
        composeTestRule
            .onNodeWithText("AP")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("AI")
            .assertIsDisplayed()
    }
}

