package com.ssbmax.ui.phase
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.Phase1Progress
import com.ssbmax.core.domain.model.TestProgress
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.TestProgressRepository
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
 * ViewModel for Phase 1 Detail Screen
 * Fetches Phase 1 progress from TestProgressRepository
 */
@HiltViewModel
class Phase1DetailViewModel @Inject constructor(
    private val testProgressRepository: TestProgressRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(Phase1DetailUiState())
    val uiState: StateFlow<Phase1DetailUiState> = _uiState.asStateFlow()
    
    init {
        loadPhase1Tests()
    }
    
    private fun loadPhase1Tests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Get current user ID
                val currentUser = observeCurrentUser().first()
                val userId = currentUser?.id
                
                if (userId == null) {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Please login to view Phase 1 progress"
                    ) }
                    return@launch
                }
                
                // Observe Phase 1 progress from repository with lifecycle awareness
                testProgressRepository.getPhase1Progress(userId)
                    .catch { error ->
                        Log.e("Phase1Detail", "Error loading Phase 1 progress", error)
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Failed to load Phase 1 progress: ${error.message}"
                        ) }
                    }
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5000),
                        initialValue = Phase1Progress(
                            oirProgress = TestProgress(TestType.OIR),
                            ppdtProgress = TestProgress(TestType.PPDT)
                        )
                    )
                    .collect { phase1Progress ->
                        // Map domain TestProgress to UI Phase1Test models
                        val tests = listOf(
                            mapToPhase1Test(
                                progress = phase1Progress.oirProgress,
                                name = "OIR Test",
                                subtitle = "Officer Intelligence Rating",
                                description = "A test designed to assess your logical reasoning, numerical ability, and general intelligence. " +
                                        "This test is crucial for screening and determines your eligibility for Phase 2.",
                                durationMinutes = 40,
                                questionCount = 50
                            ),
                            mapToPhase1Test(
                                progress = phase1Progress.ppdtProgress,
                                name = "PPDT",
                                subtitle = "Picture Perception & Description Test",
                                description = "You will be shown a hazy picture for 30 seconds. Write a story describing what you perceive, " +
                                        "including characters, mood, and action. This tests your perception and narration skills.",
                                durationMinutes = 30,
                                questionCount = 1
                            )
                        )
                        
                        // Calculate average score from completed tests
                        val completedTests = tests.filter { 
                            it.status == TestStatus.COMPLETED || it.status == TestStatus.GRADED 
                        }
                        val averageScore = if (completedTests.isNotEmpty()) {
                            completedTests.mapNotNull { it.latestScore }.average().toFloat()
                        } else {
                            0f
                        }
                        
                        _uiState.value = Phase1DetailUiState(
                            tests = tests,
                            averageScore = averageScore,
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                Log.e("Phase1Detail", "Error in loadPhase1Tests", e)
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                ) }
            }
        }
    }
    
    /**
     * Map domain TestProgress to UI Phase1Test model
     */
    private fun mapToPhase1Test(
        progress: com.ssbmax.core.domain.model.TestProgress,
        name: String,
        subtitle: String,
        description: String,
        durationMinutes: Int,
        questionCount: Int
    ): Phase1Test {
        return Phase1Test(
            type = progress.testType,
            name = name,
            subtitle = subtitle,
            description = description,
            durationMinutes = durationMinutes,
            questionCount = questionCount,
            status = progress.status,
            latestScore = progress.latestScore,
            attemptsCount = 0 // TODO: Track attempts count in domain model if needed
        )
    }
}

/**
 * UI State for Phase 1 Detail Screen
 */
data class Phase1DetailUiState(
    val tests: List<Phase1Test> = emptyList(),
    val averageScore: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null
)
