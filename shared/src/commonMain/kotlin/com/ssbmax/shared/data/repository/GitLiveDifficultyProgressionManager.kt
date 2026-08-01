package com.ssbmax.shared.data.repository

import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.db.UserPerformance as UserPerformanceRow
import com.ssbmax.shared.domain.model.TestPerformanceSummary
import com.ssbmax.shared.domain.repository.DifficultyProgressionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * Port of the Android DifficultyProgressionManager -- the writer for the
 * UserPerformance local store that GitLiveAnalyticsRepository reads. Ported
 * in the same slice specifically so the read side isn't left pointing at a
 * table nothing ever populates on the KMP side.
 *
 * Phase 3c (#17): implements [DifficultyProgressionRepository] so
 * [com.ssbmax.shared.presentation.ppdt.PPDTTestViewModel] can depend on the
 * domain interface rather than this concrete data-layer class.
 */
class GitLiveDifficultyProgressionManager(
    private val database: SharedDatabase
) : DifficultyProgressionRepository {
    private val queries get() = database.sharedDatabaseQueries

    companion object {
        private const val MIN_ATTEMPTS_FOR_PROGRESSION = 10
        private const val ACCURACY_THRESHOLD = 80f
    }

    override suspend fun getRecommendedDifficulty(testType: String): String {
        val easyPerf = queries.selectPerformance(testType, "EASY").executeAsOneOrNull()
        val mediumPerf = queries.selectPerformance(testType, "MEDIUM").executeAsOneOrNull()

        return when {
            easyPerf == null -> "EASY"
            mediumPerf != null && mediumPerf.canProgressToNextLevel(ACCURACY_THRESHOLD) -> "HARD"
            easyPerf.canProgressToNextLevel(ACCURACY_THRESHOLD) -> "MEDIUM"
            else -> "EASY"
        }
    }

    override suspend fun recordPerformance(
        testType: String,
        difficulty: String,
        score: Float,
        correctAnswers: Int,
        totalQuestions: Int,
        timeSeconds: Float
    ) {
        val existing = queries.selectPerformance(testType, difficulty).executeAsOneOrNull()
        val now = Clock.System.now().toEpochMilliseconds()

        val newTotal: Int
        val newQuestionsAttempted: Int
        val newCorrect: Int
        val newIncorrect: Int
        val newAvgScore: Float
        val newAvgTime: Float
        val firstAttemptAt: Long

        if (existing != null) {
            newTotal = existing.totalAttempts.toInt() + 1
            newQuestionsAttempted = existing.totalQuestionsAttempted.toInt() + totalQuestions
            newCorrect = existing.correctAnswers.toInt() + correctAnswers
            newIncorrect = existing.incorrectAnswers.toInt() + (totalQuestions - correctAnswers)
            newAvgScore = ((existing.averageScore.toFloat() * existing.totalAttempts) + score) / newTotal
            newAvgTime = ((existing.averageTimeSeconds.toFloat() * existing.totalAttempts) + timeSeconds) / newTotal
            firstAttemptAt = existing.firstAttemptAt
        } else {
            newTotal = 1
            newQuestionsAttempted = totalQuestions
            newCorrect = correctAnswers
            newIncorrect = totalQuestions - correctAnswers
            newAvgScore = score
            newAvgTime = timeSeconds
            firstAttemptAt = now
        }

        queries.insertOrReplacePerformance(
            testType = testType,
            difficulty = difficulty,
            totalAttempts = newTotal.toLong(),
            totalQuestionsAttempted = newQuestionsAttempted.toLong(),
            correctAnswers = newCorrect.toLong(),
            incorrectAnswers = newIncorrect.toLong(),
            averageScore = newAvgScore.toDouble(),
            averageTimeSeconds = newAvgTime.toDouble(),
            currentLevel = difficulty,
            readyForNextLevel = if (canProgress(newTotal, newAvgScore)) 1L else 0L,
            lastAttemptAt = now,
            firstAttemptAt = firstAttemptAt,
            updatedAt = now
        )
    }

    override fun getPerformanceSummary(testType: String): Flow<TestPerformanceSummary?> = flow {
        val performances = queries.selectPerformanceByTestType(testType).executeAsList()
        if (performances.isEmpty()) {
            emit(null)
            return@flow
        }

        val easyPerf = performances.find { it.difficulty == "EASY" }
        val mediumPerf = performances.find { it.difficulty == "MEDIUM" }
        val hardPerf = performances.find { it.difficulty == "HARD" }

        val currentDifficulty = performances.maxByOrNull { it.lastAttemptAt }?.currentLevel ?: "EASY"
        val totalTests = performances.sumOf { it.totalAttempts }.toInt()
        val totalQuestionsAttempted = performances.sumOf { it.totalQuestionsAttempted }.toInt()
        val overallAccuracy = if (totalQuestionsAttempted > 0) {
            performances.sumOf { it.correctAnswers }.toFloat() / totalQuestionsAttempted * 100
        } else 0f

        emit(
            TestPerformanceSummary(
                testType = testType,
                currentDifficulty = currentDifficulty,
                easyAccuracy = easyPerf?.accuracy ?: 0f,
                mediumAccuracy = mediumPerf?.accuracy ?: 0f,
                hardAccuracy = hardPerf?.accuracy ?: 0f,
                totalTests = totalTests,
                overallAccuracy = overallAccuracy,
                recommendedDifficulty = determineRecommendedDifficulty(easyPerf, mediumPerf, hardPerf)
            )
        )
    }

    override suspend fun resetPerformance(testType: String) {
        queries.deletePerformanceByTestType(testType)
    }

    private fun canProgress(attempts: Int, averageScore: Float): Boolean {
        return attempts >= MIN_ATTEMPTS_FOR_PROGRESSION && averageScore >= ACCURACY_THRESHOLD
    }

    private fun determineRecommendedDifficulty(
        easyPerf: UserPerformanceRow?,
        mediumPerf: UserPerformanceRow?,
        hardPerf: UserPerformanceRow?
    ): String {
        return when {
            mediumPerf != null && mediumPerf.canProgressToNextLevel(ACCURACY_THRESHOLD) -> "HARD"
            easyPerf != null && easyPerf.canProgressToNextLevel(ACCURACY_THRESHOLD) -> "MEDIUM"
            else -> "EASY"
        }
    }
}
