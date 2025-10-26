package com.ssbmax.ui.study

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.repository.BookmarkRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Study Materials Screen
 * Note: Study materials are currently stored as code-based content
 * TODO: Migrate to Firestore-based StudyMaterialRepository for dynamic content
 */
@HiltViewModel
class StudyMaterialsViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StudyMaterialsUiState())
    val uiState: StateFlow<StudyMaterialsUiState> = _uiState.asStateFlow()
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                // Get current user
                val currentUser = observeCurrentUser().first()
                val userId = currentUser?.id
                
                // Get bookmarked count from repository
                val bookmarkedCount = if (userId != null) {
                    try {
                        // Fetch user's bookmarked materials
                        val bookmarks = bookmarkRepository.getBookmarkedMaterials(userId).first()
                        bookmarks.size
                    } catch (e: Exception) {
                        Log.w("StudyMaterials", "Failed to load bookmarks", e)
                        0
                    }
                } else {
                    0
                }
                
                // Study categories are currently hardcoded content
                // TODO: Move to Firestore-based dynamic content system
                val categories = listOf(
                    StudyCategoryItem(
                        type = StudyCategory.OIR_PREP,
                        title = "OIR Test Prep",
                        icon = Icons.Default.Quiz,
                        articleCount = 24,
                        isPremium = false,
                        backgroundColor = Color(0xFFE3F2FD),
                        iconColor = Color(0xFF1976D2),
                        textColor = Color(0xFF0D47A1)
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.PPDT_TECHNIQUES,
                        title = "PPDT Techniques",
                        icon = Icons.Default.Image,
                        articleCount = 18,
                        isPremium = false,
                        backgroundColor = Color(0xFFF3E5F5),
                        iconColor = Color(0xFF7B1FA2),
                        textColor = Color(0xFF4A148C)
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.PSYCHOLOGY_TESTS,
                        title = "Psychology Tests",
                        icon = Icons.Default.Psychology,
                        articleCount = 32,
                        isPremium = true,
                        backgroundColor = Color(0xFFE8F5E9),
                        iconColor = Color(0xFF388E3C),
                        textColor = Color(0xFF1B5E20)
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.GTO_TASKS,
                        title = "GTO Tasks Guide",
                        icon = Icons.Default.Groups,
                        articleCount = 28,
                        isPremium = true,
                        backgroundColor = Color(0xFFFFF3E0),
                        iconColor = Color(0xFFF57C00),
                        textColor = Color(0xFFE65100)
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.INTERVIEW_PREP,
                        title = "Interview Prep",
                        icon = Icons.Default.RecordVoiceOver,
                        articleCount = 45,
                        isPremium = true,
                        backgroundColor = Color(0xFFFFEBEE),
                        iconColor = Color(0xFFC62828),
                        textColor = Color(0xFFB71C1C)
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.GENERAL_TIPS,
                        title = "General SSB Tips",
                        icon = Icons.Default.Lightbulb,
                        articleCount = 56,
                        isPremium = false,
                        backgroundColor = Color(0xFFFFF9C4),
                        iconColor = Color(0xFFF9A825),
                        textColor = Color(0xFFF57F17)
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.CURRENT_AFFAIRS,
                        title = "Current Affairs",
                        icon = Icons.Default.Public,
                        articleCount = 120,
                        isPremium = true,
                        backgroundColor = Color(0xFFE0F2F1),
                        iconColor = Color(0xFF00796B),
                        textColor = Color(0xFF004D40)
                    ),
                    StudyCategoryItem(
                        type = StudyCategory.PHYSICAL_FITNESS,
                        title = "Physical Fitness",
                        icon = Icons.Default.FitnessCenter,
                        articleCount = 22,
                        isPremium = false,
                        backgroundColor = Color(0xFFFCE4EC),
                        iconColor = Color(0xFFC2185B),
                        textColor = Color(0xFF880E4F)
                    )
                )
                
                val totalArticles = categories.sumOf { it.articleCount }
                
                _uiState.value = StudyMaterialsUiState(
                    categories = categories,
                    totalArticles = totalArticles,
                    bookmarkedCount = bookmarkedCount,
                    isLoading = false,
                    error = null
                )
                
            } catch (e: Exception) {
                Log.e("StudyMaterials", "Error loading categories", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load study materials"
                )
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
    val bookmarkedCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

