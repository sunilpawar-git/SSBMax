package com.ssbmax.shared.domain.repository

import com.ssbmax.shared.domain.model.TestPerformanceSummary
import kotlinx.coroutines.flow.Flow

/**
 * Adaptive per-test-type difficulty progression: reads the recommended
 * difficulty for a test type and records performance after each attempt.
 */
interface DifficultyProgressionRepository {

    /** Highest unlocked difficulty for [testType], based on recorded performance. */
    suspend fun getRecommendedDifficulty(testType: String): String

    /** Records one attempt's outcome and updates progression status. */
    suspend fun recordPerformance(
        testType: String,
        difficulty: String,
        score: Float,
        correctAnswers: Int,
        totalQuestions: Int,
        timeSeconds: Float
    )

    fun getPerformanceSummary(testType: String): Flow<TestPerformanceSummary?>

    suspend fun resetPerformance(testType: String)
}
