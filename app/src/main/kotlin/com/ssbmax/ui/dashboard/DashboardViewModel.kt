package com.ssbmax.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    // TODO: Inject repositories when available
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            // TODO: Load from repository
            _uiState.value = DashboardUiState(
                userName = "Aspirant",
                currentStreak = 7,
                testsCompleted = 21,
                studyHours = 34.5f,
                overallProgress = 0.42f,
                dailyTip = "Stay confident and be yourself during SSB interviews. Authenticity is key to success. Practice self-awareness and reflect on your experiences daily.",
                weakAreas = listOf("GTO Planning", "Interview Confidence"),
                recentActivities = getSampleRecentActivities()
            )
        }
    }

    private fun getSampleRecentActivities(): List<RecentActivity> {
        return listOf(
            RecentActivity(
                title = "Completed TAT practice test",
                timeAgo = "2 hours ago",
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF10B981)
            ),
            RecentActivity(
                title = "Studied GTO planning exercises",
                timeAgo = "5 hours ago",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconColor = Color(0xFF6366F1)
            ),
            RecentActivity(
                title = "Reviewed interview questions",
                timeAgo = "Yesterday",
                icon = Icons.Default.RecordVoiceOver,
                iconColor = Color(0xFFF59E0B)
            ),
            RecentActivity(
                title = "Updated progress tracker",
                timeAgo = "Yesterday",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconColor = Color(0xFFEC4899)
            ),
            RecentActivity(
                title = "Completed WAT test - 85% score",
                timeAgo = "2 days ago",
                icon = Icons.Default.Star,
                iconColor = Color(0xFF8B5CF6)
            )
        )
    }

    fun refreshDashboard() {
        loadDashboardData()
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val userName: String = "Aspirant",
    val currentStreak: Int = 0,
    val testsCompleted: Int = 0,
    val studyHours: Float = 0f,
    val overallProgress: Float = 0f,
    val dailyTip: String = "",
    val weakAreas: List<String> = emptyList(),
    val recentActivities: List<RecentActivity> = emptyList(),
    val error: String? = null
)

