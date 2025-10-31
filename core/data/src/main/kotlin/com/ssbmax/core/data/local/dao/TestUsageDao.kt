package com.ssbmax.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ssbmax.core.data.local.entity.TestUsageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for test usage tracking
 */
@Dao
interface TestUsageDao {
    
    /**
     * Get test usage for a specific user and month
     */
    @Query("SELECT * FROM test_usage WHERE userId = :userId AND month = :month LIMIT 1")
    suspend fun getUsage(userId: String, month: String): TestUsageEntity?
    
    /**
     * Observe test usage for a specific user and month
     */
    @Query("SELECT * FROM test_usage WHERE userId = :userId AND month = :month LIMIT 1")
    fun observeUsage(userId: String, month: String): Flow<TestUsageEntity?>
    
    /**
     * Insert or replace test usage
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(usage: TestUsageEntity)
    
    /**
     * Update test usage
     */
    @Update
    suspend fun update(usage: TestUsageEntity)
    
    /**
     * Get all usage records for a user
     */
    @Query("SELECT * FROM test_usage WHERE userId = :userId ORDER BY month DESC")
    suspend fun getAllUsageForUser(userId: String): List<TestUsageEntity>
    
    /**
     * Delete old usage records (older than 6 months)
     */
    @Query("DELETE FROM test_usage WHERE month < :oldestMonth")
    suspend fun deleteOldRecords(oldestMonth: String)
}

