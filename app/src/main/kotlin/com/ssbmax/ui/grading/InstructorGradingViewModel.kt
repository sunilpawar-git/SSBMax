package com.ssbmax.ui.grading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Instructor Grading Screen
 */
@HiltViewModel
class InstructorGradingViewModel @Inject constructor(
    // TODO: Inject GradingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InstructorGradingUiState())
    val uiState: StateFlow<InstructorGradingUiState> = _uiState.asStateFlow()
    
    init {
        loadGradingQueue()
    }
    
    fun loadGradingQueue() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // TODO: Load from repository
                val queueItems = generateMockQueue()
                val stats = generateMockStats()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    queueItems = queueItems,
                    stats = stats
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun refresh() {
        loadGradingQueue()
    }
    
    private fun generateMockQueue(): List<GradingQueueItem> {
        return listOf(
            GradingQueueItem(
                submissionId = "sub1",
                studentName = "Rahul Kumar",
                studentId = "student1",
                testType = TestType.PPDT,
                testName = "PPDT Test",
                submittedAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000),
                status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
                priority = GradingPriority.HIGH,
                batchName = "Batch Alpha",
                aiScore = 75f,
                hasAISuggestions = true
            ),
            GradingQueueItem(
                submissionId = "sub2",
                studentName = "Priya Sharma",
                studentId = "student2",
                testType = TestType.TAT,
                testName = "TAT Test",
                submittedAt = System.currentTimeMillis() - (4 * 60 * 60 * 1000),
                status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
                priority = GradingPriority.NORMAL,
                batchName = "Batch Alpha",
                aiScore = 68f,
                hasAISuggestions = true
            ),
            GradingQueueItem(
                submissionId = "sub3",
                studentName = "Amit Singh",
                studentId = "student3",
                testType = TestType.PPDT,
                testName = "PPDT Test",
                submittedAt = System.currentTimeMillis() - (6 * 60 * 60 * 1000),
                status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
                priority = GradingPriority.NORMAL,
                batchName = "Batch Beta",
                aiScore = 82f,
                hasAISuggestions = true
            )
        )
    }
    
    private fun generateMockStats(): InstructorGradingStats {
        return InstructorGradingStats(
            totalPending = 12,
            totalGraded = 45,
            averageGradingTimeMinutes = 8,
            todayGraded = 5,
            weekGraded = 23,
            pendingByTestType = mapOf(
                TestType.PPDT to 5,
                TestType.TAT to 4,
                TestType.WAT to 2,
                TestType.SRT to 1
            ),
            averageScoreGiven = 72.5f
        )
    }
}

/**
 * UI State for Instructor Grading Screen
 */
data class InstructorGradingUiState(
    val isLoading: Boolean = true,
    val queueItems: List<GradingQueueItem> = emptyList(),
    val stats: InstructorGradingStats = InstructorGradingStats(
        totalPending = 0,
        totalGraded = 0,
        averageGradingTimeMinutes = 0,
        todayGraded = 0,
        weekGraded = 0,
        pendingByTestType = emptyMap(),
        averageScoreGiven = 0f
    ),
    val error: String? = null
)

