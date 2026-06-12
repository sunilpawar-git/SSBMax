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

    // ============ Phase 1: composite index behaviour ============

    @Test
    fun getUnusedQuestionsByType_includesNullLastUsedAndOldRows() = runTest {
        val threshold = 1_000L
        dao.insertQuestions(listOf(
            entity(id = "a", type = "VERBAL_REASONING", lastUsed = null),    // null → included
            entity(id = "b", type = "VERBAL_REASONING", lastUsed = 500L),    // old   → included
            entity(id = "c", type = "VERBAL_REASONING", lastUsed = 2_000L),  // recent → excluded
            entity(id = "d", type = "NON_VERBAL_REASONING", lastUsed = null) // wrong type → excluded
        ))

        val result = dao.getUnusedQuestionsByType("VERBAL_REASONING", threshold, 10)
        val ids = result.map { it.id }.toSet()

        assertTrue("null lastUsed must be included", "a" in ids)
        assertTrue("old lastUsed must be included", "b" in ids)
        assertFalse("recent lastUsed must be excluded", "c" in ids)
        assertFalse("wrong type must be excluded", "d" in ids)
    }

    @Test
    fun getUnusedQuestionsByType_respectsCountLimit() = runTest {
        dao.insertQuestions((1..10).map { entity(id = "q$it", type = "VERBAL_REASONING", lastUsed = null) })

        val result = dao.getUnusedQuestionsByType("VERBAL_REASONING", Long.MAX_VALUE, 3)

        assertEquals(3, result.size)
    }

    @Test
    fun getQuestionsByType_returnsOnlyRequestedType() = runTest {
        dao.insertQuestions(listOf(
            entity(id = "v1", type = "VERBAL_REASONING", lastUsed = null),
            entity(id = "v2", type = "VERBAL_REASONING", lastUsed = null),
            entity(id = "nv1", type = "NON_VERBAL_REASONING", lastUsed = null)
        ))

        val result = dao.getQuestionsByType("VERBAL_REASONING", 10)

        assertEquals(2, result.size)
        assertTrue(result.all { it.type == "VERBAL_REASONING" })
    }

    private fun entity(
        id: String,
        usage: Int = 0,
        lastUsed: Long? = null,
        type: String = "VERBAL_REASONING"
    ): CachedOIRQuestionEntity = CachedOIRQuestionEntity(
        id = id,
        questionNumber = usage + 1,
        type = type,
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
