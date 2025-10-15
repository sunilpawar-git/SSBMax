package com.ssbmax.ui.tests.oir

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
 * ViewModel for OIR Test Screen
 */
@HiltViewModel
class OIRTestViewModel @Inject constructor(
    // TODO: Inject OIRTestRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OIRTestUiState())
    val uiState: StateFlow<OIRTestUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    private var currentSession: OIRTestSession? = null
    
    init {
        loadTest()
    }
    
    fun loadTest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // TODO: Load from repository
                // For now, generate mock questions
                val questions = generateMockQuestions()
                val config = OIRTestConfig()
                
                currentSession = OIRTestSession(
                    sessionId = UUID.randomUUID().toString(),
                    userId = "mock-user-id",
                    testId = config.testId,
                    questions = questions,
                    answers = emptyMap(),
                    currentQuestionIndex = 0,
                    startTime = System.currentTimeMillis(),
                    timeRemainingSeconds = config.totalTimeMinutes * 60
                )
                
                updateUiFromSession()
                startTimer()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load test"
                )
            }
        }
    }
    
    fun selectOption(optionId: String) {
        val session = currentSession ?: return
        val question = session.currentQuestion ?: return
        
        val answer = OIRAnswer(
            questionId = question.id,
            selectedOptionId = optionId,
            isCorrect = optionId == question.correctAnswerId,
            timeTakenSeconds = 60 - _uiState.value.timeRemainingSeconds, // Time spent on this question
            skipped = false
        )
        
        // Update session with answer
        currentSession = session.copy(
            answers = session.answers + (question.id to answer)
        )
        
        // Show immediate feedback for OIR tests
        _uiState.value = _uiState.value.copy(
            selectedOptionId = optionId,
            showFeedback = true,
            isCurrentAnswerCorrect = answer.isCorrect,
            currentQuestionAnswered = true
        )
    }
    
    fun nextQuestion() {
        val session = currentSession ?: return
        
        if (session.currentQuestionIndex < session.questions.size - 1) {
            currentSession = session.copy(
                currentQuestionIndex = session.currentQuestionIndex + 1
            )
            
            updateUiFromSession()
        }
    }
    
    fun previousQuestion() {
        val session = currentSession ?: return
        
        if (session.currentQuestionIndex > 0) {
            currentSession = session.copy(
                currentQuestionIndex = session.currentQuestionIndex - 1
            )
            
            updateUiFromSession()
        }
    }
    
    fun submitTest() {
        timerJob?.cancel()
        
        val session = currentSession ?: return
        
        viewModelScope.launch {
            // Calculate results
            val result = calculateResults(session)
            
            // TODO: Save to repository
            
            // Mark test as completed
            currentSession = session.copy(isCompleted = true)
            _uiState.value = _uiState.value.copy(
                isCompleted = true,
                sessionId = session.sessionId
            )
        }
    }
    
    fun pauseTest() {
        timerJob?.cancel()
        
        val session = currentSession ?: return
        currentSession = session.copy(isPaused = true)
        
        // TODO: Save session state to repository
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemainingSeconds > 0 && !_uiState.value.isCompleted) {
                delay(1000)
                val newTime = _uiState.value.timeRemainingSeconds - 1
                _uiState.value = _uiState.value.copy(timeRemainingSeconds = newTime)
                
                currentSession = currentSession?.copy(timeRemainingSeconds = newTime)
                
                if (newTime == 0) {
                    submitTest() // Auto-submit when time runs out
                }
            }
        }
    }
    
    private fun updateUiFromSession() {
        val session = currentSession ?: return
        val currentQuestion = session.currentQuestion
        val existingAnswer = currentQuestion?.let { session.answers[it.id] }
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            currentQuestion = currentQuestion,
            currentQuestionIndex = session.currentQuestionIndex,
            totalQuestions = session.questions.size,
            timeRemainingSeconds = session.timeRemainingSeconds,
            selectedOptionId = existingAnswer?.selectedOptionId,
            showFeedback = existingAnswer != null,
            isCurrentAnswerCorrect = existingAnswer?.isCorrect ?: false,
            currentQuestionAnswered = existingAnswer != null
        )
    }
    
    private fun calculateResults(session: OIRTestSession): OIRTestResult {
        val correctAnswers = session.answers.values.count { it.isCorrect }
        val incorrectAnswers = session.answers.values.count { !it.isCorrect && !it.skipped }
        val skippedQuestions = session.questions.size - session.answers.size
        
        val rawScore = session.answers.values.filter { it.isCorrect }.sumOf { answer ->
            val question = session.questions.find { it.id == answer.questionId }
            question?.difficulty?.points ?: 1
        }
        
        val maxScore = session.questions.sumOf { it.difficulty.points }
        val percentageScore = if (maxScore > 0) (rawScore.toFloat() / maxScore) * 100 else 0f
        
        // Calculate category scores
        val categoryScores = OIRQuestionType.values().associateWith { type ->
            val categoryQuestions = session.questions.filter { it.type == type }
            val categoryAnswers = categoryQuestions.mapNotNull { q -> session.answers[q.id] }
            val correct = categoryAnswers.count { it.isCorrect }
            val avgTime = if (categoryAnswers.isNotEmpty()) {
                categoryAnswers.map { it.timeTakenSeconds }.average().toInt()
            } else 0
            
            CategoryScore(
                category = type,
                totalQuestions = categoryQuestions.size,
                correctAnswers = correct,
                percentage = if (categoryQuestions.isNotEmpty()) {
                    (correct.toFloat() / categoryQuestions.size) * 100
                } else 0f,
                averageTimeSeconds = avgTime
            )
        }
        
        // Calculate difficulty breakdown
        val difficultyScores = QuestionDifficulty.values().associateWith { diff ->
            val diffQuestions = session.questions.filter { it.difficulty == diff }
            val diffAnswers = diffQuestions.mapNotNull { q -> session.answers[q.id] }
            val correct = diffAnswers.count { it.isCorrect }
            
            DifficultyScore(
                difficulty = diff,
                totalQuestions = diffQuestions.size,
                correctAnswers = correct,
                percentage = if (diffQuestions.isNotEmpty()) {
                    (correct.toFloat() / diffQuestions.size) * 100
                } else 0f
            )
        }
        
        // Create answered questions list
        val answeredQuestions = session.questions.mapNotNull { question ->
            val answer = session.answers[question.id] ?: return@mapNotNull null
            val correctOption = question.options.find { it.id == question.correctAnswerId }!!
            val selectedOption = answer.selectedOptionId?.let { id ->
                question.options.find { it.id == id }
            }
            
            OIRAnsweredQuestion(
                question = question,
                userAnswer = answer,
                isCorrect = answer.isCorrect,
                correctOption = correctOption,
                selectedOption = selectedOption
            )
        }
        
        val timeTaken = ((System.currentTimeMillis() - session.startTime) / 1000).toInt()
        
        return OIRTestResult(
            testId = session.testId,
            sessionId = session.sessionId,
            userId = session.userId,
            totalQuestions = session.questions.size,
            correctAnswers = correctAnswers,
            incorrectAnswers = incorrectAnswers,
            skippedQuestions = skippedQuestions,
            totalTimeSeconds = OIRTestConfig().totalTimeMinutes * 60,
            timeTakenSeconds = timeTaken,
            rawScore = rawScore,
            percentageScore = percentageScore,
            categoryScores = categoryScores,
            difficultyBreakdown = difficultyScores,
            answeredQuestions = answeredQuestions,
            completedAt = System.currentTimeMillis()
        )
    }
    
    private fun generateMockQuestions(): List<OIRQuestion> {
        // TODO: Load from repository
        // Generate mock questions for testing
        return listOf(
            OIRQuestion(
                id = "q1",
                questionNumber = 1,
                type = OIRQuestionType.VERBAL_REASONING,
                questionText = "If all roses are flowers and some flowers fade quickly, which of the following must be true?",
                options = listOf(
                    OIROption("a", "All roses fade quickly"),
                    OIROption("b", "Some roses may fade quickly"),
                    OIROption("c", "No roses fade quickly"),
                    OIROption("d", "All flowers are roses")
                ),
                correctAnswerId = "b",
                explanation = "Since some flowers fade quickly and all roses are flowers, it's possible that some roses may fade quickly, but we can't say for certain that all roses do.",
                difficulty = QuestionDifficulty.MEDIUM
            ),
            OIRQuestion(
                id = "q2",
                questionNumber = 2,
                type = OIRQuestionType.NUMERICAL_ABILITY,
                questionText = "What is the next number in the sequence: 2, 6, 12, 20, 30, ?",
                options = listOf(
                    OIROption("a", "40"),
                    OIROption("b", "42"),
                    OIROption("c", "44"),
                    OIROption("d", "46")
                ),
                correctAnswerId = "b",
                explanation = "The differences between consecutive numbers are 4, 6, 8, 10, so the next difference should be 12. Therefore, 30 + 12 = 42.",
                difficulty = QuestionDifficulty.EASY
            )
            // TODO: Add 48 more questions
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

/**
 * UI State for OIR Test Screen
 */
data class OIRTestUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentQuestion: OIRQuestion? = null,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val timeRemainingSeconds: Int = 0,
    val selectedOptionId: String? = null,
    val showFeedback: Boolean = false,
    val isCurrentAnswerCorrect: Boolean = false,
    val currentQuestionAnswered: Boolean = false,
    val isCompleted: Boolean = false,
    val sessionId: String? = null
)

