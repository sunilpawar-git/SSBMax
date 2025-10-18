package com.ssbmax.ui.submissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.SSBMaxUser
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
 * ViewModel for Submissions List Screen
 */
@HiltViewModel
class SubmissionsListViewModel @Inject constructor(
    private val getUserSubmissions: GetUserSubmissionsUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SubmissionsListUiState())
    val uiState: StateFlow<SubmissionsListUiState> = _uiState.asStateFlow()
    
    init {
        loadSubmissions()
    }
    
    fun loadSubmissions(filterType: TestType? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Get current user
                val currentUserId: String = observeCurrentUser().first()?.id ?: run {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Please login to view submissions"
                    ) }
                    return@launch
                }
                
                // Get submissions
                val result = if (filterType != null) {
                    getUserSubmissions.byTestType(currentUserId, filterType)
                } else {
                    getUserSubmissions(currentUserId)
                }
                
                result.onSuccess { submissionsData ->
                    // Parse submissions
                    val submissions = submissionsData.map { data ->
                        SubmissionItem(
                            id = data["id"] as? String ?: "",
                            testType = TestType.valueOf(data["testType"] as? String ?: "TAT"),
                            testId = data["testId"] as? String ?: "",
                            status = SubmissionStatus.valueOf(data["status"] as? String ?: "DRAFT"),
                            submittedAt = data["submittedAt"] as? Long ?: 0L,
                            score = (data["data"] as? Map<*, *>)?.let { submissionData ->
                                (submissionData["aiPreliminaryScore"] as? Map<*, *>)?.get("overallScore") as? Float
                            }
                        )
                    }.sortedByDescending { it.submittedAt }
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        submissions = submissions,
                        filteredType = filterType,
                        error = null
                    ) }
                }.onFailure { error ->
                    // If it's an index error, treat as empty list (no submissions yet)
                    val isIndexError = error.message?.contains("index", ignoreCase = true) == true ||
                                     error.message?.contains("FAILED_PRECONDITION", ignoreCase = true) == true
                    
                    if (isIndexError) {
                        // Show empty state instead of error
                        _uiState.update { it.copy(
                            isLoading = false,
                            submissions = emptyList(),
                            filteredType = filterType,
                            error = null
                        ) }
                    } else {
                        // Show actual error for other failures
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Failed to load submissions: ${error.message}"
                        ) }
                    }
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
        loadSubmissions(type)
    }
    
    fun filterByStatus(status: SubmissionStatus?) {
        val currentSubmissions = _uiState.value.submissions
        _uiState.update { it.copy(
            filteredStatus = status,
            submissions = if (status != null) {
                currentSubmissions.filter { it.status == status }
            } else {
                currentSubmissions
            }
        ) }
    }
    
    fun refresh() {
        loadSubmissions(_uiState.value.filteredType)
    }
}

/**
 * UI State for Submissions List
 */
data class SubmissionsListUiState(
    val isLoading: Boolean = true,
    val submissions: List<SubmissionItem> = emptyList(),
    val filteredType: TestType? = null,
    val filteredStatus: SubmissionStatus? = null,
    val error: String? = null
) {
    val groupedByStatus: Map<SubmissionStatus, List<SubmissionItem>>
        get() = submissions.groupBy { it.status }
    
    val pendingCount: Int
        get() = submissions.count { it.status == SubmissionStatus.SUBMITTED_PENDING_REVIEW }
    
    val gradedCount: Int
        get() = submissions.count { it.status == SubmissionStatus.GRADED }
    
    val underReviewCount: Int
        get() = submissions.count { it.status == SubmissionStatus.UNDER_REVIEW }
}

/**
 * Submission item for list display
 */
data class SubmissionItem(
    val id: String,
    val testType: TestType,
    val testId: String,
    val status: SubmissionStatus,
    val submittedAt: Long,
    val score: Float? = null
) {
    val timeAgo: String
        get() {
            val diff = System.currentTimeMillis() - submittedAt
            val hours = diff / (1000 * 60 * 60)
            val days = hours / 24
            
            return when {
                days > 30 -> "${days / 30}mo ago"
                days > 0 -> "${days}d ago"
                hours > 0 -> "${hours}h ago"
                else -> "Just now"
            }
        }
    
    val testName: String
        get() = when (testType) {
            TestType.TAT -> "TAT - Thematic Apperception Test"
            TestType.WAT -> "WAT - Word Association Test"
            TestType.SRT -> "SRT - Situation Reaction Test"
            TestType.PPDT -> "PPDT - Picture Perception Test"
            TestType.SD -> "SD - Self Description"
            TestType.OIR -> "OIR - Officers Intelligence Rating"
            TestType.GTO -> "GTO - Group Testing Officer"
            TestType.IO -> "IO - Interview Officer"
        }
    
    val statusColor: String
        get() = when (status) {
            SubmissionStatus.DRAFT -> "#9E9E9E"
            SubmissionStatus.SUBMITTED_PENDING_REVIEW -> "#FF9800"
            SubmissionStatus.UNDER_REVIEW -> "#2196F3"
            SubmissionStatus.GRADED -> "#4CAF50"
            SubmissionStatus.RETURNED_FOR_REVISION -> "#F44336"
        }
}

