package com.ssbmax.ui.tests.oir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.security.SecurityEventLogger
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestContentRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.core.domain.validation.OIRQuestionValidator
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    private val submissionRepository: com.ssbmax.core.domain.repository.SubmissionRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val userProfileRepository: com.ssbmax.core.domain.repository.UserProfileRepository,
    private val difficultyManager: com.ssbmax.core.data.repository.DifficultyProgressionManager,
    private val subscriptionManager: com.ssbmax.core.data.repository.SubscriptionManager,
    private val getOLQDashboard: com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase,
    private val securityLogger: SecurityEventLogger
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OIRTestUiState())
    val uiState: StateFlow<OIRTestUiState> = _uiState.asStateFlow()
    
    // PHASE 3: All state fully migrated to StateFlow (completed)
    // Timer managed via viewModelScope + isTimerActive flag (no Job reference needed)
    
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
                !state.isTimerActive) {
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
    
    fun loadTest(testId: String = "oir_standard") {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                loadingMessage = "Checking eligibility...",
                error = null
            ) }
            
            // Get current user - SECURITY: Require authentication
            val user = observeCurrentUser().first()
            val userId = user?.id ?: run {
                val exception = Exception("Unauthenticated OIR test access attempt")
                ErrorLogger.log(exception, "SECURITY: Unauthenticated test access attempt blocked")

                // SECURITY: Log unauthenticated access attempt to Firebase Analytics
                securityLogger.logUnauthenticatedAccess(
                    testType = TestType.OIR,
                    context = "OIRTestViewModel.loadTest"
                )
                
                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    error = "Authentication required. Please login to continue."
                ) }
                return@launch
            }
            
            android.util.Log.d("OIRTestViewModel", "✅ User authenticated: $userId")
            
            try {
                // Check subscription eligibility
                val eligibility = checkTestEligibility(userId)
                
                when (eligibility) {
                    is com.ssbmax.core.data.repository.TestEligibility.LimitReached -> {
                        // Show limit reached state
                        _uiState.update { it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = null,
                            isLimitReached = true,
                            subscriptionTier = eligibility.tier,
                            testsLimit = eligibility.limit,
                            testsUsed = eligibility.usedCount,
                            resetsAt = eligibility.resetsAt
                        ) }
                        android.util.Log.d("OIRTestViewModel", "❌ Test limit reached: ${eligibility.usedCount}/${eligibility.limit}")
                        return@launch
                    }
                    is com.ssbmax.core.data.repository.TestEligibility.Eligible -> {
                        android.util.Log.d("OIRTestViewModel", "✅ Test eligible: ${eligibility.remainingTests} remaining")
                        // Continue with test loading
                    }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception checking OIR test eligibility")
                // Continue anyway in case of error
            }
            
            _uiState.update { it.copy(
                loadingMessage = "Preparing test..."
            ) }
            
            try {
                // TODO: Check subscription eligibility when feature is implemented
                
                _uiState.update { it.copy(
                    loadingMessage = "Analyzing your level..."
                ) }
                
                // Get recommended difficulty based on past performance
                val difficulty = difficultyManager.getRecommendedDifficulty("OIR")
                android.util.Log.d("OIRTestViewModel", "📊 Recommended difficulty: $difficulty")
                
                _uiState.update { it.copy(
                    loadingMessage = "Loading $difficulty questions...",
                    currentDifficulty = difficulty
                ) }
                
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
                    val exception = Exception("Invalid OIR question detected: ${invalidResult.questionId}")
                    ErrorLogger.log(exception, "OIR question validation failed: ${invalidResult.toLogString()}")
                }
                
                if (validatedQuestions.size < questions.size) {
                    val removedCount = questions.size - validatedQuestions.size
                    android.util.Log.w("OIRTestViewModel", "⚠️  Removed $removedCount invalid questions from test")
                }
                
                if (validatedQuestions.isEmpty()) {
                    throw Exception("All questions failed validation. Please contact support.")
                }
                
                android.util.Log.d("OIRTestViewModel", "✅ Validation complete: ${validatedQuestions.size} valid questions")
                
                // PHASE 2: Create test session and store in StateFlow
                val sessionId = UUID.randomUUID().toString()
                val config = OIRTestConfig()
                
                val newSession = OIRTestSession(
                    sessionId = sessionId,
                    userId = userId,
                    testId = testId,
                    questions = validatedQuestions, // Use validated questions only!
                    answers = emptyMap(),
                    currentQuestionIndex = 0,
                    startTime = System.currentTimeMillis(),
                    timeRemainingSeconds = config.totalTimeMinutes * 60
                )
                
                _uiState.update { it.copy(session = newSession) }
                updateUiFromSession()
                startTimer()
                
            } catch (e: CancellationException) {
                throw e // Don't catch cancellation
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception loading OIR test")
                _uiState.update { it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    error = "Failed to load test: ${e.message ?: "Unknown error"}"
                ) }
            }
        }
    }
    
    fun selectOption(optionId: String) {
        android.util.Log.d("OIRTestViewModel", "🟢 selectOption() called: optionId=$optionId")
        
        val session = _uiState.value.session ?: run {
            val exception = Exception("Session is null in selectOption")
            ErrorLogger.log(exception, "OIR test session null during option selection")
            return
        }

        val question = session.currentQuestion ?: run {
            val exception = Exception("Current question is null in selectOption")
            ErrorLogger.log(exception, "OIR current question null during option selection")
            return
        }
        
        android.util.Log.d("OIRTestViewModel", "   Question: ${question.id}")
        android.util.Log.d("OIRTestViewModel", "   Current index: ${session.currentQuestionIndex}/${session.questions.size}")
        android.util.Log.d("OIRTestViewModel", "   Total answers before: ${session.answers.size}")
        
        // 🔍 RUNTIME VALIDATION - Double-check question integrity before scoring
        val validationResult = OIRQuestionValidator.validate(question)
        if (!validationResult.isValid) {
            val exception = Exception("Invalid question detected during runtime validation: ${question.id}")
            ErrorLogger.log(exception, "OIR question runtime validation failed: ${validationResult.toLogString()}")
            // Still allow the answer, but log the critical error
        }
        
        val answer = OIRAnswer(
            questionId = question.id,
            selectedOptionId = optionId,
            isCorrect = optionId == question.correctAnswerId,
            timeTakenSeconds = 60 - _uiState.value.timeRemainingSeconds, // Time spent on this question
            skipped = false
        )
        
        // Update session with answer using thread-safe .update {}
        _uiState.update { state ->
            state.copy(
                session = session.copy(
                    answers = session.answers + (question.id to answer)
                ),
                selectedOptionId = optionId,
                showFeedback = true,
                isCurrentAnswerCorrect = answer.isCorrect,
                currentQuestionAnswered = true
            )
        }
        
        android.util.Log.d("OIRTestViewModel", "   Total answers after: ${_uiState.value.session?.answers?.size}")
        android.util.Log.d("OIRTestViewModel", "   Answer correct? ${answer.isCorrect}")
        android.util.Log.d("OIRTestViewModel", "✅ selectOption() complete")
    }
    
    fun nextQuestion() {
        val session = _uiState.value.session ?: return
        
        // Only navigate if not on the last question
        // currentQuestionIndex is 0-based, so last question is at index (size - 1)
        if (session.currentQuestionIndex < session.questions.size - 1) {
            _uiState.update { it.copy(
                session = session.copy(
                    currentQuestionIndex = session.currentQuestionIndex + 1
                )
            ) }
            
            updateUiFromSession()
        } else {
            // Already on last question, do nothing
            // User should click "Submit Test" button instead
            android.util.Log.d("OIRTestViewModel", "Already on last question (${session.currentQuestionIndex + 1}/${session.questions.size}), use Submit button")
        }
    }
    
    fun previousQuestion() {
        val session = _uiState.value.session ?: return
        
        if (session.currentQuestionIndex > 0) {
            _uiState.update { it.copy(
                session = session.copy(
                    currentQuestionIndex = session.currentQuestionIndex - 1
                )
            ) }
            
            updateUiFromSession()
        }
    }
    
    fun submitTest() {
        android.util.Log.d("OIRTestViewModel", "🔵 submitTest() called")
        
        // PHASE 3: Signal timer to stop via isTimerActive flag
        _uiState.update { it.copy(isTimerActive = false) }
        
        val session = _uiState.value.session ?: run {
            val exception = Exception("Session is null in submitTest")
            ErrorLogger.log(exception, "OIR test session null during test submission")
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

                // Invalidate OLQ dashboard cache (user just completed a test)
                android.util.Log.d("OIRTestViewModel", "📍 Invalidating OLQ dashboard cache...")
                getOLQDashboard.invalidateCache(session.userId)
                android.util.Log.d("OIRTestViewModel", "✅ Dashboard cache invalidated!")

                // Create OIR submission
                // CRITICAL: Use session.sessionId as the document ID to match OIRSubmissionResultViewModel's expectations
                val submissionId = session.sessionId
                val submission = OIRSubmission(
                    id = submissionId,
                    userId = session.userId,
                    testId = session.testId,
                    testResult = result,
                    submittedAt = System.currentTimeMillis(),
                    status = com.ssbmax.core.domain.model.SubmissionStatus.SUBMITTED_PENDING_REVIEW,
                    gradedByInstructorId = null,
                    gradingTimestamp = null
                )
                
                // Submit to Firestore
                submissionRepository.submitOIR(submission, null).getOrThrow()
                android.util.Log.d("OIRTestViewModel", "✅ Submitted OIR to Firestore: $submissionId")
                
                // End test session
                testContentRepository.endTestSession(session.sessionId)
                
                // Clear cached content
                testContentRepository.clearCache()
                
                // Mark test as completed with submission ID using thread-safe .update {}
                _uiState.update { it.copy(
                    session = session.copy(isCompleted = true),
                    isCompleted = true,
                    sessionId = submissionId, // Use submissionId for consistency with other tests
                    subscriptionType = subscriptionType,
                    testResult = result
                ) }
            } catch (e: Exception) {
                // Use ErrorLogger instead of printStackTrace() for proper error tracking
                ErrorLogger.logTestError(
                    throwable = e,
                    description = "OIR test submission failed",
                    testType = "OIR",
                    userId = observeCurrentUser().first()?.id
                )

                _uiState.update { it.copy(
                    error = "Failed to submit: ${e.message}"
                ) }
            }
        }
    }
    
    fun pauseTest() {
        val session = _uiState.value.session ?: return
        
        // PHASE 3: Signal timer to stop via isTimerActive flag + update session
        _uiState.update { it.copy(
            isTimerActive = false,
            session = session.copy(isPaused = true)
        ) }
        
        // TODO: Save session state to repository
    }
    
    private fun startTimer() {
        // PHASE 3: Stop any existing timer by setting flag, then start new one
        _uiState.update { it.copy(
            isTimerActive = true,
            timerStartTime = System.currentTimeMillis()
        ) }
        
        viewModelScope.launch {
            android.util.Log.d("OIRTestViewModel", "⏰ Starting test timer")
            
            try {
                while (isActive && 
                       _uiState.value.isTimerActive && 
                       _uiState.value.timeRemainingSeconds > 0 && 
                       !_uiState.value.isCompleted) {
                    delay(1000)
                    
                    // Check if timer should still run (isTimerActive can be set false by submitTest/pauseTest)
                    if (!isActive || !_uiState.value.isTimerActive) break
                    
                    val newTime = _uiState.value.timeRemainingSeconds - 1
                    
                    // Update both timer and session's time remaining using thread-safe .update {}
                    _uiState.update { state ->
                        state.copy(
                            timeRemainingSeconds = newTime,
                            session = state.session?.copy(timeRemainingSeconds = newTime)
                        )
                    }
                    
                    if (newTime == 0 && isActive && _uiState.value.isTimerActive) {
                        submitTest() // Auto-submit when time runs out
                    }
                }
            } catch (e: CancellationException) {
                android.util.Log.d("OIRTestViewModel", "⏰ Timer cancelled")
                throw e // Re-throw to properly cancel coroutine
            } finally {
                // Ensure timer flag is cleared when loop exits
                _uiState.update { it.copy(isTimerActive = false) }
            }
        }.trackMemoryLeaks("OIRTestViewModel", "test-timer")
    }
    
    private fun updateUiFromSession() {
        val session = _uiState.value.session ?: return
        
        // Detailed debug logging
        android.util.Log.d("OIRTestViewModel", "📍 updateUiFromSession: index=${session.currentQuestionIndex}, total=${session.questions.size}")
        android.util.Log.d("OIRTestViewModel", "   Questions list size: ${session.questions.size}")
        android.util.Log.d("OIRTestViewModel", "   Trying to get question at index: ${session.currentQuestionIndex}")
        
        val currentQuestion = session.currentQuestion
        
        android.util.Log.d("OIRTestViewModel", "   currentQuestion null? ${currentQuestion == null}")
        
        // Safety check: if currentQuestion is null, we've gone past the last question
        if (currentQuestion == null) {
            val exception = Exception("Current question null at invalid index: ${session.currentQuestionIndex}/${session.questions.size}")
            ErrorLogger.log(exception, "OIR test data inconsistency: currentQuestion is null")

            // Log all question IDs to debug
            session.questions.forEachIndexed { index, q ->
                android.util.Log.d("OIRTestViewModel", "   Question[$index]: id=${q.id}")
            }
            
            _uiState.update { it.copy(
                isLoading = false,
                loadingMessage = null,
                error = "Invalid question index (${session.currentQuestionIndex}/${session.questions.size}). Please click Submit Test button."
            ) }
            return
        }
        
        android.util.Log.d("OIRTestViewModel", "   ✅ Got question: ${currentQuestion.id}")
        
        val existingAnswer = session.answers[currentQuestion.id]
        
        _uiState.update { it.copy(
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
        ) }
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
                val exception = Exception("Question ${question.id} has invalid correctAnswerId: ${question.correctAnswerId}")
                ErrorLogger.log(exception, "OIR question data validation failed: correctAnswerId not found in options")
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
        
        // PHASE 3: viewModelScope automatically cancels all child jobs
        android.util.Log.d("OIRTestViewModel", "🧹 ViewModel onCleared() - viewModelScope auto-canceling all jobs")
        
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
    val resetsAt: String = "",
    // PHASE 1: New StateFlow fields (replacing nullable vars)
    val isTimerActive: Boolean = false,
    val timerStartTime: Long = 0L,
    val session: OIRTestSession? = null  // Move session to observable state
)

