package com.ssbmax.shared.presentation.study

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ssbmax.shared.domain.util.DomainLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/study/StudyMaterialsViewModel.kt`.
 * Study categories are currently hardcoded content (same TODO the Android
 * original carries: migrate to a Firestore-based dynamic content system).
 *
 * Follows the plain-class + own-`CoroutineScope` ViewModel pattern this phase
 * already established -- NOT `androidx.lifecycle.ViewModel`.
 * `ErrorLogger.log` (Android-only, Crashlytics-backed) replaced with
 * [DomainLogger], same seam every other ported ViewModel in this phase uses.
 */
class StudyMaterialsViewModel(
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(StudyMaterialsUiState())
    val uiState: StateFlow<StudyMaterialsUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "StudyMaterialsViewModel"
    }

    init {
        loadCategories()
    }

    fun close() {
        scope.cancel()
    }

    private fun loadCategories() {
        scope.launch {
            try {
                val categories = listOf(
                    StudyCategoryItem(StudyCategory.OIR_PREP, "OIR Test Prep", Icons.Default.Quiz, 24, false, Color(0xFFE3F2FD), Color(0xFF1976D2), Color(0xFF0D47A1)),
                    StudyCategoryItem(StudyCategory.PPDT_TECHNIQUES, "PPDT Techniques", Icons.Default.Image, 18, false, Color(0xFFE8F5E9), Color(0xFF4CAF50), Color(0xFF1B5E20)),
                    StudyCategoryItem(StudyCategory.PSYCHOLOGY_TESTS, "Psychology Tests", Icons.Default.Psychology, 32, true, Color(0xFFE0F7FA), Color(0xFF009688), Color(0xFF004D40)),
                    StudyCategoryItem(StudyCategory.PIQ_PREP, "PIQ Form Guide", Icons.AutoMirrored.Filled.Assignment, 15, false, Color(0xFFF3E5F5), Color(0xFF9C27B0), Color(0xFF4A148C)),
                    StudyCategoryItem(StudyCategory.GTO_TASKS, "GTO Tasks Guide", Icons.Default.Groups, 28, true, Color(0xFFE3F2FD), Color(0xFF2196F3), Color(0xFF0D47A1)),
                    StudyCategoryItem(StudyCategory.INTERVIEW_PREP, "Interview Prep", Icons.Default.RecordVoiceOver, 45, true, Color(0xFFFCE4EC), Color(0xFFE91E63), Color(0xFF880E4F)),
                    StudyCategoryItem(StudyCategory.GENERAL_TIPS, "General SSB Tips", Icons.Default.Lightbulb, 56, false, Color(0xFFFFF8E1), Color(0xFFFF9800), Color(0xFFE65100)),
                    StudyCategoryItem(StudyCategory.CURRENT_AFFAIRS, "Current Affairs", Icons.Default.Public, 120, true, Color(0xFFF3E5F5), Color(0xFF673AB7), Color(0xFF311B92)),
                    StudyCategoryItem(StudyCategory.PHYSICAL_FITNESS, "Physical Fitness", Icons.Default.FitnessCenter, 22, false, Color(0xFFFFEBEE), Color(0xFFF44336), Color(0xFFB71C1C))
                )

                val totalArticles = categories.sumOf { it.articleCount }

                _uiState.value = StudyMaterialsUiState(
                    categories = categories,
                    totalArticles = totalArticles,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                logger.e(TAG, "Error loading study material categories", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load study materials") }
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

/**
 * Study category display item. Note: this is a UI-local enum distinct from
 * [com.ssbmax.shared.domain.model.StudyCategory] (different members --
 * OIR_PREP/PIQ_PREP/PHYSICAL_FITNESS here vs. OIR_PREP/PPDT_TECH/PSYCHOLOGY
 * there) -- a pre-existing naming collision in the Android original
 * (`app/.../ui/study/StudyMaterialsScreen.kt`'s own `StudyCategory` vs.
 * `core:domain`'s), faithfully carried forward, not introduced by this port.
 */
data class StudyCategoryItem(
    val type: StudyCategory,
    val title: String,
    val icon: ImageVector,
    val articleCount: Int,
    val isPremium: Boolean,
    val backgroundColor: Color,
    val iconColor: Color,
    val textColor: Color
)

enum class StudyCategory {
    OIR_PREP,
    PPDT_TECHNIQUES,
    PSYCHOLOGY_TESTS,
    PIQ_PREP,
    GTO_TASKS,
    INTERVIEW_PREP,
    GENERAL_TIPS,
    CURRENT_AFFAIRS,
    PHYSICAL_FITNESS
}
