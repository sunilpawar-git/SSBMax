package com.ssbmax.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ssbmax.core.data.local.SSBDatabase
import com.ssbmax.core.data.local.entity.SyncStatus
import com.ssbmax.core.data.local.entity.TestResultEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Instrumented test for TestResultDao
 */
@RunWith(AndroidJUnit4::class)
class TestResultDaoTest {
    
    private lateinit var database: SSBDatabase
    private lateinit var dao: TestResultDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            SSBDatabase::class.java
        ).build()
        dao = database.testResultDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insert_and_retrieve_test_result() = runTest {
        // Given
        val result = TestResultEntity(
            id = "result_1",
            testId = "tat_001",
            userId = "user_1",
            score = 85.0f,
            maxScore = 100.0f,
            completedAt = System.currentTimeMillis(),
            timeSpentMinutes = 30,
            syncStatus = SyncStatus.PENDING
        )
        
        // When
        dao.insert(result)
        
        // Then
        val results = dao.getResults("user_1").first()
        assertEquals(1, results.size)
        assertEquals(85.0f, results[0].score, 0.01f)
        assertEquals("tat_001", results[0].testId)
    }
    
    @Test
    fun getResultsByTest_filters_correctly() = runTest {
        // Given
        val result1 = TestResultEntity(
            id = "result_1",
            testId = "tat_001",
            userId = "user_1",
            score = 85.0f,
            maxScore = 100.0f,
            completedAt = System.currentTimeMillis(),
            timeSpentMinutes = 30
        )
        
        val result2 = TestResultEntity(
            id = "result_2",
            testId = "wat_001",
            userId = "user_1",
            score = 90.0f,
            maxScore = 100.0f,
            completedAt = System.currentTimeMillis(),
            timeSpentMinutes = 20
        )
        
        // When
        dao.insertAll(listOf(result1, result2))
        
        // Then
        val tatResults = dao.getResultsByTest("user_1", "tat_001").first()
        assertEquals(1, tatResults.size)
        assertEquals("tat_001", tatResults[0].testId)
    }
    
    @Test
    fun updateSyncStatus_works() = runTest {
        // Given
        val result = TestResultEntity(
            id = "result_1",
            testId = "tat_001",
            userId = "user_1",
            score = 85.0f,
            maxScore = 100.0f,
            completedAt = System.currentTimeMillis(),
            timeSpentMinutes = 30,
            syncStatus = SyncStatus.PENDING
        )
        dao.insert(result)
        
        // When
        dao.updateSyncStatus("result_1", SyncStatus.SYNCED)
        
        // Then
        val results = dao.getResults("user_1").first()
        assertEquals(SyncStatus.SYNCED, results[0].syncStatus)
    }
    
    @Test
    fun getPendingSync_returns_only_pending() = runTest {
        // Given
        val pending = TestResultEntity(
            id = "result_1",
            testId = "tat_001",
            userId = "user_1",
            score = 85.0f,
            maxScore = 100.0f,
            completedAt = System.currentTimeMillis(),
            timeSpentMinutes = 30,
            syncStatus = SyncStatus.PENDING
        )
        
        val synced = pending.copy(id = "result_2", syncStatus = SyncStatus.SYNCED)
        
        dao.insertAll(listOf(pending, synced))
        
        // When
        val pendingResults = dao.getPendingSync()
        
        // Then
        assertEquals(1, pendingResults.size)
        assertEquals(SyncStatus.PENDING, pendingResults[0].syncStatus)
    }
}

