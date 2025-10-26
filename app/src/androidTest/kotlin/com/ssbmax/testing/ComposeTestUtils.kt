package com.ssbmax.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput

/**
 * Extension functions for Compose UI testing
 */

/**
 * Performs click on node with text, scrolling to it if needed
 */
fun ComposeContentTestRule.clickText(text: String, substring: Boolean = false) {
    onNodeWithText(text, substring = substring)
        .performScrollTo()
        .performClick()
}

/**
 * Performs click on node with tag, scrolling to it if needed
 */
fun ComposeContentTestRule.clickTag(tag: String) {
    onNodeWithTag(tag)
        .performScrollTo()
        .performClick()
}

/**
 * Enters text in node with tag
 */
fun ComposeContentTestRule.enterText(tag: String, text: String) {
    onNodeWithTag(tag)
        .performScrollTo()
        .performTextInput(text)
}

/**
 * Enters text in node with text label
 */
fun ComposeContentTestRule.enterTextInField(label: String, text: String) {
    onNodeWithText(label)
        .performScrollTo()
        .performTextInput(text)
}

/**
 * Waits for a condition to be true
 */
fun ComposeContentTestRule.waitUntil(
    timeoutMillis: Long = 5000L,
    condition: () -> Boolean
) {
    val startTime = System.currentTimeMillis()
    while (!condition() && System.currentTimeMillis() - startTime < timeoutMillis) {
        Thread.sleep(100)
    }
    if (!condition()) {
        throw AssertionError("Condition not met within ${timeoutMillis}ms")
    }
}

/**
 * Waits for node with text to appear
 */
fun ComposeContentTestRule.waitForText(
    text: String,
    substring: Boolean = false,
    timeoutMillis: Long = 5000L
) {
    waitUntil(timeoutMillis) {
        this.onAllNodes(androidx.compose.ui.test.hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Waits for node with tag to appear
 */
fun ComposeContentTestRule.waitForTag(
    tag: String,
    timeoutMillis: Long = 5000L
) {
    waitUntil(timeoutMillis) {
        this.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }
}

