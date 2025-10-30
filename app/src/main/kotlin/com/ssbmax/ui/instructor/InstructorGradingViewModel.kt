package com.ssbmax.ui.instructor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.GradingQueueItem
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.GradingQueueRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Instructor Grading Dashboard
 * Shows pending submissions for review using GradingQueueRepository
 */
@HiltViewModel
class InstructorGradingViewModel @Inject constructor(
    private val gradingQueueRepository: GradingQueueRepository,
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
                val currentUser = observeCurrentUser().first()
                val instructorId = currentUser?.id ?: run {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Please login to view grading queue"
                    ) }
                    return@launch
                }
                
                // Observe pending submissions from repository
                val flow = if (filterType != null) {
                    gradingQueueRepository.observeSubmissionsByTestType(filterType)
                } else {
                    gradingQueueRepository.observePendingSubmissions(instructorId)
                }
                
                flow
                    .catch { error ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Failed to load submissions: ${error.message}"
                        ) }
                    }
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5000),
                        initialValue = emptyList()
                    )
                    .collect { submissions ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            submissions = submissions,
                            filteredType = filterType,
                            error = null
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


