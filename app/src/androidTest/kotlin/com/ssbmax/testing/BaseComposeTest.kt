package com.ssbmax.testing

import androidx.compose.ui.test.junit4.createComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule

/**
 * Base class for Compose UI tests with Hilt support
 */
@HiltAndroidTest
abstract class BaseComposeTest {

    /**
     * Hilt rule must be first to ensure proper DI setup
     */
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    /**
     * Compose test rule for UI testing
     */
    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    /**
     * Setup method called before each test
     */
    @Before
    open fun setup() {
        hiltRule.inject()
    }
}

