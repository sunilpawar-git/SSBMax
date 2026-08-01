package com.ssbmax.shared.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.testing.ensureComposeResourcesContextInitialized
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Port of the pre-KMP-convergence
 * `app/src/androidTest/.../ui/components/SubscriptionBadgeTest.kt` onto
 * shared's [SubscriptionBadge]/[ProfileAvatarWithBadge] (Phase 6a of the
 * KMP-convergence plan). Pure presentational composables, no ViewModel/Koin.
 *
 * Real label drift from the pre-cutover original, verified against
 * `strings.xml` rather than assumed: `SubscriptionType.FREE` now renders
 * "Free" (`subscription_badge_free`), not the old hardcoded "Basic"; `PREMIUM`
 * renders "Premium" (`subscription_badge_premium`), not the old "AI". `PRO`
 * still renders "Pro". The dropped `PREMIUM_AI`/`PREMIUM_ASSESSOR` cases from
 * the original (already commented out there) don't exist in the current
 * 3-value `SubscriptionType` enum (`FREE`/`PRO`/`PREMIUM`) -- nothing to port.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class SubscriptionBadgeUiTest {

    @Before
    fun setup() {
        ensureComposeResourcesContextInitialized()
    }

    @Test
    fun subscriptionBadge_displaysFreeTier() = runComposeUiTest {
        setContent { SubscriptionBadge(subscriptionType = SubscriptionType.FREE) }

        onNodeWithText("Free").assertIsDisplayed()
    }

    @Test
    fun subscriptionBadge_displaysProTier() = runComposeUiTest {
        setContent { SubscriptionBadge(subscriptionType = SubscriptionType.PRO) }

        onNodeWithText("Pro").assertIsDisplayed()
    }

    @Test
    fun subscriptionBadge_displaysPremiumTier() = runComposeUiTest {
        setContent { SubscriptionBadge(subscriptionType = SubscriptionType.PREMIUM) }

        onNodeWithText("Premium").assertIsDisplayed()
    }

    @Test
    fun profileAvatarWithBadge_showsBadgeWhenSubscriptionExists() = runComposeUiTest {
        setContent {
            ProfileAvatarWithBadge(initials = "TU", subscriptionType = SubscriptionType.FREE)
        }

        onNodeWithText("Free").assertIsDisplayed()
    }

    @Test
    fun profileAvatarWithBadge_hidesBadgeWhenSubscriptionNull() = runComposeUiTest {
        setContent {
            ProfileAvatarWithBadge(initials = "TU", subscriptionType = null)
        }

        onNodeWithText("Free").assertDoesNotExist()
        onNodeWithText("Pro").assertDoesNotExist()
        onNodeWithText("Premium").assertDoesNotExist()
    }

    @Test
    fun profileAvatarWithBadge_displaysInitials() = runComposeUiTest {
        setContent {
            ProfileAvatarWithBadge(initials = "JD", subscriptionType = SubscriptionType.PRO)
        }

        onNodeWithText("JD").assertIsDisplayed()
    }
}
