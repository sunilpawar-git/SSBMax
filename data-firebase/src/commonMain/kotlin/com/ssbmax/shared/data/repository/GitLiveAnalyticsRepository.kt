package com.ssbmax.shared.data.repository

import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.db.UserPerformance as UserPerformanceRow
import com.ssbmax.shared.domain.model.Achievement
import com.ssbmax.shared.domain.model.DifficultyStats
import com.ssbmax.shared.domain.model.GoalProgress
import com.ssbmax.shared.domain.model.PerformanceOverview
import com.ssbmax.shared.domain.model.ProgressionStatus
import com.ssbmax.shared.domain.model.StudyPattern
import com.ssbmax.shared.domain.model.TestPerformancePoint
import com.ssbmax.shared.domain.model.TestTypeStats
import com.ssbmax.shared.domain.repository.AnalyticsRepository
import com.ssbmax.shared.domain.repository.UserProfileRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * SQLDelight-backed port of AnalyticsRepositoryImpl. Reads the same
 * UserPerformance local store that GitLiveDifficultyProgressionManager
 * writes to (its Android counterpart, DifficultyProgressionManager, is this
 * repo's only writer -- neither is Firestore-backed, both were ported in the
 * same slice so the read side isn't left pointing at a table nothing fills).
 * userProfileRepository/firebaseAuth mirror the Android constructor exactly.
 *
 * The 4 Flow-returning methods use SQLDelight's `.asFlow().mapToList(...)`
 * (same reactive-query pattern as `GitLiveNotificationCacheManager`), not a
 * one-shot `flow { emit(...) }` -- an earlier version used the latter, which
 * lost the Android original's Room `Flow<List<UserPerformanceEntity>>`
 * live-reactivity: a dashboard screen collecting these would silently stop
 * updating after a test submission until it re-subscribed. SQLDelight's query
 * listener re-emits on every write to `UserPerformance`, restoring parity.
 */
class GitLiveAnalyticsRepository(
    private val database: SharedDatabase,
    private val userProfileRepository: UserProfileRepository
) : AnalyticsRepository {

    private val queries get() = database.sharedDatabaseQueries

    override fun getPerformanceOverview(): Flow<PerformanceOverview?> {
        val userId = Firebase.auth.currentUser?.uid ?: return flowOf(null)

        return combine(
            queries.selectAllPerformance().asFlow().mapToList(Dispatchers.Default),
            userProfileRepository.getUserProfile(userId)
        ) { performances, profileResult ->
            if (performances.isEmpty()) return@combine null

            val totalTests = performances.sumOf { it.totalAttempts }.toInt()
            val averageScore = if (totalTests > 0) {
                val weightedSum = performances.sumOf { (it.totalAttempts * it.averageScore) }
                (weightedSum / totalTests).toFloat()
            } else 0f

            val testsByType = performances.groupBy { it.testType }
                .mapValues { (_, perfs) -> perfs.sumOf { it.totalAttempts }.toInt() }

            val recentProgress = performances
                .sortedByDescending { it.lastAttemptAt }
                .take(10)
                .map { it.toPerformancePoint() }

            val currentStreak = profileResult.getOrNull()?.currentStreak ?: 0

            PerformanceOverview(
                totalTests = totalTests,
                averageScore = averageScore,
                totalStudyTimeMinutes = (performances.sumOf { it.totalAttempts * it.averageTimeSeconds }.toInt() / 60),
                currentStreak = currentStreak,
                testsByType = testsByType,
                recentProgress = recentProgress
            )
        }
    }

    override fun getTestTypeStats(testType: String): Flow<TestTypeStats?> =
        queries.selectPerformanceByTestType(testType).asFlow().mapToList(Dispatchers.Default)
            .map { performances -> buildTestTypeStats(testType, performances) }

    override fun getAllTestTypeStats(): Flow<List<TestTypeStats>> =
        queries.selectAllPerformance().asFlow().mapToList(Dispatchers.Default)
            .map { performances ->
                val testTypes = performances.map { it.testType }.distinct()
                testTypes.mapNotNull { testType -> buildTestTypeStats(testType, performances.filter { it.testType == testType }) }
            }

    override fun getRecentProgress(limit: Int): Flow<List<TestPerformancePoint>> =
        queries.selectAllPerformance().asFlow().mapToList(Dispatchers.Default)
            .map { performances -> performances.sortedByDescending { it.lastAttemptAt }.take(limit).map { it.toPerformancePoint() } }

    override suspend fun getDifficultyStats(testType: String, difficulty: String): DifficultyStats? {
        val perf = queries.selectPerformance(testType, difficulty).executeAsOneOrNull()

        if (perf == null && difficulty != "EASY") {
            return null
        }

        val nextDifficulty = when (difficulty) {
            "EASY" -> "MEDIUM"
            "MEDIUM" -> "HARD"
            else -> null
        }
        val nextPerf = nextDifficulty?.let { queries.selectPerformance(testType, it).executeAsOneOrNull() }

        return createDifficultyStats(difficulty, perf, nextPerf)
    }

    override suspend fun getProgressionStatus(testType: String): ProgressionStatus? {
        val easyPerf = queries.selectPerformance(testType, "EASY").executeAsOneOrNull()
        val mediumPerf = queries.selectPerformance(testType, "MEDIUM").executeAsOneOrNull()
        val hardPerf = queries.selectPerformance(testType, "HARD").executeAsOneOrNull()

        return calculateProgressionStatus(easyPerf, mediumPerf, hardPerf)
    }

    // Not implemented in the Android original either (both return a no-op
    // stub there) -- ported as-is, not silently upgraded beyond this slice's
    // scope.
    override suspend fun getStudyPattern(): StudyPattern? = null
    override fun getAchievements(): Flow<List<Achievement>> = flowOf(emptyList())
    override fun getGoalProgress(): Flow<List<GoalProgress>> = flowOf(emptyList())

    private fun buildTestTypeStats(testType: String, performances: List<UserPerformanceRow>): TestTypeStats? {
        if (performances.isEmpty()) return null

        val easyPerf = performances.find { it.difficulty == "EASY" }
        val mediumPerf = performances.find { it.difficulty == "MEDIUM" }
        val hardPerf = performances.find { it.difficulty == "HARD" }

        val totalAttempts = performances.sumOf { it.totalAttempts }.toInt()
        val averageScore = if (totalAttempts > 0) {
            val weightedSum = performances.sumOf { (it.totalAttempts * it.averageScore) }
            (weightedSum / totalAttempts).toFloat()
        } else 0f

        return TestTypeStats(
            testType = testType,
            totalAttempts = totalAttempts,
            averageScore = averageScore,
            bestScore = performances.maxOfOrNull { it.averageScore.toFloat() } ?: 0f,
            currentDifficulty = performances.maxByOrNull { it.lastAttemptAt }?.currentLevel ?: "EASY",
            easyStats = createDifficultyStats("EASY", easyPerf, mediumPerf),
            mediumStats = createDifficultyStats("MEDIUM", mediumPerf, hardPerf),
            hardStats = createDifficultyStats("HARD", hardPerf, null),
            recentScores = performances.take(10).map { it.averageScore.toFloat() },
            progressionStatus = calculateProgressionStatus(easyPerf, mediumPerf, hardPerf)
        )
    }

    private fun createDifficultyStats(
        difficulty: String,
        currentPerf: UserPerformanceRow?,
        nextPerf: UserPerformanceRow?
    ): DifficultyStats {
        if (currentPerf == null) {
            return DifficultyStats(
                difficulty = difficulty,
                attempts = 0,
                accuracy = 0f,
                averageScore = 0f,
                averageTimeSeconds = 0f,
                isUnlocked = difficulty == "EASY",
                progressToNext = 0f
            )
        }

        val progressToNext = if (nextPerf != null) {
            100f
        } else {
            val accuracyProgress = (currentPerf.accuracy / 80f * 100f).coerceAtMost(100f)
            val attemptsProgress = (currentPerf.totalAttempts / 10f * 100f).coerceAtMost(100f)
            (accuracyProgress + attemptsProgress) / 2
        }

        return DifficultyStats(
            difficulty = difficulty,
            attempts = currentPerf.totalAttempts.toInt(),
            accuracy = currentPerf.accuracy,
            averageScore = currentPerf.averageScore.toFloat(),
            averageTimeSeconds = currentPerf.averageTimeSeconds.toFloat(),
            isUnlocked = true,
            progressToNext = progressToNext
        )
    }

    private fun calculateProgressionStatus(
        easyPerf: UserPerformanceRow?,
        mediumPerf: UserPerformanceRow?,
        hardPerf: UserPerformanceRow?
    ): ProgressionStatus {
        return when {
            hardPerf != null -> ProgressionStatus(
                currentLevel = "HARD", nextLevel = null, progressPercentage = 100f,
                attemptsNeeded = 0, accuracyNeeded = 0f, canProgress = false
            )
            mediumPerf != null -> progressionTowards("MEDIUM", "HARD", mediumPerf)
            easyPerf != null -> progressionTowards("EASY", "MEDIUM", easyPerf)
            else -> ProgressionStatus(
                currentLevel = "EASY", nextLevel = "MEDIUM", progressPercentage = 0f,
                attemptsNeeded = 10, accuracyNeeded = 80f, canProgress = false
            )
        }
    }

    private fun progressionTowards(currentLevel: String, nextLevel: String, perf: UserPerformanceRow): ProgressionStatus {
        val attempts = perf.totalAttempts.toInt()
        val accuracy = perf.accuracy
        val attemptsNeeded = (10 - attempts).coerceAtLeast(0)
        val accuracyNeeded = (80f - accuracy).coerceAtLeast(0f)
        val progress = ((attempts / 10f * 50f) + (accuracy / 80f * 50f)).coerceAtMost(100f)
        val canProgress = attempts >= 10 && accuracy >= 80f
        return ProgressionStatus(currentLevel, nextLevel, progress, attemptsNeeded, accuracyNeeded, canProgress)
    }
}

internal val UserPerformanceRow.accuracy: Float
    get() = if (totalQuestionsAttempted > 0) (correctAnswers.toFloat() / totalQuestionsAttempted) * 100 else 0f

internal fun UserPerformanceRow.canProgressToNextLevel(accuracyThreshold: Float = 80f): Boolean {
    return totalAttempts >= 10 && accuracy >= accuracyThreshold
}

private fun UserPerformanceRow.toPerformancePoint() = TestPerformancePoint(
    testType = testType,
    difficulty = difficulty,
    score = averageScore.toFloat(),
    timestamp = lastAttemptAt,
    date = formatShortDate(lastAttemptAt)
)

private val shortMonthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/**
 * KMP-safe replacement for the Android original's
 * SimpleDateFormat("MMM dd") -- java.text is JVM-only, so date formatting is
 * done manually via kotlinx-datetime instead (same category of fix as the
 * SSBInterviewPrompts/QuestionCacheRepository String.format bugs caught in
 * earlier Phase 2 slices).
 */
internal fun formatShortDate(epochMillis: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val month = shortMonthNames[local.monthNumber - 1]
    val day = local.dayOfMonth.toString().padStart(2, '0')
    return "$month $day"
}
