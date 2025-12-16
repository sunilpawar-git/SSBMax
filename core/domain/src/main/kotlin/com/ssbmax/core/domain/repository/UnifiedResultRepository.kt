package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository for unified OLQ results across all test types
 *
 * Aggregates results from:
 * - Interview tests (via InterviewRepository)
 * - GTO tests (via GTORepository)
 * - Psychology tests (via SubmissionRepository)
 *
 * Provides a single source of truth for displaying test results
 * and OLQ performance across the entire app.
 */
interface UnifiedResultRepository {

    /**
     * Get all test results for a user, sorted by date (newest first)
     *
     * @param userId User ID
     * @return Flow of unified OLQ analysis results
     */
    fun getAllResults(userId: String): Flow<List<OLQAnalysisResult>>

    /**
     * Get overall OLQ profile for a user (average scores across all tests)
     *
     * @param userId User ID
     * @return Flow of average OLQ scores
     */
    fun getOverallOLQProfile(userId: String): Flow<Map<OLQ, Float>>

    /**
     * Get recent results (last N tests)
     *
     * @param userId User ID
     * @param limit Number of recent results to fetch
     * @return Flow of recent unified OLQ analysis results
     */
    fun getRecentResults(userId: String, limit: Int = 5): Flow<List<OLQAnalysisResult>>

    /**
     * Get top strengths across all tests
     *
     * @param userId User ID
     * @param topN Number of top OLQs to return
     * @return List of OLQs with best average scores
     */
    suspend fun getTopStrengths(userId: String, topN: Int = 3): Result<List<OLQ>>

    /**
     * Get areas needing improvement across all tests
     *
     * @param userId User ID
     * @param topN Number of weakest OLQs to return
     * @return List of OLQs with worst average scores
     */
    suspend fun getAreasForImprovement(userId: String, topN: Int = 3): Result<List<OLQ>>
}
