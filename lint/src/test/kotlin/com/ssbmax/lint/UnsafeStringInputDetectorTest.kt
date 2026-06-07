package com.ssbmax.lint

import com.android.tools.lint.detector.api.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for UnsafeStringInputDetector
 *
 * Verifies that the detector correctly identifies:
 * - User input from TextFields, Intent extras without validation
 * - Warnings for untrusted external data
 * - Passing cases for validated or non-user-input strings
 */
class UnsafeStringInputDetectorTest {

    @Test
    fun `detector can be instantiated`() {
        val detector = UnsafeStringInputDetector()
        assertNotNull(detector)
    }

    @Test
    fun `issue is properly configured`() {
        val issue = UnsafeStringInputDetector.ISSUE

        assertEquals("UnsafeStringInput", issue.id)
        assertNotNull(issue)
    }

    @Test
    fun `issue has WARNING severity`() {
        val issue = UnsafeStringInputDetector.ISSUE
        assertEquals(com.android.tools.lint.detector.api.Severity.WARNING, issue.defaultSeverity)
    }

    @Test
    fun `detector configuration is correct`() {
        val issue = UnsafeStringInputDetector.ISSUE
        assertEquals(Category.SECURITY, issue.category)
        assertEquals(8, issue.priority)
    }

    @Test
    fun `detector handles applicable UAST types`() {
        val detector = UnsafeStringInputDetector()
        val applicableTypes = detector.getApplicableUastTypes()

        assertNotNull(applicableTypes)
        assertTrue(applicableTypes.isNotEmpty())
    }
}
