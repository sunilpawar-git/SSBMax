package com.ssbmax.shared.presentation.gto.common

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.gto.GTOResult
import com.ssbmax.shared.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult

/**
 * GTO's data layer (ported in Phase 2) predates the [com.ssbmax.shared.ui.components.result.UnifiedOLQResultTemplate]
 * contract introduced later in Phase 5 for PPDT/TAT/WAT/SRT/SDT -- it uses its
 * own [GTOSubmissionStatus]/[GTOResult] shapes (a separate `getTestResult`
 * lookup, not an embedded `olqResult` on the submission) rather than the
 * generic-psych-test [AnalysisStatus]/[OLQAnalysisResult] pair. Both shapes
 * carry the same real information (same 4-state lifecycle, same OLQ score
 * map + overall score/rating/confidence + strengths/weaknesses/recommendations),
 * so rather than reshaping the GTO repository (out of this session's scope --
 * it's shared by 8 GTO test types, not just GD/Lecturette), these two mapping
 * functions let GD/Lecturette (and future GPE/PGT/etc.) result screens reuse
 * the same [UnifiedOLQResultTemplate] every other test-type result screen
 * already uses.
 */
fun GTOSubmissionStatus.toAnalysisStatus(): AnalysisStatus = when (this) {
    GTOSubmissionStatus.PENDING_ANALYSIS -> AnalysisStatus.PENDING_ANALYSIS
    GTOSubmissionStatus.ANALYZING -> AnalysisStatus.ANALYZING
    GTOSubmissionStatus.COMPLETED -> AnalysisStatus.COMPLETED
    GTOSubmissionStatus.FAILED -> AnalysisStatus.FAILED
}

fun GTOResult.toOLQAnalysisResult(testType: TestType): OLQAnalysisResult = OLQAnalysisResult(
    submissionId = submissionId,
    testType = testType,
    olqScores = olqScores,
    overallScore = overallScore,
    overallRating = overallRating,
    strengths = strengths,
    weaknesses = weaknesses,
    recommendations = recommendations,
    analyzedAt = analyzedAt,
    aiConfidence = aiConfidence
)
