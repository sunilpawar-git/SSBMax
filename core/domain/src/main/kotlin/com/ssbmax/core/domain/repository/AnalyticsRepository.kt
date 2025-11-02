package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for analytics and performance tracking
 */
interface AnalyticsRepository {
    
    /**
     * Get overall performance summary
     */
    fun getPerformanceOverview(): Flow<PerformanceOverview?>
    
    /**
     * Get detailed statistics for a specific test type
     */
    fun getTestTypeStats(testType: String): Flow<TestTypeStats?>
    
    /**
     * Get statistics for all test types
     */
    fun getAllTestTypeStats(): Flow<List<TestTypeStats>>
    
    /**
     * Get recent test progress (last N tests)
     */
    fun getRecentProgress(limit: Int = 10): Flow<List<TestPerformancePoint>>
    
    /**
     * Get difficulty-specific statistics
     */
    suspend fun getDifficultyStats(testType: String, difficulty: String): DifficultyStats?
    
    /**
     * Get progression status for a test type
     */
    suspend fun getProgressionStatus(testType: String): ProgressionStatus?
    
    /**
     * Get study patterns and time analytics
     */
    suspend fun getStudyPattern(): StudyPattern?
    
    /**
     * Get achievements
     */
    fun getAchievements(): Flow<List<Achievement>>
    
    /**
     * Get goal progress
     */
    fun getGoalProgress(): Flow<List<GoalProgress>>
}

