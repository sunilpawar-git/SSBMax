package com.ssbmax.core.data.repository

import com.ssbmax.core.data.local.dao.TestResultDao
import com.ssbmax.core.data.local.entity.TestResultEntity
import com.ssbmax.core.domain.model.SSBCategory
import com.ssbmax.core.domain.model.TestResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for TestRepositoryImpl
 */
class TestRepositoryImplTest {
    
    private lateinit var repository: TestRepositoryImpl
    private val mockDao = mockk<TestResultDao>(relaxed = true)
    
    @Before
    fun setup() {
        repository = TestRepositoryImpl(mockDao)
    }
    
    @Test
    fun getTests_returns_sample_tests() = runTest {
        // When
        val result = repository.getTests(SSBCategory.PSYCHOLOGY).first()
        
        // Then
        assertTrue(result.isSuccess)
        val tests = result.getOrNull()
        assertNotNull(tests)
        assertTrue(tests!!.size > 0)
    }
    
    @Test
    fun getTestById_returns_correct_test() = runTest {
        // When
        val result = repository.getTestById("tat_001")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("tat_001", result.getOrNull()?.id)
    }
    
    @Test
    fun startTestSession_creates_valid_session() = runTest {
        // When
        val result = repository.startTestSession("tat_001", "user_123")
        
        // Then
        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals("tat_001", session?.testId)
        assertEquals("user_123", session?.userId)
        assertEquals(0, session?.currentQuestion)
    }
    
    @Test
    fun submitTestResult_saves_to_local_database() = runTest {
        // Given
        val testResult = TestResult(
            id = "result_1",
            testId = "tat_001",
            userId = "user_123",
            score = 85.0f,
            maxScore = 100.0f,
            completedAt = System.currentTimeMillis(),
            timeSpent = 30.minutes
        )
        
        coEvery { mockDao.insert(any()) } returns Unit
        
        // When
        val result = repository.submitTestResult(testResult)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockDao.insert(any()) }
    }
    
    @Test
    fun getTestResults_returns_results_from_dao() = runTest {
        // Given
        val entities = listOf(
            TestResultEntity(
                id = "result_1",
                testId = "tat_001",
                userId = "user_123",
                score = 85.0f,
                maxScore = 100.0f,
                completedAt = System.currentTimeMillis(),
                timeSpentMinutes = 30
            )
        )
        coEvery { mockDao.getResults("user_123") } returns flowOf(entities)
        
        // When
        val result = repository.getTestResults("user_123").first()
        
        // Then
        assertTrue(result.isSuccess)
        val results = result.getOrNull()
        assertEquals(1, results?.size)
        assertEquals(85.0f, results?.get(0)?.score)
    }
    
    @Test
    fun getTestResultsByTest_filters_correctly() = runTest {
        // Given
        val entities = listOf(
            TestResultEntity(
                id = "result_1",
                testId = "tat_001",
                userId = "user_123",
                score = 85.0f,
                maxScore = 100.0f,
                completedAt = System.currentTimeMillis(),
                timeSpentMinutes = 30
            )
        )
        coEvery { mockDao.getResultsByTest("user_123", "tat_001") } returns flowOf(entities)
        
        // When
        val result = repository.getTestResultsByTest("user_123", "tat_001").first()
        
        // Then
        assertTrue(result.isSuccess)
        val results = result.getOrNull()
        assertEquals(1, results?.size)
        assertEquals("tat_001", results?.get(0)?.testId)
    }
}

