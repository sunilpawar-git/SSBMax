package com.ssbmax.ui.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.StudyMaterial
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Study Material Detail Screen
 * Handles material content loading, progress tracking, and bookmark state
 */
@HiltViewModel
class StudyMaterialDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookmarkRepository: com.ssbmax.core.domain.repository.BookmarkRepository,
    private val observeCurrentUser: com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
) : ViewModel() {
    
    private val materialId: String = savedStateHandle.get<String>("categoryId") ?: ""
    
    private val _uiState = MutableStateFlow(StudyMaterialDetailUiState())
    val uiState: StateFlow<StudyMaterialDetailUiState> = _uiState.asStateFlow()
    
    private var startTime: Long = 0L
    private var currentUserId: String = ""
    
    init {
        loadMaterial()
        observeBookmarkStatus()
        startTime = System.currentTimeMillis()
    }
    
    private fun loadMaterial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get current user (use first() to get single value, not collect which blocks)
                val user = observeCurrentUser().first()
                currentUserId = user?.id ?: ""
                
                // TODO: Load from repository
                // For now, use mock data
                val material = getMockMaterial(materialId)
                
                _uiState.update {
                    it.copy(
                        material = material,
                        readingProgress = 0f,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load material"
                    )
                }
            }
        }
    }
    
    private fun observeBookmarkStatus() {
        viewModelScope.launch {
            observeCurrentUser().collect { user ->
                val userId = user?.id ?: return@collect
                currentUserId = userId
                
                bookmarkRepository.isBookmarked(userId, materialId).collect { isBookmarked ->
                    _uiState.update { it.copy(isBookmarked = isBookmarked) }
                }
            }
        }
    }
    
    fun toggleBookmark() {
        viewModelScope.launch {
            if (currentUserId.isNotEmpty()) {
                bookmarkRepository.toggleBookmark(currentUserId, materialId)
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
    val isBookmarked: Boolean = false,
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

