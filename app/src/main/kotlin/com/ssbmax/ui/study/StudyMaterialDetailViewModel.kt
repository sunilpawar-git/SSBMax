package com.ssbmax.ui.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.StudyMaterial
import com.ssbmax.core.domain.model.StudyProgress
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.study.GetStudyMaterialDetailUseCase
import com.ssbmax.core.domain.usecase.study.SaveStudyProgressUseCase
import com.ssbmax.core.domain.usecase.study.TrackStudySessionUseCase
import com.ssbmax.core.domain.usecase.study.GetStudyProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * ViewModel for Study Material Detail Screen
 * Handles material content loading and progress tracking
 * REFACTORED: Now uses use cases for business logic separation
 */
@HiltViewModel
class StudyMaterialDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getStudyMaterialDetail: GetStudyMaterialDetailUseCase,
    private val saveStudyProgress: SaveStudyProgressUseCase,
    private val trackStudySession: TrackStudySessionUseCase,
    private val getStudyProgress: GetStudyProgressUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val materialId: String = savedStateHandle.get<String>("categoryId") ?: ""

    private val _uiState = MutableStateFlow(StudyMaterialDetailUiState())
    val uiState: StateFlow<StudyMaterialDetailUiState> = _uiState.asStateFlow()

    private var sessionId: String? = null

    init {
        loadMaterial()
        startStudySession()
    }
    
    private fun loadMaterial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Try to load from cloud first using use case
                val cloudResult = getStudyMaterialDetail(materialId)

                val material = cloudResult.getOrNull()?.let { cloudMaterial ->
                    // Convert cloud material to UI model
                    StudyMaterialContent(
                        id = cloudMaterial.id,
                        title = cloudMaterial.title,
                        category = cloudMaterial.category,
                        author = cloudMaterial.author.ifEmpty { "SSB Expert" },
                        publishedDate = "2025",
                        readTime = cloudMaterial.readTime.ifEmpty { "10 min read" },
                        content = cloudMaterial.contentMarkdown,
                        isPremium = cloudMaterial.isPremium,
                        tags = emptyList(),
                        relatedMaterials = emptyList()
                    )
                } ?: run {
                    // Fallback to local content
                    val localMaterial = getMockMaterial(materialId)
                    // If it's HTML content, load from assets
                    if (localMaterial.content.startsWith("<!DOCTYPE html>")) {
                        localMaterial.copy(
                            content = StudyMaterialContentProvider.loadHTMLFromAssets(
                                context,
                                "piq_form.html"
                            )
                        )
                    } else {
                        localMaterial
                    }
                }

                // Load existing progress
                val currentUser = observeCurrentUser().first()
                if (currentUser != null) {
                    val progressResult = getStudyProgress(currentUser.id, materialId)
                    val existingProgress = progressResult.getOrNull()

                    _uiState.update {
                        it.copy(
                            material = material,
                            readingProgress = existingProgress?.progress ?: 0f,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            material = material,
                            readingProgress = 0f,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                // On error, fallback to local content
                try {
                    val fallbackMaterial = getMockMaterial(materialId)
                    val finalMaterial = if (fallbackMaterial.content.startsWith("<!DOCTYPE html>")) {
                        fallbackMaterial.copy(
                            content = StudyMaterialContentProvider.loadHTMLFromAssets(
                                context,
                                "piq_form.html"
                            )
                        )
                    } else {
                        fallbackMaterial
                    }
                    _uiState.update {
                        it.copy(
                            material = finalMaterial,
                            readingProgress = 0f,
                            isLoading = false,
                            error = null
                        )
                    }
                } catch (fallbackError: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = fallbackError.message ?: "Failed to load material"
                        )
                    }
                }
            }
        }
    }

    private fun startStudySession() {
        viewModelScope.launch {
            val currentUser = observeCurrentUser().first()
            if (currentUser != null) {
                val sessionResult = trackStudySession.startSession(currentUser.id, materialId)
                sessionId = sessionResult.getOrNull()?.id
            }
        }
    }

    fun updateProgress(progress: Float) {
        val coercedProgress = progress.coerceIn(0f, 100f)
        _uiState.update {
            it.copy(readingProgress = coercedProgress)
        }

        // Save progress to repository
        viewModelScope.launch {
            val currentUser = observeCurrentUser().first()
            if (currentUser != null) {
                val studyProgress = StudyProgress(
                    materialId = materialId,
                    userId = currentUser.id,
                    progress = coercedProgress,
                    lastReadAt = System.currentTimeMillis(),
                    timeSpent = 0L, // Will be updated on session end
                    isCompleted = coercedProgress >= 100f
                )
                saveStudyProgress(studyProgress)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        endStudySession()
    }

    private fun endStudySession() {
        viewModelScope.launch {
            sessionId?.let { id ->
                val progressIncrement = _uiState.value.readingProgress
                trackStudySession.endSession(id, progressIncrement)
            }
        }
    }
    
    private fun getMockMaterial(materialId: String): StudyMaterialContent {
        return StudyMaterialContentProvider.getMaterial(materialId)
    }
}

/**
 * UI State for Study Material Detail Screen
 */
data class StudyMaterialDetailUiState(
    val material: StudyMaterialContent? = null,
    val readingProgress: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Study material content model
 */
data class StudyMaterialContent(
    val id: String,
    val title: String,
    val category: String,
    val author: String,
    val publishedDate: String,
    val readTime: String,
    val content: String,
    val isPremium: Boolean,
    val tags: List<String>,
    val relatedMaterials: List<RelatedMaterial>
)

/**
 * Related material item
 */
data class RelatedMaterial(
    val id: String,
    val title: String
)

