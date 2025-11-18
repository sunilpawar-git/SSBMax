package com.ssbmax.core.data.repository

import com.ssbmax.core.data.local.dao.UserPerformanceDao
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AnalyticsRepository
 * Aggregates performance data from UserPerformanceDao
 */
@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val performanceDao: UserPerformanceDao
) : AnalyticsRepository {
    
    companion object {
        private val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    }
    
    override fun getPerformanceOverview(): Flow<PerformanceOverview?> {
        return performanceDao.getAllPerformance().map { performances ->
            if (performances.isEmpty()) return@map null
            
            val totalTests = performances.sumOf { it.totalAttempts }
            val averageScore = if (totalTests > 0) {
                val weightedSum = performances.sumOf { (it.totalAttempts * it.averageScore).toDouble() }
                weightedSum.toFloat() / totalTests
            } else 0f
            
            val testsByType = performances.groupBy { it.testType }
                .mapValues { (_, perfs) -> perfs.sumOf { it.totalAttempts } }
            
            // Recent progress (last 10 data points)
            val recentProgress = performances
                .sortedByDescending { it.lastAttemptAt }
                .take(10)
                .map { perf ->
                    TestPerformancePoint(
                        testType = perf.testType,
                        difficulty = perf.difficulty,
                        score = perf.averageScore,
                        timestamp = perf.lastAttemptAt,
                        date = dateFormatter.format(Date(perf.lastAttemptAt))
                    )
                }
            
            PerformanceOverview(
                totalTests = totalTests,
                averageScore = averageScore,
                totalStudyTimeMinutes = (performances.sumOf { (it.totalAttempts * it.averageTimeSeconds).toInt() } / 60),
                currentStreak = 0, // TODO: Calculate streak from timestamps
                testsByType = testsByType,
                recentProgress = recentProgress
            )
        }
    }
    
    override fun getTestTypeStats(testType: String): Flow<TestTypeStats?> {
        return performanceDao.getPerformanceByTestType(testType).map { performances ->
            if (performances.isEmpty()) return@map null
            
            val easyPerf = performances.find { it.difficulty == "EASY" }
            val mediumPerf = performances.find { it.difficulty == "MEDIUM" }
            val hardPerf = performances.find { it.difficulty == "HARD" }
            
            val totalAttempts = performances.sumOf { it.totalAttempts }
            val averageScore = if (totalAttempts > 0) {
                val weightedSum = performances.sumOf { (it.totalAttempts * it.averageScore).toDouble() }
                weightedSum.toFloat() / totalAttempts
            } else 0f
            
            val bestScore = performances.maxOfOrNull { it.averageScore } ?: 0f
            val currentDifficulty = performances.maxByOrNull { it.lastAttemptAt }?.currentLevel ?: "EASY"
            
            val easyStats = createDifficultyStats("EASY", easyPerf, mediumPerf)
            val mediumStats = createDifficultyStats("MEDIUM", mediumPerf, hardPerf)
            val hardStats = createDifficultyStats("HARD", hardPerf, null)
            
            val progressionStatus = calculateProgressionStatus(easyPerf, mediumPerf, hardPerf)
            
            TestTypeStats(
                testType = testType,
                totalAttempts = totalAttempts,
                averageScore = averageScore,
                bestScore = bestScore,
                currentDifficulty = currentDifficulty,
                easyStats = easyStats,
                mediumStats = mediumStats,
                hardStats = hardStats,
                recentScores = performances.take(10).map { it.averageScore },
                progressionStatus = progressionStatus
            )
        }
    }
    
    override fun getAllTestTypeStats(): Flow<List<TestTypeStats>> {
        return performanceDao.getAllPerformance().map { performances ->
            val testTypes = performances.map { it.testType }.distinct()
            testTypes.mapNotNull { testType ->
                val typePerfs = performances.filter { it.testType == testType }
                if (typePerfs.isEmpty()) return@mapNotNull null
                
                val easyPerf = typePerfs.find { it.difficulty == "EASY" }
                val mediumPerf = typePerfs.find { it.difficulty == "MEDIUM" }
                val hardPerf = typePerfs.find { it.difficulty == "HARD" }
                
                val totalAttempts = typePerfs.sumOf { it.totalAttempts }
                val averageScore = if (totalAttempts > 0) {
                    val weightedSum = typePerfs.sumOf { (it.totalAttempts * it.averageScore).toDouble() }
                    weightedSum.toFloat() / totalAttempts
                } else 0f
                
                TestTypeStats(
                    testType = testType,
                    totalAttempts = totalAttempts,
                    averageScore = averageScore,
                    bestScore = typePerfs.maxOfOrNull { it.averageScore } ?: 0f,
                    currentDifficulty = typePerfs.maxByOrNull { it.lastAttemptAt }?.currentLevel ?: "EASY",
                    easyStats = createDifficultyStats("EASY", easyPerf, mediumPerf),
                    mediumStats = createDifficultyStats("MEDIUM", mediumPerf, hardPerf),
                    hardStats = createDifficultyStats("HARD", hardPerf, null),
                    recentScores = typePerfs.take(5).map { it.averageScore },
                    progressionStatus = calculateProgressionStatus(easyPerf, mediumPerf, hardPerf)
                )
            }
        }
    }
    
    override fun getRecentProgress(limit: Int): Flow<List<TestPerformancePoint>> {
        return performanceDao.getAllPerformance().map { performances ->
            performances
                .sortedByDescending { it.lastAttemptAt }
                .take(limit)
                .map { perf ->
                    TestPerformancePoint(
                        testType = perf.testType,
                        difficulty = perf.difficulty,
                        score = perf.averageScore,
                        timestamp = perf.lastAttemptAt,
                        date = dateFormatter.format(Date(perf.lastAttemptAt))
                    )
                }
        }
    }
    
    override suspend fun getDifficultyStats(testType: String, difficulty: String): DifficultyStats? {
        val perf = performanceDao.getPerformance(testType, difficulty)
        
        // For EASY difficulty, always return stats (even if null data) since it's always unlocked
        // For MEDIUM/HARD, return null if no data exists (not unlocked yet)
        if (perf == null && difficulty != "EASY") {
            return null
        }
        
        // Check if next level is unlocked
        val nextDifficulty = when (difficulty) {
            "EASY" -> "MEDIUM"
            "MEDIUM" -> "HARD"
            else -> null
        }
        val nextPerf = nextDifficulty?.let { performanceDao.getPerformance(testType, it) }
        
        return createDifficultyStats(difficulty, perf, nextPerf)
    }
    
    override suspend fun getProgressionStatus(testType: String): ProgressionStatus? {
        val easyPerf = performanceDao.getPerformance(testType, "EASY")
        val mediumPerf = performanceDao.getPerformance(testType, "MEDIUM")
        val hardPerf = performanceDao.getPerformance(testType, "HARD")
        
        return calculateProgressionStatus(easyPerf, mediumPerf, hardPerf)
    }
    
    override suspend fun getStudyPattern(): StudyPattern? {
        // TODO: Implement study pattern tracking
        return null
    }
    
    override fun getAchievements(): Flow<List<Achievement>> {
        // TODO: Implement achievements system
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    override fun getGoalProgress(): Flow<List<GoalProgress>> {
        // TODO: Implement goals system
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    // Helper functions
    
    private fun createDifficultyStats(
        difficulty: String,
        currentPerf: com.ssbmax.core.data.local.entity.UserPerformanceEntity?,
        nextPerf: com.ssbmax.core.data.local.entity.UserPerformanceEntity?
    ): DifficultyStats {
        if (currentPerf == null) {
            return DifficultyStats(
                difficulty = difficulty,
                attempts = 0,
                accuracy = 0f,
                averageScore = 0f,
                averageTimeSeconds = 0f,
                isUnlocked = difficulty == "EASY", // EASY is always unlocked
                progressToNext = 0f
            )
        }
        
        val progressToNext = if (nextPerf != null) {
            100f // Next level already unlocked
        } else {
            // Calculate progress: need 80% accuracy + 10 attempts
            val accuracyProgress = (currentPerf.accuracy / 80f * 100f).coerceAtMost(100f)
            val attemptsProgress = (currentPerf.totalAttempts / 10f * 100f).coerceAtMost(100f)
            (accuracyProgress + attemptsProgress) / 2 // Average of both criteria
        }
        
        return DifficultyStats(
            difficulty = difficulty,
            attempts = currentPerf.totalAttempts,
            accuracy = currentPerf.accuracy,
            averageScore = currentPerf.averageScore,
            averageTimeSeconds = currentPerf.averageTimeSeconds,
            isUnlocked = true,
            progressToNext = progressToNext
        )
    }
    
    private fun calculateProgressionStatus(
        easyPerf: com.ssbmax.core.data.local.entity.UserPerformanceEntity?,
        mediumPerf: com.ssbmax.core.data.local.entity.UserPerformanceEntity?,
        hardPerf: com.ssbmax.core.data.local.entity.UserPerformanceEntity?
    ): ProgressionStatus {
        val currentLevel: String
        val nextLevel: String?
        val progress: Float
        val attemptsNeeded: Int
        val accuracyNeeded: Float
        val canProgress: Boolean
        
        when {
            hardPerf != null -> {
                // At HARD level, no next level
                currentLevel = "HARD"
                nextLevel = null
                progress = 100f
                attemptsNeeded = 0
                accuracyNeeded = 0f
                canProgress = false
            }
            mediumPerf != null -> {
                // At MEDIUM, working toward HARD
                currentLevel = "MEDIUM"
                nextLevel = "HARD"
                val attempts = mediumPerf.totalAttempts
                val accuracy = mediumPerf.accuracy
                attemptsNeeded = (10 - attempts).coerceAtLeast(0)
                accuracyNeeded = (80f - accuracy).coerceAtLeast(0f)
                progress = ((attempts / 10f * 50f) + (accuracy / 80f * 50f)).coerceAtMost(100f)
                canProgress = attempts >= 10 && accuracy >= 80f
            }
            easyPerf != null -> {
                // At EASY, working toward MEDIUM
                currentLevel = "EASY"
                nextLevel = "MEDIUM"
                val attempts = easyPerf.totalAttempts
                val accuracy = easyPerf.accuracy
                attemptsNeeded = (10 - attempts).coerceAtLeast(0)
                accuracyNeeded = (80f - accuracy).coerceAtLeast(0f)
                progress = ((attempts / 10f * 50f) + (accuracy / 80f * 50f)).coerceAtMost(100f)
                canProgress = attempts >= 10 && accuracy >= 80f
            }
            else -> {
                // No data yet, start at EASY
                currentLevel = "EASY"
                nextLevel = "MEDIUM"
                progress = 0f
                attemptsNeeded = 10
                accuracyNeeded = 80f
                canProgress = false
            }
        }
        
        return ProgressionStatus(
            currentLevel = currentLevel,
            nextLevel = nextLevel,
            progressPercentage = progress,
            attemptsNeeded = attemptsNeeded,
            accuracyNeeded = accuracyNeeded,
            canProgress = canProgress
        )
    }
}

