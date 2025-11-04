package com.ssbmax.ui.tests.oir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.validation.OIRQuestionValidator
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for OIR Test Screen
 * Loads test questions from cloud via TestContentRepository
 */
@HiltViewModel
class OIRTestViewModel @Inject constructor(
    private val testContentRepository: TestContentRepository,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    private val difficultyManager: com.ssbmax.core.data.repository.DifficultyProgressionManager,
    private val subscriptionManager: com.ssbmax.core.data.repository.SubscriptionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OIRTestUiState())
    val uiState: StateFlow<OIRTestUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    private var currentSession: OIRTestSession? = null
    
    init {
        // Register for memory leak tracking
        trackMemoryLeaks("OIRTestViewModel")
        android.util.Log.d("OIRTestViewModel", "🚀 ViewModel initialized with leak tracking")
        
        loadTest()
        
        // Restore timer if test was in progress (configuration change recovery)
        restoreTimerIfNeeded()
    }
    
    /**
     * Restore timer after configuration change (e.g., screen rotation)
     * If test was in progress, restart the timer
     */
    private fun restoreTimerIfNeeded() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // Only restore if we're in active test with time remaining
            if (!state.isLoading && 
                !state.isCompleted && 
                state.timeRemainingSeconds > 0 && 
                state.totalQuestions > 0 && 
                timerJob == null) {
                android.util.Log.d("OIRTestViewModel", "🔄 Restoring timer after configuration change")
                startTimer()
            }
        }
    }
    
    /**
     * Check if user is eligible to take the test based on subscription tier
     */
    private suspend fun checkTestEligibility(userId: String): com.ssbmax.core.data.repository.TestEligibility {
        return subscriptionManager.canTakeTest(TestType.OIR, userId)
    }
    
    fun loadTest(testId: String = "oir_standard", userId: String = "mock-user-id") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Checking eligibility...",
                error = null
            )
            
            try {
                // Get actual user ID
                val userProfile = userProfileRepository.getUserProfile(userId).first().getOrNull()
                val actualUserId = userProfile?.userId ?: userId
                
                // Check subscription eligibility
                val eligibility = checkTestEligibility(actualUserId)
                
                when (eligibility) {
                    is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
                        // Show limit reached state
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = null,
                            isLimitReached = true,
                            subscriptionTier = eligibility.tier,
                            testsLimit = eligibility.limit,
                            testsUsed = eligibility.usedCount,
                            resetsAt = eligibility.resetsAt
                        )
                        android.util.Log.d("OIRTestViewModel", "❌ Test limit reached: ${eligibility.usedCount}/${eligibility.limit}")
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                        android.util.Log.d("OIRTestViewModel", "✅ Test eligible: ${eligibility.remainingTests} remaining")
                        // Continue with test loading
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("OIRTestViewModel", "Error checking eligibility", e)
                // Continue anyway in case of error
            }
            
            _uiState.value = _uiState.value.copy(
                loadingMessage = "Preparing test..."
            )
            
            try {
                // TODO: Check subscription eligibility when feature is implemented
                
                _uiState.value = _uiState.value.copy(
                    loadingMessage = "Analyzing your level..."
                )
                
                // Get recommended difficulty based on past performance
                val difficulty = difficultyManager.getRecommendedDifficulty("OIR")
                android.util.Log.d("OIRTestViewModel", "📊 Recommended difficulty: $difficulty")
                
                _uiState.value = _uiState.value.copy(
                    loadingMessage = "Loading $difficulty questions...",
                    currentDifficulty = difficulty
                )
                
                // Fetch questions using the new caching system with difficulty
                val questionsResult = testContentRepository.getOIRTestQuestions(count = 50, difficulty = difficulty)
                
                if (questionsResult.isFailure) {
                    throw questionsResult.exceptionOrNull() ?: Exception("Failed to load test questions")
                }
                
                val questions = questionsResult.getOrNull() ?: emptyList()
                
                if (questions.isEmpty()) {
                    throw Exception("No questions available. Please check your internet connection.")
                }
                
                android.util.Log.d("OIRTestViewModel", "✅ Loaded ${questions.size} questions")
                
                // 🔍 VALIDATE ALL QUESTIONS - Catch data corruption early!
                android.util.Log.d("OIRTestViewModel", "🔍 Validating ${questions.size} questions...")
                val validatedQuestions = OIRQuestionValidator.validateAndFilter(questions) { invalidResult ->
                    android.util.Log.e("OIRTestViewModel", "❌ INVALID QUESTION DETECTED:\n${invalidResult.toLogString()}")
                }
                
                if (validatedQuestions.size < questions.size) {
                    val removedCount = questions.size - validatedQuestions.size
                    android.util.Log.w("OIRTestViewModel", "⚠️  Removed $removedCount invalid questions from test")
                }
                
                if (validatedQuestions.isEmpty()) {
                    throw Exception("All questions failed validation. Please contact support.")
                }
                
                android.util.Log.d("OIRTestViewModel", "✅ Validation complete: ${validatedQuestions.size} valid questions")
                
                // Create test session
                val sessionId = UUID.randomUUID().toString()
                val config = OIRTestConfig()
                
                currentSession = OIRTestSession(
                    sessionId = sessionId,
                    userId = userId,
                    testId = testId,
                    questions = validatedQuestions, // Use validated questions only!
                    answers = emptyMap(),
                    currentQuestionIndex = 0,
                    startTime = System.currentTimeMillis(),
                    timeRemainingSeconds = config.totalTimeMinutes * 60
                )
                
                updateUiFromSession()
                startTimer()
                
            } catch (e: CancellationException) {
                throw e // Don't catch cancellation
            } catch (e: Exception) {
                android.util.Log.e("OIRTestViewModel", "Failed to load test", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    error = "Failed to load test: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }
    
    fun selectOption(optionId: String) {
        android.util.Log.d("OIRTestViewModel", "🟢 selectOption() called: optionId=$optionId")
        
        val session = currentSession ?: run {
            android.util.Log.e("OIRTestViewModel", "❌ currentSession is null in selectOption!")
            return
        }
        
        val question = session.currentQuestion ?: run {
            android.util.Log.e("OIRTestViewModel", "❌ currentQuestion is null in selectOption!")
            return
        }
        
        android.util.Log.d("OIRTestViewModel", "   Question: ${question.id}")
        android.util.Log.d("OIRTestViewModel", "   Current index: ${session.currentQuestionIndex}/${session.questions.size}")
        android.util.Log.d("OIRTestViewModel", "   Total answers before: ${session.answers.size}")
        
        // 🔍 RUNTIME VALIDATION - Double-check question integrity before scoring
        val validationResult = OIRQuestionValidator.validate(question)
        if (!validationResult.isValid) {
            android.util.Log.e("OIRTestViewModel", "❌ FATAL: Invalid question detected during answer selection!\n${validationResult.toLogString()}")
            // Still allow the answer, but log the critical error
        }
        
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
        
        android.util.Log.d("OIRTestViewModel", "   Total answers after: ${currentSession?.answers?.size}")
        android.util.Log.d("OIRTestViewModel", "   Answer correct? ${answer.isCorrect}")
        
        // Show immediate feedback for OIR tests
        _uiState.value = _uiState.value.copy(
            selectedOptionId = optionId,
            showFeedback = true,
            isCurrentAnswerCorrect = answer.isCorrect,
            currentQuestionAnswered = true
        )
        
        android.util.Log.d("OIRTestViewModel", "✅ selectOption() complete")
    }
    
    fun nextQuestion() {
        val session = currentSession ?: return
        
        // Only navigate if not on the last question
        // currentQuestionIndex is 0-based, so last question is at index (size - 1)
        if (session.currentQuestionIndex < session.questions.size - 1) {
            currentSession = session.copy(
                currentQuestionIndex = session.currentQuestionIndex + 1
            )
            
            updateUiFromSession()
        } else {
            // Already on last question, do nothing
            // User should click "Submit Test" button instead
            android.util.Log.d("OIRTestViewModel", "Already on last question (${session.currentQuestionIndex + 1}/${session.questions.size}), use Submit button")
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
        android.util.Log.d("OIRTestViewModel", "🔵 submitTest() called")
        timerJob?.cancel()
        
        val session = currentSession ?: run {
            android.util.Log.e("OIRTestViewModel", "❌ currentSession is null in submitTest!")
            return
        }
        
        android.util.Log.d("OIRTestViewModel", "   Session ID: ${session.sessionId}")
        android.util.Log.d("OIRTestViewModel", "   Questions: ${session.questions.size}")
        android.util.Log.d("OIRTestViewModel", "   Answers: ${session.answers.size}")
        android.util.Log.d("OIRTestViewModel", "   Current index: ${session.currentQuestionIndex}")
        
        viewModelScope.launch {
            try {
                android.util.Log.d("OIRTestViewModel", "   Starting test submission...")
                // Get user profile for subscription type
                val userProfileResult = userProfileRepository.getUserProfile(session.userId).first()
                val userProfile = userProfileResult.getOrNull()
                val subscriptionType = userProfile?.subscriptionType ?: com.ssbmax.core.domain.model.SubscriptionType.FREE
                
                // Calculate results
                val result = calculateResults(session)
                
                // Record performance for adaptive difficulty
                val difficulty = _uiState.value.currentDifficulty
                val timeSpent = (30 * 60) - _uiState.value.timeRemainingSeconds // seconds
                difficultyManager.recordPerformance(
                    testType = "OIR",
                    difficulty = difficulty,
                    score = result.percentageScore,
                    correctAnswers = result.correctAnswers,
                    totalQuestions = result.totalQuestions,
                    timeSeconds = timeSpent.toFloat()
                )
                android.util.Log.d("OIRTestViewModel", "📊 Recorded performance: ${result.percentageScore}% ($difficulty)")
                
                // Record test usage for subscription tracking
                subscriptionManager.recordTestUsage(TestType.OIR, session.userId)
                android.util.Log.d("OIRTestViewModel", "📝 Recorded test usage for subscription tracking")
                
                // End test session
                testContentRepository.endTestSession(session.sessionId)
                
                // Clear cached content
                testContentRepository.clearCache()
                
                // TODO: Save results to repository (OIR submission model not yet implemented)
                // For now, we pass the sessionId which will be used to show results directly
                // instead of trying to fetch from Firestore (which would fail with PERMISSION_DENIED)
                
                // Mark test as completed
                currentSession = session.copy(isCompleted = true)
                _uiState.value = _uiState.value.copy(
                    isCompleted = true,
                    sessionId = session.sessionId,
                    subscriptionType = subscriptionType,
                    testResult = result
                )
            } catch (e: Exception) {
                android.util.Log.e("OIRTestViewModel", "❌ Test submission failed", e)
                android.util.Log.e("OIRTestViewModel", "   Error type: ${e.javaClass.simpleName}")
                android.util.Log.e("OIRTestViewModel", "   Error message: ${e.message}")
                e.printStackTrace()
                
                _uiState.value = _uiState.value.copy(
                    error = "Failed to submit: ${e.message}"
                )
            }
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
            android.util.Log.d("OIRTestViewModel", "⏰ Starting test timer")
            
            try {
                while (isActive && _uiState.value.timeRemainingSeconds > 0 && !_uiState.value.isCompleted) {
                    delay(1000)
                    if (!isActive) break // Double-check after delay
                    
                    val newTime = _uiState.value.timeRemainingSeconds - 1
                    _uiState.value = _uiState.value.copy(timeRemainingSeconds = newTime)
                    
                    currentSession = currentSession?.copy(timeRemainingSeconds = newTime)
                    
                    if (newTime == 0 && isActive) {
                        submitTest() // Auto-submit when time runs out
                    }
                }
            } catch (e: CancellationException) {
                android.util.Log.d("OIRTestViewModel", "⏰ Timer cancelled")
                throw e // Re-throw to properly cancel coroutine
            }
        }.also { job ->
            // Register AFTER job is created
            job.trackMemoryLeaks("OIRTestViewModel", "test-timer")
        }
    }
    
    private fun updateUiFromSession() {
        val session = currentSession ?: return
        
        // Detailed debug logging
        android.util.Log.d("OIRTestViewModel", "📍 updateUiFromSession: index=${session.currentQuestionIndex}, total=${session.questions.size}")
        android.util.Log.d("OIRTestViewModel", "   Questions list size: ${session.questions.size}")
        android.util.Log.d("OIRTestViewModel", "   Trying to get question at index: ${session.currentQuestionIndex}")
        
        val currentQuestion = session.currentQuestion
        
        android.util.Log.d("OIRTestViewModel", "   currentQuestion null? ${currentQuestion == null}")
        
        // Safety check: if currentQuestion is null, we've gone past the last question
        if (currentQuestion == null) {
            android.util.Log.e("OIRTestViewModel", "⚠️ currentQuestion is null at index ${session.currentQuestionIndex}/${session.questions.size}")
            android.util.Log.e("OIRTestViewModel", "   This should NOT happen! Index should be 0..${session.questions.size - 1}")
            
            // Log all question IDs to debug
            session.questions.forEachIndexed { index, q ->
                android.util.Log.d("OIRTestViewModel", "   Question[$index]: id=${q.id}")
            }
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                loadingMessage = null,
                error = "Invalid question index (${session.currentQuestionIndex}/${session.questions.size}). Please click Submit Test button."
            )
            return
        }
        
        android.util.Log.d("OIRTestViewModel", "   ✅ Got question: ${currentQuestion.id}")
        
        val existingAnswer = session.answers[currentQuestion.id]
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            loadingMessage = null,
            error = null, // Clear any previous errors
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
        android.util.Log.d("OIRTestViewModel", "📋 Creating answered questions list from ${session.questions.size} questions")
        
        val answeredQuestions = session.questions.mapNotNull { question ->
            val answer = session.answers[question.id] ?: return@mapNotNull null
            
            android.util.Log.d("OIRTestViewModel", "   Processing question: ${question.id}")
            android.util.Log.d("OIRTestViewModel", "     correctAnswerId: ${question.correctAnswerId}")
            android.util.Log.d("OIRTestViewModel", "     Options: ${question.options.map { it.id }}")
            
            val correctOption = question.options.find { it.id == question.correctAnswerId }
            
            if (correctOption == null) {
                android.util.Log.e("OIRTestViewModel", "❌ FATAL: Question ${question.id} has correctAnswerId='${question.correctAnswerId}' but no matching option!")
                android.util.Log.e("OIRTestViewModel", "   Available options: ${question.options.joinToString { "${it.id}: ${it.text.take(50)}" }}")
                return@mapNotNull null // Skip this question instead of crashing
            }
            
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
        
        android.util.Log.d("OIRTestViewModel", "✅ Created ${answeredQuestions.size} answered questions")
        
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
    
    override fun onCleared() {
        super.onCleared()
        
        // Critical: Stop all timers to prevent leaks
        android.util.Log.d("OIRTestViewModel", "🧹 ViewModel onCleared() - stopping timers")
        timerJob?.cancel()
        timerJob = null
        
        // Unregister from memory leak tracker
        MemoryLeakTracker.unregisterViewModel("OIRTestViewModel")
        
        // Force GC to help profiler detect cleanup
        MemoryLeakTracker.forceGcAndLog("OIRTestViewModel-Cleared")
        
        android.util.Log.d("OIRTestViewModel", "✅ OIRTestViewModel cleanup complete")
    }
}

/**
 * UI State for OIR Test Screen
 */
data class OIRTestUiState(
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
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
    val sessionId: String? = null,
    val subscriptionType: com.ssbmax.core.domain.model.SubscriptionType? = null,
    val testResult: OIRTestResult? = null,  // Result calculated locally, no Firestore needed
    val currentDifficulty: String = "EASY",  // Current difficulty level for adaptive progression
    // Subscription limit fields
    val isLimitReached: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val testsLimit: Int = 1,
    val testsUsed: Int = 0,
    val resetsAt: String = ""
)

