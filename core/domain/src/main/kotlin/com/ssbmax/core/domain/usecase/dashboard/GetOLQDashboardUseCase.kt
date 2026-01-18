package com.ssbmax.core.domain.usecase.dashboard

import com.ssbmax.core.domain.model.dashboard.OLQDashboardData
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.util.DomainLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val overallAverageScore: Float?,
    val cacheMetadata: CacheMetadata
)

/**
 * Metadata about cache performance for analytics tracking
 */
data class CacheMetadata(
    val cacheHit: Boolean,
    val loadTimeMs: Long,
    val forcedRefresh: Boolean
)

/**
 * Use case to fetch and aggregate all test results for dashboard display
 *
 * Fetches:
 * - Phase 1: OIR, PPDT
 * - Phase 2: TAT, WAT, SRT, SD (Psychology), 8 GTO tests, Interview
 *
 * PERFORMANCE: All aggregations computed once, not on every UI access
 * CACHING: In-memory cache with 5-minute TTL to reduce Firestore reads
 */
@Singleton
class GetOLQDashboardUseCase @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val gtoRepository: GTORepository,
    private val interviewRepository: InterviewRepository,
    private val logger: DomainLogger
) {
    /**
     * Cached dashboard entry with timestamp for TTL validation
     */
    private data class CachedDashboard(
        val data: ProcessedDashboardData,
        val timestamp: Long
    )

    private val cache = mutableMapOf<String, CachedDashboard>()
    private val cacheMutex = Mutex()

    companion object {
        private const val TAG = "GetOLQDashboard"
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    /**
     * Fetch complete dashboard data with pre-computed aggregations
     *
     * @param userId User ID to fetch dashboard for
     * @param forceRefresh If true, bypasses cache and fetches fresh data
     * @return Processed dashboard with all aggregations computed once
     */
    suspend operator fun invoke(
        userId: String,
        forceRefresh: Boolean = false
    ): Result<ProcessedDashboardData> {
        val startTime = System.currentTimeMillis()
        var cacheHit = false
        
        logger.d(TAG, "📍 Fetching dashboard for user: $userId (forceRefresh=$forceRefresh)")

        return try {
            cacheMutex.withLock {
                // Check cache validity if not forcing refresh
                val cachedData = cache[userId]
                val now = System.currentTimeMillis()

                if (!forceRefresh && cachedData != null &&
                    (now - cachedData.timestamp) < CACHE_TTL_MS) {
                    // Cache hit - return cached data
                    cacheHit = true
                    val loadTime = System.currentTimeMillis() - startTime
                    logger.d(TAG, "✅ Cache hit - returning cached data (load time: ${loadTime}ms)")
                    logger.d(TAG, "   OIR result in cache: ${cachedData.data.dashboard.phase1Results.oirResult?.percentageScore}")

                    // Update cache metadata
                    val dataWithMetadata = cachedData.data.copy(
                        cacheMetadata = CacheMetadata(
                            cacheHit = true,
                            loadTimeMs = loadTime,
                            forcedRefresh = false
                        )
                    )

                    return Result.success(dataWithMetadata)
                }

                // Cache miss or expired - fetch fresh data
                logger.d(TAG, "❌ Cache miss - fetching from Firestore")
                val freshData = fetchDashboardData(userId)
                val loadTime = System.currentTimeMillis() - startTime
                logger.d(TAG, "   Fetch completed in ${loadTime}ms")

                // Update cache
                cache[userId] = CachedDashboard(
                    data = freshData,
                    timestamp = now
                )

                // Add cache metadata
                val dataWithMetadata = freshData.copy(
                    cacheMetadata = CacheMetadata(
                        cacheHit = false,
                        loadTimeMs = loadTime,
                        forcedRefresh = forceRefresh
                    )
                )
                
                logger.d(TAG, "✅ Dashboard data ready (OIR score: ${dataWithMetadata.dashboard.phase1Results.oirResult?.percentageScore})")

                Result.success(dataWithMetadata)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Invalidate cache for specific user
     * Called after test completion to ensure fresh scores on next dashboard load
     */
    suspend fun invalidateCache(userId: String) {
        cacheMutex.withLock {
            val hadCache = cache.containsKey(userId)
            cache.remove(userId)
            logger.d(TAG, "🗑️ Cache invalidated for user: $userId (had cache: $hadCache)")
        }
    }

    /**
     * Fetch fresh dashboard data from Firestore
     * Extracted from invoke() to support caching layer
     */
    private suspend fun fetchDashboardData(userId: String): ProcessedDashboardData {
        // Fetch OIR result
        logger.d(TAG, "🔍 Fetching OIR submission for user: $userId")
        val oirSubmission = submissionRepository.getLatestOIRSubmission(userId).getOrNull()
        val oirResult = oirSubmission?.testResult
        logger.d(TAG, "   OIR submission found: ${oirSubmission != null}")
        logger.d(TAG, "   OIR submission ID: ${oirSubmission?.id}")
        logger.d(TAG, "   OIR testResult extracted: ${oirResult != null}")
        logger.d(TAG, "   OIR percentageScore: ${oirResult?.percentageScore}")
        
        // Fetch PPDT submission and fetch OLQ result from ppdt_results collection (GTO pattern)
            // Important: Try fetching from ppdt_results first - the submission's analysisStatus may be stale
            val ppdtSubmission = submissionRepository.getLatestPPDTSubmission(userId)
                .getOrNull()
            
            // Directly try to fetch from ppdt_results - this collection is always fresh
            val ppdtOLQResult = ppdtSubmission?.let { 
                submissionRepository.getPPDTResult(it.submissionId).getOrNull() 
            }

            // Fetch Phase 2 Psychology test results (OLQ-based)
            logger.d(TAG, "🔍 Fetching Phase 2 Psychology test results...")
            val tatResult = getLatestCompletedOLQResult(userId, "TAT")
            val watResult = getLatestCompletedOLQResult(userId, "WAT")
            val srtResult = getLatestCompletedOLQResult(userId, "SRT")
            val sdResult = getLatestCompletedOLQResult(userId, "SDT")
            logger.d(TAG, "   Phase 2 results: TAT=${tatResult != null}, WAT=${watResult != null}, SRT=${srtResult != null}, SDT=${sdResult != null}")

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
                    // CRITICAL: Ensure sessionId matches document ID for navigation consistency
                    // This fixes old records where sessionId might have been different from document ID
                    oirResult = oirResult?.copy(sessionId = oirSubmission?.id ?: ""),  
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

            val finalData = ProcessedDashboardData(
                dashboard = dashboard,
                averageOLQScores = averageOLQScores,
                topOLQs = topOLQs,
                improvementOLQs = improvementOLQs,
                overallAverageScore = overallAverage,
                cacheMetadata = CacheMetadata(
                    cacheHit = false,
                    loadTimeMs = 0L,
                    forcedRefresh = false
                ) // Will be replaced by invoke() with actual metadata
            )
            
            logger.d(TAG, "✅ Dashboard built successfully")
            logger.d(TAG, "   Final OIR result: ${finalData.dashboard.phase1Results.oirResult?.percentageScore}")
            logger.d(TAG, "   Phase 2 results: TAT=${finalData.dashboard.phase2Results.tatResult != null}, WAT=${finalData.dashboard.phase2Results.watResult != null}, SRT=${finalData.dashboard.phase2Results.srtResult != null}, SDT=${finalData.dashboard.phase2Results.sdResult != null}")
            logger.d(TAG, "   SDT overallScore: ${finalData.dashboard.phase2Results.sdResult?.overallScore}")
            logger.d(TAG, "   Average OLQ scores count: ${finalData.averageOLQScores.size}")
            logger.d(TAG, "   Overall average score: ${finalData.overallAverageScore}")
            
            return finalData
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
        logger.d(TAG, "🔍 Fetching $testType result for user: $userId")
        
        // 1. Get latest submission to find the submissionId
        val submissionId = when (testType) {
            "TAT" -> submissionRepository.getLatestTATSubmission(userId).getOrNull()?.id
            "WAT" -> submissionRepository.getLatestWATSubmission(userId).getOrNull()?.id
            "SRT" -> submissionRepository.getLatestSRTSubmission(userId).getOrNull()?.id
            "SDT" -> submissionRepository.getLatestSDTSubmission(userId).getOrNull()?.id
            else -> {
                logger.d(TAG, "   ❌ Unknown test type: $testType")
                return null
            }
        }
        
        if (submissionId == null) {
            logger.d(TAG, "   ⚠️ No $testType submission found for user")
            return null
        }
        
        logger.d(TAG, "   ✅ Found $testType submission ID: $submissionId")

        // 2. Fetch the result from psych_results (via repository)
        val result = when (testType) {
            "TAT" -> submissionRepository.getTATResult(submissionId).getOrNull()
            "WAT" -> submissionRepository.getWATResult(submissionId).getOrNull()
            "SRT" -> submissionRepository.getSRTResult(submissionId).getOrNull()
            "SDT" -> submissionRepository.getSDTResult(submissionId).getOrNull()
            else -> null
        }
        
        if (result == null) {
            logger.w(TAG, "   ⚠️ No $testType OLQ result found for submission: $submissionId")
        } else {
            logger.d(TAG, "   ✅ Found $testType OLQ result with ${result.olqScores.size} OLQ scores, overallScore: ${result.overallScore}")
        }
        
        return result
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
