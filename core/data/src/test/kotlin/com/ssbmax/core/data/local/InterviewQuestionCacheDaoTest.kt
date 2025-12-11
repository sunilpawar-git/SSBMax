package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.InterviewQuestionCacheDao
import com.ssbmax.core.data.local.entity.CachedInterviewQuestionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class InterviewQuestionCacheDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: InterviewQuestionCacheDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.interviewQuestionCacheDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun getRandomQuestions_prioritizesLeastUsed() = runTest {
        val qs = listOf(
            entity(id = "a", usage = 3),
            entity(id = "b", usage = 0),
            entity(id = "c", usage = 1)
        )
        dao.insertQuestions(qs)

        val random = dao.getRandomQuestions(2)
        assertEquals(setOf("b", "c"), random.map { it.id }.toSet())
    }

    @Test
    fun markQuestionsAsUsed_incrementsUsageAndLastUsed() = runTest {
        val qs = listOf(
            entity(id = "a", usage = 0, lastUsed = null),
            entity(id = "b", usage = 0, lastUsed = null)
        )
        dao.insertQuestions(qs)

        dao.markQuestionsAsUsed(listOf("a", "b"), timestamp = 99L)

        val all = dao.getAllQuestions()
        assertTrue(all.all { it.usageCount == 1 })
        assertTrue(all.all { it.lastUsed == 99L })
    }

    private fun entity(
        id: String,
        usage: Int,
        lastUsed: Long? = null
    ): CachedInterviewQuestionEntity = CachedInterviewQuestionEntity(
        id = id,
        question = "Q$id",
        category = "personal",
        difficulty = "easy",
        suggestedAnswer = null,
        keyPoints = null,
        commonMistakes = null,
        followUpQuestions = null,
        batchId = "batch",
        cachedAt = 0L,
        lastUsed = lastUsed,
        usageCount = usage
    )
}
