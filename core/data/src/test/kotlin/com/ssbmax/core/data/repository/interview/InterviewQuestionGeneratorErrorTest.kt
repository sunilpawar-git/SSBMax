package com.ssbmax.core.data.repository.interview

import android.content.Context
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.TEST_PIQ_SUBMISSION_ID
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.createMockPIQData
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.createMockQuestions
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.createMockedContext
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.setupLogMocking
import com.ssbmax.core.data.repository.interview.InterviewQuestionGeneratorTestHelper.tearDownLogMocking
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.service.AIService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Error handling and fallback tests for InterviewQuestionGenerator
 *
 * Tests failure scenarios and graceful degradation.
 */
class InterviewQuestionGeneratorErrorTest {

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
    fun `generateQuestions handles cache repository failure gracefully`() = runTest {
        // Given - Cache throws exception
        coEvery {
            questionCacheRepository.getPIQQuestions(any(), any(), any())
        } returns Result.failure(Exception("Cache error"))

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), any(), any())
        } returns Result.failure(Exception("Cache error"))

        val mockPIQData = createMockPIQData()
        coEvery {
            submissionRepository.getSubmission(TEST_PIQ_SUBMISSION_ID)
        } returns Result.success(mockPIQData)

        val aiQuestions = createMockQuestions(10, QuestionSource.AI_GENERATED)
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.success(aiQuestions)

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, 10)

        // Then - Should proceed with AI generation
        assertTrue("Should succeed", result.isSuccess)
    }

    @Test
    fun `generateQuestions handles submission repository failure`() = runTest {
        // Given - Cache is empty and submission fetch fails
        coEvery {
            questionCacheRepository.getPIQQuestions(any(), any(), any())
        } returns Result.success(emptyList())

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), any(), any())
        } returns Result.success(emptyList())

        coEvery {
            submissionRepository.getSubmission(TEST_PIQ_SUBMISSION_ID)
        } returns Result.failure(Exception("Network error"))

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, 5)

        // Then - Should fallback to mock questions
        assertTrue("Should succeed with fallback", result.isSuccess)
        assertTrue("Should have some questions",
            (result.getOrNull()?.size ?: 0) >= 1)
    }

    @Test
    fun `generateQuestions handles AI service failure`() = runTest {
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

        // AI service fails
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.failure(Exception("AI service unavailable"))

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, 5)

        // Then - Should fallback to mock questions
        assertTrue("Should succeed with fallback", result.isSuccess)
        assertTrue("Should have some questions",
            (result.getOrNull()?.size ?: 0) >= 1)
    }

    @Test
    fun `generateQuestions falls back to mock when AI fails`() = runTest {
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

        // AI fails
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.failure(Exception("AI unavailable"))

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, 5)

        // Then - Should still succeed with mock questions
        assertTrue("Should succeed with fallback", result.isSuccess)
        assertTrue("Should have at least 1 question",
            (result.getOrNull()?.size ?: 0) >= 1)
    }

    @Test
    fun `generateQuestions falls back to mock when PIQ fetch fails`() = runTest {
        // Given - Cache is empty
        coEvery {
            questionCacheRepository.getPIQQuestions(any(), any(), any())
        } returns Result.success(emptyList())

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), any(), any())
        } returns Result.success(emptyList())

        // PIQ fetch fails
        coEvery {
            submissionRepository.getSubmission(TEST_PIQ_SUBMISSION_ID)
        } returns Result.failure(Exception("Not found"))

        // When
        val result = generator.generateQuestions(TEST_PIQ_SUBMISSION_ID, 5)

        // Then - Should still succeed with mock questions
        assertTrue("Should succeed with fallback", result.isSuccess)
        assertTrue("Should have at least 1 question",
            (result.getOrNull()?.size ?: 0) >= 1)
    }
}
