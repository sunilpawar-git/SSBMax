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

/**
 * ViewModel for Study Material Detail Screen
 * Handles material content loading, progress tracking, and bookmark state
 */
@HiltViewModel
class StudyMaterialDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
    // TODO: Inject StudyMaterialRepository when available
) : ViewModel() {
    
    private val materialId: String = savedStateHandle.get<String>("materialId") ?: ""
    
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
                // TODO: Load from repository
                // For now, use mock data
                val material = getMockMaterial(materialId)
                
                _uiState.update {
                    it.copy(
                        material = material,
                        isBookmarked = false, // TODO: Load from repository
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
    
    fun toggleBookmark() {
        _uiState.update {
            it.copy(isBookmarked = !it.isBookmarked)
        }
        // TODO: Save bookmark state to repository
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
        return StudyMaterialContent(
            id = materialId,
            title = "Understanding OIR Test - Complete Guide",
            category = "OIR Preparation",
            author = "SSB Expert",
            publishedDate = "Oct 15, 2024",
            readTime = "10 min read",
            content = """
                # Officer Intelligence Rating (OIR) Test
                
                The Officer Intelligence Rating test is a crucial component of the SSB selection process. 
                This comprehensive guide will help you understand and prepare for the OIR test effectively.
                
                ## What is OIR?
                
                The OIR test evaluates your cognitive abilities, logical reasoning, and problem-solving skills. 
                It consists of both verbal and non-verbal reasoning questions designed to assess your mental 
                alertness and decision-making capabilities under time pressure.
                
                ## Test Structure
                
                - **Duration**: 30-40 minutes
                - **Questions**: 40-50 questions
                - **Sections**: 
                  - Verbal Reasoning
                  - Numerical Ability
                  - Abstract Reasoning
                  - Spatial Visualization
                
                ## Preparation Tips
                
                1. **Practice Regularly**: Consistent practice is key to improving your speed and accuracy.
                2. **Time Management**: Learn to allocate time wisely for each question.
                3. **Pattern Recognition**: Develop your ability to identify patterns quickly.
                4. **Stay Calm**: Maintain composure during the test to think clearly.
                
                ## Common Question Types
                
                ### Verbal Reasoning
                - Analogies
                - Synonyms and Antonyms
                - Sentence Completion
                - Reading Comprehension
                
                ### Numerical Ability
                - Number Series
                - Arithmetic Operations
                - Percentages and Ratios
                - Data Interpretation
                
                ### Abstract Reasoning
                - Pattern Completion
                - Figure Matrices
                - Odd One Out
                - Series Continuation
                
                ## Practice Strategy
                
                Start with easier questions to build confidence, then gradually move to more complex problems. 
                Review your mistakes and understand the reasoning behind correct answers. Time yourself 
                during practice sessions to simulate test conditions.
                
                ## Important Points to Remember
                
                - Read instructions carefully
                - Don't spend too much time on any single question
                - Use elimination method for multiple choice questions
                - Trust your first instinct if you're unsure
                - Keep track of time throughout the test
                
                ## Conclusion
                
                Success in the OIR test comes from a combination of knowledge, practice, and test-taking 
                strategy. Use this guide as a foundation and supplement it with regular practice tests.
                
                Good luck with your preparation!
            """.trimIndent(),
            isPremium = false,
            tags = listOf("OIR", "Cognitive Tests", "SSB Preparation"),
            relatedMaterials = listOf(
                RelatedMaterial("2", "Advanced OIR Techniques"),
                RelatedMaterial("3", "OIR Practice Questions")
            )
        )
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

