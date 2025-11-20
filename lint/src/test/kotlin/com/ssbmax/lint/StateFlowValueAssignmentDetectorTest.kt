package com.ssbmax.lint

import org.junit.Assert.*
import org.junit.Test

/**
 * Basic smoke tests for StateFlowValueAssignmentDetector
 *
 * NOTE: Full lint detector testing requires Android lint test infrastructure.
 * These are simplified tests to verify the detector can be instantiated
 * and the issue is properly configured.
 *
 * For comprehensive lint testing, run: ./gradle.sh :app:lintDebug
 * The lint rule will catch actual .value = .value.copy() usage in the real codebase.
 */
class StateFlowValueAssignmentDetectorTest {

    @Test
    fun `detector can be instantiated`() {
        val detector = StateFlowValueAssignmentDetector()
        assertNotNull(detector)
    }

    @Test
    fun `issue is properly configured`() {
        val issue = StateFlowValueAssignmentDetector.ISSUE

        assertEquals("StateFlowValueAssignment", issue.id)
        assertEquals("Use .update {} instead of .value = .value.copy()", issue.getBriefDescription(com.android.tools.lint.detector.api.TextFormat.TEXT))
        assertNotNull(issue)
    }

    @Test
    fun `detector implements SourceCodeScanner`() {
        val detector = StateFlowValueAssignmentDetector()
        val applicableUastTypes = detector.getApplicableUastTypes()

        assertNotNull(applicableUastTypes)
        assertTrue(applicableUastTypes?.isNotEmpty() == true)
    }

    @Test
    fun `issue has ERROR severity`() {
        val issue = StateFlowValueAssignmentDetector.ISSUE

        // Verify it's an ERROR (will fail the build)
        assertEquals(com.android.tools.lint.detector.api.Severity.ERROR, issue.defaultSeverity)
    }

    @Test
    fun `issue has high priority`() {
        val issue = StateFlowValueAssignmentDetector.ISSUE

        // Priority 8 = high priority (thread safety is critical)
        assertEquals(8, issue.priority)
    }

    @Test
    fun `issue category is CORRECTNESS`() {
        val issue = StateFlowValueAssignmentDetector.ISSUE

        assertEquals(com.android.tools.lint.detector.api.Category.CORRECTNESS, issue.category)
    }
}

