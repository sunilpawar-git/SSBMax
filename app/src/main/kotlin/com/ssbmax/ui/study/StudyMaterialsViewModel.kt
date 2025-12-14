package com.ssbmax.ui.study

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Study Materials Screen
 * Note: Study materials are currently stored as code-based content
 * TODO: Migrate to Firestore-based StudyMaterialRepository for dynamic content
 */
@HiltViewModel
class StudyMaterialsViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(StudyMaterialsUiState())
    val uiState: StateFlow<StudyMaterialsUiState> = _uiState.asStateFlow()
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                // Study categories are currently hardcoded content
                // TODO: Move to Firestore-based dynamic content system
                val categories = listOf(
                    StudyCategoryItem(
                        type = StudyCategory.OIR_PREP,
                        title = "OIR Test Prep",
                        icon = Icons.Default.Quiz,
                        articleCount = 24,
                        isPremium = false,
                        backgroundColor = Color(0xFFE3F2FD), // Light blue background
                        iconColor = Color(0xFF1976D2),      // Bright Blue (matches OIR test)
                        textColor = Color(0xFF0D47A1)       // Dark blue text
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.PPDT_TECHNIQUES,
                        title = "PPDT Techniques",
                        icon = Icons.Default.Image,
                        articleCount = 18,
                        isPremium = false,
                        backgroundColor = Color(0xFFE8F5E9), // Light green background
                        iconColor = Color(0xFF4CAF50),      // Bright Green (matches PPDT test)
                        textColor = Color(0xFF1B5E20)       // Dark green text
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.PSYCHOLOGY_TESTS,
                        title = "Psychology Tests",
                        icon = Icons.Default.Psychology,
                        articleCount = 32,
                        isPremium = true,
                        backgroundColor = Color(0xFFE0F7FA), // Light cyan background
                        iconColor = Color(0xFF009688),      // Bright Teal
                        textColor = Color(0xFF004D40)       // Dark teal text
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.PIQ_PREP,
                        title = "PIQ Form Guide",
                        icon = Icons.Default.Assignment,
                        articleCount = 15,
                        isPremium = false,
                        backgroundColor = Color(0xFFF3E5F5), // Light purple background
                        iconColor = Color(0xFF9C27B0),      // Bright Purple
                        textColor = Color(0xFF4A148C)       // Dark purple text
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.GTO_TASKS,
                        title = "GTO Tasks Guide",
                        icon = Icons.Default.Groups,
                        articleCount = 28,
                        isPremium = true,
                        backgroundColor = Color(0xFFE3F2FD), // Light blue background
                        iconColor = Color(0xFF2196F3),      // Bright Blue (matches GTO_GD)
                        textColor = Color(0xFF0D47A1)       // Dark blue text
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.INTERVIEW_PREP,
                        title = "Interview Prep",
                        icon = Icons.Default.RecordVoiceOver,
                        articleCount = 45,
                        isPremium = true,
                        backgroundColor = Color(0xFFFCE4EC), // Light pink background
                        iconColor = Color(0xFFE91E63),      // Bright Pink
                        textColor = Color(0xFF880E4F)       // Dark pink text
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.GENERAL_TIPS,
                        title = "General SSB Tips",
                        icon = Icons.Default.Lightbulb,
                        articleCount = 56,
                        isPremium = false,
                        backgroundColor = Color(0xFFFFF8E1), // Light amber background
                        iconColor = Color(0xFFFF9800),     // Bright Orange
                        textColor = Color(0xFFE65100)      // Dark orange text
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.CURRENT_AFFAIRS,
                        title = "Current Affairs",
                        icon = Icons.Default.Public,
                        articleCount = 120,
                        isPremium = true,
                        backgroundColor = Color(0xFFF3E5F5), // Light purple background
                        iconColor = Color(0xFF673AB7),     // Bright Deep Purple
                        textColor = Color(0xFF311B92)      // Dark deep purple text
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.PHYSICAL_FITNESS,
                        title = "Physical Fitness",
                        icon = Icons.Default.FitnessCenter,
                        articleCount = 22,
                        isPremium = false,
                        backgroundColor = Color(0xFFFFEBEE), // Light red background
                        iconColor = Color(0xFFF44336),     // Bright Red
                        textColor = Color(0xFFB71C1C)      // Dark red text
                    )
                )
                
                val totalArticles = categories.sumOf { it.articleCount }
                
                _uiState.value = StudyMaterialsUiState(
                    categories = categories,
                    totalArticles = totalArticles,
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                ErrorLogger.log(e, "Error loading study material categories")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load study materials"
                ) }
            }
        }
    }
}

/**
 * UI State for Study Materials Screen
 */
data class StudyMaterialsUiState(
    val categories: List<StudyCategoryItem> = emptyList(),
    val totalArticles: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

