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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
/**
 * ViewModel for Instructor Home Screen
 * Manages student list, batches, and grading queue
 */
@HiltViewModel
class InstructorHomeViewModel @Inject constructor(
    private val gradingQueueRepository: GradingQueueRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val batchRepository: com.ssbmax.core.domain.repository.BatchRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InstructorHomeUiState())
    val uiState: StateFlow<InstructorHomeUiState> = _uiState.asStateFlow()
    
    init {
        loadInstructorData()
    }
    
    private fun loadInstructorData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Get current instructor ID from authenticated user
            val currentUser = observeCurrentUser().first()
            val instructorId = currentUser?.id ?: run {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "You must be logged in to view instructor dashboard"
                ) }
                return@launch
            }
            // Observe grading statistics
            launch {
                gradingQueueRepository.observeGradingStats(instructorId).collect { stats ->
                    _uiState.update { it.copy(
                        pendingGradingCount = stats.totalPending,
                        testsGradedToday = stats.todayGraded,
                        avgResponseTime = stats.averageGradingTimeMinutes / 60, // Convert to hours
                        isLoading = false
                    ) }
                }
            }
            
            // Observe batches from repository
            launch {
                batchRepository.getBatchesForInstructor(instructorId).collect { result ->
                    val batches = result.getOrDefault(emptyList())
                    val batchesList = batches.map { batch ->
                        BatchInfo(
                            id = batch.id,
                            name = batch.name,
                            inviteCode = batch.inviteCode,
                            studentCount = batch.studentIds.size
                        )
                    }
                    _uiState.update { it.copy(
                        batches = batchesList,
                        activeBatches = batchesList.size
                    ) }
                    
                    // Observe students dynamically from the first active batch
                    if (batches.isNotEmpty()) {
                        launch {
                            batchRepository.getStudentsInBatch(batches.first().id).collect { studentResult ->
                                val studentsList = studentResult.getOrDefault(emptyList())
                                _uiState.update { it.copy(
                                    students = studentsList,
                                    totalStudents = studentsList.size
                                ) }
                            }
                        }
                    }
                }
            }
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
