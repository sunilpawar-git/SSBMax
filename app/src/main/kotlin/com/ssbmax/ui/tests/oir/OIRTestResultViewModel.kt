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
 * ViewModel for OIR Test Result Screen
 * Fetches OIR test results from SubmissionRepository
 */
@HiltViewModel
class OIRTestResultViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OIRTestResultUiState())
    val uiState: StateFlow<OIRTestResultUiState> = _uiState.asStateFlow()
    
    fun loadResult(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            submissionRepository.getSubmission(sessionId)
                .onSuccess { data ->
                    if (data == null) {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Test result not found"
                        ) }
                        return@onSuccess
                    }

                    // Parse OIR test result from map
                    val result = parseOIRTestResult(data, sessionId)
                    if (result != null) {
                        _uiState.update { it.copy(
                            isLoading = false,
                            result = result
                        ) }
                    } else {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Failed to parse test result data"
                        ) }
                    }
                }
                .onFailure { error ->
                    Log.e("OIRTestResult", "Error loading result", error)
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load test result"
                    ) }
                }
        }
    }
    
    /**
     * Parse OIR test result from Firestore document data
     */
    private fun parseOIRTestResult(data: Map<String, Any>, sessionId: String): OIRTestResult? {
        return try {
            val resultData = data["data"] as? Map<*, *> ?: return null

            // Parse category scores
            val categoryScoresData = resultData["categoryScores"] as? Map<*, *> ?: emptyMap<Any, Any>()
            val categoryScores = categoryScoresData.mapNotNull { (key, value) ->
                val typeStr = key as? String ?: return@mapNotNull null
                val scoreData = value as? Map<*, *> ?: return@mapNotNull null
                try {
                    val type = OIRQuestionType.valueOf(typeStr)
                    val score = CategoryScore(
                        category = type,
                        totalQuestions = (scoreData["totalQuestions"] as? Number)?.toInt() ?: 0,
                        correctAnswers = (scoreData["correctAnswers"] as? Number)?.toInt() ?: 0,
                        percentage = (scoreData["percentage"] as? Number)?.toFloat() ?: 0f,
                        averageTimeSeconds = (scoreData["averageTimeSeconds"] as? Number)?.toInt() ?: 0
                    )
                    type to score
                } catch (e: Exception) {
                    Log.w("OIRTestResult", "Failed to parse category $typeStr", e)
                    null
                }
            }.toMap()

            // Parse difficulty breakdown
            val difficultyData = resultData["difficultyBreakdown"] as? Map<*, *> ?: emptyMap<Any, Any>()
            val difficultyBreakdown = difficultyData.mapNotNull { (key, value) ->
                val diffStr = key as? String ?: return@mapNotNull null
                val scoreData = value as? Map<*, *> ?: return@mapNotNull null
                try {
                    val difficulty = QuestionDifficulty.valueOf(diffStr)
                    val score = DifficultyScore(
                        difficulty = difficulty,
                        totalQuestions = (scoreData["totalQuestions"] as? Number)?.toInt() ?: 0,
                        correctAnswers = (scoreData["correctAnswers"] as? Number)?.toInt() ?: 0,
                        percentage = (scoreData["percentage"] as? Number)?.toFloat() ?: 0f
                    )
                    difficulty to score
                } catch (e: Exception) {
                    Log.w("OIRTestResult", "Failed to parse difficulty $diffStr", e)
                    null
                }
            }.toMap()

            // Parse answered questions - Complex structure, returning empty for now
            // TODO: Implement full OIRAnsweredQuestion parsing when detailed review is needed
            val answeredQuestions = emptyList<OIRAnsweredQuestion>()

            OIRTestResult(
                testId = resultData["testId"] as? String ?: data["testId"] as? String ?: "",
                sessionId = sessionId,
                userId = resultData["userId"] as? String ?: data["userId"] as? String ?: "",
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
                completedAt = (resultData["completedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("OIRTestResult", "Error parsing OIR test result", e)
            null
        }
    }
}

/**
 * UI State for OIR Test Result Screen
 */
data class OIRTestResultUiState(
    val isLoading: Boolean = true,
    val result: OIRTestResult? = null,
    val error: String? = null
)

