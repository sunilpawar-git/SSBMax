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
                // For now, use mock data
                val topicInfo = getTopicInfo(testType)
                
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
    
    private fun getTopicInfo(testType: String): TopicInfo {
        return when (testType.uppercase()) {
            "OIR" -> TopicInfo(
                title = "Officer Intelligence Rating",
                introduction = getMockIntroduction(testType),
                studyMaterials = getMockStudyMaterials(testType),
                tests = listOf(TestType.OIR)
            )
            "PPDT" -> TopicInfo(
                title = "Picture Perception & Description Test",
                introduction = getMockIntroduction(testType),
                studyMaterials = getMockStudyMaterials(testType),
                tests = listOf(TestType.PPDT)
            )
            "PSYCHOLOGY" -> TopicInfo(
                title = "Psychology Tests",
                introduction = getMockIntroduction(testType),
                studyMaterials = getMockStudyMaterials(testType),
                tests = listOf(TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD)
            )
            "GTO" -> TopicInfo(
                title = "Group Testing Officer Tasks",
                introduction = getMockIntroduction(testType),
                studyMaterials = getMockStudyMaterials(testType),
                tests = listOf(TestType.GTO)
            )
            "INTERVIEW" -> TopicInfo(
                title = "Interview Preparation",
                introduction = getMockIntroduction(testType),
                studyMaterials = getMockStudyMaterials(testType),
                tests = listOf(TestType.IO)
            )
            else -> TopicInfo(
                title = "SSB Topic",
                introduction = "Learn about SSB selection process.",
                studyMaterials = emptyList(),
                tests = emptyList()
            )
        }
    }
    
    private fun getMockIntroduction(testType: String): String {
        return when (testType.uppercase()) {
            "OIR" -> """
                The Officer Intelligence Rating (OIR) test evaluates your cognitive abilities, 
                logical reasoning, and problem-solving skills. It consists of verbal and 
                non-verbal reasoning questions designed to assess your mental alertness and 
                decision-making capabilities under time pressure.
                
                The test typically includes:
                • Verbal reasoning questions
                • Numerical ability problems
                • Abstract reasoning puzzles
                • Spatial visualization tasks
                
                Duration: 30-40 minutes
                Questions: 40-50 questions
                Difficulty: Moderate to High
            """.trimIndent()
            "PPDT" -> """
                Picture Perception and Description Test (PPDT) assesses your perception, 
                imagination, and ability to construct a meaningful story from an ambiguous picture.
                
                The test evaluates:
                • Power of perception
                • Ability to interpret situations
                • Narration skills
                • Group discussion capabilities
                
                Process:
                1. Picture shown for 30 seconds
                2. Write a story in 4 minutes
                3. Group discussion on stories
                4. Final narration
            """.trimIndent()
            else -> "Detailed information about this topic will be available soon."
        }
    }
    
    private fun getMockStudyMaterials(testType: String): List<StudyMaterialItem> {
        // TODO: Replace with actual data from repository
        return listOf(
            StudyMaterialItem(
                id = "1",
                title = "Getting Started with $testType",
                duration = "10 min read",
                isPremium = false
            ),
            StudyMaterialItem(
                id = "2",
                title = "Advanced Techniques",
                duration = "15 min read",
                isPremium = true
            ),
            StudyMaterialItem(
                id = "3",
                title = "Practice Strategies",
                duration = "12 min read",
                isPremium = false
            )
        )
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
 * Topic information model
 */
private data class TopicInfo(
    val title: String,
    val introduction: String,
    val studyMaterials: List<StudyMaterialItem>,
    val tests: List<TestType>
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

