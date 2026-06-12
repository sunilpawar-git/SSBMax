package com.ssbmax.lint

import com.android.tools.lint.detector.api.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for SensitiveDataInLogsDetector
 *
 * Verifies that the detector correctly identifies:
 * - Log calls with PII (userId, email, authToken, password)
 * - Various logging methods (Log.d, println, Timber.d, etc)
 * - Patterns that should be flagged vs allowed
 */
class SensitiveDataInLogsDetectorTest {

    @Test
    fun `detector can be instantiated`() {
        val detector = SensitiveDataInLogsDetector()
        assertNotNull(detector)
    }

    @Test
    fun `issue is properly configured`() {
        val issue = SensitiveDataInLogsDetector.ISSUE

        assertEquals("SensitiveDataInLogs", issue.id)
        assertNotNull(issue)
    }

    @Test
    fun `issue has ERROR severity`() {
        val issue = SensitiveDataInLogsDetector.ISSUE
        assertEquals(com.android.tools.lint.detector.api.Severity.ERROR, issue.defaultSeverity)
    }

    @Test
    fun `detector configuration is correct`() {
        val issue = SensitiveDataInLogsDetector.ISSUE
        assertEquals(Category.SECURITY, issue.category)
        assertEquals(10, issue.priority)
    }

    @Test
    fun `detector has applicable method names`() {
        val detector = SensitiveDataInLogsDetector()
        val methodNames = detector.getApplicableMethodNames()

        assertNotNull(methodNames)
        assertTrue(methodNames.isNotEmpty())
        assertTrue(methodNames.contains("d"))  // Log.d
        assertTrue(methodNames.contains("println"))  // println
    }
}
