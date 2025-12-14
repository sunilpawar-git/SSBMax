package com.ssbmax.ui.tests
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.TestProgressRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Get current user ID
                val currentUser = observeCurrentUser().first()
                val userId = currentUser?.id
                
                if (userId == null) {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Please login to view your tests"
                    ) }
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
                        type = TestType.GTO_GD,
                        name = "Group Discussion",
                        icon = Icons.Default.Forum,
                        category = "GTO",
                        durationMinutes = 20,
                        questionCount = 1,
                        status = phase2Progress.gtoProgress.status,
                        latestScore = phase2Progress.gtoProgress.latestScore
                    ),
                    TestOverviewItem(
                        type = TestType.GTO_GPE,
                        name = "Group Planning Exercise",
                        icon = Icons.Default.Map,
                        category = "GTO",
                        durationMinutes = 30,
                        questionCount = 1,
                        status = phase2Progress.gtoProgress.status,
                        latestScore = phase2Progress.gtoProgress.latestScore
                    ),
                    TestOverviewItem(
                        type = TestType.GTO_PGT,
                        name = "Progressive Group Task",
                        icon = Icons.Default.TrendingUp,
                        category = "GTO",
                        durationMinutes = 45,
                        questionCount = 1,
                        status = phase2Progress.gtoProgress.status,
                        latestScore = phase2Progress.gtoProgress.latestScore
                    ),
                    TestOverviewItem(
                        type = TestType.GTO_GOR,
                        name = "Group Obstacle Race",
                        icon = Icons.Default.DirectionsRun,
                        category = "GTO",
                        durationMinutes = 60,
                        questionCount = 1,
                        status = phase2Progress.gtoProgress.status,
                        latestScore = phase2Progress.gtoProgress.latestScore
                    ),
                    TestOverviewItem(
                        type = TestType.GTO_HGT,
                        name = "Half Group Task",
                        icon = Icons.Default.People,
                        category = "GTO",
                        durationMinutes = 25,
                        questionCount = 1,
                        status = phase2Progress.gtoProgress.status,
                        latestScore = phase2Progress.gtoProgress.latestScore
                    ),
                    TestOverviewItem(
                        type = TestType.GTO_LECTURETTE,
                        name = "Lecturette",
                        icon = Icons.Default.Mic,
                        category = "GTO",
                        durationMinutes = 3,
                        questionCount = 1,
                        status = phase2Progress.gtoProgress.status,
                        latestScore = phase2Progress.gtoProgress.latestScore
                    ),
                    TestOverviewItem(
                        type = TestType.GTO_IO,
                        name = "Individual Obstacles",
                        icon = Icons.Default.Person,
                        category = "GTO",
                        durationMinutes = 10,
                        questionCount = 10,
                        status = phase2Progress.gtoProgress.status,
                        latestScore = phase2Progress.gtoProgress.latestScore
                    ),
                    TestOverviewItem(
                        type = TestType.GTO_CT,
                        name = "Command Task",
                        icon = Icons.Default.MilitaryTech,
                        category = "GTO",
                        durationMinutes = 15,
                        questionCount = 1,
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
                ErrorLogger.log(e, "Error loading student tests")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load tests"
                ) }
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
