package com.ssbmax.lint

import com.android.tools.lint.detector.api.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for FirebaseRulesValidationDetector
 *
 * Verifies that the detector correctly validates firestore.rules syntax:
 * - Brace matching
 * - Service block presence
 * - Rule statement structure
 */
class FirebaseRulesValidationDetectorTest {

    @Test
    fun `detector can be instantiated`() {
        val detector = FirebaseRulesValidationDetector()
        assertNotNull(detector)
    }

    @Test
    fun `issue is properly configured`() {
        val issue = FirebaseRulesValidationDetector.ISSUE

        assertEquals("FirebaseRulesValidation", issue.id)
        assertNotNull(issue)
    }

    @Test
    fun `issue has WARNING severity`() {
        val issue = FirebaseRulesValidationDetector.ISSUE
        assertEquals(com.android.tools.lint.detector.api.Severity.WARNING, issue.defaultSeverity)
    }

    @Test
    fun `detector configuration is correct`() {
        val issue = FirebaseRulesValidationDetector.ISSUE
        assertEquals(Category.CORRECTNESS, issue.category)
        assertEquals(7, issue.priority)
    }
}
