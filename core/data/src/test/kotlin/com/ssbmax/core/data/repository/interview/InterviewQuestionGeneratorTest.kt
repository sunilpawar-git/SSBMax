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
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for InterviewQuestionGenerator
 *
 * Tests the question generation flow including:
 * - Cache hit/miss handling
 * - AI service integration
 * - Fallback behavior
 * - Question distribution
 *
 * Partial cache tests are in InterviewQuestionGeneratorPartialCacheTest.kt
 */
class InterviewQuestionGeneratorTest {

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

    // ============================================
    // CACHE HIT TESTS
    // ============================================

    @Test
    fun `generateQuestions returns cached questions when available`() = runTest {
        // Given - Cache has questions
        val cachedPIQQuestions = createMockQuestions(14, QuestionSource.PIQ_BASED)
        val cachedGenericQuestions = createMockQuestions(6, QuestionSource.GENERIC_POOL)

        coEvery {
            questionCacheRepository.getPIQQuestions(
                piqSnapshotId = TEST_PIQ_SUBMISSION_ID,
                limit = any(),
                excludeUsed = true
            )
        } returns Result.success(cachedPIQQuestions)

        coEvery {
            questionCacheRepository.getGenericQuestions(
                targetOLQs = null,
                difficulty = any(),
                limit = any(),
                excludeUsed = true
            )
        } returns Result.success(cachedGenericQuestions)

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, 20)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should return 20 questions", 20, result.getOrNull()?.size)

        // Verify AI was NOT called (cache hit)
        coVerify(exactly = 0) { aiService.generatePIQBasedQuestions(any(), any(), any(), any()) }
    }

    // ============================================
    // CACHE MISS + AI GENERATION TESTS
    // ============================================

    @Test
    fun `generateQuestions uses AI when cache is empty`() = runTest {
        // Given - Cache is empty
        coEvery {
            questionCacheRepository.getPIQQuestions(any(), any(), any())
        } returns Result.success(emptyList())

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), any(), any())
        } returns Result.success(emptyList())

        // Mock PIQ submission fetch
        val mockPIQData = createMockPIQData()
        coEvery {
            submissionRepository.getSubmission(TEST_PIQ_SUBMISSION_ID)
        } returns Result.success(mockPIQData)

        // Mock AI generation
        val aiGeneratedQuestions = createMockQuestions(10, QuestionSource.AI_GENERATED)
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.success(aiGeneratedQuestions)

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, 10)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should return AI-generated questions", 10, result.getOrNull()?.size)

        // Verify AI was called
        coVerify(exactly = 1) { aiService.generatePIQBasedQuestions(any(), any(), any(), any()) }
    }

    @Test
    fun `generateQuestions passes comprehensive PIQ context to AI`() = runTest {
        // Given - Cache is empty
        coEvery {
            questionCacheRepository.getPIQQuestions(any(), any(), any())
        } returns Result.success(emptyList())

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), any(), any())
        } returns Result.success(emptyList())

        // Mock PIQ submission with rich data
        val mockPIQData = createMockPIQData()
        coEvery {
            submissionRepository.getSubmission(TEST_PIQ_SUBMISSION_ID)
        } returns Result.success(mockPIQData)

        // Capture the piqData parameter
        val piqDataSlot = slot<String>()
        coEvery {
            aiService.generatePIQBasedQuestions(
                piqData = capture(piqDataSlot),
                targetOLQs = any(),
                count = any(),
                difficulty = any()
            )
        } returns Result.success(createMockQuestions(10, QuestionSource.AI_GENERATED))

        // When
        generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, 10)

        // Then - Verify comprehensive context was passed
        val passedContext = piqDataSlot.captured
        assertTrue("Should contain CANDIDATE PROFILE header",
            passedContext.contains("CANDIDATE PROFILE"))
        assertTrue("Should contain PERSONAL BACKGROUND",
            passedContext.contains("PERSONAL BACKGROUND"))
        assertTrue("Should contain candidate name",
            passedContext.contains("Test Candidate"))
    }

    @Test
    fun `generateQuestions caches AI-generated questions`() = runTest {
        // Given - Cache is empty
        coEvery {
            questionCacheRepository.getPIQQuestions(any(), any(), any())
        } returns Result.success(emptyList())

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), any(), any())
        } returns Result.success(emptyList())

        val mockPIQData = createMockPIQData()
        coEvery {
            submissionRepository.getSubmission(TEST_PIQ_SUBMISSION_ID)
        } returns Result.success(mockPIQData)

        val aiGeneratedQuestions = createMockQuestions(10, QuestionSource.AI_GENERATED)
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.success(aiGeneratedQuestions)

        // When
        generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, 10)

        // Then - Verify questions were cached
        coVerify(exactly = 1) {
            questionCacheRepository.cachePIQQuestions(
                piqSnapshotId = TEST_PIQ_SUBMISSION_ID,
                questions = aiGeneratedQuestions,
                expirationDays = InterviewConstants.DEFAULT_CACHE_EXPIRATION_DAYS
            )
        }
    }

    // Fallback tests are in InterviewQuestionGeneratorErrorTest.kt

    // ============================================
    // QUESTION DISTRIBUTION TESTS
    // ============================================

    @Test
    fun `generateQuestions respects 70-25 distribution ratio`() = runTest {
        // Given - Request 20 questions, cache has enough (14 PIQ + 6 generic = 20)
        val cachedPIQQuestions = createMockQuestions(14, QuestionSource.PIQ_BASED)
        val cachedGenericQuestions = createMockQuestions(6, QuestionSource.GENERIC_POOL)

        coEvery {
            questionCacheRepository.getPIQQuestions(any(), limit = any(), any())
        } returns Result.success(cachedPIQQuestions)

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), limit = any(), any())
        } returns Result.success(cachedGenericQuestions)

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, 20)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should return 20 questions", 20, result.getOrNull()?.size)

        // Verify correct limits were requested (70% and 25% of 20)
        coVerify {
            questionCacheRepository.getPIQQuestions(
                piqSnapshotId = TEST_PIQ_SUBMISSION_ID,
                limit = 14, // 70% of 20
                excludeUsed = true
            )
        }
        coVerify {
            questionCacheRepository.getGenericQuestions(
                targetOLQs = null,
                difficulty = any(),
                limit = 5, // 25% of 20
                excludeUsed = true
            )
        }
    }

    // Error handling tests are in InterviewQuestionGeneratorErrorTest.kt
}
