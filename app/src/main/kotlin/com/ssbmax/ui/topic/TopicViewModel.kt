package com.ssbmax.ui.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.StudyMaterial
import com.ssbmax.core.domain.model.TestType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Topic Screen
 * Manages topic information, study materials, and tests for a specific SSB topic
 */
@HiltViewModel
class TopicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
    // TODO: Inject StudyMaterialRepository, TestRepository when available
) : ViewModel() {
    
    private val testType: String = savedStateHandle.get<String>("testType") ?: "OIR"
    
    private val _uiState = MutableStateFlow(TopicUiState())
    val uiState: StateFlow<TopicUiState> = _uiState.asStateFlow()
    
    init {
        loadTopicContent()
    }
    
    private fun loadTopicContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // TODO: Load from repository
                // For now, use TopicContentLoader with mock data
                val topicInfo = TopicContentLoader.getTopicInfo(testType)
                
                _uiState.update {
                    it.copy(
                        testType = testType,
                        topicTitle = topicInfo.title,
                        introduction = topicInfo.introduction,
                        studyMaterials = topicInfo.studyMaterials,
                        availableTests = topicInfo.tests,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load topic content"
                    )
                }
            }
        }
    }
    
    fun refresh() {
        loadTopicContent()
    }
}

/**
 * UI State for Topic Screen
 */
data class TopicUiState(
    val testType: String = "",
    val topicTitle: String = "",
    val introduction: String = "",
    val studyMaterials: List<StudyMaterialItem> = emptyList(),
    val availableTests: List<TestType> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Study material item for list display
 */
data class StudyMaterialItem(
    val id: String,
    val title: String,
    val duration: String,
    val isPremium: Boolean
)

