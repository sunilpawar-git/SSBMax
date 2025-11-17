package com.ssbmax.lint

import org.junit.Assert.*
import org.junit.Test

/**
 * Basic smoke tests for HardcodedTextDetector
 *
 * NOTE: Full lint detector testing requires Android lint test infrastructure.
 * These are simplified tests to verify the detector can be instantiated
 * and the issue is properly configured.
 *
 * For comprehensive lint testing, run: ./gradlew lintDebug
 */
class HardcodedTextDetectorTest {

    @Test
    fun `detector can be instantiated`() {
        val detector = HardcodedTextDetector()
        assertNotNull(detector)
    }

    @Test
    fun `issue is properly configured`() {
        val issue = HardcodedTextDetector.ISSUE

        assertEquals("ComposeHardcodedText", issue.id)
        assertNotNull(issue)
    }

    @Test
    fun `detector implements SourceCodeScanner`() {
        val detector = HardcodedTextDetector()
        val applicableTypes = detector.getApplicableUastTypes()

        assertNotNull(applicableTypes)
        assertTrue(applicableTypes?.isNotEmpty() == true)
    }
}
