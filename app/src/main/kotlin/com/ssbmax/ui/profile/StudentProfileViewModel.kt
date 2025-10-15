package com.ssbmax.ui.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for Student Profile Screen
 */
@HiltViewModel
class StudentProfileViewModel @Inject constructor(
    // TODO: Inject UserRepository, TestResultRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StudentProfileUiState())
    val uiState: StateFlow<StudentProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        // TODO: Load from repository
        // For now, use mock data
        _uiState.value = StudentProfileUiState(
            userName = "SSB Aspirant",
            userEmail = "aspirant@ssbmax.com",
            photoUrl = null,
            isPremium = false,
            totalTestsAttempted = 15,
            totalStudyHours = 42,
            streakDays = 7,
            averageScore = 78.5f,
            phase1Completion = 60,
            phase2Completion = 30,
            recentAchievements = listOf(
                "Completed First OIR Test",
                "7-Day Study Streak",
                "Phase 1 Master - 85%+"
            ),
            recentTests = listOf(
                RecentTest(
                    name = "OIR Test",
                    date = "2 days ago",
                    score = 85
                ),
                RecentTest(
                    name = "PPDT",
                    date = "5 days ago",
                    score = 72
                ),
                RecentTest(
                    name = "OIR Test",
                    date = "1 week ago",
                    score = 80
                )
            ),
            isLoading = false
        )
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
    val isLoading: Boolean = true
)

