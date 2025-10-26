package com.ssbmax.ui.tests.tat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.usecase.submission.SubmitTATTestUseCase
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
 * ViewModel for TAT Test Screen
 * Loads test questions from cloud via TestContentRepository
 */
@HiltViewModel
class TATTestViewModel @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val submitTATTest: SubmitTATTestUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TATTestUiState())
    val uiState: StateFlow<TATTestUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    
    fun loadTest(testId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                loadingMessage = "Fetching questions from cloud..."
            ) }
            
            try {
                val user = observeCurrentUser().first()
                val userId = user?.id ?: "mock-user-id"
                
                // Create test session
                val sessionResult = testContentRepository.createTestSession(
                    userId = userId,
                    testId = testId,
                    testType = TestType.TAT
                )
                
                if (sessionResult.isFailure) {
                    throw sessionResult.exceptionOrNull() ?: Exception("Failed to create test session")
                }
                
                // Fetch questions from cloud
                val questionsResult = testContentRepository.getTATQuestions(testId)
                
                if (questionsResult.isFailure) {
                    throw questionsResult.exceptionOrNull() ?: Exception("Failed to load test questions")
                }
                
                val questions = questionsResult.getOrNull() ?: emptyList()
                
                if (questions.isEmpty()) {
                    throw Exception("No questions found for this test")
                }
                
                val config = TATTestConfig()
                
                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    testId = testId,
                    questions = questions,
                    config = config,
                    phase = TATPhase.INSTRUCTIONS
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    error = "Cloud connection required. Please check your internet connection."
                ) }
            }
        }
    }
    
    fun startTest() {
        _uiState.update { it.copy(
            phase = TATPhase.IMAGE_VIEWING,
            currentQuestionIndex = 0
        ) }
        startViewingTimer()
    }
    
    fun updateStory(story: String) {
        _uiState.update { it.copy(currentStory = story) }
    }
    
    fun moveToNextQuestion() {
        val state = _uiState.value
        
        // Save current story
        val currentQuestion = state.currentQuestion
        if (state.currentStory.isNotBlank() && currentQuestion != null) {
            val response = TATStoryResponse(
                questionId = currentQuestion.id,
                story = state.currentStory,
                charactersCount = state.currentStory.length,
                viewingTimeTakenSeconds = 30 - state.viewingTimeRemaining,
                writingTimeTakenSeconds = (4 * 60) - state.writingTimeRemaining,
                submittedAt = System.currentTimeMillis()
            )
            
            val updatedResponses = state.responses.toMutableList().apply {
                removeAll { it.questionId == response.questionId }
                add(response)
            }
            
            _uiState.update { it.copy(responses = updatedResponses) }
        }
        
        // Move to next or finish
        if (state.currentQuestionIndex < state.questions.size - 1) {
            _uiState.update { it.copy(
                currentQuestionIndex = state.currentQuestionIndex + 1,
                currentStory = "",
                phase = TATPhase.IMAGE_VIEWING
            ) }
            startViewingTimer()
        } else {
            // All pictures shown, allow review or submit
            stopTimer()
        }
    }
    
    fun moveToPreviousQuestion() {
        val state = _uiState.value
        
        if (state.currentQuestionIndex > 0) {
            stopTimer()
            
            // Load previous story if exists
            val previousQuestionId = state.questions[state.currentQuestionIndex - 1].id
            val previousStory = state.responses.find { it.questionId == previousQuestionId }?.story ?: ""
            
            _uiState.update { it.copy(
                currentQuestionIndex = state.currentQuestionIndex - 1,
                currentStory = previousStory,
                phase = TATPhase.WRITING
            ) }
            startWritingTimer()
        }
    }
    
    fun editCurrentStory() {
        _uiState.update { it.copy(phase = TATPhase.WRITING) }
        startWritingTimer()
    }
    
    fun confirmCurrentStory() {
        moveToNextQuestion()
    }
    
    fun submitTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                stopTimer()
                
                // Get current user
                val currentUserId: String = observeCurrentUser().first()?.id ?: run {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Please login to submit test"
                    ) }
                    return@launch
                }
                
                // Get user profile for subscription type
                val userProfileResult = userProfileRepository.getUserProfile(currentUserId).first()
                val userProfile = userProfileResult.getOrNull()
                val subscriptionType = userProfile?.subscriptionType ?: SubscriptionType.FREE
                
                // Create submission
                val state = _uiState.value
                val submission = TATSubmission(
                    userId = currentUserId,
                    testId = state.testId,
                    stories = state.responses,
                    totalTimeTakenMinutes = ((System.currentTimeMillis() - state.startTime) / 60000).toInt(),
                    submittedAt = System.currentTimeMillis(),
                    aiPreliminaryScore = generateMockAIScore(state.responses)
                )
                
                // Submit to Firestore (but also store locally to bypass permission issues)
                val result = submitTATTest(submission, batchId = null)
                
                result.onSuccess { submissionId ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        isSubmitted = true,
                        submissionId = submissionId,
                        subscriptionType = subscriptionType,
                        submission = submission,  // Store locally to show results directly
                        phase = TATPhase.SUBMITTED
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
    
    private fun startViewingTimer() {
        stopTimer()
        
        _uiState.update { it.copy(
            viewingTimeRemaining = it.config?.viewingTimePerPictureSeconds ?: 30
        ) }
        
        timerJob = viewModelScope.launch {
            while (_uiState.value.viewingTimeRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(
                    viewingTimeRemaining = it.viewingTimeRemaining - 1
                ) }
            }
            
            // Auto-transition to writing
            _uiState.update { it.copy(phase = TATPhase.WRITING) }
            startWritingTimer()
        }
    }
    
    private fun startWritingTimer() {
        stopTimer()
        
        _uiState.update { it.copy(
            writingTimeRemaining = (it.config?.writingTimePerPictureMinutes ?: 4) * 60
        ) }
        
        timerJob = viewModelScope.launch {
            while (_uiState.value.writingTimeRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(
                    writingTimeRemaining = it.writingTimeRemaining - 1
                ) }
            }
            
            // Time's up - move to review
            _uiState.update { it.copy(phase = TATPhase.REVIEW_CURRENT) }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
    
    private fun generateMockQuestions(): List<TATQuestion> {
        return (1..12).map { index ->
            TATQuestion(
                id = "tat_q_$index",
                imageUrl = "https://via.placeholder.com/800x600/3498db/ffffff?text=TAT+Picture+$index",
                sequenceNumber = index,
                prompt = "Write a story about what you see in the picture",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4,
                minCharacters = 150,
                maxCharacters = 800
            )
        }
    }
    
    private fun generateMockAIScore(stories: List<TATStoryResponse>): TATAIScore {
        // TODO: Actual AI scoring
        return TATAIScore(
            overallScore = 78f,
            thematicPerceptionScore = 16f,
            imaginationScore = 15f,
            characterDepictionScore = 16f,
            emotionalToneScore = 16f,
            narrativeStructureScore = 15f,
            feedback = "Good storytelling with positive themes. Shows leadership qualities and imagination.",
            storyWiseAnalysis = stories.mapIndexed { index, story ->
                StoryAnalysis(
                    questionId = story.questionId,
                    sequenceNumber = index + 1,
                    score = (70..85).random().toFloat(),
                    themes = listOf("Leadership", "Courage", "Teamwork").shuffled().take(2),
                    sentimentScore = kotlin.random.Random.nextFloat() * 0.4f + 0.5f, // 0.5 to 0.9
                    keyInsights = listOf("Shows initiative", "Positive resolution")
                )
            },
            strengths = listOf(
                "Creative storytelling",
                "Positive outlook",
                "Good character development"
            ),
            areasForImprovement = listOf(
                "Add more detail to situation descriptions",
                "Include emotional depth"
            )
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

/**
 * UI State for TAT Test
 */
data class TATTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val testId: String = "",
    val questions: List<TATQuestion> = emptyList(),
    val config: TATTestConfig? = null,
    val currentQuestionIndex: Int = 0,
    val responses: List<TATStoryResponse> = emptyList(),
    val currentStory: String = "",
    val phase: TATPhase = TATPhase.INSTRUCTIONS,
    val viewingTimeRemaining: Int = 30,
    val writingTimeRemaining: Int = 240, // 4 minutes
    val startTime: Long = System.currentTimeMillis(),
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val subscriptionType: SubscriptionType? = null,
    val submission: TATSubmission? = null,  // Submission stored locally to bypass Firestore permission issues
    val error: String? = null
) {
    val currentQuestion: TATQuestion?
        get() = questions.getOrNull(currentQuestionIndex)
    
    val completedStories: Int
        get() = responses.size
    
    val progress: Float
        get() = if (questions.isEmpty()) 0f else (completedStories.toFloat() / questions.size)
    
    val canMoveToNextQuestion: Boolean
        get() = when (phase) {
            TATPhase.WRITING -> currentStory.length >= (currentQuestion?.minCharacters ?: 150) &&
                                currentStory.length <= (currentQuestion?.maxCharacters ?: 800)
            TATPhase.REVIEW_CURRENT -> true
            else -> false
        }
    
    val canMoveToPreviousQuestion: Boolean
        get() = currentQuestionIndex > 0 && phase == TATPhase.WRITING
    
    val canSubmitTest: Boolean
        get() = completedStories >= 11 // Allow submission after 11 stories minimum
}

