package com.ssbmax.core.domain.usecase.dashboard

import com.ssbmax.core.domain.model.dashboard.OLQDashboardData
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubmissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processed dashboard data with pre-computed aggregations
 * Calculations are done ONCE in use case, not on every UI access
 */
data class ProcessedDashboardData(
    val dashboard: OLQDashboardData,
    val averageOLQScores: Map<OLQ, Float>,
    val topOLQs: List<Pair<OLQ, Float>>,
    val improvementOLQs: List<Pair<OLQ, Float>>,
    val overallAverageScore: Float?
)

/**
 * Use case to fetch and aggregate all test results for dashboard display
 * 
 * Fetches:
 * - Phase 1: OIR, PPDT
 * - Phase 2: TAT, WAT, SRT, SD (Psychology), 8 GTO tests, Interview
 * 
 * PERFORMANCE: All aggregations computed once, not on every UI access
 */
@Singleton
class GetOLQDashboardUseCase @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val gtoRepository: GTORepository,
    private val interviewRepository: InterviewRepository
) {
    /**
     * Fetch complete dashboard data with pre-computed aggregations
     * 
     * @param userId User ID to fetch dashboard for
     * @return Processed dashboard with all aggregations computed once
     */
    suspend operator fun invoke(userId: String): Result<ProcessedDashboardData> {
        return try {
            // Fetch Phase 1 results
            val oirResult = submissionRepository.getLatestOIRSubmission(userId)
                .getOrNull()
                ?.let { submission ->
                    // OIR has its own result structure, not OLQAnalysisResult
                    null // Keep as OIRSubmission
                }
            
            // Fetch PPDT submission and fetch OLQ result from ppdt_results collection (GTO pattern)
            // Important: Try fetching from ppdt_results first - the submission's analysisStatus may be stale
            val ppdtSubmission = submissionRepository.getLatestPPDTSubmission(userId)
                .getOrNull()
            
            // Directly try to fetch from ppdt_results - this collection is always fresh
            val ppdtOLQResult = ppdtSubmission?.let { 
                submissionRepository.getPPDTResult(it.submissionId).getOrNull() 
            }

            // Fetch Phase 2 Psychology test results (OLQ-based)
            val tatResult = getLatestCompletedOLQResult(userId, "TAT")
            val watResult = getLatestCompletedOLQResult(userId, "WAT")
            val srtResult = getLatestCompletedOLQResult(userId, "SRT")
            val sdResult = getLatestCompletedOLQResult(userId, "SDT")

            // Fetch GTO results (all 8 tests)
            val gtoResults = mutableMapOf<GTOTestType, OLQAnalysisResult>()
            
            GTOTestType.entries.forEach { testType ->
                gtoRepository.getUserResults(userId, testType)
                    .getOrNull()
                    ?.firstOrNull() // Get latest result
                    ?.let { gtoResult ->
                        // Convert GTOResult to OLQAnalysisResult
                        if (gtoResult.olqScores.isNotEmpty()) {
                            gtoResults[testType] = OLQAnalysisResult(
                                submissionId = gtoResult.submissionId,
                                testType = when (testType) {
                                    GTOTestType.GROUP_DISCUSSION -> com.ssbmax.core.domain.model.TestType.GTO_GD
                                    GTOTestType.GROUP_PLANNING_EXERCISE -> com.ssbmax.core.domain.model.TestType.GTO_GPE
                                    GTOTestType.LECTURETTE -> com.ssbmax.core.domain.model.TestType.GTO_LECTURETTE
                                    GTOTestType.PROGRESSIVE_GROUP_TASK -> com.ssbmax.core.domain.model.TestType.GTO_PGT
                                    GTOTestType.HALF_GROUP_TASK -> com.ssbmax.core.domain.model.TestType.GTO_HGT
                                    GTOTestType.GROUP_OBSTACLE_RACE -> com.ssbmax.core.domain.model.TestType.GTO_GOR
                                    GTOTestType.INDIVIDUAL_OBSTACLES -> com.ssbmax.core.domain.model.TestType.GTO_IO
                                    GTOTestType.COMMAND_TASK -> com.ssbmax.core.domain.model.TestType.GTO_CT
                                },
                                olqScores = gtoResult.olqScores,
                                overallScore = gtoResult.overallScore,
                                overallRating = gtoResult.overallRating,
                                strengths = gtoResult.strengths,
                                weaknesses = gtoResult.weaknesses,
                                recommendations = gtoResult.recommendations,
                                analyzedAt = gtoResult.analyzedAt,
                                aiConfidence = gtoResult.aiConfidence
                            )
                        }
                    }
            }

            // Fetch Interview result (getUserResults returns Flow, so we need to collect first)
            val interviewResult = try {
                interviewRepository.getUserResults(userId).first().firstOrNull() // Get latest result from list
            } catch (e: Exception) {
                null
            }

            // Build dashboard
            val dashboard = OLQDashboardData(
                userId = userId,
                phase1Results = OLQDashboardData.Phase1Results(
                    oirResult = submissionRepository.getLatestOIRSubmission(userId).getOrNull()?.let {
                        // Convert OIRSubmission to OIRTestResult if needed
                        null // TODO: Map to OIRTestResult
                    },
                    ppdtResult = ppdtSubmission,  // Full submission for UI display
                    ppdtOLQResult = ppdtOLQResult  // Extracted OLQ scores
                ),
                phase2Results = OLQDashboardData.Phase2Results(
                    tatResult = tatResult,
                    watResult = watResult,
                    srtResult = srtResult,
                    sdResult = sdResult,
                    gtoResults = gtoResults,
                    interviewResult = interviewResult
                )
            )

            // PERFORMANCE: Compute all aggregations ONCE, not on every UI access
            val averageOLQScores = computeAverageOLQScores(dashboard)
            val topOLQs = averageOLQScores.entries
                .sortedBy { it.value }  // Lower is better
                .take(3)
                .map { it.key to it.value }
            val improvementOLQs = averageOLQScores.entries
                .sortedByDescending { it.value }  // Higher needs improvement
                .take(3)
                .map { it.key to it.value }
            val overallAverage = computeOverallAverage(dashboard)

            Result.success(
                ProcessedDashboardData(
                    dashboard = dashboard,
                    averageOLQScores = averageOLQScores,
                    topOLQs = topOLQs,
                    improvementOLQs = improvementOLQs,
                    overallAverageScore = overallAverage
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper to get latest completed OLQ result for a psychology test
     */
    /**
     * Helper to get latest completed OLQ result for a psychology test
     * 
     * Uses Split Strategy:
     * 1. Get latest submission to get ID
     * 2. Fetch result from psych_results collection using ID
     */
    private suspend fun getLatestCompletedOLQResult(
        userId: String,
        testType: String
    ): OLQAnalysisResult? {
        // 1. Get latest submission to find the submissionId
        val submissionId = when (testType) {
            "TAT" -> submissionRepository.getLatestTATSubmission(userId).getOrNull()?.id
            "WAT" -> submissionRepository.getLatestWATSubmission(userId).getOrNull()?.id
            "SRT" -> submissionRepository.getLatestSRTSubmission(userId).getOrNull()?.id
            "SDT" -> submissionRepository.getLatestSDTSubmission(userId).getOrNull()?.id
            else -> return null
        } ?: return null

        // 2. Fetch the result from psych_results (via repository)
        return when (testType) {
            "TAT" -> submissionRepository.getTATResult(submissionId).getOrNull()
            "WAT" -> submissionRepository.getWATResult(submissionId).getOrNull()
            "SRT" -> submissionRepository.getSRTResult(submissionId).getOrNull()
            "SDT" -> submissionRepository.getSDTResult(submissionId).getOrNull()
            else -> null
        }
    }
    
    /**
     * Compute average OLQ scores across all tests (Phase 1 PPDT + Phase 2)
     */
    private fun computeAverageOLQScores(dashboard: OLQDashboardData): Map<OLQ, Float> {
        val olqScoresMap = mutableMapOf<OLQ, Float>()
        
        OLQ.entries.forEach { olq ->
            val scores = mutableListOf<Float>()
            
            // Phase 1: PPDT
            dashboard.phase1Results.ppdtOLQResult?.olqScores?.get(olq)?.score?.let { scores.add(it.toFloat()) }
            
            // Phase 2: Psychology tests
            dashboard.phase2Results.tatResult?.olqScores?.get(olq)?.score?.let { scores.add(it.toFloat()) }
            dashboard.phase2Results.watResult?.olqScores?.get(olq)?.score?.let { scores.add(it.toFloat()) }
            dashboard.phase2Results.srtResult?.olqScores?.get(olq)?.score?.let { scores.add(it.toFloat()) }
            dashboard.phase2Results.sdResult?.olqScores?.get(olq)?.score?.let { scores.add(it.toFloat()) }
            
            // GTO tests
            dashboard.phase2Results.gtoResults.values.forEach { gtoResult ->
                gtoResult.olqScores[olq]?.score?.let { scores.add(it.toFloat()) }
            }
            
            // Interview
            dashboard.phase2Results.interviewResult?.overallOLQScores?.get(olq)?.score?.let { scores.add(it.toFloat()) }
            
            if (scores.isNotEmpty()) {
                olqScoresMap[olq] = scores.average().toFloat()
            }
        }
        
        return olqScoresMap
    }
    
    /**
     * Compute overall average score across all tests (Phase 1 PPDT + Phase 2)
     */
    private fun computeOverallAverage(dashboard: OLQDashboardData): Float? {
        val allScores = mutableListOf<Float>()
        
        // Phase 1: PPDT
        dashboard.phase1Results.ppdtOLQResult?.overallScore?.let { allScores.add(it) }
        
        // Phase 2: Psychology tests
        dashboard.phase2Results.tatResult?.overallScore?.let { allScores.add(it) }
        dashboard.phase2Results.watResult?.overallScore?.let { allScores.add(it) }
        dashboard.phase2Results.srtResult?.overallScore?.let { allScores.add(it) }
        dashboard.phase2Results.sdResult?.overallScore?.let { allScores.add(it) }
        dashboard.phase2Results.gtoResults.values.forEach { it.overallScore.let { score -> allScores.add(score) } }
        dashboard.phase2Results.interviewResult?.getAverageOLQScore()?.let { allScores.add(it) }
        
        return if (allScores.isNotEmpty()) allScores.average().toFloat() else null
    }
}
