package com.ssbmax.ui.tests.wat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitWATTestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for WAT Test Screen
 * Loads test questions from cloud via TestContentRepository
 */
@HiltViewModel
class WATTestViewModel @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val submitWATTest: SubmitWATTestUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WATTestUiState())
    val uiState: StateFlow<WATTestUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    
    fun loadTest(testId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val user = observeCurrentUser().first()
                val userId = user?.id ?: "mock-user-id"
                
                // Create test session
                val sessionResult = testContentRepository.createTestSession(
                    userId = userId,
                    testId = testId,
                    testType = TestType.WAT
                )
                
                if (sessionResult.isFailure) {
                    throw sessionResult.exceptionOrNull() ?: Exception("Failed to create test session")
                }
                
                // Fetch words from cloud
                val wordsResult = testContentRepository.getWATQuestions(testId)
                
                if (wordsResult.isFailure) {
                    throw wordsResult.exceptionOrNull() ?: Exception("Failed to load test words")
                }
                
                val words = wordsResult.getOrNull() ?: emptyList()
                
                if (words.isEmpty()) {
                    throw Exception("No words found for this test")
                }
                
                val config = WATTestConfig()
                
                _uiState.update { it.copy(
                    isLoading = false,
                    testId = testId,
                    words = words,
                    config = config,
                    phase = WATPhase.INSTRUCTIONS
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }
    
    fun startTest() {
        _uiState.update { it.copy(
            phase = WATPhase.IN_PROGRESS,
            currentWordIndex = 0,
            startTime = System.currentTimeMillis()
        ) }
        startWordTimer()
    }
    
    fun updateResponse(response: String) {
        // Limit to max length
        val maxLength = _uiState.value.config?.maxResponseLength ?: 50
        if (response.length <= maxLength) {
            _uiState.update { it.copy(currentResponse = response) }
        }
    }
    
    fun submitResponse() {
        val state = _uiState.value
        val currentWord = state.currentWord ?: return
        
        // Save response
        val response = WATWordResponse(
            wordId = currentWord.id,
            word = currentWord.word,
            response = state.currentResponse,
            timeTakenSeconds = (state.config?.timePerWordSeconds ?: 15) - state.timeRemaining,
            submittedAt = System.currentTimeMillis(),
            isSkipped = false
        )
        
        val updatedResponses = state.responses + response
        _uiState.update { it.copy(responses = updatedResponses) }
        
        // Move to next word or finish
        moveToNextWord()
    }
    
    fun skipWord() {
        val state = _uiState.value
        val currentWord = state.currentWord ?: return
        
        // Save skipped response
        val response = WATWordResponse(
            wordId = currentWord.id,
            word = currentWord.word,
            response = "",
            timeTakenSeconds = 0,
            submittedAt = System.currentTimeMillis(),
            isSkipped = true
        )
        
        val updatedResponses = state.responses + response
        _uiState.update { it.copy(responses = updatedResponses) }
        
        // Move to next word or finish
        moveToNextWord()
    }
    
    private fun moveToNextWord() {
        val state = _uiState.value
        
        // Clear current response
        _uiState.update { it.copy(currentResponse = "") }
        
        // Check if more words
        if (state.currentWordIndex < state.words.size - 1) {
            _uiState.update { it.copy(
                currentWordIndex = state.currentWordIndex + 1
            ) }
            startWordTimer()
        } else {
            // Test complete
            completeTest()
        }
    }
    
    private fun completeTest() {
        stopTimer()
        _uiState.update { it.copy(phase = WATPhase.COMPLETED) }
        submitTest()
    }
    
    private fun submitTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get current user
                val currentUserId: String = observeCurrentUser().first()?.id ?: run {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Please login to submit test"
                    ) }
                    return@launch
                }
                
                val state = _uiState.value
                
                // Create submission
                val totalTimeMinutes = ((System.currentTimeMillis() - state.startTime) / 60000).toInt()
                val submission = WATSubmission(
                    userId = currentUserId,
                    testId = state.testId,
                    responses = state.responses,
                    totalTimeTakenMinutes = totalTimeMinutes,
                    submittedAt = System.currentTimeMillis(),
                    aiPreliminaryScore = generateMockAIScore(state.responses)
                )
                
                // Submit to Firestore
                val result = submitWATTest(submission, batchId = null)
                
                result.onSuccess { submissionId ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        isSubmitted = true,
                        submissionId = submissionId,
                        phase = WATPhase.SUBMITTED
                    ) }
                }.onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Failed to submit: ${error.message}"
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }
    
    private fun startWordTimer() {
        stopTimer()
        
        val timePerWord = _uiState.value.config?.timePerWordSeconds ?: 15
        _uiState.update { it.copy(timeRemaining = timePerWord) }
        
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(
                    timeRemaining = it.timeRemaining - 1
                ) }
            }
            
            // Time's up - auto-skip
            skipWord()
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
    
    private fun generateMockWords(): List<WATWord> {
        val commonWords = listOf(
            "Friend", "Success", "Failure", "Army", "Courage", "Fear", "Leader", "Team",
            "Challenge", "Victory", "Defeat", "Mother", "Father", "Love", "Hate", "War",
            "Peace", "Fight", "Help", "Sacrifice", "Duty", "Honor", "Brave", "Weak",
            "Strong", "Win", "Lose", "Run", "Stand", "Fall", "Rise", "Dark", "Light",
            "Night", "Day", "Fast", "Slow", "Good", "Bad", "Right", "Wrong", "Truth",
            "Lie", "Future", "Past", "Present", "Hope", "Dream", "Goal", "Path", "Choice",
            "Difficult", "Easy", "Hard", "Soft", "Hot", "Cold", "High", "Low", "Up", "Down"
        )
        
        return commonWords.shuffled().take(60).mapIndexed { index, word ->
            WATWord(
                id = "wat_w_${index + 1}",
                word = word,
                sequenceNumber = index + 1,
                timeAllowedSeconds = 15
            )
        }
    }
    
    private fun generateMockAIScore(responses: List<WATWordResponse>): WATAIScore {
        val validResponses = responses.filter { it.isValidResponse }
        
        // Simple sentiment analysis (mock)
        val positiveWords = listOf("success", "win", "good", "happy", "love", "help", "friend")
        val negativeWords = listOf("fail", "lose", "bad", "sad", "hate", "hurt", "enemy")
        
        val positiveCount = validResponses.count { response ->
            positiveWords.any { response.response.contains(it, ignoreCase = true) }
        }
        val negativeCount = validResponses.count { response ->
            negativeWords.any { response.response.contains(it, ignoreCase = true) }
        }
        val neutralCount = validResponses.size - positiveCount - negativeCount
        
        val uniqueResponses = validResponses.map { it.response.lowercase() }.toSet().size
        
        return WATAIScore(
            overallScore = 75f,
            positivityScore = 16f,
            creativityScore = 15f,
            speedScore = 15f,
            relevanceScore = 14f,
            emotionalMaturityScore = 15f,
            feedback = "Good spontaneity and positive associations. Shows emotional maturity.",
            positiveWords = positiveCount,
            negativeWords = negativeCount,
            neutralWords = neutralCount,
            uniqueResponsesCount = uniqueResponses,
            repeatedPatterns = listOf("Leadership themes", "Action-oriented"),
            strengths = listOf(
                "Positive outlook",
                "Quick responses",
                "Unique associations"
            ),
            areasForImprovement = listOf(
                "Be more creative with associations",
                "Avoid negative responses when possible"
            )
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

/**
 * UI State for WAT Test
 */
data class WATTestUiState(
    val isLoading: Boolean = true,
    val testId: String = "",
    val words: List<WATWord> = emptyList(),
    val config: WATTestConfig? = null,
    val currentWordIndex: Int = 0,
    val responses: List<WATWordResponse> = emptyList(),
    val currentResponse: String = "",
    val phase: WATPhase = WATPhase.INSTRUCTIONS,
    val timeRemaining: Int = 15,
    val startTime: Long = System.currentTimeMillis(),
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val error: String? = null
) {
    val currentWord: WATWord?
        get() = words.getOrNull(currentWordIndex)
    
    val completedWords: Int
        get() = responses.size
    
    val progress: Float
        get() = if (words.isEmpty()) 0f else (completedWords.toFloat() / words.size)
}

