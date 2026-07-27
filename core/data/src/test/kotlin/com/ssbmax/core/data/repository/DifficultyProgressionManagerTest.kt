package com.ssbmax.core.data.repository

import android.util.Log
import com.ssbmax.core.data.local.dao.UserPerformanceDao
import com.ssbmax.core.data.local.entity.UserPerformanceEntity
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for the accuracy >100% bug: `totalAttempts` counts sessions, but
 * `correctAnswers` accumulates raw per-question counts. Before the fix, any multi-question
 * test overflowed `accuracy` past 100%; these assert the cumulative `totalQuestionsAttempted`
 * denominator keeps it in range.
 */
class DifficultyProgressionManagerTest {

    private lateinit var performanceDao: UserPerformanceDao
    private lateinit var manager: DifficultyProgressionManager

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        performanceDao = mockk()
        manager = DifficultyProgressionManager(performanceDao)
    }

    @After
    fun tearDown() = clearAllMocks()

    @Test
    fun `accuracy stays at 80 percent on the very first session of a 10-question test`() = runTest {
        // WHY: old formula divided correctAnswers by totalAttempts (session count = 1),
        // giving 8/1*100 = 800% on the very first test taken. This is the most common
        // real-world trigger, since almost every test has more than 1 question.
        coEvery { performanceDao.getPerformance("OIR", "EASY") } returns null
        val inserted = slot<UserPerformanceEntity>()
        coEvery { performanceDao.insertPerformance(capture(inserted)) } returns 1L

        manager.recordPerformance(
            testType = "OIR", difficulty = "EASY", score = 80f,
            correctAnswers = 8, totalQuestions = 10, timeSeconds = 30f
        )

        assertEquals(80f, inserted.captured.accuracy, 0.01f)
        assertTrue(inserted.captured.accuracy <= 100f)
    }

    @Test
    fun `accuracy stays at 80 percent across two sessions of a 10-question test`() = runTest {
        // WHY: totalAttempts counts sessions (2), but correctAnswers accumulates per-question
        // correct counts (8 correct x 2 sessions = 16 raw correct out of 20 raw questions).
        // Old formula: 16/2*100 = 800%. Fixed formula: 16/20*100 = 80%.
        coEvery { performanceDao.getPerformance("OIR", "EASY") } returns null
        val firstInsert = slot<UserPerformanceEntity>()
        coEvery { performanceDao.insertPerformance(capture(firstInsert)) } returns 1L

        manager.recordPerformance(
            testType = "OIR", difficulty = "EASY", score = 80f,
            correctAnswers = 8, totalQuestions = 10, timeSeconds = 30f
        )

        coEvery { performanceDao.getPerformance("OIR", "EASY") } returns firstInsert.captured
        val secondInsert = slot<UserPerformanceEntity>()
        coEvery { performanceDao.insertPerformance(capture(secondInsert)) } returns 1L

        manager.recordPerformance(
            testType = "OIR", difficulty = "EASY", score = 80f,
            correctAnswers = 8, totalQuestions = 10, timeSeconds = 30f
        )

        assertEquals(2, secondInsert.captured.totalAttempts)
        assertEquals(20, secondInsert.captured.totalQuestionsAttempted)
        assertEquals(16, secondInsert.captured.correctAnswers)
        assertEquals(80f, secondInsert.captured.accuracy, 0.01f)
        assertTrue(secondInsert.captured.accuracy <= 100f)
    }
}
