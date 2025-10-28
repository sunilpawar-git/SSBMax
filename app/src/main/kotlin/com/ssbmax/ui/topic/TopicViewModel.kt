package com.ssbmax.ui.topic

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.StudyMaterial
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.TestProgressRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Topic Screen
 * Manages topic information, study materials, and tests for a specific SSB topic
 * Note: Topic content currently uses TopicContentLoader (code-based)
 * TODO: Migrate to Firestore-based dynamic content system
 */
@HiltViewModel
class TopicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val testProgressRepository: TestProgressRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {
    
    private val testType: String = savedStateHandle.get<String>("topicId") ?: "OIR"
    
    private val _uiState = MutableStateFlow(TopicUiState())
    val uiState: StateFlow<TopicUiState> = _uiState.asStateFlow()
    
    init {
        loadTopicContent()
    }
    
    private fun loadTopicContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get current user for personalized data
                val currentUser = observeCurrentUser().first()
                val userId = currentUser?.id
                
                // Load topic content from TopicContentLoader (code-based)
                // TODO: Migrate to Firestore-based dynamic content
                val topicInfo = TopicContentLoader.getTopicInfo(testType)
                
                // Load test progress if logged in
                val testProgress = if (userId != null) {
                    try {
                        // Get progress based on test type
                        when (testType) {
                            "OIR" -> {
                                val phase1 = testProgressRepository.getPhase1Progress(userId).first()
                                phase1.oirProgress
                            }
                            "PPDT" -> {
                                val phase1 = testProgressRepository.getPhase1Progress(userId).first()
                                phase1.ppdtProgress
                            }
                            else -> {
                                // For Phase 2 tests, get appropriate progress
                                val phase2 = testProgressRepository.getPhase2Progress(userId).first()
                                phase2.psychologyProgress // Simplified
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("Topic", "Failed to load test progress", e)
                        null
                    }
                } else {
                    null
                }
                
                _uiState.update {
                    it.copy(
                        testType = testType,
                        topicTitle = topicInfo.title,
                        introduction = topicInfo.introduction,
                        studyMaterials = topicInfo.studyMaterials,
                        availableTests = topicInfo.tests,
                        testCompletionStatus = testProgress?.status,
                        testLatestScore = testProgress?.latestScore,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("Topic", "Error loading topic content", e)
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
    val testCompletionStatus: com.ssbmax.core.domain.model.TestStatus? = null,
    val testLatestScore: Float? = null,
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

