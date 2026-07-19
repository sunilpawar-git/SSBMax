package com.ssbmax.core.data.repository

import android.util.Log
import com.google.gson.Gson
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.shared.domain.model.OIRQuestionDistribution
import com.ssbmax.shared.domain.model.OIRQuestionType
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Verifies the selector honours the single-source-of-truth distribution
 * [OIRQuestionDistribution] (V=20, NV=20, N=10 for 50 Qs) and returns exactly
 * the requested count across many shuffles given a clean pool.
 */
class OIRQuestionSelectorTest {

    private lateinit var mockDao: OIRQuestionCacheDao
    private lateinit var selector: OIRQuestionSelector

    private fun makeEntity(type: String, id: String = type) = CachedOIRQuestionEntity(
        id = id,
        questionNumber = 1,
        type = type,
        subtype = null,
        questionText = "Sample valid question?",
        optionsJson = """[{"id":"opt_a","text":"A"},{"id":"opt_b","text":"B"},{"id":"opt_c","text":"C"},{"id":"opt_d","text":"D"}]""",
        correctAnswerId = "opt_a",
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
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        mockDao = mockk(relaxed = true)
        // Provide 25 questions per live type — more than enough for any count.
        coEvery { mockDao.getUnusedQuestionsByType(OIRQuestionType.VERBAL_REASONING.name, any(), any()) } returns
                (1..25).map { makeEntity(OIRQuestionType.VERBAL_REASONING.name, "v$it") }
        coEvery { mockDao.getUnusedQuestionsByType(OIRQuestionType.NON_VERBAL_REASONING.name, any(), any()) } returns
                (1..25).map { makeEntity(OIRQuestionType.NON_VERBAL_REASONING.name, "nv$it") }
        coEvery { mockDao.getUnusedQuestionsByType(OIRQuestionType.NUMERICAL_ABILITY.name, any(), any()) } returns
                (1..25).map { makeEntity(OIRQuestionType.NUMERICAL_ABILITY.name, "n$it") }
        // No spatial questions in the bank.
        coEvery { mockDao.getUnusedQuestionsByType(OIRQuestionType.SPATIAL_REASONING.name, any(), any()) } returns
                emptyList()
        // Fallback stubs (should not be needed but prevent NPE if triggered).
        coEvery { mockDao.getQuestionsByType(any(), any()) } returns emptyList()
        selector = OIRQuestionSelector(cacheDao = mockDao, gson = Gson())
    }

    @After
    fun tearDown() { clearAllMocks() }

    @Test
    fun `counts(50) splits as V20 NV20 N10`() {
        val counts = OIRQuestionDistribution.counts(50)
        assertEquals(20, counts[OIRQuestionType.VERBAL_REASONING])
        assertEquals(20, counts[OIRQuestionType.NON_VERBAL_REASONING])
        assertEquals(10, counts[OIRQuestionType.NUMERICAL_ABILITY])
        assertEquals(50, counts.values.sum())
    }

    @Test
    fun `counts always sums to total via largest-remainder`() {
        for (total in 1..120) {
            assertEquals(
                "counts($total) must sum to $total",
                total,
                OIRQuestionDistribution.counts(total).values.sum()
            )
        }
    }

    @Test
    fun `selectQuestions distribution matches SSOT`() = runTest {
        val questions = selector.selectQuestions(count = 50).getOrThrow()
        val byType = questions.groupBy { it.type }
        OIRQuestionDistribution.counts(50).forEach { (type, expected) ->
            assertEquals(
                "Count for $type should match OIRQuestionDistribution.counts(50)",
                expected, byType[type]?.size ?: 0
            )
        }
    }

    @Test
    fun `selectQuestions returns exactly 50 across many seeds`() = runTest {
        repeat(50) {
            val questions = selector.selectQuestions(count = 50).getOrThrow()
            assertEquals(50, questions.size)
        }
    }

    // --- Phase 4: toEntity / toDomain correctAnswerIds serialisation ---

    @Test
    fun `toEntity with correctAnswerIds list serialises to JSON`() {
        val questionMap = mapOf(
            "id" to "q1",
            "questionNumber" to 1L,
            "type" to "VERBAL_REASONING",
            "questionText" to "Which two figures belong to Class A?",
            "options" to listOf(
                mapOf("id" to "opt_a", "text" to "A"),
                mapOf("id" to "opt_b", "text" to "B")
            ),
            "correctAnswerId" to "",
            "correctAnswerIds" to listOf("opt_b", "opt_c"),
            "explanation" to "Answer: 2 and 3.",
            "difficulty" to "MEDIUM",
            "tags" to listOf<String>()
        )
        val entity = selector.toEntity(questionMap, "batch1", 0)
        // Must serialise the list as a JSON array; exact string form is Gson default
        assertEquals("""["opt_b","opt_c"]""", entity.correctAnswerIds)
    }

    @Test
    fun `toEntity with null correctAnswerIds stores null`() {
        val questionMap = mapOf(
            "id" to "q2",
            "questionNumber" to 2L,
            "type" to "VERBAL_REASONING",
            "questionText" to "Single-answer question?",
            "options" to listOf(mapOf("id" to "opt_a", "text" to "A")),
            "correctAnswerId" to "opt_a",
            "explanation" to "",
            "difficulty" to "MEDIUM",
            "tags" to listOf<String>()
        )
        val entity = selector.toEntity(questionMap, "batch1", 1)
        assertEquals(null, entity.correctAnswerIds)
    }

    @Test
    fun `toDomain with JSON correctAnswerIds deserialises to list`() {
        val entity = makeEntity(OIRQuestionType.VERBAL_REASONING.name, "q3").copy(
            correctAnswerIds = """["opt_b","opt_c"]"""
        )
        val domain = selector.toDomain(entity)
        assertEquals(listOf("opt_b", "opt_c"), domain.correctAnswerIds)
    }

    @Test
    fun `toDomain with null correctAnswerIds returns empty list`() {
        val entity = makeEntity(OIRQuestionType.VERBAL_REASONING.name, "q4").copy(
            correctAnswerIds = null
        )
        val domain = selector.toDomain(entity)
        assertEquals(emptyList<String>(), domain.correctAnswerIds)
    }

    /** A legacy optionless "dud" (no options, no image) — the validator must reject it. */
    private fun makeDud(type: String, id: String) = makeEntity(type, id).copy(
        optionsJson = "[]",
        correctAnswerId = ""
    )

    @Test
    fun `selectQuestions skips duds and still fills the target count`() = runTest {
        // VERBAL pool: 20 valid + 40 duds interleaved. Over-fetch + validate must
        // still return 20 valid verbal and zero duds.
        val verbalPool = buildList {
            (1..20).forEach { add(makeEntity(OIRQuestionType.VERBAL_REASONING.name, "v$it")) }
            (1..40).forEach { add(makeDud(OIRQuestionType.VERBAL_REASONING.name, "dud$it")) }
        }.shuffled()
        coEvery { mockDao.getUnusedQuestionsByType(OIRQuestionType.VERBAL_REASONING.name, any(), any()) } returns verbalPool
        coEvery { mockDao.getQuestionsByType(OIRQuestionType.VERBAL_REASONING.name, any()) } returns verbalPool

        val questions = selector.selectQuestions(count = 50).getOrThrow()

        assertEquals(50, questions.size)
        assertEquals("no duds (empty options) should survive selection", emptyList<Int>(),
            questions.filter { it.options.isEmpty() }.map { 0 })
        assertEquals("verbal target met from valid-only pool", 20,
            questions.count { it.type == OIRQuestionType.VERBAL_REASONING })
    }
}
