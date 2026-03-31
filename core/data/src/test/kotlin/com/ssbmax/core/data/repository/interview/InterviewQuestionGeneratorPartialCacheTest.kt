package com.ssbmax.core.data.repository.interview

import android.content.Context
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.TEST_PIQ_SUBMISSION_ID
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.createMockPIQData
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.createMockQuestions
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.createMockedContext
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.setupLogMocking
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.tearDownLogMocking
import com.ssbmax.core.domain.constants.InterviewConstants
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.service.AIService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for InterviewQuestionGenerator partial cache scenarios
 *
 * These tests verify the fix for the bug where only 6 questions were generated
 * when cache had partial data (some generic questions but no PIQ questions).
 *
 * The bug was: `if (allQuestions.isEmpty())` should be `if (allQuestions.size < count)`
 */
class InterviewQuestionGeneratorPartialCacheTest {

    private lateinit var generator: InterviewQuestionGenerator
    private lateinit var context: Context
    private lateinit var questionCacheRepository: QuestionCacheRepository
    private lateinit var aiService: AIService
    private lateinit var submissionRepository: SubmissionRepository

    @Before
    fun setUp() {
        setupLogMocking()
        context = createMockedContext()
        questionCacheRepository = mockk(relaxed = true)
        aiService = mockk(relaxed = true)
        submissionRepository = mockk(relaxed = true)

        generator = InterviewQuestionGeneratorTestHelper.createGenerator(
            context = context,
            questionCacheRepository = questionCacheRepository,
            aiService = aiService,
            submissionRepository = submissionRepository
        )
    }

    @After
    fun tearDown() {
        tearDownLogMocking()
    }

    @Test
    fun `generateQuestions with partial cache should generate missing questions via AI`() = runTest {
        // Given - Cache has only 6 generic questions, 0 PIQ questions
        // This is the exact scenario that caused the bug (only 6 questions returned)
        val cachedGenericQuestions = createMockQuestions(6, QuestionSource.GENERIC_POOL)
        val targetCount = InterviewConstants.TARGET_TOTAL_QUESTIONS // 25

        coEvery {
            questionCacheRepository.getPIQQuestions(any(), any(), any())
        } returns Result.success(emptyList()) // No PIQ questions cached

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), any(), any())
        } returns Result.success(cachedGenericQuestions) // Only 6 generic questions

        // Mock PIQ submission fetch for AI generation
        val mockPIQData = createMockPIQData()
        coEvery {
            submissionRepository.getSubmission(TEST_PIQ_SUBMISSION_ID)
        } returns Result.success(mockPIQData)

        // Mock AI generation to return the missing questions
        val missingCount = targetCount - cachedGenericQuestions.size // 25 - 6 = 19
        val aiGeneratedQuestions = createMockQuestions(missingCount, QuestionSource.AI_GENERATED)
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), count = missingCount, any())
        } returns Result.success(aiGeneratedQuestions)

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, targetCount)

        // Then - Should return 25 questions (6 cached + 19 AI-generated)
        assertTrue("Should succeed", result.isSuccess)
        assertEquals(
            "Should return target count of questions",
            targetCount,
            result.getOrNull()?.size
        )

        // Verify AI was called to generate missing questions
        coVerify(exactly = 1) {
            aiService.generatePIQBasedQuestions(any(), any(), count = missingCount, any())
        }
    }

    @Test
    fun `generateQuestions with partial cache and AI failure should use cache plus fallback`() = runTest {
        // Given - Cache has only 6 generic questions
        val cachedGenericQuestions = createMockQuestions(6, QuestionSource.GENERIC_POOL)
        val targetCount = InterviewConstants.TARGET_TOTAL_QUESTIONS // 25

        coEvery {
            questionCacheRepository.getPIQQuestions(any(), any(), any())
        } returns Result.success(emptyList())

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), any(), any())
        } returns Result.success(cachedGenericQuestions)

        // Mock PIQ submission fetch
        val mockPIQData = createMockPIQData()
        coEvery {
            submissionRepository.getSubmission(TEST_PIQ_SUBMISSION_ID)
        } returns Result.success(mockPIQData)

        // AI generation fails
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.failure(Exception("AI unavailable"))

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, targetCount)

        // Then - Should return combined cache + fallback questions
        assertTrue("Should succeed with combined fallback", result.isSuccess)
        val questions = result.getOrNull()
        assertTrue(
            "Should have more than just cached questions (cache + fallback)",
            (questions?.size ?: 0) > cachedGenericQuestions.size
        )
    }

    @Test
    fun `generateQuestions should not skip AI generation when cache has insufficient questions`() = runTest {
        // Given - Cache has some questions but not enough
        val cachedPIQQuestions = createMockQuestions(5, QuestionSource.PIQ_BASED)
        val cachedGenericQuestions = createMockQuestions(3, QuestionSource.GENERIC_POOL)
        val totalCached = cachedPIQQuestions.size + cachedGenericQuestions.size // 8
        val targetCount = 20

        coEvery {
            questionCacheRepository.getPIQQuestions(any(), any(), any())
        } returns Result.success(cachedPIQQuestions)

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), any(), any())
        } returns Result.success(cachedGenericQuestions)

        // Mock PIQ submission fetch
        val mockPIQData = createMockPIQData()
        coEvery {
            submissionRepository.getSubmission(TEST_PIQ_SUBMISSION_ID)
        } returns Result.success(mockPIQData)

        // Mock AI to return the missing questions
        val missingCount = targetCount - totalCached // 20 - 8 = 12
        val aiGeneratedQuestions = createMockQuestions(missingCount, QuestionSource.AI_GENERATED)
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), count = missingCount, any())
        } returns Result.success(aiGeneratedQuestions)

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, targetCount)

        // Then - Should return exactly 20 questions
        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should return target count", targetCount, result.getOrNull()?.size)

        // Verify AI was called to generate missing questions
        coVerify(exactly = 1) {
            aiService.generatePIQBasedQuestions(any(), any(), count = missingCount, any())
        }
    }

    @Test
    fun `generateQuestions with full cache should not call AI`() = runTest {
        // Given - Cache has enough questions (18 PIQ + 6 generic = 24)
        val cachedPIQQuestions = createMockQuestions(18, QuestionSource.PIQ_BASED)
        val cachedGenericQuestions = createMockQuestions(6, QuestionSource.GENERIC_POOL)
        val targetCount = 20

        coEvery {
            questionCacheRepository.getPIQQuestions(any(), any(), any())
        } returns Result.success(cachedPIQQuestions)

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), any(), any())
        } returns Result.success(cachedGenericQuestions)

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, targetCount)

        // Then - Should return 20 questions from cache
        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should return target count", targetCount, result.getOrNull()?.size)

        // Verify AI was NOT called (cache was sufficient)
        coVerify(exactly = 0) { aiService.generatePIQBasedQuestions(any(), any(), any(), any()) }
    }
}
