package com.ssbmax.ui.tests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.ViewModel
import com.ssbmax.core.domain.model.TestStatus
import com.ssbmax.core.domain.model.TestType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for Student Tests Screen
 */
@HiltViewModel
class StudentTestsViewModel @Inject constructor(
    // TODO: Inject TestResultRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StudentTestsUiState())
    val uiState: StateFlow<StudentTestsUiState> = _uiState.asStateFlow()
    
    init {
        loadAllTests()
    }
    
    private fun loadAllTests() {
        // TODO: Load from repository
        // For now, use mock data
        
        val phase1Tests = listOf(
            TestOverviewItem(
                type = TestType.OIR,
                name = "OIR Test",
                icon = Icons.Default.Quiz,
                category = "Screening",
                durationMinutes = 40,
                questionCount = 50,
                status = TestStatus.COMPLETED,
                latestScore = 85f
            ),
            TestOverviewItem(
                type = TestType.PPDT,
                name = "PPDT",
                icon = Icons.Default.Image,
                category = "Screening",
                durationMinutes = 30,
                questionCount = 1,
                status = TestStatus.COMPLETED,
                latestScore = 72f
            )
        )
        
        val phase2Tests = listOf(
            // Psychology Tests
            TestOverviewItem(
                type = TestType.TAT,
                name = "TAT",
                icon = Icons.Default.EditNote,
                category = "Psychology",
                durationMinutes = 30,
                questionCount = 12,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null
            ),
            TestOverviewItem(
                type = TestType.WAT,
                name = "WAT",
                icon = Icons.Default.EditNote,
                category = "Psychology",
                durationMinutes = 15,
                questionCount = 60,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null
            ),
            TestOverviewItem(
                type = TestType.SRT,
                name = "SRT",
                icon = Icons.Default.EditNote,
                category = "Psychology",
                durationMinutes = 30,
                questionCount = 60,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null
            ),
            TestOverviewItem(
                type = TestType.SD,
                name = "Self Description",
                icon = Icons.Default.EditNote,
                category = "Psychology",
                durationMinutes = 15,
                questionCount = 5,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null
            ),
            // GTO Tests
            TestOverviewItem(
                type = TestType.GTO,
                name = "GTO Tasks",
                icon = Icons.Default.Groups,
                category = "GTO",
                durationMinutes = 180,
                questionCount = 8,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null
            ),
            // Interview
            TestOverviewItem(
                type = TestType.IO,
                name = "Personal Interview",
                icon = Icons.Default.RecordVoiceOver,
                category = "Interview",
                durationMinutes = 45,
                questionCount = 1,
                status = TestStatus.NOT_ATTEMPTED,
                latestScore = null
            )
        )
        
        _uiState.value = StudentTestsUiState(
            phase1Tests = phase1Tests,
            phase2Tests = phase2Tests,
            isLoading = false
        )
    }
}

/**
 * UI State for Student Tests Screen
 */
data class StudentTestsUiState(
    val phase1Tests: List<TestOverviewItem> = emptyList(),
    val phase2Tests: List<TestOverviewItem> = emptyList(),
    val isLoading: Boolean = true
)

