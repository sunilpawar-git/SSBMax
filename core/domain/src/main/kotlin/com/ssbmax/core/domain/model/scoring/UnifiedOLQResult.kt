package com.ssbmax.core.domain.model.scoring

import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore

/**
 * Unified OLQ analysis result for all tests (Psychology, GTO, Interview)
 */
data class OLQAnalysisResult(
    val submissionId: String,
    val testType: TestType,
    val olqScores: Map<OLQ, OLQScore>,
    val overallScore: Float,  // 1-10 SSB scale (lower is better)
    val overallRating: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val recommendations: List<String>,
    val analyzedAt: Long,
    val aiConfidence: Int  // 0-100
)

/**
 * Analysis status for test submissions
 */
enum class AnalysisStatus {
    PENDING_ANALYSIS,
    ANALYZING,
    COMPLETED,
    FAILED
}
