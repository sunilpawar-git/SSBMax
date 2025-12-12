package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.UserPerformanceDao
import com.ssbmax.core.data.local.entity.UserPerformanceEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class UserPerformanceDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: UserPerformanceDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.userPerformanceDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndQueryCurrentLevelAndReadiness() = runTest {
        val perf = entity(
            testType = "WAT",
            difficulty = "EASY",
            currentLevel = "EASY",
            readyForNextLevel = true,
            averageScore = 85f
        )
        dao.insertPerformance(perf)

        val level = dao.getCurrentLevel("WAT")
        val ready = dao.isReadyForNextLevel("WAT", "EASY")
        val avg = dao.getAverageScore("WAT", "EASY")

        assertEquals("EASY", level)
        assertTrue(ready ?: false)
        assertEquals(85f, avg)
    }

    @Test
    fun aggregatesAttemptsAndOverallAverage() = runTest {
        val easy = entity(testType = "SRT", difficulty = "EASY", totalAttempts = 5, averageScore = 70f)
        val hard = entity(testType = "SRT", difficulty = "HARD", totalAttempts = 3, averageScore = 90f)
        dao.insertPerformance(easy)
        dao.insertPerformance(hard)

        val totalAttempts = dao.getTotalAttempts("SRT")
        val overallAvg = dao.getOverallAverageScore("SRT")

        assertEquals(8, totalAttempts)
        // average of averages (70 + 90) / 2
        assertEquals(80f, overallAvg)
    }

    private fun entity(
        testType: String,
        difficulty: String,
        totalAttempts: Int = 0,
        averageScore: Float = 0f,
        currentLevel: String = difficulty,
        readyForNextLevel: Boolean = false
    ): UserPerformanceEntity = UserPerformanceEntity(
        testType = testType,
        difficulty = difficulty,
        totalAttempts = totalAttempts,
        correctAnswers = 0,
        incorrectAnswers = 0,
        averageScore = averageScore,
        averageTimeSeconds = 0f,
        currentLevel = currentLevel,
        readyForNextLevel = readyForNextLevel,
        lastAttemptAt = 1L,
        firstAttemptAt = 1L,
        updatedAt = 1L
    )
}


