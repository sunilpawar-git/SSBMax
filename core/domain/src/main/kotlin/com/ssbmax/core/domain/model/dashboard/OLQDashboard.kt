package com.ssbmax.core.domain.model.dashboard

import com.ssbmax.core.domain.model.OIRTestResult
import com.ssbmax.core.domain.model.PPDTSubmission
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult

/**
 * Unified dashboard data showing all test results
 * Two-column layout: Phase 1 (OIR, PPDT) | Phase 2 (Psychology, GTO, Interview)
 */
data class OLQDashboardData(
    val userId: String,
    val phase1Results: Phase1Results,
    val phase2Results: Phase2Results
) {
    /**
     * Phase 1 SSB Tests (Day 1)
     * - OIR: Officer Intelligence Rating
     * - PPDT: Picture Perception & Description Test
     */
    data class Phase1Results(
        val oirResult: OIRTestResult?,
        val ppdtResult: PPDTSubmission?,  // Full submission for UI details
        val ppdtOLQResult: OLQAnalysisResult?  // Extracted OLQ scores for aggregation
    )

    /**
     * Phase 2 SSB Tests (Days 2-5)
     * - Psychology Tests: TAT, WAT, SRT, Self Description
     * - GTO Tests: 8 tests (GD, GPE, Lecturette, PGT, HGT, GOR, IO, CT)
     * - Interview
     */
    data class Phase2Results(
        val tatResult: OLQAnalysisResult?,
        val watResult: OLQAnalysisResult?,
        val srtResult: OLQAnalysisResult?,
        val sdResult: OLQAnalysisResult?,
        val gtoResults: Map<GTOTestType, OLQAnalysisResult>,
        val interviewResult: InterviewResult?
    )

    // NOTE: Heavy calculations moved to GetOLQDashboardUseCase
    // This data model is now a simple DTO

    /**
     * Count of completed tests
     */
    val completedTestsCount: Int
        get() {
            var count = 0
            if (phase1Results.oirResult != null) count++
            if (phase1Results.ppdtOLQResult != null) count++  // Count only when OLQ analyzed
            if (phase2Results.tatResult != null) count++
            if (phase2Results.watResult != null) count++
            if (phase2Results.srtResult != null) count++
            if (phase2Results.sdResult != null) count++
            count += phase2Results.gtoResults.size
            if (phase2Results.interviewResult != null) count++
            return count
        }

    /**
     * Total available tests (2 Phase 1 + 4 Psychology + 8 GTO + 1 Interview = 15)
     */
    val totalTests: Int = 15

    /**
     * Progress percentage
     */
    val progressPercentage: Float
        get() = (completedTestsCount.toFloat() / totalTests.toFloat()) * 100
}
