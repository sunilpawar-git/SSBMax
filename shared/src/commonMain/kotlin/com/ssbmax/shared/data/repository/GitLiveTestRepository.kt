package com.ssbmax.shared.data.repository

import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.db.TestResult as TestResultRow
import com.ssbmax.shared.domain.model.SSBCategory
import com.ssbmax.shared.domain.model.SSBTest
import com.ssbmax.shared.domain.model.TestResult
import com.ssbmax.shared.domain.model.TestSession
import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.TestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * SQLDelight-backed port of the Android TestRepositoryImpl. Unlike the
 * Firestore-backed repos ported earlier in Phase 2, this repository has no
 * remote counterpart in the Android original -- getTests/getTestById return
 * hardcoded sample tests (ported verbatim, including the "In real
 * implementation" TODO, since replacing the sample-data stub with a real
 * catalog is out of this slice's scope), and submitTestResult/getTestResults
 * are a pure local store. syncStatus tracking was dropped -- see the .sq
 * schema comment for why.
 */
class GitLiveTestRepository(
    private val database: SharedDatabase
) : TestRepository {

    override fun getTests(category: SSBCategory): Flow<Result<List<SSBTest>>> = flow {
        emit(Result.success(getSampleTests(category)))
    }

    override suspend fun getTestById(testId: String): Result<SSBTest> {
        return runCatching {
            getSampleTests(SSBCategory.PSYCHOLOGY).first { it.id == testId }
        }
    }

    override suspend fun startTestSession(testId: String, userId: String): Result<TestSession> {
        return runCatching {
            TestSession(
                sessionId = randomId(),
                testId = testId,
                userId = userId,
                startedAt = Clock.System.now().toEpochMilliseconds(),
                currentQuestion = 0,
                responses = emptyMap()
            )
        }
    }

    override suspend fun submitTestResult(result: TestResult): Result<Unit> {
        return runCatching {
            database.sharedDatabaseQueries.insertOrReplaceTestResult(
                id = result.id,
                testId = result.testId,
                userId = result.userId,
                score = result.score.toDouble(),
                maxScore = result.maxScore.toDouble(),
                completedAt = result.completedAt,
                timeSpentMinutes = result.timeSpent.inWholeMinutes
            )
        }
    }

    override fun getTestResults(userId: String): Flow<Result<List<TestResult>>> = flow {
        val results = database.sharedDatabaseQueries
            .selectTestResultsByUser(userId)
            .executeAsList()
            .map { it.toDomain() }
        emit(Result.success(results))
    }

    override fun getTestResultsByTest(userId: String, testId: String): Flow<Result<List<TestResult>>> = flow {
        val results = database.sharedDatabaseQueries
            .selectTestResultsByUserAndTest(userId, testId)
            .executeAsList()
            .map { it.toDomain() }
        emit(Result.success(results))
    }

    private fun getSampleTests(category: SSBCategory): List<SSBTest> {
        return when (category) {
            SSBCategory.PSYCHOLOGY -> listOf(
                SSBTest(
                    id = "tat_001",
                    type = TestType.TAT,
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
                    type = TestType.WAT,
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

private fun TestResultRow.toDomain(): TestResult {
    return TestResult(
        id = id,
        testId = testId,
        userId = userId,
        score = score.toFloat(),
        maxScore = maxScore.toFloat(),
        completedAt = completedAt,
        timeSpent = timeSpentMinutes.minutes
    )
}
