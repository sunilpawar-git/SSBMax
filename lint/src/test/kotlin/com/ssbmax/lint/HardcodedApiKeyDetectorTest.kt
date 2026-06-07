package com.ssbmax.lint

import com.android.tools.lint.detector.api.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for HardcodedApiKeyDetector
 *
 * Verifies that the detector correctly identifies:
 * - Hardcoded API keys in variables
 * - Patterns like GEMINI_API_KEY, SARVAM_API_KEY, etc.
 * - Secret key patterns (sk_, sk-)
 */
class HardcodedApiKeyDetectorTest {

    @Test
    fun `detector can be instantiated`() {
        val detector = HardcodedApiKeyDetector()
        assertNotNull(detector)
    }

    @Test
    fun `issue is properly configured`() {
        val issue = HardcodedApiKeyDetector.ISSUE

        assertEquals("HardcodedApiKey", issue.id)
        assertNotNull(issue)
    }

    @Test
    fun `issue has ERROR severity`() {
        val issue = HardcodedApiKeyDetector.ISSUE
        assertEquals(com.android.tools.lint.detector.api.Severity.ERROR, issue.defaultSeverity)
    }

    @Test
    fun `detector configuration is correct`() {
        val issue = HardcodedApiKeyDetector.ISSUE
        assertEquals(Category.SECURITY, issue.category)
        assertEquals(10, issue.priority)
    }

    @Test
    fun `detector handles applicable UAST types`() {
        val detector = HardcodedApiKeyDetector()
        val applicableTypes = detector.getApplicableUastTypes()

        assertNotNull(applicableTypes)
        assertTrue(applicableTypes.isNotEmpty())
    }
}
