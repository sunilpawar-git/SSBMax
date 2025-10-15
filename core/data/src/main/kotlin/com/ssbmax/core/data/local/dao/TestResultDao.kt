package com.ssbmax.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ssbmax.core.data.local.entity.SyncStatus
import com.ssbmax.core.data.local.entity.TestResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for test results
 */
@Dao
interface TestResultDao {
    
    /**
     * Get all test results for a user
     */
    @Query("SELECT * FROM test_results WHERE userId = :userId ORDER BY completedAt DESC")
    fun getResults(userId: String): Flow<List<TestResultEntity>>
    
    /**
     * Get test results for a specific test
     */
    @Query("SELECT * FROM test_results WHERE userId = :userId AND testId = :testId ORDER BY completedAt DESC")
    fun getResultsByTest(userId: String, testId: String): Flow<List<TestResultEntity>>
    
    /**
     * Insert a test result
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: TestResultEntity)
    
    /**
     * Insert multiple test results
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<TestResultEntity>)
    
    /**
     * Get all pending sync results
     */
    @Query("SELECT * FROM test_results WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<TestResultEntity>
    
    /**
     * Update sync status for a result
     */
    @Query("UPDATE test_results SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)
    
    /**
     * Delete all test results (for testing)
     */
    @Query("DELETE FROM test_results")
    suspend fun deleteAll()
}

