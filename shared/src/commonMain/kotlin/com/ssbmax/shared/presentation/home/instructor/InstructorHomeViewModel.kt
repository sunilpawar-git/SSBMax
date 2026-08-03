package com.ssbmax.shared.presentation.home.instructor

import com.ssbmax.shared.domain.model.StudentPerformance
import com.ssbmax.shared.domain.repository.GradingQueueRepository
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * ViewModel for the Instructor Home Screen — Phase 5 KMP port of the Android
 * original (`app/.../ui/home/instructor/InstructorHomeViewModel.kt`).
 *
 * A real `androidx.lifecycle.ViewModel` using `viewModelScope` (Phase 1 of
 * the KMP-convergence plan). Real change from the Android original:
 * `System.currentTimeMillis()` (JVM-only) ->
 * `Clock.System.now().toEpochMilliseconds()`. The mock `students`/`batches`
 * data and the `// TODO: Load batches and students from BatchRepository`
 * comment are carried forward unchanged — `BatchRepository` doesn't exist in
 * either the Android original or `:shared` yet; not something this port
 * introduces or is scoped to fix.
 */
class InstructorHomeViewModel(
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
            _uiState.update { it.copy(isLoading = true) }
            val currentUser = observeCurrentUser().first()
            val instructorId = currentUser?.id ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "You must be logged in to view instructor dashboard"
                    )
                }
                return@launch
            }
            // Observe grading statistics
            launch {
                gradingQueueRepository.observeGradingStats(instructorId)
                    .catch { error ->
                        _uiState.update {
                            it.copy(isLoading = false, error = "Failed to load grading stats: ${error.message}")
                        }
                    }
                    .collect { stats ->
                        _uiState.update {
                            it.copy(
                                pendingGradingCount = stats.totalPending,
                                testsGradedToday = stats.todayGraded,
                                avgResponseTime = stats.averageGradingTimeMinutes / 60, // Convert to hours
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            }
            // TODO: Load batches and students from BatchRepository
            // For now, keep mock data for batches and students until BatchRepository is implemented
            val now = Clock.System.now().toEpochMilliseconds()
            _uiState.update {
                it.copy(
                    totalStudents = 24,
                    activeBatches = 3,
                    students = listOf(
                        StudentPerformance(
                            studentId = "1",
                            studentName = "Rahul Sharma",
                            averageScore = 78.5f,
                            testsCompleted = 8,
                            lastActiveAt = now,
                            currentStreak = 5,
                            phase1Score = 82f,
                            phase2Score = 75f
                        ),
                        StudentPerformance(
                            studentId = "2",
                            studentName = "Priya Patel",
                            averageScore = 85.2f,
                            testsCompleted = 12,
                            lastActiveAt = now,
                            currentStreak = 7,
                            phase1Score = 88f,
                            phase2Score = 82f
                        ),
                        StudentPerformance(
                            studentId = "3",
                            studentName = "Amit Kumar",
                            averageScore = 72.3f,
                            testsCompleted = 6,
                            lastActiveAt = now,
                            currentStreak = 3,
                            phase1Score = 75f,
                            phase2Score = 69f
                        ),
                        StudentPerformance(
                            studentId = "4",
                            studentName = "Sneha Singh",
                            averageScore = 91.0f,
                            testsCompleted = 15,
                            lastActiveAt = now,
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
    }

    fun refreshData() {
        loadInstructorData()
    }
}

/** UI State for Instructor Home Screen */
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

/** Data class for batch info display. */
data class BatchInfo(
    val id: String,
    val name: String,
    val inviteCode: String,
    val studentCount: Int
)
