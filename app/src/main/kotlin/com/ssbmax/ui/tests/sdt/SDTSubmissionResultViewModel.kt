package com.ssbmax.ui.tests.sdt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SDTSubmissionResultViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SDTSubmissionResultUiState())
    val uiState: StateFlow<SDTSubmissionResultUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "SDTResultViewModel"
    }

    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            submissionRepository.getSubmission(submissionId)
                .onSuccess { data ->
                    if (data == null) {
                        ErrorLogger.logTestError(
                            throwable = IllegalStateException("Submission data is null"),
                            description = "SDT submission not found: $submissionId",
                            testType = "SDT"
                        )
                        _uiState.update { it.copy(isLoading = false, submission = null, error = "Submission not found") }
                        return@onSuccess
                    }

                    val submission = parseSDTSubmission(data)

                    if (submission == null) {
                        ErrorLogger.logTestError(
                            throwable = IllegalStateException("Failed to parse submission"),
                            description = "SDT submission parsing failed: $submissionId",
                            testType = "SDT"
                        )
                    }

                    _uiState.update { it.copy(isLoading = false, submission = submission,
                        error = if (submission == null) "Submission not found" else null) }
                }
                .onFailure { error ->
                    ErrorLogger.logTestError(error, "Failed to load SDT submission result", "SDT")
                    _uiState.update { it.copy(isLoading = false,
                        error = error.message ?: "Failed to load submission") }
                }
        }
    }

    private fun parseSDTSubmission(data: Map<String, Any>): SDTSubmission? {
        return try {
            val submissionData = data["data"] as? Map<*, *> ?: return null
            val responsesList = submissionData["responses"] as? List<*> ?: emptyList<Any>()
            val responses = responsesList.mapNotNull { responseData ->
                val response = responseData as? Map<*, *> ?: return@mapNotNull null
                SDTQuestionResponse(
                    questionId = response["questionId"] as? String ?: "",
                    question = response["question"] as? String ?: "",
                    answer = response["answer"] as? String ?: "",
                    wordCount = (response["wordCount"] as? Number)?.toInt() ?: 0,
                    timeTakenSeconds = (response["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (response["submittedAt"] as? Number)?.toLong() ?: 0L,
                    isSkipped = response["isSkipped"] as? Boolean ?: false
                )
            }

            val aiScoreData = submissionData["aiPreliminaryScore"] as? Map<*, *>
            val aiScore = aiScoreData?.let {
                SDTAIScore(
                    overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                    selfAwarenessScore = (it["selfAwarenessScore"] as? Number)?.toFloat() ?: 0f,
                    emotionalMaturityScore = (it["emotionalMaturityScore"] as? Number)?.toFloat() ?: 0f,
                    socialPerceptionScore = (it["socialPerceptionScore"] as? Number)?.toFloat() ?: 0f,
                    introspectionScore = (it["introspectionScore"] as? Number)?.toFloat() ?: 0f,
                    feedback = it["feedback"] as? String,
                    positiveTraits = (it["positiveTraits"] as? List<*>)?.mapNotNull { trait -> trait as? String } ?: emptyList(),
                    concerningPatterns = (it["concerningPatterns"] as? List<*>)?.mapNotNull { pattern -> pattern as? String } ?: emptyList(),
                    responseQuality = try { ResponseQuality.valueOf(it["responseQuality"] as? String ?: "AVERAGE") }
                        catch (e: Exception) { ResponseQuality.AVERAGE },
                    strengths = (it["strengths"] as? List<*>)?.mapNotNull { s -> s as? String } ?: emptyList(),
                    areasForImprovement = (it["areasForImprovement"] as? List<*>)?.mapNotNull { a -> a as? String } ?: emptyList()
                )
            }

            SDTSubmission(
                id = submissionData["id"] as? String ?: "",
                userId = submissionData["userId"] as? String ?: "",
                testId = submissionData["testId"] as? String ?: "",
                responses = responses,
                totalTimeTakenMinutes = (submissionData["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
                submittedAt = (submissionData["submittedAt"] as? Number)?.toLong() ?: 0L,
                aiPreliminaryScore = aiScore
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class SDTSubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: SDTSubmission? = null,
    val error: String? = null
)

