package com.ssbmax.shared.ui.components.drawer

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.ssbmax.shared.domain.model.EntryType
import com.ssbmax.shared.domain.model.Gender
import com.ssbmax.shared.domain.model.SubscriptionType
import com.ssbmax.shared.domain.model.UserProfile
import com.ssbmax.shared.testing.ensureComposeResourcesContextInitialized
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Port of the pre-KMP-convergence
 * `app/src/androidTest/.../ui/components/drawer/DrawerHeaderTest.kt` onto
 * shared's [DrawerHeader] (Phase 6a of the KMP-convergence plan). Pure
 * presentational composable, no ViewModel/Koin.
 *
 * Real drift from the pre-cutover original, verified against `strings.xml`
 * rather than assumed: badge labels are "Free"/"Pro"/"Premium" now, not
 * "Basic"/"Pro"/"AI" (same drift [SubscriptionBadgeUiTest] documents). The
 * old test's `TestDataFactory` (`app/src/androidTest/testing/`) isn't
 * ported wholesale -- only the two builders this file needs are inlined
 * below, since it's the only Phase 6a test that needs [UserProfile] fixtures.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class DrawerHeaderUiTest {

    @Before
    fun setup() {
        ensureComposeResourcesContextInitialized()
    }

    @Test
    fun drawerHeader_showsFreeBadge_forFreeUser() = runComposeUiTest {
        val userProfile = testUserProfile(fullName = "John Doe", age = 25, subscriptionType = SubscriptionType.FREE)

        setContent { DrawerHeader(userProfile = userProfile, isLoading = false, onEditProfile = {}) }

        onNodeWithText("John Doe").assertIsDisplayed()
        onNodeWithText("Free").assertIsDisplayed()
    }

    @Test
    fun drawerHeader_showsProBadge_forProUser() = runComposeUiTest {
        val userProfile = testUserProfile(fullName = "Jane Smith", age = 28, subscriptionType = SubscriptionType.PRO)

        setContent { DrawerHeader(userProfile = userProfile, isLoading = false, onEditProfile = {}) }

        onNodeWithText("Jane Smith").assertIsDisplayed()
        onNodeWithText("Pro").assertIsDisplayed()
    }

    @Test
    fun drawerHeader_showsPremiumBadge_forPremiumUser() = runComposeUiTest {
        val userProfile = testUserProfile(fullName = "Alex Kumar", age = 26, subscriptionType = SubscriptionType.PREMIUM)

        setContent { DrawerHeader(userProfile = userProfile, isLoading = false, onEditProfile = {}) }

        onNodeWithText("Alex Kumar").assertIsDisplayed()
        onNodeWithText("Premium").assertIsDisplayed()
    }

    @Test
    fun drawerHeader_noBadge_whenNoProfile() = runComposeUiTest {
        setContent { DrawerHeader(userProfile = null, isLoading = false, onEditProfile = {}) }

        onNodeWithText("Free").assertDoesNotExist()
        onNodeWithText("Pro").assertDoesNotExist()
        onNodeWithText("Premium").assertDoesNotExist()
        onNodeWithText("Complete Your Profile").assertIsDisplayed()
    }

    @Test
    fun drawerHeader_noBadge_whenLoading() = runComposeUiTest {
        setContent { DrawerHeader(userProfile = null, isLoading = true, onEditProfile = {}) }

        onNodeWithText("Loading profile...").assertIsDisplayed()
        onNodeWithText("Free").assertDoesNotExist()
        onNodeWithText("Pro").assertDoesNotExist()
    }

    @Test
    fun drawerHeader_showsUserInfo_withBadge() = runComposeUiTest {
        val userProfile = testUserProfile(
            fullName = "Sarah Connor",
            age = 30,
            gender = Gender.FEMALE,
            subscriptionType = SubscriptionType.PREMIUM
        )

        setContent { DrawerHeader(userProfile = userProfile, isLoading = false, onEditProfile = {}) }

        onNodeWithText("Sarah Connor").assertIsDisplayed()
        onNodeWithText("30 years", substring = true).assertIsDisplayed()
        onNodeWithText("Female", substring = true).assertIsDisplayed()
        onNodeWithText("Premium").assertIsDisplayed()
        onNodeWithText("Edit Profile").assertIsDisplayed()
    }

    @Test
    fun drawerHeader_editButton_isClickable() = runComposeUiTest {
        var editClicked = false
        val userProfile = testUserProfile(fullName = "Test User", subscriptionType = SubscriptionType.PRO)

        setContent {
            DrawerHeader(userProfile = userProfile, isLoading = false, onEditProfile = { editClicked = true })
        }

        onNodeWithText("Edit Profile").performClick()
        waitForIdle()

        assert(editClicked) { "Edit profile callback should be called" }
    }

    @Test
    fun drawerHeader_showsInitials_whenProfileExists() = runComposeUiTest {
        val userProfile = testUserProfile(fullName = "Bob Anderson", subscriptionType = SubscriptionType.FREE)

        setContent { DrawerHeader(userProfile = userProfile, isLoading = false, onEditProfile = {}) }

        onNodeWithText("BA").assertIsDisplayed()
        onNodeWithText("Free").assertIsDisplayed()
    }

    private fun testUserProfile(
        fullName: String,
        age: Int = 25,
        gender: Gender = Gender.MALE,
        subscriptionType: SubscriptionType = SubscriptionType.FREE
    ) = UserProfile(
        userId = "test-user-123",
        fullName = fullName,
        age = age,
        gender = gender,
        entryType = EntryType.ENTRY_10_PLUS_2,
        subscriptionType = subscriptionType
    )
}
