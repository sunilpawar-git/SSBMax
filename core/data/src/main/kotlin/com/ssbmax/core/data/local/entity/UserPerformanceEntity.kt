package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks user performance across different test types and difficulty levels
 * Used for adaptive difficulty progression
 */
@Entity(tableName = "user_performance")
data class UserPerformanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Test identification
    val testType: String, // "OIR", "WAT", "SRT", etc.
    val difficulty: String, // "EASY", "MEDIUM", "HARD"
    
    // Performance metrics
    val totalAttempts: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val averageScore: Float = 0f, // 0-100
    val averageTimeSeconds: Float = 0f,
    
    // Progression tracking
    val currentLevel: String, // Current difficulty level user is at
    val readyForNextLevel: Boolean = false, // True when user can progress to next difficulty
    
    // Timestamps
    val lastAttemptAt: Long = System.currentTimeMillis(),
    val firstAttemptAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculate accuracy percentage
     */
    val accuracy: Float
        get() = if (totalAttempts > 0) {
            (correctAnswers.toFloat() / totalAttempts) * 100
        } else 0f
    
    /**
     * Check if user is ready to progress based on threshold
     */
    fun canProgressToNextLevel(accuracyThreshold: Float = 80f): Boolean {
        return totalAttempts >= 10 && accuracy >= accuracyThreshold
    }
}

/**
 * Aggregated performance summary for a test type
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

