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
 * ViewModel for Phase 2 Detail Screen
 */
@HiltViewModel
class Phase2DetailViewModel @Inject constructor(
    // TODO: Inject TestResultRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(Phase2DetailUiState())
    val uiState: StateFlow<Phase2DetailUiState> = _uiState.asStateFlow()
    
    init {
        loadPhase2Tests()
    }
    
    private fun loadPhase2Tests() {
        // TODO: Load from repository
        // For now, use mock data
        val psychologyTests = listOf(
            Phase2Test(
                type = TestType.TAT,
                name = "TAT",
                subtitle = "Thematic Apperception Test",
                description = "You will be shown 12 pictures one by one. Write a story for each picture describing " +
                        "what led to the situation, what is happening, and what the outcome will be.",
                durationMinutes = 30,
                questionCount = 12,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null,
                attemptsCount = 0
            ),
            Phase2Test(
                type = TestType.WAT,
                name = "WAT",
                subtitle = "Word Association Test",
                description = "You will be shown 60 words one by one for 15 seconds each. Write the first thought " +
                        "that comes to your mind as a sentence.",
                durationMinutes = 15,
                questionCount = 60,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null,
                attemptsCount = 0
            ),
            Phase2Test(
                type = TestType.SRT,
                name = "SRT",
                subtitle = "Situation Reaction Test",
                description = "You will be given 60 real-life situations. Write what you would do in each situation " +
                        "as quickly and honestly as possible.",
                durationMinutes = 30,
                questionCount = 60,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null,
                attemptsCount = 0
            ),
            Phase2Test(
                type = TestType.SD,
                name = "SD",
                subtitle = "Self Description",
                description = "Write about yourself, your parents, teachers, and friends' opinions about you. " +
                        "Be honest and introspective in your responses.",
                durationMinutes = 15,
                questionCount = 5,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null,
                attemptsCount = 0
            )
        )
        
        val gtoTests = listOf(
            Phase2Test(
                type = TestType.GTO,
                name = "GTO Tasks",
                subtitle = "Group Testing Officer",
                description = "Group tasks including Group Discussion, Group Planning Exercise, Progressive Group Task, " +
                        "Half Group Task, Lecturette, and Command Task. Assessed on leadership, cooperation, and problem-solving.",
                durationMinutes = 180,
                questionCount = 8,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null,
                attemptsCount = 0
            )
        )
        
        val ioTests = listOf(
            Phase2Test(
                type = TestType.IO,
                name = "Personal Interview",
                subtitle = "Interviewing Officer",
                description = "A personal interview with the Interviewing Officer to assess your personality, knowledge, " +
                        "current affairs awareness, and motivation for joining the armed forces.",
                durationMinutes = 45,
                questionCount = 1,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null,
                attemptsCount = 0
            )
        )
        
        val allTests = psychologyTests + gtoTests + ioTests
        val completedTests = allTests.filter { it.status == TestStatus.COMPLETED }
        val averageScore = if (completedTests.isNotEmpty()) {
            completedTests.mapNotNull { it.latestScore }.average().toFloat()
        } else {
            0f
        }
        
        _uiState.value = Phase2DetailUiState(
            tests = allTests,
            psychologyTests = psychologyTests,
            gtoTests = gtoTests,
            ioTests = ioTests,
            averageScore = averageScore,
            isLoading = false
        )
    }
}

/**
 * UI State for Phase 2 Detail Screen
 */
data class Phase2DetailUiState(
    val tests: List<Phase2Test> = emptyList(),
    val psychologyTests: List<Phase2Test> = emptyList(),
    val gtoTests: List<Phase2Test> = emptyList(),
    val ioTests: List<Phase2Test> = emptyList(),
    val averageScore: Float = 0f,
    val isLoading: Boolean = true
)

