package com.ssbmax.ui.instructor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.GetUserSubmissionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Instructor Grading Dashboard
 * Shows pending submissions for review
 */
@HiltViewModel
class InstructorGradingViewModel @Inject constructor(
    private val getUserSubmissions: GetUserSubmissionsUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InstructorGradingUiState())
    val uiState: StateFlow<InstructorGradingUiState> = _uiState.asStateFlow()
    
    init {
        loadPendingSubmissions()
    }
    
    fun loadPendingSubmissions(filterType: TestType? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Get current user
                val currentUserId: String = observeCurrentUser().first()?.id ?: run {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Please login to view grading queue"
                    ) }
                    return@launch
                }
                
                // Get all submissions (in a real app, filter by batch/instructor)
                val result = if (filterType != null) {
                    getUserSubmissions.byTestType(currentUserId, filterType)
                } else {
                    getUserSubmissions(currentUserId)
                }
                
                result.onSuccess { submissionsData ->
                    // Parse and filter pending submissions
                    val submissions = submissionsData
                        .filter {
                            val status = (it["status"] as? String) ?: ""
                            status == "SUBMITTED_PENDING_REVIEW" || status == "UNDER_REVIEW"
                        }
                        .map { data ->
                            GradingQueueItem(
                                id = data["id"] as? String ?: "",
                                studentId = data["userId"] as? String ?: "",
                                testType = TestType.valueOf(data["testType"] as? String ?: "TAT"),
                                testId = data["testId"] as? String ?: "",
                                status = SubmissionStatus.valueOf(data["status"] as? String ?: "DRAFT"),
                                submittedAt = data["submittedAt"] as? Long ?: 0L,
                                aiScore = (data["data"] as? Map<*, *>)?.let { submissionData ->
                                    (submissionData["aiPreliminaryScore"] as? Map<*, *>)?.get("overallScore") as? Float
                                }
                            )
                        }
                        .sortedBy { it.submittedAt } // Oldest first
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        submissions = submissions,
                        filteredType = filterType
                    ) }
                }.onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Failed to load submissions: ${error.message}"
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }
    
    fun filterByType(type: TestType?) {
        _uiState.update { it.copy(filteredType = type) }
        loadPendingSubmissions(type)
    }
    
    fun refresh() {
        loadPendingSubmissions(_uiState.value.filteredType)
    }
}

/**
 * UI State for Instructor Grading Dashboard
 */
data class InstructorGradingUiState(
    val isLoading: Boolean = true,
    val submissions: List<GradingQueueItem> = emptyList(),
    val filteredType: TestType? = null,
    val error: String? = null
) {
    val pendingCount: Int
        get() = submissions.count { it.status == SubmissionStatus.SUBMITTED_PENDING_REVIEW }
    
    val underReviewCount: Int
        get() = submissions.count { it.status == SubmissionStatus.UNDER_REVIEW }
    
    val groupedByTestType: Map<TestType, List<GradingQueueItem>>
        get() = submissions.groupBy { it.testType }
}

/**
 * Grading queue item for instructor
 */
data class GradingQueueItem(
    val id: String,
    val studentId: String,
    val testType: TestType,
    val testId: String,
    val status: SubmissionStatus,
    val submittedAt: Long,
    val aiScore: Float? = null
) {
    val testName: String
        get() = when (testType) {
            TestType.TAT -> "TAT"
            TestType.WAT -> "WAT"
            TestType.SRT -> "SRT"
            TestType.PPDT -> "PPDT"
            TestType.SD -> "SD"
            TestType.OIR -> "OIR"
            TestType.GTO -> "GTO"
            TestType.IO -> "IO"
        }
    
    val timeWaiting: String
        get() {
            val diff = System.currentTimeMillis() - submittedAt
            val hours = diff / (1000 * 60 * 60)
            val days = hours / 24
            
            return when {
                days > 7 -> "${days}d waiting"
                days > 0 -> "${days}d ${hours % 24}h"
                hours > 0 -> "${hours}h"
                else -> "Just submitted"
            }
        }
    
    val priority: GradingPriority
        get() {
            val hours = (System.currentTimeMillis() - submittedAt) / (1000 * 60 * 60)
            return when {
                hours > 72 -> GradingPriority.URGENT
                hours > 48 -> GradingPriority.HIGH
                hours > 24 -> GradingPriority.NORMAL
                else -> GradingPriority.LOW
            }
        }
}

/**
 * Grading priority levels
 */
enum class GradingPriority {
    URGENT, HIGH, NORMAL, LOW;
    
    val displayName: String
        get() = when (this) {
            URGENT -> "Urgent"
            HIGH -> "High"
            NORMAL -> "Normal"
            LOW -> "Low"
        }
}

