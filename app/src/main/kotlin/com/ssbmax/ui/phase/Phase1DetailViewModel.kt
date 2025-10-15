package com.ssbmax.ui.phase

import androidx.lifecycle.ViewModel
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for Phase 1 Detail Screen
 */
@HiltViewModel
class Phase1DetailViewModel @Inject constructor(
    // TODO: Inject TestResultRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(Phase1DetailUiState())
    val uiState: StateFlow<Phase1DetailUiState> = _uiState.asStateFlow()
    
    init {
        loadPhase1Tests()
    }
    
    private fun loadPhase1Tests() {
        // TODO: Load from repository
        // For now, use mock data
        val tests = listOf(
            Phase1Test(
                type = TestType.OIR,
                name = "OIR Test",
                subtitle = "Officer Intelligence Rating",
                description = "A test designed to assess your logical reasoning, numerical ability, and general intelligence. " +
                        "This test is crucial for screening and determines your eligibility for Phase 2.",
                durationMinutes = 40,
                questionCount = 50,
                status = TestStatus.COMPLETED,
                latestScore = 85f,
                attemptsCount = 3
            ),
            Phase1Test(
                type = TestType.PPDT,
                name = "PPDT",
                subtitle = "Picture Perception & Description Test",
                description = "You will be shown a hazy picture for 30 seconds. Write a story describing what you perceive, " +
                        "including characters, mood, and action. This tests your perception and narration skills.",
                durationMinutes = 30,
                questionCount = 1,
                status = TestStatus.COMPLETED,
                latestScore = 72f,
                attemptsCount = 2
            )
        )
        
        val completedTests = tests.filter { it.status == TestStatus.COMPLETED }
        val averageScore = if (completedTests.isNotEmpty()) {
            completedTests.mapNotNull { it.latestScore }.average().toFloat()
        } else {
            0f
        }
        
        _uiState.value = Phase1DetailUiState(
            tests = tests,
            averageScore = averageScore,
            isLoading = false
        )
    }
}

/**
 * UI State for Phase 1 Detail Screen
 */
data class Phase1DetailUiState(
    val tests: List<Phase1Test> = emptyList(),
    val averageScore: Float = 0f,
    val isLoading: Boolean = true
)

