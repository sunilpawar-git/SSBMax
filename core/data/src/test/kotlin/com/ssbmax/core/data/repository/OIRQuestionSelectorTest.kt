package com.ssbmax.core.data.repository

import android.util.Log
import com.google.gson.Gson
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.domain.model.OIRQuestionType
import com.ssbmax.core.domain.model.OIRTestConfig
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Phase 5-B RED: OIRTestConfig.questionDistribution must match the actual
 * distribution used by OIRQuestionSelector (V=20, NV=20, N=7, S=3 for 50 Qs).
 *
 * RED: current config has V=15/NV=15/N=10/S=10 — test will fail until GREEN.
 */
class OIRQuestionSelectorTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            mockkStatic(Log::class)
            every { Log.d(any(), any()) } returns 0
            every { Log.w(any(), any<String>()) } returns 0
        }
    }

    private lateinit var mockDao: OIRQuestionCacheDao
    private lateinit var selector: OIRQuestionSelector

    private fun makeEntity(type: String, id: String = type) = CachedOIRQuestionEntity(
        id = id,
        questionNumber = 1,
        type = type,
        subtype = null,
        questionText = "Q?",
        optionsJson = "[]",
        correctAnswerId = "a",
        explanation = "",
        difficulty = "MEDIUM",
        tags = "",
        batchId = "batch1",
        cachedAt = 0L,
        lastUsed = 0L,
        usageCount = 0
    )

    @Before
    fun setUp() {
        mockDao = mockk(relaxed = true)
        // Provide 25 questions per type — more than enough for any count
        coEvery { mockDao.getUnusedQuestionsByType(OIRQuestionType.VERBAL_REASONING.name, any(), any()) } returns
                (1..25).map { makeEntity(OIRQuestionType.VERBAL_REASONING.name, "v$it") }
        coEvery { mockDao.getUnusedQuestionsByType(OIRQuestionType.NON_VERBAL_REASONING.name, any(), any()) } returns
                (1..25).map { makeEntity(OIRQuestionType.NON_VERBAL_REASONING.name, "nv$it") }
        coEvery { mockDao.getUnusedQuestionsByType(OIRQuestionType.NUMERICAL_ABILITY.name, any(), any()) } returns
                (1..25).map { makeEntity(OIRQuestionType.NUMERICAL_ABILITY.name, "n$it") }
        coEvery { mockDao.getUnusedQuestionsByType(OIRQuestionType.SPATIAL_REASONING.name, any(), any()) } returns
                (1..25).map { makeEntity(OIRQuestionType.SPATIAL_REASONING.name, "s$it") }
        // Fallback stubs (should not be needed but prevent NPE if triggered)
        coEvery { mockDao.getQuestionsByType(any(), any()) } returns emptyList()
        selector = OIRQuestionSelector(cacheDao = mockDao, gson = Gson())
    }

    @After
    fun tearDown() { clearAllMocks() }

    // ==================== Phase 5-B RED ====================

    @Test
    fun `selectQuestions distributionMatchesOIRTestConfig`() = runTest {
        val result = selector.selectQuestions(count = 50)
        val questions = result.getOrThrow()
        val config = OIRTestConfig()

        val byType = questions.groupBy { it.type }

        // The distribution returned by selectQuestions must match config.questionDistribution
        config.questionDistribution.forEach { (type, expected) ->
            val actual = byType[type]?.size ?: 0
            assertEquals(
                "Count for $type should match OIRTestConfig.questionDistribution",
                expected, actual
            )
        }
    }
}
