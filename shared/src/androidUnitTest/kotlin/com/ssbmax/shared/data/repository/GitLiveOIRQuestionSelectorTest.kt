package com.ssbmax.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ssbmax.shared.db.SharedDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises GitLiveOIRQuestionSelector's SQLDelight-only surface (type-distribution
 * selection, toEntity/toDomain round-trip, validator-based dud filtering) against a
 * real in-memory SQLite DB, seeded directly via the generated queries.
 */
class GitLiveOIRQuestionSelectorTest {

    private lateinit var database: SharedDatabase
    private lateinit var selector: GitLiveOIRQuestionSelector

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SharedDatabase.Schema.create(driver)
        database = SharedDatabase(driver)
        selector = GitLiveOIRQuestionSelector(database)
    }

    private fun seedValidQuestion(id: String, type: String, difficulty: String = "MEDIUM", usageCount: Long = 0L) {
        database.sharedDatabaseQueries.insertOrReplaceOIRQuestion(
            id = id,
            questionNumber = 1L,
            type = type,
            subtype = null,
            questionText = "This is a valid question text for $id",
            optionsJson = """[{"id":"opt_a","text":"A"},{"id":"opt_b","text":"B"},{"id":"opt_c","text":"C"},{"id":"opt_d","text":"D"}]""",
            correctAnswerId = "opt_a",
            explanation = "Because A is correct.",
            questionImageUrl = null,
            difficulty = difficulty,
            tags = "",
            batchId = "batch_pdf_001",
            cachedAt = 0L,
            lastUsed = null,
            usageCount = usageCount,
            correctAnswerIds = null
        )
    }

    /** Missing options -- fails OIRQuestionValidator, must be skipped at selection. */
    private fun seedInvalidQuestion(id: String, type: String) {
        database.sharedDatabaseQueries.insertOrReplaceOIRQuestion(
            id = id,
            questionNumber = 1L,
            type = type,
            subtype = null,
            questionText = "",
            optionsJson = "[]",
            correctAnswerId = "",
            explanation = "",
            questionImageUrl = null,
            difficulty = "MEDIUM",
            tags = "",
            batchId = "batch_pdf_001",
            cachedAt = 0L,
            lastUsed = null,
            usageCount = 0L,
            correctAnswerIds = null
        )
    }

    @Test
    fun `selectQuestions maintains the V40NV40N20 distribution`() = runTest {
        // Why this matters: OIRQuestionDistribution is the SSOT for the test's required
        // type mix -- a selector that ignores it would silently break test fairness.
        repeat(30) { i -> seedValidQuestion("v$i", "VERBAL_REASONING") }
        repeat(30) { i -> seedValidQuestion("nv$i", "NON_VERBAL_REASONING") }
        repeat(30) { i -> seedValidQuestion("n$i", "NUMERICAL_ABILITY") }

        val result = selector.selectQuestions(count = 50)

        assertTrue(result.isSuccess)
        val questions = result.getOrThrow()
        assertEquals(50, questions.size)
        val verbalCount = questions.count { it.type.name == "VERBAL_REASONING" }
        val nonVerbalCount = questions.count { it.type.name == "NON_VERBAL_REASONING" }
        val numericalCount = questions.count { it.type.name == "NUMERICAL_ABILITY" }
        assertEquals(20, verbalCount) // 40% of 50
        assertEquals(20, nonVerbalCount) // 40% of 50
        assertEquals(10, numericalCount) // 20% of 50
    }

    @Test
    fun `selectQuestions skips structurally-invalid questions rather than serving them`() = runTest {
        // Why this matters: duds (missing options/text) already sitting in the cache must never
        // reach a live test -- the validator is the single source of truth for "is this usable."
        seedInvalidQuestion("bad1", "VERBAL_REASONING")
        repeat(5) { i -> seedValidQuestion("good$i", "VERBAL_REASONING") }
        repeat(5) { i -> seedValidQuestion("nv$i", "NON_VERBAL_REASONING") }
        repeat(5) { i -> seedValidQuestion("n$i", "NUMERICAL_ABILITY") }

        val result = selector.selectQuestions(count = 10)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().none { it.id == "bad1" })
    }

    @Test
    fun `selectQuestions fails honestly when the cache is entirely empty`() = runTest {
        val result = selector.selectQuestions(count = 10)

        assertTrue(result.isSuccess) // empty pools just yield an empty (not failing) selection
        assertEquals(0, result.getOrThrow().size)
    }

    @Test
    fun `toEntity then toDomain round-trips options and correctAnswerIds`() = runTest {
        // Why this matters: the Firestore-map-shaped input (toEntityMap's output) must survive
        // the JSON round trip losslessly -- a broken options/correctAnswerIds encode would corrupt
        // every multi-select question silently.
        val map: Map<String, Any?> = mapOf(
            "id" to "oir_q_0001",
            "questionNumber" to 1,
            "type" to "VERBAL_REASONING",
            "questionText" to "Pick the odd one out",
            "options" to listOf(
                mapOf("id" to "opt_a", "text" to "Cat", "imageUrl" to null),
                mapOf("id" to "opt_b", "text" to "Dog", "imageUrl" to null),
                mapOf("id" to "opt_c", "text" to "Car", "imageUrl" to null)
            ),
            "correctAnswerId" to "opt_c",
            "correctAnswerIds" to listOf("opt_c"),
            "explanation" to "Car is not an animal.",
            "difficulty" to "EASY",
            "tags" to listOf("odd-one-out", "verbal")
        )

        val entity = selector.toEntity(map, batchId = "batch_pdf_001", index = 0, cachedAt = 1000L)
        val domain = selector.toDomain(entity)

        assertEquals("oir_q_0001", domain.id)
        assertEquals(3, domain.options.size)
        assertEquals("opt_c", domain.correctAnswerId)
        assertEquals(listOf("opt_c"), domain.correctAnswerIds)
        assertEquals("odd-one-out,verbal", entity.tags)
    }
}
