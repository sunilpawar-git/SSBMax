package com.ssbmax.core.domain.usecase.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ProcessedDashboardData.empty() factory.
 *
 * SSOT contract: every caller that needs a pre-fetch/empty dashboard state must use
 * ProcessedDashboardData.empty(). These tests pin that contract so no caller
 * can silently diverge from it.
 */
class ProcessedDashboardDataTest {

    // ── Domain contract ──────────────────────────────────────────────────────

    @Test
    fun `empty() completedTestsCount is zero`() {
        assertEquals(0, ProcessedDashboardData.empty().dashboard.completedTestsCount)
    }

    @Test
    fun `empty() totalTests is 15`() {
        assertEquals(15, ProcessedDashboardData.empty().dashboard.totalTests)
    }

    @Test
    fun `empty() overallAverageScore is null`() {
        assertNull(ProcessedDashboardData.empty().overallAverageScore)
    }

    @Test
    fun `empty() topOLQs is empty`() {
        assertTrue(ProcessedDashboardData.empty().topOLQs.isEmpty())
    }

    @Test
    fun `empty() improvementOLQs is empty`() {
        assertTrue(ProcessedDashboardData.empty().improvementOLQs.isEmpty())
    }

    @Test
    fun `empty() averageOLQScores is empty`() {
        assertTrue(ProcessedDashboardData.empty().averageOLQScores.isEmpty())
    }

    // ── Phase 1 results ──────────────────────────────────────────────────────

    @Test
    fun `empty() phase1 oirResult is null`() {
        assertNull(ProcessedDashboardData.empty().dashboard.phase1Results.oirResult)
    }

    @Test
    fun `empty() phase1 ppdtResult is null`() {
        assertNull(ProcessedDashboardData.empty().dashboard.phase1Results.ppdtResult)
    }

    @Test
    fun `empty() phase1 ppdtOLQResult is null`() {
        assertNull(ProcessedDashboardData.empty().dashboard.phase1Results.ppdtOLQResult)
    }

    // ── Phase 2 results ──────────────────────────────────────────────────────

    @Test
    fun `empty() phase2 tatResult is null`() {
        assertNull(ProcessedDashboardData.empty().dashboard.phase2Results.tatResult)
    }

    @Test
    fun `empty() phase2 watResult is null`() {
        assertNull(ProcessedDashboardData.empty().dashboard.phase2Results.watResult)
    }

    @Test
    fun `empty() phase2 srtResult is null`() {
        assertNull(ProcessedDashboardData.empty().dashboard.phase2Results.srtResult)
    }

    @Test
    fun `empty() phase2 sdResult is null`() {
        assertNull(ProcessedDashboardData.empty().dashboard.phase2Results.sdResult)
    }

    @Test
    fun `empty() phase2 gtoResults is empty map`() {
        assertTrue(ProcessedDashboardData.empty().dashboard.phase2Results.gtoResults.isEmpty())
    }

    @Test
    fun `empty() phase2 interviewResult is null`() {
        assertNull(ProcessedDashboardData.empty().dashboard.phase2Results.interviewResult)
    }

    // ── Idempotency ──────────────────────────────────────────────────────────

    @Test
    fun `empty() is idempotent - two calls return structurally equal objects`() {
        assertEquals(ProcessedDashboardData.empty(), ProcessedDashboardData.empty())
    }

    @Test
    fun `empty() progressPercentage is zero`() {
        assertEquals(0f, ProcessedDashboardData.empty().dashboard.progressPercentage, 0.001f)
    }
}
