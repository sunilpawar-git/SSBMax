package com.ssbmax.ui.tests.oir

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OIRTestResultViewModelTest : BaseViewModelTest() {

    private lateinit var viewModel: OIRTestResultViewModel
    private val mockSubmissionRepository = mockk<SubmissionRepository>(relaxed = true)

    private val mockResultData = mapOf(
        "testId" to "oir-test-123",
        "userId" to "user-123",
        "data" to mapOf(
            "testId" to "oir-test-123",
            "userId" to "user-123",
            "totalQuestions" to 50,
            "correctAnswers" to 42,
            "incorrectAnswers" to 6,
            "skippedQuestions" to 2,
            "totalTimeSeconds" to 1800,
            "timeTakenSeconds" to 1650,
            "rawScore" to 42,
            "percentageScore" to 84.0f,
            "completedAt" to System.currentTimeMillis(),
            "categoryScores" to mapOf(
                "VERBAL_REASONING" to mapOf(
                    "totalQuestions" to 15,
                    "correctAnswers" to 13,
                    "percentage" to 86.7f,
                    "averageTimeSeconds" to 30
                ),
                "NON_VERBAL_REASONING" to mapOf(
                    "totalQuestions" to 15,
                    "correctAnswers" to 14,
                    "percentage" to 93.3f,
                    "averageTimeSeconds" to 28
                ),
                "NUMERICAL_ABILITY" to mapOf(
                    "totalQuestions" to 20,
                    "correctAnswers" to 15,
                    "percentage" to 75.0f,
                    "averageTimeSeconds" to 35
                )
            ),
            "difficultyBreakdown" to mapOf(
                "EASY" to mapOf(
                    "totalQuestions" to 20,
                    "correctAnswers" to 19,
                    "percentage" to 95.0f
                ),
                "MEDIUM" to mapOf(
                    "totalQuestions" to 20,
                    "correctAnswers" to 17,
                    "percentage" to 85.0f
                ),
                "HARD" to mapOf(
                    "totalQuestions" to 10,
                    "correctAnswers" to 6,
                    "percentage" to 60.0f
                )
            )
        )
    )

    @Before
    fun setup() {
        coEvery { mockSubmissionRepository.getSubmission(any()) } returns 
            Result.success(mockResultData)

        viewModel = OIRTestResultViewModel(mockSubmissionRepository)
    }

    @Test
    fun `initial state is loading`() = runTest {
        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertNull(state.result)
        assertNull(state.error)
    }

    @Test
    fun `loadResult success parses and updates state`() = runTest {
        viewModel.loadResult("session-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.result)
        assertNull(state.error)

        val result = state.result!!
        assertEquals("oir-test-123", result.testId)
        assertEquals("session-123", result.sessionId)
        assertEquals("user-123", result.userId)
        assertEquals(50, result.totalQuestions)
        assertEquals(42, result.correctAnswers)
        assertEquals(6, result.incorrectAnswers)
        assertEquals(2, result.skippedQuestions)
        assertEquals(84.0f, result.percentageScore, 0.01f)

        coVerify { mockSubmissionRepository.getSubmission("session-123") }
    }

    @Test
    fun `loadResult parses category scores correctly`() = runTest {
        viewModel.loadResult("session-123")
        advanceUntilIdle()

        val result = viewModel.uiState.value.result!!

        assertEquals(3, result.categoryScores.size)

        val verbalScore = result.categoryScores[OIRQuestionType.VERBAL_REASONING]
        assertNotNull(verbalScore)
        assertEquals(15, verbalScore?.totalQuestions)
        assertEquals(13, verbalScore?.correctAnswers)
        assertEquals(86.7f, verbalScore?.percentage ?: 0f, 0.1f)

        val nonVerbalScore = result.categoryScores[OIRQuestionType.NON_VERBAL_REASONING]
        assertNotNull(nonVerbalScore)
        assertEquals(93.3f, nonVerbalScore?.percentage ?: 0f, 0.1f)

        val numericalScore = result.categoryScores[OIRQuestionType.NUMERICAL_ABILITY]
        assertNotNull(numericalScore)
        assertEquals(75.0f, numericalScore?.percentage ?: 0f, 0.1f)
    }

    @Test
    fun `loadResult parses difficulty breakdown correctly`() = runTest {
        viewModel.loadResult("session-123")
        advanceUntilIdle()

        val result = viewModel.uiState.value.result!!

        assertEquals(3, result.difficultyBreakdown.size)

        val easyScore = result.difficultyBreakdown[QuestionDifficulty.EASY]
        assertNotNull(easyScore)
        assertEquals(20, easyScore?.totalQuestions)
        assertEquals(19, easyScore?.correctAnswers)
        assertEquals(95.0f, easyScore?.percentage ?: 0f, 0.1f)

        val mediumScore = result.difficultyBreakdown[QuestionDifficulty.MEDIUM]
        assertNotNull(mediumScore)
        assertEquals(85.0f, mediumScore?.percentage ?: 0f, 0.1f)

        val hardScore = result.difficultyBreakdown[QuestionDifficulty.HARD]
        assertNotNull(hardScore)
        assertEquals(60.0f, hardScore?.percentage ?: 0f, 0.1f)
    }

    @Test
    fun `loadResult shows error when submission not found`() = runTest {
        coEvery { mockSubmissionRepository.getSubmission(any()) } returns 
            Result.success(null)

        viewModel.loadResult("invalid-session")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNull(state.result)
        assertNotNull(state.error)
        assertEquals("Test result not found", state.error)
    }

    @Test
    fun `loadResult handles repository failure gracefully`() = runTest {
        coEvery { mockSubmissionRepository.getSubmission(any()) } returns 
            Result.failure(Exception("Network error"))

        viewModel.loadResult("session-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNull(state.result)
        assertNotNull(state.error)
        assertEquals("Network error", state.error)
    }

    @Test
    fun `loadResult handles malformed data gracefully`() = runTest {
        val malformedData = mapOf(
            "testId" to "oir-test-123",
            "data" to mapOf(
                "invalidField" to "value"
                // Missing required fields
            )
        )
        coEvery { mockSubmissionRepository.getSubmission(any()) } returns 
            Result.success(malformedData)

        viewModel.loadResult("session-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        // Parser uses default values for missing fields, so result is created
        assertNotNull(state.result)
        assertEquals(0, state.result?.totalQuestions)
        assertEquals(0f, state.result?.percentageScore)
    }

    @Test
    fun `loadResult handles missing data field`() = runTest {
        val dataWithoutDataField = mapOf(
            "testId" to "oir-test-123",
            "userId" to "user-123"
            // Missing "data" field
        )
        coEvery { mockSubmissionRepository.getSubmission(any()) } returns 
            Result.success(dataWithoutDataField)

        viewModel.loadResult("session-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNull(state.result)
        assertNotNull(state.error)
    }

    @Test
    fun `loadResult handles empty category scores`() = runTest {
        val dataWithEmptyCategories = mapOf(
            "testId" to "oir-test-123",
            "userId" to "user-123",
            "data" to mapOf(
                "testId" to "oir-test-123",
                "userId" to "user-123",
                "totalQuestions" to 50,
                "correctAnswers" to 42,
                "incorrectAnswers" to 6,
                "skippedQuestions" to 2,
                "totalTimeSeconds" to 1800,
                "timeTakenSeconds" to 1650,
                "rawScore" to 42,
                "percentageScore" to 84.0f,
                "completedAt" to System.currentTimeMillis(),
                "categoryScores" to emptyMap<String, Any>(),
                "difficultyBreakdown" to emptyMap<String, Any>()
            )
        )
        coEvery { mockSubmissionRepository.getSubmission(any()) } returns 
            Result.success(dataWithEmptyCategories)

        viewModel.loadResult("session-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.result)
        assertTrue(state.result!!.categoryScores.isEmpty())
        assertTrue(state.result!!.difficultyBreakdown.isEmpty())
    }

    @Test
    fun `loadResult handles invalid enum values in category scores`() = runTest {
        val dataWithInvalidCategory = mapOf(
            "testId" to "oir-test-123",
            "userId" to "user-123",
            "data" to mapOf(
                "testId" to "oir-test-123",
                "userId" to "user-123",
                "totalQuestions" to 50,
                "correctAnswers" to 42,
                "incorrectAnswers" to 6,
                "skippedQuestions" to 2,
                "totalTimeSeconds" to 1800,
                "timeTakenSeconds" to 1650,
                "rawScore" to 42,
                "percentageScore" to 84.0f,
                "completedAt" to System.currentTimeMillis(),
                "categoryScores" to mapOf(
                    "INVALID_CATEGORY" to mapOf(
                        "totalQuestions" to 15,
                        "correctAnswers" to 13,
                        "percentage" to 86.7f,
                        "averageTimeSeconds" to 30
                    )
                ),
                "difficultyBreakdown" to emptyMap<String, Any>()
            )
        )
        coEvery { mockSubmissionRepository.getSubmission(any()) } returns 
            Result.success(dataWithInvalidCategory)

        viewModel.loadResult("session-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.result)
        // Invalid category should be filtered out
        assertTrue(state.result!!.categoryScores.isEmpty())
    }
}

