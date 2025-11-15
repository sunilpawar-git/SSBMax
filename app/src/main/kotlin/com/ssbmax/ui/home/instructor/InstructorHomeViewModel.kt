package com.ssbmax.ui.home.instructor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.StudentPerformance
import com.ssbmax.core.domain.repository.GradingQueueRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Instructor Home Screen
 * Manages student list, batches, and grading queue
 */
@HiltViewModel
class InstructorHomeViewModel @Inject constructor(
    private val gradingQueueRepository: GradingQueueRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InstructorHomeUiState())
    val uiState: StateFlow<InstructorHomeUiState> = _uiState.asStateFlow()
    
    init {
        loadInstructorData()
    }
    
    private fun loadInstructorData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Get current instructor ID from authenticated user
            val currentUser = observeCurrentUser().first()
            val instructorId = currentUser?.id ?: run {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "You must be logged in to view instructor dashboard"
                )
                return@launch
            }

            // Observe grading statistics
            launch {
                gradingQueueRepository.observeGradingStats(instructorId).collect { stats ->
                    _uiState.value = _uiState.value.copy(
                        pendingGradingCount = stats.totalPending,
                        testsGradedToday = stats.todayGraded,
                        avgResponseTime = stats.averageGradingTimeMinutes / 60, // Convert to hours
                        isLoading = false
                    )
                }
            }

            // TODO: Load batches and students from BatchRepository
            // For now, keep mock data for batches and students until BatchRepository is implemented
            _uiState.value = _uiState.value.copy(
                totalStudents = 24,
                activeBatches = 3,
                students = listOf(
                    StudentPerformance(
                        studentId = "1",
                        studentName = "Rahul Sharma",
                        averageScore = 78.5f,
                        testsCompleted = 8,
                        lastActiveAt = System.currentTimeMillis(),
                        currentStreak = 5,
                        phase1Score = 82f,
                        phase2Score = 75f
                    ),
                    StudentPerformance(
                        studentId = "2",
                        studentName = "Priya Patel",
                        averageScore = 85.2f,
                        testsCompleted = 12,
                        lastActiveAt = System.currentTimeMillis(),
                        currentStreak = 7,
                        phase1Score = 88f,
                        phase2Score = 82f
                    ),
                    StudentPerformance(
                        studentId = "3",
                        studentName = "Amit Kumar",
                        averageScore = 72.3f,
                        testsCompleted = 6,
                        lastActiveAt = System.currentTimeMillis(),
                        currentStreak = 3,
                        phase1Score = 75f,
                        phase2Score = 69f
                    ),
                    StudentPerformance(
                        studentId = "4",
                        studentName = "Sneha Singh",
                        averageScore = 91.0f,
                        testsCompleted = 15,
                        lastActiveAt = System.currentTimeMillis(),
                        currentStreak = 12,
                        phase1Score = 93f,
                        phase2Score = 89f
                    )
                ),
                batches = listOf(
                    BatchInfo(
                        id = "batch1",
                        name = "NDA Batch 2024",
                        inviteCode = "NDA2024",
                        studentCount = 15
                    ),
                    BatchInfo(
                        id = "batch2",
                        name = "CDS Preparation",
                        inviteCode = "CDS2024",
                        studentCount = 8
                    ),
                    BatchInfo(
                        id = "batch3",
                        name = "AFCAT Group",
                        inviteCode = "AFC2024",
                        studentCount = 6
                    )
                )
            )
        }
    }
    
    fun refreshData() {
        loadInstructorData()
    }
}

/**
 * UI State for Instructor Home Screen
 */
data class InstructorHomeUiState(
    val isLoading: Boolean = false,
    val totalStudents: Int = 0,
    val activeBatches: Int = 0,
    val pendingGradingCount: Int = 0,
    val testsGradedToday: Int = 0,
    val avgResponseTime: Int = 0, // in hours
    val students: List<StudentPerformance> = emptyList(),
    val batches: List<BatchInfo> = emptyList(),
    val error: String? = null
)

