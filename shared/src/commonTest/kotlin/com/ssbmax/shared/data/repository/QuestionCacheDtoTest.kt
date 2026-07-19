package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.QuestionCacheEntry
import com.ssbmax.shared.domain.model.interview.QuestionCacheType
import com.ssbmax.shared.domain.model.interview.QuestionSource
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Round-trips InterviewQuestion/QuestionCacheEntry through the Firestore wire
 * DTOs GitLiveQuestionCacheRepository depends on. Why it matters: the Android
 * original's generic_questions collection has both "questionText" (current)
 * and legacy "text" documents (QuestionCacheMappers.mapToQuestion's fallback)
 * — dropping that fallback here would silently return zero questions for any
 * un-migrated legacy document.
 */
class QuestionCacheDtoTest {

    private val question = InterviewQuestion(
        id = "q-1",
        questionText = "Tell me about a time you led a team.",
        expectedOLQs = listOf(OLQ.INITIATIVE, OLQ.INFLUENCE_GROUP),
        context = "leadership",
        source = QuestionSource.GENERIC_POOL
    )

    @Test
    fun `interview question round-trips through its wire DTO`() {
        assertEquals(question, question.toDto().toDomain())
    }

    @Test
    fun `cache entry round-trips including nested question and expiry`() {
        val entry = QuestionCacheEntry(
            id = "entry-1",
            question = question,
            cacheKey = "piq-snapshot-1",
            cacheType = QuestionCacheType.PIQ_BASED,
            createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
            usageCount = 2,
            lastUsedAt = Instant.fromEpochMilliseconds(1_700_000_500_000L),
            expiresAt = Instant.fromEpochMilliseconds(1_702_600_000_000L)
        )
        assertEquals(entry, entry.toDto().toDomain())
    }

    @Test
    fun `generic question document uses questionText when present`() {
        val dto = GenericQuestionDto(
            id = "g-1",
            questionText = "Describe a challenge you overcame.",
            targetOLQs = listOf(OLQ.DETERMINATION.name),
            source = QuestionSource.GENERIC_POOL.name
        )
        val domain = dto.toDomain()
        assertEquals("Describe a challenge you overcame.", domain?.questionText)
        assertEquals(listOf(OLQ.DETERMINATION), domain?.expectedOLQs)
    }

    @Test
    fun `generic question document falls back to legacy text field when questionText is absent`() {
        val dto = GenericQuestionDto(id = "g-2", questionText = null, text = "Legacy field question")
        assertEquals("Legacy field question", dto.toDomain()?.questionText)
    }

    @Test
    fun `generic question document with neither field maps to null instead of throwing`() {
        val dto = GenericQuestionDto(id = "g-3", questionText = null, text = null)
        assertNull(dto.toDomain())
    }

    @Test
    fun `unrecognized OLQ names are dropped instead of throwing`() {
        val dto = InterviewQuestionDto(
            id = "q-2",
            questionText = "text",
            targetOLQs = listOf("NOT_A_REAL_OLQ", OLQ.COOPERATION.name),
            source = QuestionSource.GENERIC_POOL.name
        )
        assertEquals(listOf(OLQ.COOPERATION), dto.toDomain().expectedOLQs)
    }
}
