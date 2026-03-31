package com.ssbmax.core.data.repository.interview

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.service.AIService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.ByteArrayInputStream

/**
 * Test helper for InterviewQuestionGenerator tests
 *
 * Provides common setup, mock creation, and utility methods
 * to keep individual test classes under 300 lines.
 */
object InterviewQuestionGeneratorTestHelper {

    const val TEST_PIQ_SUBMISSION_ID = "piq-test-123"

    /**
     * Creates a configured InterviewQuestionGenerator with mocked dependencies
     */
    fun createGenerator(
        context: Context,
        questionCacheRepository: QuestionCacheRepository,
        aiService: AIService,
        submissionRepository: SubmissionRepository
    ): InterviewQuestionGenerator {
        return InterviewQuestionGenerator(
            context = context,
            questionCacheRepository = questionCacheRepository,
            aiService = aiService,
            submissionRepository = submissionRepository,
            piqDataMapper = PIQDataMapper()
        )
    }

    /**
     * Sets up Android Log mocking for tests
     */
    fun setupLogMocking() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    /**
     * Tears down Android Log mocking
     */
    fun tearDownLogMocking() {
        unmockkStatic(Log::class)
    }

    /**
     * Creates a mocked Context with AssetManager for fallback questions
     */
    fun createMockedContext(fallbackQuestionsJson: String = createMockFallbackQuestionsJson()): Context {
        val context = mockk<Context>(relaxed = true)
        val assetManager = mockk<AssetManager>()
        every { context.assets } returns assetManager
        every { assetManager.open(any()) } returns ByteArrayInputStream(
            fallbackQuestionsJson.toByteArray()
        )
        return context
    }

    /**
     * Creates mock interview questions for testing
     */
    fun createMockQuestions(count: Int, source: QuestionSource): List<InterviewQuestion> {
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

    /**
     * Creates mock PIQ data for testing
     */
    fun createMockPIQData(): Map<String, Any> {
        return mapOf(
            "id" to TEST_PIQ_SUBMISSION_ID,
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

    /**
     * Creates mock fallback questions JSON for testing
     * Contains 27 questions to match the expanded production JSON
     */
    fun createMockFallbackQuestionsJson(): String {
        val questions = (1..27).map { index ->
            """
                {
                    "questionText": "Test question $index",
                    "expectedOLQs": ["SELF_CONFIDENCE", "POWER_OF_EXPRESSION"],
                    "context": "Test context $index"
                }
            """.trimIndent()
        }.joinToString(",\n")

        return """
        {
            "version": "2.0",
            "description": "Test fallback questions",
            "lastUpdated": "2026-02-04",
            "totalQuestions": 27,
            "questions": [
                $questions
            ]
        }
        """.trimIndent()
    }

    /** Minimum questions required in fallback JSON */
    const val MIN_FALLBACK_QUESTIONS = 25
}
