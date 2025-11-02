package com.ssbmax.core.data.local.dao

import androidx.room.*
import com.ssbmax.core.data.local.entity.UserPerformanceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user performance tracking and adaptive difficulty progression
 */
@Dao
interface UserPerformanceDao {
    
    /**
     * Get performance for specific test type and difficulty
     */
    @Query("""
        SELECT * FROM user_performance 
        WHERE testType = :testType AND difficulty = :difficulty
        LIMIT 1
    """)
    suspend fun getPerformance(testType: String, difficulty: String): UserPerformanceEntity?
    
    /**
     * Get all performance records for a test type
     */
    @Query("""
        SELECT * FROM user_performance 
        WHERE testType = :testType
        ORDER BY difficulty ASC
    """)
    fun getPerformanceByTestType(testType: String): Flow<List<UserPerformanceEntity>>
    
    /**
     * Get performance for all test types
     */
    @Query("SELECT * FROM user_performance ORDER BY testType, difficulty")
    fun getAllPerformance(): Flow<List<UserPerformanceEntity>>
    
    /**
     * Get current difficulty level for a test type
     */
    @Query("""
        SELECT currentLevel FROM user_performance 
        WHERE testType = :testType 
        ORDER BY updatedAt DESC
        LIMIT 1
    """)
    suspend fun getCurrentLevel(testType: String): String?
    
    /**
     * Check if user is ready for next difficulty level
     */
    @Query("""
        SELECT readyForNextLevel FROM user_performance 
        WHERE testType = :testType AND difficulty = :currentDifficulty
        LIMIT 1
    """)
    suspend fun isReadyForNextLevel(testType: String, currentDifficulty: String): Boolean?
    
    /**
     * Get average score for a specific difficulty
     */
    @Query("""
        SELECT averageScore FROM user_performance 
        WHERE testType = :testType AND difficulty = :difficulty
        LIMIT 1
    """)
    suspend fun getAverageScore(testType: String, difficulty: String): Float?
    
    /**
     * Get total attempts for a test type
     */
    @Query("""
        SELECT SUM(totalAttempts) FROM user_performance 
        WHERE testType = :testType
    """)
    suspend fun getTotalAttempts(testType: String): Int?
    
    /**
     * Insert or update performance record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformance(performance: UserPerformanceEntity): Long
    
    /**
     * Update performance record
     */
    @Update
    suspend fun updatePerformance(performance: UserPerformanceEntity)
    
    /**
     * Delete performance record
     */
    @Delete
    suspend fun deletePerformance(performance: UserPerformanceEntity)
    
    /**
     * Delete all performance records for a test type
     */
    @Query("DELETE FROM user_performance WHERE testType = :testType")
    suspend fun deleteByTestType(testType: String)
    
    /**
     * Delete all performance records
     */
    @Query("DELETE FROM user_performance")
    suspend fun deleteAll()
    
    /**
     * Get total test count for a test type (all difficulties combined)
     */
    @Query("""
        SELECT SUM(totalAttempts) FROM user_performance 
        WHERE testType = :testType
    """)
    suspend fun getTotalTestCount(testType: String): Int?
    
    /**
     * Get overall average score for a test type (all difficulties combined)
     */
    @Query("""
        SELECT AVG(averageScore) FROM user_performance 
        WHERE testType = :testType
    """)
    suspend fun getOverallAverageScore(testType: String): Float?
}

