package com.ssbmax.core.domain.model

/**
 * Analytics models for performance tracking and visualization
 */

/**
 * Overall performance summary across all tests
 */
data class PerformanceOverview(
    val totalTests: Int,
    val averageScore: Float,
    val totalStudyTimeMinutes: Int,
    val currentStreak: Int,
    val testsByType: Map<String, Int>,
    val recentProgress: List<TestPerformancePoint>
)

/**
 * Progress data point for charts
 */
data class TestPerformancePoint(
    val testType: String,
    val difficulty: String,
    val score: Float,
    val timestamp: Long,
    val date: String // Formatted date for display
)

/**
 * Detailed statistics for a specific test type
 */
data class TestTypeStats(
    val testType: String,
    val totalAttempts: Int,
    val averageScore: Float,
    val bestScore: Float,
    val currentDifficulty: String,
    val easyStats: DifficultyStats,
    val mediumStats: DifficultyStats,
    val hardStats: DifficultyStats,
    val recentScores: List<Float>,
    val progressionStatus: ProgressionStatus
)

/**
 * Statistics for a specific difficulty level
 */
data class DifficultyStats(
    val difficulty: String,
    val attempts: Int,
    val accuracy: Float,
    val averageScore: Float,
    val averageTimeSeconds: Float,
    val isUnlocked: Boolean,
    val progressToNext: Float // 0-100%, progress toward unlocking next level
)

/**
 * Progression status for difficulty advancement
 */
data class ProgressionStatus(
    val currentLevel: String,
    val nextLevel: String?,
    val progressPercentage: Float, // 0-100%
    val attemptsNeeded: Int, // Remaining attempts to unlock next level
    val accuracyNeeded: Float, // Accuracy % needed
    val canProgress: Boolean
)

/**
 * Category-wise performance breakdown
 */
data class CategoryPerformance(
    val category: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val accuracy: Float,
    val averageTime: Float,
    val strength: PerformanceLevel
)

/**
 * Performance level classification
 */
enum class PerformanceLevel {
    EXCELLENT,  // 90%+
    GOOD,       // 75-89%
    AVERAGE,    // 60-74%
    NEEDS_WORK; // <60%
    
    companion object {
        fun fromAccuracy(accuracy: Float): PerformanceLevel {
            return when {
                accuracy >= 90f -> EXCELLENT
                accuracy >= 75f -> GOOD
                accuracy >= 60f -> AVERAGE
                else -> NEEDS_WORK
            }
        }
    }
}

/**
 * Time-based analytics for study patterns
 */
data class StudyPattern(
    val totalMinutes: Int,
    val averageSessionMinutes: Int,
    val mostActiveDay: String,
    val mostActiveHour: Int,
    val weeklyDistribution: Map<String, Int> // Day -> Minutes
)

/**
 * Achievement/milestone data
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlockedAt: Long?,
    val isUnlocked: Boolean,
    val progress: Float // 0-100%
)

/**
 * Comparison with goals or benchmarks
 */
data class GoalProgress(
    val goalType: GoalType,
    val target: Float,
    val current: Float,
    val progress: Float, // 0-100%
    val deadline: Long?,
    val isAchieved: Boolean
)

enum class GoalType {
    TOTAL_TESTS,
    AVERAGE_SCORE,
    DIFFICULTY_UNLOCK,
    STREAK_DAYS,
    STUDY_TIME
}

