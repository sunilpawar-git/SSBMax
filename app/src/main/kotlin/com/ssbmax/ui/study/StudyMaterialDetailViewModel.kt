package com.ssbmax.ui.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.StudyMaterial
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * ViewModel for Study Material Detail Screen
 * Handles material content loading and progress tracking
 */
@HiltViewModel
class StudyMaterialDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val studyContentRepository: com.ssbmax.core.domain.repository.StudyContentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val materialId: String = savedStateHandle.get<String>("categoryId") ?: ""
    
    private val _uiState = MutableStateFlow(StudyMaterialDetailUiState())
    val uiState: StateFlow<StudyMaterialDetailUiState> = _uiState.asStateFlow()
    
    private var startTime: Long = 0L
    
    init {
        loadMaterial()
        startTime = System.currentTimeMillis()
    }
    
    private fun loadMaterial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Try to load from Firestore first
                val cloudResult = studyContentRepository.getStudyMaterial(materialId)
                
                val material = cloudResult.getOrNull()?.let { cloudMaterial ->
                    // Convert cloud material to UI model
                    StudyMaterialContent(
                        id = cloudMaterial.id,
                        title = cloudMaterial.title,
                        category = cloudMaterial.category,
                        author = cloudMaterial.author.ifEmpty { "SSB Expert" },
                        publishedDate = "2025", // TODO: Add to cloud model
                        readTime = cloudMaterial.readTime.ifEmpty { "10 min read" },
                        content = cloudMaterial.contentMarkdown,
                        isPremium = cloudMaterial.isPremium,
                        tags = emptyList(), // TODO: Parse from cloud
                        relatedMaterials = emptyList() // TODO: Parse from cloud
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
                
                _uiState.update {
                    it.copy(
                        material = material,
                        readingProgress = 0f,
                        isLoading = false,
                        error = null
                    )
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
    
    fun updateProgress(progress: Float) {
        _uiState.update {
            it.copy(readingProgress = progress.coerceIn(0f, 100f))
        }
        // TODO: Save progress to repository
    }
    
    fun trackReadingTime() {
        val timeSpent = System.currentTimeMillis() - startTime
        // TODO: Save reading time to repository
    }
    
    override fun onCleared() {
        super.onCleared()
        trackReadingTime()
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

