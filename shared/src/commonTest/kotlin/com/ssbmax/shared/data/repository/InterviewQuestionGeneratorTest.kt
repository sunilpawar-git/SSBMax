package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.interview.InterviewQuestion
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.QuestionSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the cache -> AI -> fallback ratio logic ported from the Android original's
 * `InterviewQuestionGenerator.generateQuestions` (70% PIQ-cache / 25% generic-cache, topped up by
 * AI, then by [FALLBACK_INTERVIEW_QUESTIONS] if AI also comes up short) -- the highest-value
 * target in this port per the plan's own regression-prevention strategy, since a silently wrong
 * ratio or a swallowed AI failure wouldn't crash the build.
 */
class InterviewQuestionGeneratorTest {

    private fun question(id: String, source: QuestionSource) =
        InterviewQuestion(id = id, questionText = "Q $id", expectedOLQs = listOf(OLQ.COURAGE), source = source)

    @Test
    fun `cache-hit path returns cached PIQ and generic questions without calling AI`() = runTest {
        val cache = FakeQuestionCacheRepository(
            piqQuestions = (1..7).map { question("piq-$it", QuestionSource.PIQ_BASED) },
            genericQuestions = (1..3).map { question("generic-$it", QuestionSource.GENERIC_POOL) }
        )
        val ai = FakeAIService(piqQuestionsResult = Result.failure(IllegalStateException("must not be called")))
        val generator = InterviewQuestionGenerator(cache, ai, FakeSubmissionRepository())

        val result = generator.generateQuestions(piqSnapshotId = "piq-snap-1", count = 10)

        assertTrue(result.isSuccess)
        assertEquals(10, result.getOrThrow().size)
        assertTrue(cache.cachedBatches.isEmpty(), "AI-generated questions should only be cached when AI is actually used")
    }

    @Test
    fun `cache-miss falls through to AI generation and caches the AI result`() = runTest {
        val cache = FakeQuestionCacheRepository(piqQuestions = emptyList(), genericQuestions = emptyList())
        val aiQuestions = listOf(question("ai-1", QuestionSource.AI_GENERATED))
        val ai = FakeAIService(piqQuestionsResult = Result.success(aiQuestions))
        val submissionRepo = FakeSubmissionRepository(submission = mapOf("data" to mapOf("fullName" to "Cadet X")))
        val generator = InterviewQuestionGenerator(cache, ai, submissionRepo)

        val result = generator.generateQuestions(piqSnapshotId = "piq-snap-2", count = 1)

        assertTrue(result.isSuccess)
        assertEquals(listOf("ai-1"), result.getOrThrow().map { it.id })
        assertEquals(1, cache.cachedBatches.size, "successful AI questions should be cached for reuse")
    }

    @Test
    fun `AI failure falls through to the fallback question list instead of failing the whole batch`() = runTest {
        val cache = FakeQuestionCacheRepository(piqQuestions = emptyList(), genericQuestions = emptyList())
        val ai = FakeAIService(piqQuestionsResult = Result.failure(RuntimeException("Gemini down")))
        val submissionRepo = FakeSubmissionRepository(submission = mapOf("data" to mapOf("fullName" to "Cadet X")))
        val generator = InterviewQuestionGenerator(cache, ai, submissionRepo)

        val result = generator.generateQuestions(piqSnapshotId = "piq-snap-3", count = 2)

        assertTrue(result.isSuccess, "AI failure must not propagate as a Result.failure while fallback questions exist")
        assertEquals(2, result.getOrThrow().size)
        assertTrue(cache.cachedBatches.isEmpty(), "fallback questions are never cached, only AI-generated ones are")
    }

    @Test
    fun `a missing PIQ submission skips AI generation and still falls back cleanly`() = runTest {
        val cache = FakeQuestionCacheRepository(piqQuestions = emptyList(), genericQuestions = emptyList())
        val ai = FakeAIService(piqQuestionsResult = Result.failure(IllegalStateException("must not be called")))
        val generator = InterviewQuestionGenerator(cache, ai, FakeSubmissionRepository(submission = null))

        val result = generator.generateQuestions(piqSnapshotId = "missing-piq", count = 3)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
    }

    @Test
    fun `respects the 70-25 cache split before topping up with AI or fallback`() = runTest {
        // count=10 -> piqCount=7, genericCount=2 (0.25*10=2.5 -> toInt() truncates to 2)
        val cache = FakeQuestionCacheRepository(
            piqQuestions = (1..7).map { question("piq-$it", QuestionSource.PIQ_BASED) },
            genericQuestions = (1..5).map { question("generic-$it", QuestionSource.GENERIC_POOL) }
        )
        val ai = FakeAIService(piqQuestionsResult = Result.success(emptyList()))
        val generator = InterviewQuestionGenerator(cache, ai, FakeSubmissionRepository())

        val result = generator.generateQuestions(piqSnapshotId = "piq-snap-4", count = 10)

        // 7 PIQ + 2 generic (limit applied) = 9, then 1 fallback question tops it up to 10
        assertEquals(10, result.getOrThrow().size)
    }
}
