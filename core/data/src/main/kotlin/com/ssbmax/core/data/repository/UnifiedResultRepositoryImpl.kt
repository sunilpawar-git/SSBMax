package com.ssbmax.core.data.repository

import android.util.Log
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.UnifiedResultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UnifiedResultRepository
 *
 * Aggregates OLQ results from multiple sources:
 * - Interview tests
 * - GTO tests (GD, GPE, Lecturette)
 * - Psychology tests (future)
 */
@Singleton
class UnifiedResultRepositoryImpl @Inject constructor(
    private val interviewRepository: InterviewRepository,
    private val gtoRepository: GTORepository
) : UnifiedResultRepository {

    companion object {
        private const val TAG = "UnifiedResultRepository"
    }

    override fun getAllResults(userId: String): Flow<List<OLQAnalysisResult>> {
        return combine(
            getInterviewResults(userId),
            getGTOResultsFlow(userId)
        ) { interviewResults, gtoResults ->
            // Combine and sort by date (newest first)
            (interviewResults + gtoResults).sortedByDescending { it.analyzedAt }
        }
    }

    override fun getRecentResults(userId: String, limit: Int): Flow<List<OLQAnalysisResult>> {
        return getAllResults(userId).map { results ->
            results.take(limit)
        }
    }

    override fun getOverallOLQProfile(userId: String): Flow<Map<OLQ, Float>> {
        return getAllResults(userId).map { results ->
            if (results.isEmpty()) {
                emptyMap()
            } else {
                // Calculate average score for each OLQ across all tests
                OLQ.entries.associateWith { olq ->
                    val scores = results.mapNotNull { result ->
                        result.olqScores[olq]?.score?.toFloat()
                    }
                    if (scores.isNotEmpty()) scores.average().toFloat() else 0f
                }
            }
        }
    }

    override suspend fun getTopStrengths(userId: String, topN: Int): Result<List<OLQ>> {
        return try {
            // Get all results
            val gtoResults = gtoRepository.getUserResults(userId).getOrNull() ?: emptyList()
            val interviewResults = interviewRepository.getUserResults(userId).first()

            // Combine OLQ scores from all results
            val combinedScores = mutableMapOf<OLQ, MutableList<Int>>()
            
            // Add GTO scores
            gtoResults.forEach { result ->
                result.olqScores.forEach { (olq, score) ->
                    combinedScores.getOrPut(olq) { mutableListOf() }.add(score.score)
                }
            }
            
            // Add Interview scores
            interviewResults.forEach { result ->
                result.overallOLQScores.forEach { (olq, score) ->
                    combinedScores.getOrPut(olq) { mutableListOf() }.add(score.score)
                }
            }

            // Calculate averages and sort (lower scores = better in SSB)
            val averages = combinedScores.mapValues { (_, scores) ->
                scores.average().toFloat()
            }

            val topOLQs = averages.entries
                .sortedBy { it.value }  // Lower is better
                .take(topN)
                .map { it.key }

            Result.success(topOLQs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get top strengths", e)
            Result.failure(e)
        }
    }

    override suspend fun getAreasForImprovement(userId: String, topN: Int): Result<List<OLQ>> {
        return try {
            // Get all results
            val gtoResults = gtoRepository.getUserResults(userId).getOrNull() ?: emptyList()
            val interviewResults = interviewRepository.getUserResults(userId).first()

            // Combine OLQ scores from all results
            val combinedScores = mutableMapOf<OLQ, MutableList<Int>>()
            
            // Add GTO scores
            gtoResults.forEach { result ->
                result.olqScores.forEach { (olq, score) ->
                    combinedScores.getOrPut(olq) { mutableListOf() }.add(score.score)
                }
            }
            
            // Add Interview scores
            interviewResults.forEach { result ->
                result.overallOLQScores.forEach { (olq, score) ->
                    combinedScores.getOrPut(olq) { mutableListOf() }.add(score.score)
                }
            }

            // Calculate averages and sort (higher scores = needs improvement in SSB)
            val averages = combinedScores.mapValues { (_, scores) ->
                scores.average().toFloat()
            }

            val weakOLQs = averages.entries
                .sortedByDescending { it.value }  // Higher is worse
                .take(topN)
                .map { it.key }

            Result.success(weakOLQs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get areas for improvement", e)
            Result.failure(e)
        }
    }

    /**
     * Get interview results as OLQAnalysisResult
     */
    private fun getInterviewResults(userId: String): Flow<List<OLQAnalysisResult>> {
        return interviewRepository.getUserResults(userId).map { results ->
            results.map { interviewResult ->
                OLQAnalysisResult(
                    submissionId = interviewResult.id,
                    testType = TestType.IO, // Interview Officer test
                    olqScores = interviewResult.overallOLQScores,
                    overallScore = interviewResult.getAverageOLQScore(),
                    overallRating = interviewResult.getPerformanceLevel().displayName,
                    strengths = interviewResult.strengths.map { it.displayName },
                    weaknesses = interviewResult.weaknesses.map { it.displayName },
                    recommendations = listOf(interviewResult.feedback),
                    analyzedAt = interviewResult.completedAt.toEpochMilli(),
                    aiConfidence = interviewResult.overallConfidence
                )
            }
        }
    }

    /**
     * Get GTO results as Flow (converts Result to Flow)
     */
    private fun getGTOResultsFlow(userId: String): Flow<List<OLQAnalysisResult>> {
        return kotlinx.coroutines.flow.flow {
            try {
                // Fetch results for all GTO test types
                val gdResults = gtoRepository.getUserResults(userId, GTOTestType.GROUP_DISCUSSION).getOrNull() ?: emptyList()
                val gpeResults = gtoRepository.getUserResults(userId, GTOTestType.GROUP_PLANNING_EXERCISE).getOrNull() ?: emptyList()
                val lecturetteResults = gtoRepository.getUserResults(userId, GTOTestType.LECTURETTE).getOrNull() ?: emptyList()

                // Combine all results
                val allGTOResults = gdResults + gpeResults + lecturetteResults

                // Map to OLQAnalysisResult
                val mappedResults = allGTOResults.map { gtoResult ->
                    // Map GTOTestType to TestType
                    val mappedTestType = when (gtoResult.testType) {
                        GTOTestType.GROUP_DISCUSSION -> TestType.GTO_GD
                        GTOTestType.GROUP_PLANNING_EXERCISE -> TestType.GTO_GPE
                        GTOTestType.LECTURETTE -> TestType.GTO_LECTURETTE
                        GTOTestType.PROGRESSIVE_GROUP_TASK -> TestType.GTO_PGT
                        GTOTestType.HALF_GROUP_TASK -> TestType.GTO_HGT
                        GTOTestType.GROUP_OBSTACLE_RACE -> TestType.GTO_GOR
                        GTOTestType.INDIVIDUAL_OBSTACLES -> TestType.GTO_IO
                        GTOTestType.COMMAND_TASK -> TestType.GTO_CT
                    }

                    OLQAnalysisResult(
                        submissionId = gtoResult.submissionId,
                        testType = mappedTestType,
                        olqScores = gtoResult.olqScores,
                        overallScore = gtoResult.overallScore,
                        overallRating = gtoResult.overallRating,
                        strengths = emptyList(), // GTO doesn't have explicit strengths/weaknesses
                        weaknesses = emptyList(),
                        recommendations = emptyList(),
                        analyzedAt = gtoResult.analyzedAt,
                        aiConfidence = gtoResult.aiConfidence
                    )
                }

                emit(mappedResults)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch GTO results", e)
                emit(emptyList())
            }
        }
    }
}
