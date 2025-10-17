package com.ssbmax.ui.tests.ppdt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for PPDT Test Screen
 */
@HiltViewModel
class PPDTTestViewModel @Inject constructor(
    // TODO: Inject PPDTTestRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PPDTTestUiState())
    val uiState: StateFlow<PPDTTestUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    private var currentSession: PPDTTestSession? = null
    
    init {
        loadTest()
    }
    
    fun loadTest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // TODO: Load from repository
                val question = generateMockQuestion()
                val config = PPDTTestConfig()
                
                currentSession = PPDTTestSession(
                    sessionId = UUID.randomUUID().toString(),
                    userId = "mock-user-id",
                    questionId = question.id,
                    question = question,
                    startTime = System.currentTimeMillis(),
                    imageViewingStartTime = null,
                    writingStartTime = null,
                    currentPhase = PPDTPhase.INSTRUCTIONS,
                    story = "",
                    isCompleted = false,
                    isPaused = false
                )
                
                updateUiFromSession()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load test"
                )
            }
        }
    }
    
    fun startTest() {
        val session = currentSession ?: return
        
        currentSession = session.copy(
            currentPhase = PPDTPhase.IMAGE_VIEWING,
            imageViewingStartTime = System.currentTimeMillis()
        )
        
        updateUiFromSession()
        startTimer(30) // 30 seconds for image viewing
        
        // Auto-proceed to writing after viewing
        viewModelScope.launch {
            delay(30000)
            if (_uiState.value.currentPhase == PPDTPhase.IMAGE_VIEWING) {
                proceedToNextPhase()
            }
        }
    }
    
    fun proceedToNextPhase() {
        val session = currentSession ?: return
        
        when (session.currentPhase) {
            PPDTPhase.IMAGE_VIEWING -> {
                timerJob?.cancel()
                currentSession = session.copy(
                    currentPhase = PPDTPhase.WRITING,
                    writingStartTime = System.currentTimeMillis()
                )
                updateUiFromSession()
                startTimer(session.question.writingTimeMinutes * 60)
            }
            PPDTPhase.WRITING -> {
                if (_uiState.value.story.length >= session.question.minCharacters) {
                    timerJob?.cancel()
                    currentSession = session.copy(currentPhase = PPDTPhase.REVIEW)
                    updateUiFromSession()
                }
            }
            else -> {}
        }
    }
    
    fun returnToWriting() {
        val session = currentSession ?: return
        currentSession = session.copy(currentPhase = PPDTPhase.WRITING)
        updateUiFromSession()
        startTimer(session.question.writingTimeMinutes * 60)
    }
    
    fun updateStory(newStory: String) {
        val session = currentSession ?: return
        currentSession = session.copy(story = newStory)
        _uiState.value = _uiState.value.copy(
            story = newStory,
            charactersCount = newStory.length,
            canProceedToNextPhase = newStory.length >= session.question.minCharacters
        )
    }
    
    fun submitTest() {
        timerJob?.cancel()
        
        val session = currentSession ?: return
        
        viewModelScope.launch {
            // TODO: Submit to repository
            // Generate mock submission
            val submissionId = UUID.randomUUID().toString()
            
            // Generate AI preliminary score
            val aiScore = generateMockAIScore(session.story)
            
            // Mark as submitted
            currentSession = session.copy(
                currentPhase = PPDTPhase.SUBMITTED,
                isCompleted = true
            )
            
            _uiState.value = _uiState.value.copy(
                isSubmitted = true,
                submissionId = submissionId
            )
        }
    }
    
    fun pauseTest() {
        timerJob?.cancel()
        val session = currentSession ?: return
        currentSession = session.copy(isPaused = true)
        // TODO: Save session state
    }
    
    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(timeRemainingSeconds = seconds)
        
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemainingSeconds > 0) {
                delay(1000)
                val newTime = _uiState.value.timeRemainingSeconds - 1
                _uiState.value = _uiState.value.copy(timeRemainingSeconds = newTime)
                
                if (newTime == 0) {
                    // Auto-proceed when time runs out
                    when (_uiState.value.currentPhase) {
                        PPDTPhase.IMAGE_VIEWING -> proceedToNextPhase()
                        PPDTPhase.WRITING -> proceedToNextPhase()
                        else -> {}
                    }
                }
            }
        }
    }
    
    private fun updateUiFromSession() {
        val session = currentSession ?: return
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            currentPhase = session.currentPhase,
            imageUrl = "https://example.com/ppdt-image.jpg", // TODO: Real URL
            story = session.story,
            charactersCount = session.story.length,
            minCharacters = session.question.minCharacters,
            maxCharacters = session.question.maxCharacters,
            canProceedToNextPhase = session.story.length >= session.question.minCharacters
        )
    }
    
    private fun generateMockQuestion(): PPDTQuestion {
        return PPDTQuestion(
            id = "ppdt_q1",
            imageUrl = "https://example.com/ppdt-image.jpg",
            imageDescription = "A hazy image showing people in a situation",
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4,
            minCharacters = 200,
            maxCharacters = 1000
        )
    }
    
    private fun generateMockAIScore(story: String): PPDTAIScore {
        // TODO: Actual AI scoring
        return PPDTAIScore(
            perceptionScore = 16f,
            imaginationScore = 14f,
            narrationScore = 15f,
            characterDepictionScore = 14f,
            positivityScore = 17f,
            overallScore = 76f,
            feedback = "Good overall story with positive outlook. Could improve imagination.",
            strengths = listOf(
                "Good character development",
                "Positive and optimistic outlook",
                "Clear narrative structure"
            ),
            areasForImprovement = listOf(
                "Could add more imaginative elements",
                "Describe the situation's background in more detail"
            )
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

/**
 * UI State for PPDT Test Screen
 */
data class PPDTTestUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentPhase: PPDTPhase = PPDTPhase.INSTRUCTIONS,
    val imageUrl: String = "",
    val story: String = "",
    val charactersCount: Int = 0,
    val minCharacters: Int = 200,
    val maxCharacters: Int = 1000,
    val timeRemainingSeconds: Int = 0,
    val canProceedToNextPhase: Boolean = false,
    val isSubmitted: Boolean = false,
    val submissionId: String? = null
)

