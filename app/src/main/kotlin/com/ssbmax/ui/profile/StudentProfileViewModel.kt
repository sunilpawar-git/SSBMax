package com.ssbmax.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.repository.TestProgressRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
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
 * ViewModel for Student Profile Screen
 * Fetches profile data from UserProfileRepository and progress from TestProgressRepository
 */
@HiltViewModel
class StudentProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val testProgressRepository: TestProgressRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StudentProfileUiState())
    val uiState: StateFlow<StudentProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current user
                val currentUser = observeCurrentUser().first()
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Please login to view your profile"
                    )
                    return@launch
                }
                
                // Load user profile
                val profileResult = userProfileRepository.getUserProfile(currentUser.id).first()
                val userProfile = profileResult.getOrNull()
                
                // Load test progress
                val phase1Progress = testProgressRepository.getPhase1Progress(currentUser.id).first()
                val phase2Progress = testProgressRepository.getPhase2Progress(currentUser.id).first()
                
                // Calculate stats
                val testsWithScores = listOfNotNull(
                    phase1Progress.oirProgress.latestScore,
                    phase1Progress.ppdtProgress.latestScore,
                    phase2Progress.psychologyProgress.latestScore,
                    phase2Progress.gtoProgress.latestScore,
                    phase2Progress.interviewProgress.latestScore
                )
                
                val totalTestsAttempted = testsWithScores.size
                val averageScore = if (testsWithScores.isNotEmpty()) {
                    testsWithScores.average().toFloat()
                } else {
                    0f
                }
                
                // Calculate phase completion (simplified - based on status)
                val phase1Completion = ((if (phase1Progress.oirProgress.latestScore != null) 50 else 0) +
                        (if (phase1Progress.ppdtProgress.latestScore != null) 50 else 0))
                
                val phase2Tests = listOf(
                    phase2Progress.psychologyProgress,
                    phase2Progress.gtoProgress,
                    phase2Progress.interviewProgress
                )
                val phase2Completion = (phase2Tests.count { it.latestScore != null } * 33).coerceAtMost(100)
                
                _uiState.value = StudentProfileUiState(
                    userName = userProfile?.fullName ?: currentUser.displayName ?: "SSB Aspirant",
                    userEmail = userProfile?.userId ?: currentUser.email ?: "",
                    photoUrl = userProfile?.profilePictureUrl ?: currentUser.photoUrl,
                    isPremium = userProfile?.subscriptionType?.name == "PREMIUM",
                    totalTestsAttempted = totalTestsAttempted,
                    totalStudyHours = 0, // TODO: Track study hours
                    streakDays = 0, // TODO: Track streak
                    averageScore = averageScore,
                    phase1Completion = phase1Completion,
                    phase2Completion = phase2Completion,
                    recentAchievements = emptyList(), // TODO: Implement achievements system
                    recentTests = emptyList(), // TODO: Fetch recent test history
                    isLoading = false,
                    error = null
                )
                
            } catch (e: Exception) {
                Log.e("StudentProfile", "Error loading profile", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
        }
    }
}

/**
 * UI State for Student Profile Screen
 */
data class StudentProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val photoUrl: String? = null,
    val isPremium: Boolean = false,
    val totalTestsAttempted: Int = 0,
    val totalStudyHours: Int = 0,
    val streakDays: Int = 0,
    val averageScore: Float = 0f,
    val phase1Completion: Int = 0,
    val phase2Completion: Int = 0,
    val recentAchievements: List<String> = emptyList(),
    val recentTests: List<RecentTest> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

