package com.ssbmax.core.data.repository.interview

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.ssbmax.core.domain.constants.InterviewConstants
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.service.AIService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Unit tests for InterviewQuestionGenerator
 *
 * Tests the question generation flow that:
 * - Uses comprehensive PIQ context from PIQDataMapper
 * - Generates questions via AI service
 * - Falls back to cached/mock questions when needed
 * - Caches generated questions for future use
 *
 * Key test areas:
 * - Question generation with full PIQ context
 * - Cache hit/miss handling
 * - AI service failure fallback
 * - Question distribution (PIQ vs generic)
 */
class InterviewQuestionGeneratorTest {

    private lateinit var generator: InterviewQuestionGenerator
    private lateinit var context: Context
    private lateinit var questionCacheRepository: QuestionCacheRepository
    private lateinit var aiService: AIService
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var piqDataMapper: PIQDataMapper

    private val testPIQSubmissionId = "piq-test-123"

    @Before
    fun setUp() {
        // Mock Android Log class
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        context = mockk(relaxed = true)
        questionCacheRepository = mockk(relaxed = true)
        aiService = mockk(relaxed = true)
        submissionRepository = mockk(relaxed = true)
        piqDataMapper = PIQDataMapper() // Use real mapper to test integration

        // Mock assets for fallback questions
        val assetManager = mockk<AssetManager>()
        every { context.assets } returns assetManager
        every { assetManager.open(any()) } returns ByteArrayInputStream(
            createMockFallbackQuestionsJson().toByteArray()
        )

        generator = InterviewQuestionGenerator(
            context = context,
            questionCacheRepository = questionCacheRepository,
            aiService = aiService,
            submissionRepository = submissionRepository,
            piqDataMapper = piqDataMapper
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
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
                piqSnapshotId = testPIQSubmissionId,
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
        val result = generator.generateQuestions(testPIQSubmissionId, 20)

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
            submissionRepository.getSubmission(testPIQSubmissionId)
        } returns Result.success(mockPIQData)

        // Mock AI generation
        val aiGeneratedQuestions = createMockQuestions(10, QuestionSource.AI_GENERATED)
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.success(aiGeneratedQuestions)

        // When
        val result = generator.generateQuestions(testPIQSubmissionId, 10)

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
            submissionRepository.getSubmission(testPIQSubmissionId)
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
        generator.generateQuestions(testPIQSubmissionId, 10)

        // Then - Verify comprehensive context was passed
        val passedContext = piqDataSlot.captured
        assertTrue("Should contain CANDIDATE PROFILE header", 
            passedContext.contains("CANDIDATE PROFILE"))
        assertTrue("Should contain PERSONAL BACKGROUND", 
            passedContext.contains("PERSONAL BACKGROUND"))
        assertTrue("Should contain FAMILY ENVIRONMENT", 
            passedContext.contains("FAMILY ENVIRONMENT"))
        assertTrue("Should contain candidate name", 
            passedContext.contains("Test Candidate"))
        assertTrue("Should contain PERSONALIZATION NOTES", 
            passedContext.contains("PERSONALIZATION NOTES"))
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
            submissionRepository.getSubmission(testPIQSubmissionId)
        } returns Result.success(mockPIQData)

        val aiGeneratedQuestions = createMockQuestions(10, QuestionSource.AI_GENERATED)
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.success(aiGeneratedQuestions)

        // When
        generator.generateQuestions(testPIQSubmissionId, 10)

        // Then - Verify questions were cached
        coVerify(exactly = 1) {
            questionCacheRepository.cachePIQQuestions(
                piqSnapshotId = testPIQSubmissionId,
                questions = aiGeneratedQuestions,
                expirationDays = InterviewConstants.DEFAULT_CACHE_EXPIRATION_DAYS
            )
        }
    }

    // ============================================
    // FALLBACK TESTS
    // ============================================

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
            submissionRepository.getSubmission(testPIQSubmissionId)
        } returns Result.success(mockPIQData)

        // AI fails
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.failure(Exception("AI unavailable"))

        // When
        val result = generator.generateQuestions(testPIQSubmissionId, 5)

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
            submissionRepository.getSubmission(testPIQSubmissionId)
        } returns Result.failure(Exception("Not found"))

        // When
        val result = generator.generateQuestions(testPIQSubmissionId, 5)

        // Then - Should still succeed with mock questions
        assertTrue("Should succeed with fallback", result.isSuccess)
        assertTrue("Should have at least 1 question", 
            (result.getOrNull()?.size ?: 0) >= 1)
    }

    // ============================================
    // QUESTION DISTRIBUTION TESTS
    // ============================================

    @Test
    fun `generateQuestions respects 70-25 distribution ratio`() = runTest {
        // Given - Request 20 questions
        // Expected: 14 PIQ (70%) + 5 Generic (25%) = 19, rounded
        val cachedPIQQuestions = createMockQuestions(14, QuestionSource.PIQ_BASED)
        val cachedGenericQuestions = createMockQuestions(5, QuestionSource.GENERIC_POOL)

        coEvery {
            questionCacheRepository.getPIQQuestions(any(), limit = any(), any())
        } returns Result.success(cachedPIQQuestions)

        coEvery {
            questionCacheRepository.getGenericQuestions(any(), any(), limit = any(), any())
        } returns Result.success(cachedGenericQuestions)

        // When
        val result = generator.generateQuestions(testPIQSubmissionId, 20)

        // Then
        assertTrue("Should succeed", result.isSuccess)

        // Verify correct limits were requested (70% and 25% of 20)
        coVerify {
            questionCacheRepository.getPIQQuestions(
                piqSnapshotId = testPIQSubmissionId,
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

    // ============================================
    // ERROR HANDLING TESTS
    // ============================================

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
            submissionRepository.getSubmission(testPIQSubmissionId)
        } returns Result.success(mockPIQData)

        val aiQuestions = createMockQuestions(10, QuestionSource.AI_GENERATED)
        coEvery {
            aiService.generatePIQBasedQuestions(any(), any(), any(), any())
        } returns Result.success(aiQuestions)

        // When
        val result = generator.generateQuestions(testPIQSubmissionId, 10)

        // Then - Should proceed with AI generation
        assertTrue("Should succeed", result.isSuccess)
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private fun createMockQuestions(count: Int, source: QuestionSource): List<InterviewQuestion> {
        return List(count) { index ->
            InterviewQuestion(
                id = "q-$source-$index",
                questionText = "Test question $index for $source",
                expectedOLQs = listOf(OLQ.EFFECTIVE_INTELLIGENCE, OLQ.INITIATIVE),
                context = "Test context",
                source = source
            )
        }
    }

    private fun createMockPIQData(): Map<String, Any> {
        return mapOf(
            "id" to testPIQSubmissionId,
            "data" to mapOf(
                "fullName" to "Test Candidate",
                "age" to "25",
                "gender" to "Male",
                "state" to "Maharashtra",
                "district" to "Pune",
                "fatherName" to "Father Name",
                "fatherOccupation" to "Engineer",
                "motherName" to "Mother Name",
                "motherOccupation" to "Teacher",
                "hobbies" to "Reading, Sports",
                "whyDefenseForces" to "To serve the nation",
                "strengths" to "Leadership",
                "weaknesses" to "Perfectionism",
                "nccTraining" to mapOf(
                    "hasTraining" to true,
                    "wing" to "Army",
                    "certificateObtained" to "C Certificate"
                ),
                "previousInterviews" to listOf(
                    mapOf(
                        "typeOfEntry" to "CDS",
                        "ssbPlace" to "Allahabad"
                    )
                )
            )
        )
    }

    private fun createMockFallbackQuestionsJson(): String {
        return """
        {
            "version": "1.0",
            "description": "Test fallback questions",
            "lastUpdated": "2024-01-01",
            "questions": [
                {
                    "questionText": "Tell me about yourself.",
                    "expectedOLQs": ["SELF_CONFIDENCE", "POWER_OF_EXPRESSION"],
                    "context": "Introduction question"
                },
                {
                    "questionText": "Why do you want to join the armed forces?",
                    "expectedOLQs": ["DETERMINATION", "SENSE_OF_RESPONSIBILITY"],
                    "context": "Motivation question"
                }
            ]
        }
        """.trimIndent()
    }
}
