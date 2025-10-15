package com.ssbmax.ui.home.student

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
 * ViewModel for Student Home Screen
 * Manages user progress, recommendations, and daily tips
 */
@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    // TODO: Inject repositories
    // private val userRepository: UserRepository,
    // private val testRepository: TestRepository,
    // private val studyRepository: StudyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StudentHomeUiState())
    val uiState: StateFlow<StudentHomeUiState> = _uiState.asStateFlow()
    
    init {
        loadUserProgress()
    }
    
    private fun loadUserProgress() {
        viewModelScope.launch {
            // TODO: Load from repository
            // For now, use mock data
            _uiState.value = StudentHomeUiState(
                userName = "Aspirant",
                currentStreak = 7,
                testsCompleted = 12,
                notificationCount = 3,
                phase1Progress = PhaseProgress(
                    phase = TestPhase.PHASE_1,
                    totalTests = 2,
                    completedTests = 1,
                    testsInProgress = 1,
                    testsPendingReview = 0,
                    averageScore = 72.5f,
                    subTests = listOf(
                        SubTestProgress(
                            testType = TestType.OIR,
                            status = TestStatus.COMPLETED,
                            latestScore = 85f,
                            attemptsCount = 3,
                            bestScore = 85f
                        ),
                        SubTestProgress(
                            testType = TestType.PPDT,
                            status = TestStatus.IN_PROGRESS,
                            latestScore = 60f,
                            attemptsCount = 1
                        )
                    )
                ),
                phase2Progress = PhaseProgress(
                    phase = TestPhase.PHASE_2,
                    totalTests = 6,
                    completedTests = 0,
                    testsInProgress = 0,
                    testsPendingReview = 0,
                    averageScore = 0f,
                    subTests = listOf(
                        SubTestProgress(
                            testType = TestType.TAT,
                            status = TestStatus.NOT_ATTEMPTED,
                            attemptsCount = 0
                        ),
                        SubTestProgress(
                            testType = TestType.WAT,
                            status = TestStatus.NOT_ATTEMPTED,
                            attemptsCount = 0
                        ),
                        SubTestProgress(
                            testType = TestType.SRT,
                            status = TestStatus.NOT_ATTEMPTED,
                            attemptsCount = 0
                        ),
                        SubTestProgress(
                            testType = TestType.SD,
                            status = TestStatus.NOT_ATTEMPTED,
                            attemptsCount = 0
                        )
                    )
                ),
                recommendedTests = listOf(
                    TestType.TAT,
                    TestType.WAT,
                    TestType.GTO
                ),
                dailyTip = "Practice TAT regularly to improve your storytelling skills. Focus on creating coherent narratives that demonstrate leadership qualities and problem-solving abilities."
            )
        }
    }
    
    fun refreshProgress() {
        loadUserProgress()
    }
}

/**
 * UI State for Student Home Screen
 */
data class StudentHomeUiState(
    val isLoading: Boolean = false,
    val userName: String = "Student",
    val currentStreak: Int = 0,
    val testsCompleted: Int = 0,
    val notificationCount: Int = 0,
    val phase1Progress: PhaseProgress? = null,
    val phase2Progress: PhaseProgress? = null,
    val recommendedTests: List<TestType> = emptyList(),
    val dailyTip: String = "",
    val error: String? = null
)

