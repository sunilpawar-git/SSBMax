package com.ssbmax.core.data.repository

import com.ssbmax.core.data.local.dao.TestResultDao
import com.ssbmax.core.data.local.entity.TestResultEntity
import com.ssbmax.core.domain.model.SSBCategory
import com.ssbmax.core.domain.model.SSBTest
import com.ssbmax.core.domain.model.TestResult
import com.ssbmax.core.domain.model.TestSession
import com.ssbmax.core.domain.repository.TestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * Implementation of TestRepository with offline-first architecture
 * 
 * Strategy:
 * 1. Return cached data immediately
 * 2. Sync with remote in background
 * 3. Update cache with remote data
 */
class TestRepositoryImpl @Inject constructor(
    private val localDataSource: TestResultDao
) : TestRepository {
    
    override fun getTests(category: SSBCategory): Flow<Result<List<SSBTest>>> = flow {
        // For now, emit hardcoded sample tests
        // In real implementation, this would fetch from API and cache
        emit(Result.success(getSampleTests(category)))
    }
    
    override suspend fun getTestById(testId: String): Result<SSBTest> {
        return runCatching {
            // For now, return a sample test
            // In real implementation, would query local DB or API
            getSampleTests(SSBCategory.PSYCHOLOGY).first { it.id == testId }
        }
    }
    
    override suspend fun startTestSession(testId: String, userId: String): Result<TestSession> {
        return runCatching {
            TestSession(
                sessionId = UUID.randomUUID().toString(),
                testId = testId,
                userId = userId,
                startedAt = System.currentTimeMillis(),
                currentQuestion = 0,
                responses = emptyMap()
            )
        }
    }
    
    override suspend fun submitTestResult(result: TestResult): Result<Unit> {
        return runCatching {
            // Save to local database
            localDataSource.insert(result.toEntity())
            
            // In real implementation:
            // 1. Save locally first (offline-first)
            // 2. Try to sync to remote
            // 3. If sync fails, mark as PENDING for later sync
        }
    }
    
    override fun getTestResults(userId: String): Flow<Result<List<TestResult>>> {
        return localDataSource.getResults(userId)
            .map { entities ->
                Result.success(entities.map { it.toDomain() })
            }
    }
    
    override fun getTestResultsByTest(userId: String, testId: String): Flow<Result<List<TestResult>>> {
        return localDataSource.getResultsByTest(userId, testId)
            .map { entities ->
                Result.success(entities.map { it.toDomain() })
            }
    }
    
    /**
     * Sample tests for development
     * TODO: Replace with real API calls
     */
    private fun getSampleTests(category: SSBCategory): List<SSBTest> {
        return when (category) {
            SSBCategory.PSYCHOLOGY -> listOf(
                SSBTest(
                    id = "tat_001",
                    type = com.ssbmax.core.domain.model.TestType.TAT,
                    category = category,
                    title = "TAT Test",
                    description = "Thematic Apperception Test - View images and write stories",
                    timeLimit = 60.minutes,
                    questionCount = 12,
                    instructions = "You will be shown 12 images. Write a story for each image.",
                    isPremium = false
                ),
                SSBTest(
                    id = "wat_001",
                    type = com.ssbmax.core.domain.model.TestType.WAT,
                    category = category,
                    title = "WAT Test",
                    description = "Word Association Test - Quick word responses",
                    timeLimit = 15.minutes,
                    questionCount = 60,
                    instructions = "You will see 60 words. Write the first thought that comes to mind.",
                    isPremium = false
                )
            )
            else -> emptyList()
        }
    }
}

/**
 * Extension function to convert TestResult to Entity
 */
private fun TestResult.toEntity(): TestResultEntity {
    return TestResultEntity(
        id = id,
        testId = testId,
        userId = userId,
        score = score,
        maxScore = maxScore,
        completedAt = completedAt,
        timeSpentMinutes = timeSpent.inWholeMinutes
    )
}

/**
 * Extension function to convert Entity to TestResult
 */
private fun TestResultEntity.toDomain(): TestResult {
    return TestResult(
        id = id,
        testId = testId,
        userId = userId,
        score = score,
        maxScore = maxScore,
        completedAt = completedAt,
        timeSpent = timeSpentMinutes.minutes
    )
}

