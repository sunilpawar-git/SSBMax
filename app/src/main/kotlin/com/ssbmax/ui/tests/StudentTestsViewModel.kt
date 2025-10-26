package com.ssbmax.ui.tests

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.TestProgressRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Student Tests Screen
 * Fetches test progress from TestProgressRepository
 */
@HiltViewModel
class StudentTestsViewModel @Inject constructor(
    private val testProgressRepository: TestProgressRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StudentTestsUiState())
    val uiState: StateFlow<StudentTestsUiState> = _uiState.asStateFlow()
    
    init {
        loadAllTests()
    }
    
    private fun loadAllTests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user ID
                val currentUser = observeCurrentUser().first()
                val userId = currentUser?.id
                
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Please login to view your tests"
                    )
                    return@launch
                }
                
                // Load Phase 1 and Phase 2 progress
                val phase1Progress = testProgressRepository.getPhase1Progress(userId).first()
                val phase2Progress = testProgressRepository.getPhase2Progress(userId).first()
                
                // Map Phase 1 tests
                val phase1Tests = listOf(
                    TestOverviewItem(
                        type = TestType.OIR,
                        name = "OIR Test",
                        icon = Icons.Default.Quiz,
                        category = "Screening",
                        durationMinutes = 40,
                        questionCount = 50,
                        status = phase1Progress.oirProgress.status,
                        latestScore = phase1Progress.oirProgress.latestScore
                    ),
                    TestOverviewItem(
                        type = TestType.PPDT,
                        name = "PPDT",
                        icon = Icons.Default.Image,
                        category = "Screening",
                        durationMinutes = 30,
                        questionCount = 1,
                        status = phase1Progress.ppdtProgress.status,
                        latestScore = phase1Progress.ppdtProgress.latestScore
                    )
                )
                
                // Map Phase 2 tests (Psychology tests share same progress in domain model)
                val phase2Tests = listOf(
                    // Psychology Tests
                    TestOverviewItem(
                        type = TestType.TAT,
                        name = "TAT",
                        icon = Icons.Default.EditNote,
                        category = "Psychology",
                        durationMinutes = 30,
                        questionCount = 12,
                        status = phase2Progress.psychologyProgress.status,
                        latestScore = phase2Progress.psychologyProgress.latestScore
                    ),
                    TestOverviewItem(
                        type = TestType.WAT,
                        name = "WAT",
                        icon = Icons.Default.EditNote,
                        category = "Psychology",
                        durationMinutes = 15,
                        questionCount = 60,
                        status = phase2Progress.psychologyProgress.status,
                        latestScore = phase2Progress.psychologyProgress.latestScore
                    ),
                    TestOverviewItem(
                        type = TestType.SRT,
                        name = "SRT",
                        icon = Icons.Default.EditNote,
                        category = "Psychology",
                        durationMinutes = 30,
                        questionCount = 60,
                        status = phase2Progress.psychologyProgress.status,
                        latestScore = phase2Progress.psychologyProgress.latestScore
                    ),
                    TestOverviewItem(
                        type = TestType.SD,
                        name = "Self Description",
                        icon = Icons.Default.EditNote,
                        category = "Psychology",
                        durationMinutes = 15,
                        questionCount = 5,
                        status = phase2Progress.psychologyProgress.status,
                        latestScore = phase2Progress.psychologyProgress.latestScore
                    ),
                    // GTO Tests
                    TestOverviewItem(
                        type = TestType.GTO,
                        name = "GTO Tasks",
                        icon = Icons.Default.Groups,
                        category = "GTO",
                        durationMinutes = 180,
                        questionCount = 8,
                        status = phase2Progress.gtoProgress.status,
                        latestScore = phase2Progress.gtoProgress.latestScore
                    ),
                    // Interview
                    TestOverviewItem(
                        type = TestType.IO,
                        name = "Personal Interview",
                        icon = Icons.Default.RecordVoiceOver,
                        category = "Interview",
                        durationMinutes = 45,
                        questionCount = 1,
                        status = phase2Progress.interviewProgress.status,
                        latestScore = phase2Progress.interviewProgress.latestScore
                    )
                )
                
                _uiState.value = StudentTestsUiState(
                    phase1Tests = phase1Tests,
                    phase2Tests = phase2Tests,
                    isLoading = false,
                    error = null
                )
                
            } catch (e: Exception) {
                Log.e("StudentTests", "Error loading tests", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load tests"
                )
            }
        }
    }
}

/**
 * UI State for Student Tests Screen
 */
data class StudentTestsUiState(
    val phase1Tests: List<TestOverviewItem> = emptyList(),
    val phase2Tests: List<TestOverviewItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

