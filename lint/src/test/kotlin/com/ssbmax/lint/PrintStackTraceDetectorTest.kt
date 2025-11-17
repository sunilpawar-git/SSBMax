package com.ssbmax.lint

import org.junit.Assert.*
import org.junit.Test

/**
 * Basic smoke tests for PrintStackTraceDetector
 *
 * NOTE: Full lint detector testing requires Android lint test infrastructure.
 * These are simplified tests to verify the detector can be instantiated
 * and the issue is properly configured.
 *
 * For comprehensive lint testing, run: ./gradlew lintDebug
 * The lint rule will catch actual printStackTrace() usage in the real codebase.
 */
class PrintStackTraceDetectorTest {

    @Test
    fun `detector can be instantiated`() {
        val detector = PrintStackTraceDetector()
        assertNotNull(detector)
    }

    @Test
    fun `issue is properly configured`() {
        val issue = PrintStackTraceDetector.ISSUE

        assertEquals("PrintStackTraceUsage", issue.id)
        assertNotNull(issue)
    }

    @Test
    fun `detector implements SourceCodeScanner`() {
        val detector = PrintStackTraceDetector()
        val applicableMethodNames = detector.getApplicableMethodNames()

        assertNotNull(applicableMethodNames)
        assertTrue(applicableMethodNames?.isNotEmpty() == true)
        assertTrue(applicableMethodNames?.contains("printStackTrace") == true)
    }

    @Test
    fun `issue has ERROR severity`() {
        val issue = PrintStackTraceDetector.ISSUE

        // Verify it's an ERROR (will fail the build)
        assertEquals(com.android.tools.lint.detector.api.Severity.ERROR, issue.defaultSeverity)
    }

    @Test
    fun `issue has high priority`() {
        val issue = PrintStackTraceDetector.ISSUE

        // Priority 9 = high priority
        assertEquals(9, issue.priority)
    }
}
