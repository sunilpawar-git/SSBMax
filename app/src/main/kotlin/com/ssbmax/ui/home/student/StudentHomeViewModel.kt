package com.ssbmax.ui.home.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
        loadUserProgress()
        observeTestProgress()
    }
    
    private fun loadUserProgress() {
        viewModelScope.launch {
            // Load user profile for name
            val currentUser = authRepository.currentUser.first()
            if (currentUser != null) {
                userProfileRepository.getUserProfile(currentUser.id).collect { result ->
                    result.onSuccess { profile ->
                        _uiState.value = _uiState.value.copy(
                            userName = profile?.fullName ?: "Aspirant"
                        )
                    }
                }
            }
        }
    }
    
    private fun observeTestProgress() {
        viewModelScope.launch {
            val userId = authRepository.currentUser.first()?.id ?: return@launch
            
            kotlinx.coroutines.flow.combine(
                testProgressRepository.getPhase1Progress(userId),
                testProgressRepository.getPhase2Progress(userId)
            ) { phase1, phase2 ->
                Pair(phase1, phase2)
            }.collect { (phase1, phase2) ->
                _uiState.update { 
                    it.copy(
                        phase1Progress = phase1,
                        phase2Progress = phase2
                    )
                }
            }
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
    val userName: String = "Aspirant",
    val currentStreak: Int = 0,
    val testsCompleted: Int = 0,
    val notificationCount: Int = 0,
    val phase1Progress: com.ssbmax.core.domain.model.Phase1Progress? = null,
    val phase2Progress: com.ssbmax.core.domain.model.Phase2Progress? = null,
    val error: String? = null
)

