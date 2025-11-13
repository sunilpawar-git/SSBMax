package com.ssbmax.ui.home.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Student Home Screen
 * Manages user progress and displays user info
 */
@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val testProgressRepository: com.ssbmax.core.domain.repository.TestProgressRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StudentHomeUiState())
    val uiState: StateFlow<StudentHomeUiState> = _uiState.asStateFlow()
    
    init {
        observeUserProfile()
        observeTestProgress()
    }
    
    private fun observeUserProfile() {
        viewModelScope.launch {
            // Get current user and observe their profile with lifecycle awareness
            val currentUser = authRepository.currentUser.value
            if (currentUser != null) {
                userProfileRepository.getUserProfile(currentUser.id)
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5000),
                        initialValue = Result.success(null)
                    )
                    .collect { result ->
                        result.onSuccess { profile ->
                            _uiState.update {
                                it.copy(
                                    userName = profile?.fullName ?: "Aspirant",
                                    currentStreak = profile?.currentStreak ?: 0
                                )
                            }
                        }
                    }
            }
        }
    }
    
    private fun observeTestProgress() {
        viewModelScope.launch {
            val userId = authRepository.currentUser.value?.id ?: return@launch
            
            // Lifecycle-aware combined progress flow
            kotlinx.coroutines.flow.combine(
                testProgressRepository.getPhase1Progress(userId),
                testProgressRepository.getPhase2Progress(userId)
            ) { phase1, phase2 ->
                Pair(phase1, phase2)
            }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = Pair(
                        Phase1Progress(
                            oirProgress = TestProgress(TestType.OIR),
                            ppdtProgress = TestProgress(TestType.PPDT)
                        ),
                        Phase2Progress(
                            psychologyProgress = TestProgress(TestType.TAT), // Psychology encompasses multiple tests
                            gtoProgress = TestProgress(TestType.GTO_GD),
                            interviewProgress = TestProgress(TestType.IO)
                        )
                    )
                )
                .collect { (phase1, phase2) ->
                    // Calculate tests completed (tests that have been attempted)
                    // Count tests with status other than NOT_ATTEMPTED
                    val allProgress = listOf(
                        phase1.oirProgress,
                        phase1.ppdtProgress,
                        phase2.psychologyProgress,
                        phase2.gtoProgress,
                        phase2.interviewProgress
                    )
                    
                    val completedTests = allProgress.count { progress ->
                        progress.status != TestStatus.NOT_ATTEMPTED
                    }
                    
                    _uiState.update { 
                        it.copy(
                            phase1Progress = phase1,
                            phase2Progress = phase2,
                            testsCompleted = completedTests
                        )
                    }
                }
        }
    }
    
    fun refreshProgress() {
        // Flows are already observing, just re-trigger by reinitializing observers
        observeUserProfile()
        observeTestProgress()
    }
}

/**
 * UI State for Student Home Screen
 */
data class StudentHomeUiState(
    val isLoading: Boolean = false,
    val userName: String = "Aspirant",
    val currentStreak: Int = 0,
    val testsCompleted: Int = 0,
    val notificationCount: Int = 0,
    val phase1Progress: com.ssbmax.core.domain.model.Phase1Progress? = null,
    val phase2Progress: com.ssbmax.core.domain.model.Phase2Progress? = null,
    val error: String? = null
)

