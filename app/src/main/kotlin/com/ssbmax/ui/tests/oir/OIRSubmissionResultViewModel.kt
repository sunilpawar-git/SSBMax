package com.ssbmax.ui.tests.oir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.CategoryScore
import com.ssbmax.core.domain.model.DifficultyScore
import com.ssbmax.core.domain.model.OIRAnswer
import com.ssbmax.core.domain.model.OIRAnsweredQuestion
import com.ssbmax.core.domain.model.OIROption
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.core.domain.model.OIRQuestionType
import com.ssbmax.core.domain.model.OIRTestResult
import com.ssbmax.core.domain.model.QuestionDifficulty
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for OIR Submission Result Screen
 * Fetches OIR submission data from SubmissionRepository
 *
 * This replaces the OIRTestResultHolder anti-pattern by properly fetching
 * the result from Firestore using the submissionId
 */
@HiltViewModel
class OIRSubmissionResultViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OIRSubmissionResultUiState())
    val uiState: StateFlow<OIRSubmissionResultUiState> = _uiState.asStateFlow()

    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            submissionRepository.getSubmission(submissionId)
                .onSuccess { data ->
                    if (data == null) {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Submission not found"
                        ) }
                        return@onSuccess
                    }

                    // Parse OIR submission from Firestore document
                    val result = parseOIRTestResult(data)
                    if (result != null) {
                        _uiState.update { it.copy(
                            isLoading = false,
                            result = result
                        ) }
                    } else {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Failed to parse submission data"
                        ) }
                    }
                }
                .onFailure { error ->
                    ErrorLogger.logTestError(error, "Failed to load OIR submission result", "OIR")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load submission"
                    ) }
                }
        }
    }

    /**
     * Retry loading the submission (for error recovery)
     */
    fun retry(submissionId: String) {
        loadSubmission(submissionId)
    }

    /**
     * Parse OIR test result from Firestore document data
     */
    private fun parseOIRTestResult(data: Map<String, Any>): OIRTestResult? {
        return try {
            // The OIRTestResult is nested under the "data" -> "testResult" field
            val submissionData = data["data"] as? Map<*, *> ?: return null
            val resultData = submissionData["testResult"] as? Map<*, *> ?: return null

            val categoryScores = parseCategoryScores(resultData["categoryScores"] as? Map<*, *>)
            val difficultyBreakdown = parseDifficultyBreakdown(resultData["difficultyBreakdown"] as? Map<*, *>)
            val answeredQuestions = parseAnsweredQuestions(resultData["answeredQuestions"] as? List<*>)

            buildOIRTestResult(resultData, categoryScores, difficultyBreakdown, answeredQuestions)
        } catch (e: Exception) {
            ErrorLogger.logTestError(e, "Error parsing OIR test result data", "OIR")
            null
        }
    }

    private fun parseCategoryScores(categoryScoresMap: Map<*, *>?): Map<OIRQuestionType, CategoryScore> {
        return (categoryScoresMap ?: emptyMap<String, Any>()).mapNotNull { (key, value) ->
            val scoreData = value as? Map<*, *> ?: return@mapNotNull null
            val categoryName = key as? String ?: return@mapNotNull null

            try {
                val category = OIRQuestionType.valueOf(categoryName)
                val categoryScore = CategoryScore(
                    category = category,
                    totalQuestions = (scoreData["totalQuestions"] as? Number)?.toInt() ?: 0,
                    correctAnswers = (scoreData["correctAnswers"] as? Number)?.toInt() ?: 0,
                    percentage = (scoreData["percentage"] as? Number)?.toFloat() ?: 0f,
                    averageTimeSeconds = (scoreData["averageTimeSeconds"] as? Number)?.toInt() ?: 0
                )
                category to categoryScore
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }

    private fun parseDifficultyBreakdown(difficultyBreakdownMap: Map<*, *>?): Map<QuestionDifficulty, DifficultyScore> {
        return (difficultyBreakdownMap ?: emptyMap<String, Any>()).mapNotNull { (key, value) ->
            val scoreData = value as? Map<*, *> ?: return@mapNotNull null
            val difficultyName = key as? String ?: return@mapNotNull null

            try {
                val difficulty = QuestionDifficulty.valueOf(difficultyName)
                val difficultyScore = DifficultyScore(
                    difficulty = difficulty,
                    totalQuestions = (scoreData["totalQuestions"] as? Number)?.toInt() ?: 0,
                    correctAnswers = (scoreData["correctAnswers"] as? Number)?.toInt() ?: 0,
                    percentage = (scoreData["percentage"] as? Number)?.toFloat() ?: 0f
                )
                difficulty to difficultyScore
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }

    private fun parseAnsweredQuestions(answeredQuestionsList: List<*>?): List<OIRAnsweredQuestion> {
        return (answeredQuestionsList ?: emptyList<Any>()).mapNotNull { aqData ->
            val aq = aqData as? Map<*, *> ?: return@mapNotNull null
            parseSingleAnsweredQuestion(aq)
        }
    }

    private fun parseSingleAnsweredQuestion(aq: Map<*, *>): OIRAnsweredQuestion? {
        return try {
            // Create minimal question object (we don't need full question data for result screen)
            val question = OIRQuestion(
                id = aq["questionId"] as? String ?: "",
                questionNumber = (aq["questionNumber"] as? Number)?.toInt() ?: 0,
                type = OIRQuestionType.valueOf(aq["questionType"] as? String ?: "VERBAL_REASONING"),
                questionText = aq["questionText"] as? String ?: "",
                options = emptyList(), // Not needed for result display
                correctAnswerId = aq["correctAnswerId"] as? String ?: "",
                explanation = "",
                difficulty = QuestionDifficulty.valueOf(aq["difficulty"] as? String ?: "MEDIUM")
            )

            val userAnswer = OIRAnswer(
                questionId = aq["questionId"] as? String ?: "",
                selectedOptionId = aq["selectedOptionId"] as? String,
                isCorrect = aq["isCorrect"] as? Boolean ?: false,
                timeTakenSeconds = (aq["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                skipped = aq["skipped"] as? Boolean ?: false
            )

            // Create placeholder options for correct/selected
            val correctOption = OIROption(id = question.correctAnswerId, text = "")
            val selectedOption = userAnswer.selectedOptionId?.let {
                OIROption(id = it, text = "")
            }

            OIRAnsweredQuestion(
                question = question,
                userAnswer = userAnswer,
                isCorrect = aq["isCorrect"] as? Boolean ?: false,
                correctOption = correctOption,
                selectedOption = selectedOption
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun buildOIRTestResult(
        resultData: Map<*, *>,
        categoryScores: Map<OIRQuestionType, CategoryScore>,
        difficultyBreakdown: Map<QuestionDifficulty, DifficultyScore>,
        answeredQuestions: List<OIRAnsweredQuestion>
    ): OIRTestResult {
        return OIRTestResult(
            testId = resultData["testId"] as? String ?: "",
            sessionId = resultData["sessionId"] as? String ?: "",
            userId = resultData["userId"] as? String ?: "",
            totalQuestions = (resultData["totalQuestions"] as? Number)?.toInt() ?: 0,
            correctAnswers = (resultData["correctAnswers"] as? Number)?.toInt() ?: 0,
            incorrectAnswers = (resultData["incorrectAnswers"] as? Number)?.toInt() ?: 0,
            skippedQuestions = (resultData["skippedQuestions"] as? Number)?.toInt() ?: 0,
            totalTimeSeconds = (resultData["totalTimeSeconds"] as? Number)?.toInt() ?: 0,
            timeTakenSeconds = (resultData["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
            rawScore = (resultData["rawScore"] as? Number)?.toInt() ?: 0,
            percentageScore = (resultData["percentageScore"] as? Number)?.toFloat() ?: 0f,
            categoryScores = categoryScores,
            difficultyBreakdown = difficultyBreakdown,
            answeredQuestions = answeredQuestions,
            completedAt = (resultData["completedAt"] as? Number)?.toLong() ?: 0L
        )
    }
}

/**
 * UI State for OIR Submission Result
 */
data class OIRSubmissionResultUiState(
    val isLoading: Boolean = true,
    val result: OIRTestResult? = null,
    val error: String? = null
)
