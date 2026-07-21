package com.ssbmax.testing

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule

/**
 * Base class for Compose UI tests.
 *
 * Previously used Hilt's `@HiltAndroidTest`/`HiltAndroidRule` to inject the
 * test application before each test. Koin has no per-test injection rule
 * equivalent for this app's setup — `SSBMaxApplication.onCreate()` already
 * calls `startKoin()` unconditionally (see [com.ssbmax.SSBMaxApplication]),
 * so the instrumented test process's real Application instance brings up
 * the same Koin graph production does, with no extra rule needed here.
 */
abstract class BaseComposeTest {

    /**
     * Compose test rule for UI testing
     */
    @get:Rule(order = 0)
    val composeTestRule = createComposeRule()
}
