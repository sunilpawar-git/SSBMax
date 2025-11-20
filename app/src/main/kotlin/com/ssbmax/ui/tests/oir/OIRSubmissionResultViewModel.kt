package com.ssbmax.ui.tests.oir

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
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
                    Log.e("OIRSubmissionResult", "Error loading submission", error)
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

            // Parse category scores
            val categoryScoresMap = resultData["categoryScores"] as? Map<*, *> ?: emptyMap<String, Any>()
            val categoryScores = categoryScoresMap.mapNotNull { (key, value) ->
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
                    Log.w("OIRSubmissionResult", "Failed to parse category: $categoryName", e)
                    null
                }
            }.toMap()

            // Parse difficulty breakdown
            val difficultyBreakdownMap = resultData["difficultyBreakdown"] as? Map<*, *> ?: emptyMap<String, Any>()
            val difficultyBreakdown = difficultyBreakdownMap.mapNotNull { (key, value) ->
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
                    Log.w("OIRSubmissionResult", "Failed to parse difficulty: $difficultyName", e)
                    null
                }
            }.toMap()

            // Parse answered questions (simplified - just store question IDs and basic data)
            val answeredQuestionsList = resultData["answeredQuestions"] as? List<*> ?: emptyList<Any>()
            val answeredQuestions = answeredQuestionsList.mapNotNull { aqData ->
                val aq = aqData as? Map<*, *> ?: return@mapNotNull null

                try {
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
                    Log.w("OIRSubmissionResult", "Failed to parse answered question", e)
                    null
                }
            }

            // Create the OIRTestResult object
            OIRTestResult(
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
        } catch (e: Exception) {
            Log.e("OIRSubmissionResult", "Error parsing OIR test result", e)
            null
        }
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
