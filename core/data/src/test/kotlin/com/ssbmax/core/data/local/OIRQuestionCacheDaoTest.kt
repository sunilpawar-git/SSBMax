package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
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
class OIRQuestionCacheDaoTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: OIRQuestionCacheDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.oirQuestionCacheDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun getLeastUsedQuestions_ordersByUsage() = runTest {
        val questions = listOf(
            entity(id = "a", usage = 2, lastUsed = 1L),
            entity(id = "b", usage = 0, lastUsed = 2L),
            entity(id = "c", usage = 1, lastUsed = 3L)
        )
        dao.insertQuestions(questions)

        val least = dao.getLeastUsedQuestions(limit = 2)
        assertEquals(listOf("b", "c"), least.map { it.id })
    }

    @Test
    fun markQuestionsUsed_incrementsUsageAndLastUsed() = runTest {
        val questions = listOf(
            entity(id = "a", usage = 0, lastUsed = null),
            entity(id = "b", usage = 0, lastUsed = null)
        )
        dao.insertQuestions(questions)

        dao.markQuestionsUsed(listOf("a", "b"), timestamp = 5L)

        val all = dao.getAllQuestions()
        assertTrue(all.all { it.usageCount == 1 })
        assertTrue(all.all { it.lastUsed == 5L })
    }

    private fun entity(
        id: String,
        usage: Int,
        lastUsed: Long?
    ): CachedOIRQuestionEntity = CachedOIRQuestionEntity(
        id = id,
        questionNumber = usage + 1,
        type = "VERBAL",
        subtype = "ANALOGY",
        questionText = "Q$id",
        optionsJson = """["A","B","C","D"]""",
        correctAnswerId = "A",
        explanation = "exp",
        difficulty = "EASY",
        tags = "tag",
        batchId = "batch",
        cachedAt = 0L,
        lastUsed = lastUsed,
        usageCount = usage
    )
}
