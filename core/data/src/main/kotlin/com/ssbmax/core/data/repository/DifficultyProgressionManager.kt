package com.ssbmax.core.data.repository

import android.util.Log
import com.ssbmax.core.data.local.dao.UserPerformanceDao
import com.ssbmax.core.data.local.entity.UserPerformanceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages adaptive difficulty progression based on user performance
 * 
 * Algorithm:
 * - Start with EASY questions
 * - If accuracy >= 80% after 10 attempts → Progress to MEDIUM
 * - If accuracy >= 80% on MEDIUM after 10 attempts → Progress to HARD
 * - Always serve questions at current difficulty level
 */
@Singleton
class DifficultyProgressionManager @Inject constructor(
    private val performanceDao: UserPerformanceDao
) {
    companion object {
        private const val TAG = "DifficultyProgression"
        
        // Progression thresholds
        private const val MIN_ATTEMPTS_FOR_PROGRESSION = 10
        private const val ACCURACY_THRESHOLD = 80f // 80% accuracy required
        
        // Difficulty levels in order
        private val DIFFICULTY_ORDER = listOf("EASY", "MEDIUM", "HARD")
    }
    
    /**
     * Get recommended difficulty level for a test type
     * Returns the highest unlocked difficulty based on performance
     */
    suspend fun getRecommendedDifficulty(testType: String): String {
        Log.d(TAG, "Getting recommended difficulty for $testType")
        
        // Check performance for each difficulty level
        val easyPerf = performanceDao.getPerformance(testType, "EASY")
        val mediumPerf = performanceDao.getPerformance(testType, "MEDIUM")
        
        return when {
            // No history → Start with EASY
            easyPerf == null -> {
                Log.d(TAG, "No history found, starting with EASY")
                "EASY"
            }
            
            // Check if ready for HARD
            mediumPerf != null && mediumPerf.canProgressToNextLevel(ACCURACY_THRESHOLD) -> {
                Log.d(TAG, "User ready for HARD (Medium accuracy: ${mediumPerf.accuracy}%)")
                "HARD"
            }
            
            // Check if ready for MEDIUM
            easyPerf.canProgressToNextLevel(ACCURACY_THRESHOLD) -> {
                Log.d(TAG, "User ready for MEDIUM (Easy accuracy: ${easyPerf.accuracy}%)")
                "MEDIUM"
            }
            
            // Stay at EASY
            else -> {
                Log.d(TAG, "User stays at EASY (accuracy: ${easyPerf.accuracy}%, attempts: ${easyPerf.totalAttempts})")
                "EASY"
            }
        }
    }
    
    /**
     * Record test performance and update progression status
     */
    suspend fun recordPerformance(
        testType: String,
        difficulty: String,
        score: Float, // 0-100
        correctAnswers: Int,
        totalQuestions: Int,
        timeSeconds: Float
    ) {
        Log.d(TAG, "Recording performance: $testType, $difficulty, score=$score")
        
        val existing = performanceDao.getPerformance(testType, difficulty)
        
        val updated = if (existing != null) {
            // Update existing record
            val newTotal = existing.totalAttempts + 1
            val newCorrect = existing.correctAnswers + correctAnswers
            val newIncorrect = existing.incorrectAnswers + (totalQuestions - correctAnswers)
            val newAvgScore = ((existing.averageScore * existing.totalAttempts) + score) / newTotal
            val newAvgTime = ((existing.averageTimeSeconds * existing.totalAttempts) + timeSeconds) / newTotal
            
            existing.copy(
                totalAttempts = newTotal,
                correctAnswers = newCorrect,
                incorrectAnswers = newIncorrect,
                averageScore = newAvgScore,
                averageTimeSeconds = newAvgTime,
                currentLevel = difficulty,
                readyForNextLevel = canProgress(newTotal, newAvgScore),
                lastAttemptAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            // Create new record
            UserPerformanceEntity(
                testType = testType,
                difficulty = difficulty,
                totalAttempts = 1,
                correctAnswers = correctAnswers,
                incorrectAnswers = totalQuestions - correctAnswers,
                averageScore = score,
                averageTimeSeconds = timeSeconds,
                currentLevel = difficulty,
                readyForNextLevel = false, // Need more attempts
                lastAttemptAt = System.currentTimeMillis(),
                firstAttemptAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        
        performanceDao.insertPerformance(updated)
        
        Log.d(TAG, "Performance updated: attempts=${updated.totalAttempts}, avgScore=${updated.averageScore}, readyForNext=${updated.readyForNextLevel}")
    }
    
    /**
     * Get performance summary for a test type
     */
    fun getPerformanceSummary(testType: String): Flow<TestPerformanceSummary?> {
        return performanceDao.getPerformanceByTestType(testType).map { performances ->
            if (performances.isEmpty()) return@map null
            
            val easyPerf = performances.find { it.difficulty == "EASY" }
            val mediumPerf = performances.find { it.difficulty == "MEDIUM" }
            val hardPerf = performances.find { it.difficulty == "HARD" }
            
            val currentDifficulty = performances.maxByOrNull { it.lastAttemptAt }?.currentLevel ?: "EASY"
            val totalTests = performances.sumOf { it.totalAttempts }
            val overallAccuracy = if (totalTests > 0) {
                performances.sumOf { it.correctAnswers }.toFloat() / 
                performances.sumOf { it.totalAttempts } * 100
            } else 0f
            
            TestPerformanceSummary(
                testType = testType,
                currentDifficulty = currentDifficulty,
                easyAccuracy = easyPerf?.accuracy ?: 0f,
                mediumAccuracy = mediumPerf?.accuracy ?: 0f,
                hardAccuracy = hardPerf?.accuracy ?: 0f,
                totalTests = totalTests,
                overallAccuracy = overallAccuracy,
                recommendedDifficulty = determineRecommendedDifficulty(easyPerf, mediumPerf, hardPerf)
            )
        }
    }
    
    /**
     * Reset performance for a test type (for testing/debugging)
     */
    suspend fun resetPerformance(testType: String) {
        Log.d(TAG, "Resetting performance for $testType")
        performanceDao.deleteByTestType(testType)
    }
    
    /**
     * Check if user can progress to next difficulty level
     */
    private fun canProgress(attempts: Int, averageScore: Float): Boolean {
        return attempts >= MIN_ATTEMPTS_FOR_PROGRESSION && averageScore >= ACCURACY_THRESHOLD
    }
    
    /**
     * Determine recommended difficulty based on all performance data
     */
    private fun determineRecommendedDifficulty(
        easyPerf: UserPerformanceEntity?,
        mediumPerf: UserPerformanceEntity?,
        hardPerf: UserPerformanceEntity?
    ): String {
        return when {
            mediumPerf != null && mediumPerf.canProgressToNextLevel(ACCURACY_THRESHOLD) -> "HARD"
            easyPerf != null && easyPerf.canProgressToNextLevel(ACCURACY_THRESHOLD) -> "MEDIUM"
            else -> "EASY"
        }
    }
}

/**
 * Performance summary for displaying to user
 */
data class TestPerformanceSummary(
    val testType: String,
    val currentDifficulty: String,
    val easyAccuracy: Float,
    val mediumAccuracy: Float,
    val hardAccuracy: Float,
    val totalTests: Int,
    val overallAccuracy: Float,
    val recommendedDifficulty: String
)

