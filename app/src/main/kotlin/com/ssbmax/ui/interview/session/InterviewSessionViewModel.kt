package com.ssbmax.ui.interview.session

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.BuildConfig
import com.ssbmax.R
import com.ssbmax.core.data.analytics.AnalyticsManager
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.utils.tts.AndroidTTS
import com.ssbmax.utils.tts.ElevenLabsTTS
import com.ssbmax.utils.tts.SarvamTTS
import com.ssbmax.utils.tts.TTSService
import com.ssbmax.workers.InterviewAnalysisWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for Interview Session screen
 *
 * OPTIMIZATION: Uses BACKGROUND AI analysis.
 * - Responses are stored locally during interview (NO real-time AI)
 * - At completion, all responses saved to Firestore WITHOUT OLQ scores
 * - WorkManager handles AI analysis in background
 * - User navigates away instantly, notified when results ready
 *
 * This reduces per-question time from 6-8 seconds to <100ms.
 */
@HiltViewModel
class InterviewSessionViewModel @Inject constructor(
    private val interviewRepository: InterviewRepository,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val workManager: WorkManager,
    private val analyticsManager: AnalyticsManager,
    @AndroidTTS private val androidTTSService: TTSService,
    @SarvamTTS private val sarvamTTSService: TTSService,
    @ElevenLabsTTS private val elevenLabsTTSService: TTSService,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "VoiceInterviewVM"
    }

    private val sessionId: String = checkNotNull(savedStateHandle.get<String>("sessionId"))

    private val _uiState = MutableStateFlow(InterviewSessionUiState())
    val uiState: StateFlow<InterviewSessionUiState> = _uiState.asStateFlow()
    
    @Volatile
    private var isExiting: Boolean = false
    
    private var ttsService: TTSService = androidTTSService
    private var usingPremiumVoice: Boolean = false

    init {
        Log.d(TAG, "🚀 ViewModel initializing for session: $sessionId")
        trackMemoryLeaks("InterviewSessionViewModel")
        initializeTTS()
        loadSession()
    }
    
    private fun initializeTTS() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔊 [TTS-INIT] Starting TTS initialization...")

                if (BuildConfig.DEBUG && BuildConfig.FORCE_PREMIUM_TTS) {
                    Log.d(TAG, "🔊 [TTS-INIT] DEBUG MODE: Forcing Sarvam AI TTS for testing")
                    selectTTSService(SubscriptionType.PREMIUM)
                    return@launch
                }

                val userId = withTimeout(3000L) { // 3 second timeout for auth state
                    authRepository.currentUser.first()?.id
                }
                if (userId == null) {
                    Log.d(TAG, "🔊 [TTS-INIT] No user ID, using Android TTS")
                    selectTTSService(SubscriptionType.FREE)
                    return@launch
                }

                Log.d(TAG, "🔊 [TTS-INIT] Fetching user profile for userId: $userId")
                val profileResult = withTimeout(5000L) { // 5 second timeout for user profile fetch
                    userProfileRepository.getUserProfile(userId).first()
                }
                val subscriptionType = profileResult.getOrNull()?.subscriptionType ?: SubscriptionType.FREE

                Log.d(TAG, "🔊 [TTS-INIT] User subscription: $subscriptionType")
                selectTTSService(subscriptionType)
            } catch (e: Exception) {
                ErrorLogger.log(e, "[TTS-INIT] Failed to initialize TTS with user subscription, falling back to Android TTS")
                selectTTSService(SubscriptionType.FREE)
            }
        }
    }
    
    private fun selectTTSService(subscriptionType: SubscriptionType) {
        Log.d(TAG, "🔊 [TTS-SELECT] Selecting TTS service for: $subscriptionType")

        ttsService = when (subscriptionType) {
            SubscriptionType.PRO, SubscriptionType.PREMIUM -> {
                Log.d(TAG, "🔊 [TTS-SELECT] Checking premium TTS services...")
                Log.d(TAG, "🔊 [TTS-SELECT] Enhanced Android TTS ready: ${androidTTSService.isReady()}")
                Log.d(TAG, "🔊 [TTS-SELECT] Sarvam AI ready: ${sarvamTTSService.isReady()}")
                Log.d(TAG, "🔊 [TTS-SELECT] ElevenLabs ready: ${elevenLabsTTSService.isReady()}")

                // Priority: Android TTS (local) → Sarvam API → ElevenLabs API
                if (androidTTSService.isReady()) {
                    Log.d(TAG, "🔊 [TTS-SELECT] ✅ Using Enhanced Android TTS (Local Pro/Premium with Indian English)")
                    usingPremiumVoice = true
                    androidTTSService
                } else if (sarvamTTSService.isReady()) {
                    Log.d(TAG, "🔊 [TTS-SELECT] ✅ Using Sarvam AI TTS (Pro/Premium)")
                    usingPremiumVoice = true
                    sarvamTTSService
                } else if (elevenLabsTTSService.isReady()) {
                    Log.d(TAG, "🔊 [TTS-SELECT] ✅ Using ElevenLabs TTS (Fallback)")
                    usingPremiumVoice = true
                    elevenLabsTTSService
                } else {
                    Log.w(TAG, "⚠️ [TTS-SELECT] No TTS services available")
                    usingPremiumVoice = false
                    androidTTSService // Last resort
                }
            }
            SubscriptionType.FREE -> {
                Log.d(TAG, "🔊 [TTS-SELECT] ✅ Using Android TTS (Free)")
                Log.d(TAG, "🔊 [TTS-SELECT] Android TTS ready: ${androidTTSService.isReady()}")
                usingPremiumVoice = false
                androidTTSService
            }
        }

        Log.d(TAG, "🔊 [TTS-SELECT] Selected TTS service: ${ttsService.javaClass.simpleName}")
        observeTTSEvents()
    }
    
    private fun observeTTSEvents() {
        Log.d(TAG, "🔊 [TTS-EVENTS] Starting to observe TTS events for ${ttsService.javaClass.simpleName}")

        viewModelScope.launch {
            ttsService.events.collect { event ->
                if (isExiting) {
                    Log.d(TAG, "🔊 [TTS-EVENTS] Ignoring event (exiting): $event")
                    return@collect
                }

                Log.d(TAG, "🔊 [TTS-EVENTS] Received event: $event")

                when (event) {
                    is TTSService.TTSEvent.Ready -> {
                        Log.d(TAG, "🔊 [TTS-EVENTS] ✅ TTS ready - Setting isTTSReady=true")
                        _uiState.update { it.copy(isTTSReady = true) }
                        _uiState.value.currentQuestion?.let {
                            Log.d(TAG, "🔊 [TTS-EVENTS] Current question exists, will speak if not muted")
                            speakQuestion(it.questionText)
                        } ?: Log.d(TAG, "🔊 [TTS-EVENTS] No current question to speak")
                    }
                    is TTSService.TTSEvent.SpeechComplete -> {
                        Log.d(TAG, "✅ [TTS-EVENTS] TTS speech complete")
                        _uiState.update { it.copy(isTTSSpeaking = false) }
                    }
                    is TTSService.TTSEvent.Error -> {
                        ErrorLogger.log(Exception(event.message), "[TTS-EVENTS] TTS service error")
                        _uiState.update { it.copy(isTTSSpeaking = false) }
                        if (event.fallbackToAndroid && usingPremiumVoice) {
                            // Fallback chain: Android → Sarvam → ElevenLabs
                            when {
                                ttsService == androidTTSService && sarvamTTSService.isReady() -> {
                                    Log.d(TAG, "🔄 Enhanced Android TTS failed, falling back to Sarvam AI TTS")
                                    analyticsManager.trackFeatureUsed(
                                        "tts_fallback",
                                        mapOf("from_service" to "android_enhanced", "to_service" to "sarvam_ai")
                                    )
                                    ttsService = sarvamTTSService
                                    observeTTSEvents()
                                }
                                (ttsService == androidTTSService || ttsService == sarvamTTSService) && elevenLabsTTSService.isReady() -> {
                                    Log.d(TAG, "🔄 Premium TTS failed, falling back to ElevenLabs TTS")
                                    analyticsManager.trackFeatureUsed(
                                        "tts_fallback",
                                        mapOf("from_service" to if (ttsService == androidTTSService) "android_enhanced" else "sarvam_ai", "to_service" to "elevenlabs")
                                    )
                                    ttsService = elevenLabsTTSService
                                    observeTTSEvents()
                                }
                                else -> {
                                    Log.w(TAG, "⚠️ All premium TTS services failed, staying with basic Android TTS")
                                    analyticsManager.trackFeatureUsed(
                                        "tts_fallback",
                                        mapOf(
                                            "from_service" to when (ttsService) {
                                                androidTTSService -> "android_enhanced"
                                                sarvamTTSService -> "sarvam_ai"
                                                elevenLabsTTSService -> "elevenlabs"
                                                else -> "unknown"
                                            },
                                            "to_service" to "android_basic"
                                        )
                                    )
                                    usingPremiumVoice = false
                                    // Stay with current service but mark as not premium
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun speakQuestion(questionText: String) {
        if (isExiting) {
            Log.d(TAG, "🔊 [TTS-SPEAK] Skipping speech - exiting")
            return
        }
        if (_uiState.value.isTTSMuted) {
            Log.d(TAG, "🔊 [TTS-SPEAK] Skipping speech - TTS is muted")
            return
        }

        // Always attempt to speak - TTS service handles queuing internally
        Log.d(TAG, "🔊 [TTS-SPEAK] Speaking question: ${questionText.take(50)}...")
        Log.d(TAG, "🔊 [TTS-SPEAK] TTS service ready: ${ttsService.isReady()}")
        _uiState.update { it.copy(isTTSSpeaking = true) }
        viewModelScope.launch { ttsService.speak(questionText) }
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = context.getString(R.string.interview_loading_session)) }

            try {
                val sessionResult = interviewRepository.getSession(sessionId)
                if (sessionResult.isFailure) {
                    handleError(sessionResult.exceptionOrNull() ?: Exception("Unknown"), R.string.interview_error_load_session)
                    return@launch
                }

                val session = sessionResult.getOrNull()
                if (session == null) {
                    setError(R.string.interview_error_session_not_found)
                    return@launch
                }

                val questionId = session.questionIds.getOrNull(session.currentQuestionIndex) ?: run {
                    setError(R.string.interview_error_no_questions)
                    return@launch
                }

                val questionResult = interviewRepository.getQuestion(questionId)
                if (questionResult.isFailure) {
                    handleError(questionResult.exceptionOrNull() ?: Exception("Unknown"), R.string.interview_error_load_question)
                    return@launch
                }
                val question = questionResult.getOrNull()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        session = session,
                        currentQuestion = question,
                        currentQuestionIndex = session.currentQuestionIndex,
                        totalQuestions = session.questionIds.size,
                        thinkingStartTime = System.currentTimeMillis()
                    )
                }
                
                if (!isExiting) {
                    question?.let { speakQuestion(it.questionText) }
                }
            } catch (e: Exception) {
                handleError(e, R.string.interview_error_generic)
            }
        }
    }

    fun updateResponseText(text: String) {
        _uiState.update { it.copy(responseText = text) }
    }

    /**
     * Toggle TTS mute/unmute
     *
     * When muting: stops current speech
     * When unmuting: speaks current question from beginning
     */
    fun toggleTTSMute() {
        val currentState = _uiState.value.isTTSMuted
        val newMutedState = !currentState
        Log.d(TAG, "🔊 [TTS-MUTE] Toggle requested: $currentState → $newMutedState")
        Log.d(TAG, "🔊 [TTS-MUTE] Current state - isTTSReady: ${_uiState.value.isTTSReady}, isTTSSpeaking: ${_uiState.value.isTTSSpeaking}")

        _uiState.update { it.copy(isTTSMuted = newMutedState) }

        if (newMutedState) {
            // Muting - stop current speech
            Log.d(TAG, "🔊 [TTS-MUTE] Muting TTS - stopping any current speech")
            ttsService.stop()
            _uiState.update { it.copy(isTTSSpeaking = false) }
            Log.d(TAG, "🔊 [TTS-MUTE] ✅ TTS muted successfully")
        } else {
            // Unmuting - speak current question
            Log.d(TAG, "🔊 [TTS-MUTE] Unmuting TTS - attempting to speak current question")
            _uiState.value.currentQuestion?.let {
                Log.d(TAG, "🔊 [TTS-MUTE] Current question found, speaking...")
                speakQuestion(it.questionText)
            } ?: Log.d(TAG, "🔊 [TTS-MUTE] No current question to speak")
            Log.d(TAG, "🔊 [TTS-MUTE] ✅ TTS unmuted successfully")
        }
    }


    /**
     * Submit current response and move to next question INSTANTLY.
     *
     * OPTIMIZATION: Stores response locally, NO AI analysis here.
     * AI analysis is deferred to completeInterview() for batch processing.
     * This reduces per-question time from 6-8s to <100ms.
     */
    fun submitResponse() {
        if (!_uiState.value.canSubmitResponse()) {
            Log.d(TAG, "⚠️ Cannot submit response - validation failed")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingResponse = true, error = null) }

            try {
                val state = _uiState.value
                val currentQuestion = state.currentQuestion ?: return@launch

                // Store response locally (NO AI analysis - instant!)
                Log.d(TAG, "⚡ Storing response locally - Question: ${currentQuestion.id}")
                val pendingResponse = PendingResponse(
                    questionId = currentQuestion.id,
                    questionText = currentQuestion.questionText,
                    responseText = state.responseText,
                    thinkingTimeSec = state.getThinkingTimeSeconds()
                )

                val updatedPending = state.pendingResponses + pendingResponse
                Log.d(TAG, "📝 Stored locally (${updatedPending.size}/${state.totalQuestions} responses)")

                if (state.hasMoreQuestions()) {
                    _uiState.update { it.copy(pendingResponses = updatedPending) }
                    loadNextQuestion()
                } else {
                    // Last question - pass responses directly to avoid race condition
                    Log.d(TAG, "🏁 Last question - completing with ${updatedPending.size} responses")
                    completeInterview(updatedPending)
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception during interview response submission")
                handleError(e, R.string.interview_error_generic)
                _uiState.update { it.copy(isSubmittingResponse = false) }
            }
        }
    }

    private suspend fun loadNextQuestion() {
        val state = _uiState.value
        val session = state.session ?: return
        val nextIndex = state.currentQuestionIndex + 1
        val nextQuestionId = session.questionIds.getOrNull(nextIndex) ?: return
        
        Log.d(TAG, "📥 Loading question $nextIndex: $nextQuestionId")

        val updatedSession = session.copy(currentQuestionIndex = nextIndex)
        interviewRepository.updateSession(updatedSession)

        val question = interviewRepository.getQuestion(nextQuestionId).getOrElse {
            handleError(it, R.string.interview_error_load_next_question)
            _uiState.update { s -> s.copy(isSubmittingResponse = false) }
            return
        }

        _uiState.update {
            it.copy(
                isSubmittingResponse = false,
                session = updatedSession,
                currentQuestion = question,
                currentQuestionIndex = nextIndex,
                thinkingStartTime = System.currentTimeMillis(),
                responseText = ""
            )
        }
        
        speakQuestion(question.questionText)
    }

    /**
     * Complete interview with BACKGROUND analysis.
     *
     * 1. Save all responses to Firestore WITHOUT OLQ scores (instant)
     * 2. Update session status to PENDING_ANALYSIS
     * 3. Schedule WorkManager for background AI analysis
     * 4. Navigate user away immediately
     * 5. User gets notification when results ready
     *
     * @param pendingResponses All responses collected during interview (passed directly to avoid race condition)
     */
    private suspend fun completeInterview(pendingResponses: List<PendingResponse>) {
        val state = _uiState.value
        val session = state.session ?: return

        Log.d(TAG, "🏁 Completing interview with ${pendingResponses.size} responses (background analysis)")

        _uiState.update {
            it.copy(
                isSubmittingResponse = true,
                loadingMessage = context.getString(R.string.interview_submitting_answers)
            )
        }

        try {
            // STEP 1: Save all responses to Firestore WITHOUT OLQ scores
            Log.d(TAG, "💾 Saving ${pendingResponses.size} responses to Firestore...")
            
            for ((index, pending) in pendingResponses.withIndex()) {
                val response = InterviewResponse(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    questionId = pending.questionId,
                    responseText = pending.responseText,
                    responseMode = state.mode,
                    respondedAt = Instant.ofEpochMilli(pending.respondedAt),
                    thinkingTimeSec = pending.thinkingTimeSec,
                    audioUrl = null,
                    olqScores = emptyMap(),  // Empty - background worker will fill
                    confidenceScore = 0
                )

                val submitResult = interviewRepository.submitResponse(response)
                if (submitResult.isFailure) {
                    Log.w(TAG, "Failed to save response ${index + 1}: ${submitResult.exceptionOrNull()?.message}")
                }
            }
            Log.d(TAG, "✅ Saved ${pendingResponses.size} responses")

            // STEP 2: Update session status to PENDING_ANALYSIS
            val updatedSession = session.copy(status = InterviewStatus.PENDING_ANALYSIS)
            interviewRepository.updateSession(updatedSession)
            Log.d(TAG, "✅ Session marked as PENDING_ANALYSIS")

            // STEP 3: Enqueue background analysis worker
            Log.d(TAG, "🔄 Enqueuing InterviewAnalysisWorker...")
            enqueueAnalysisWorker(sessionId)

            // STEP 4: Navigate user away (results pending)
            _uiState.update {
                it.copy(
                    isSubmittingResponse = false,
                    loadingMessage = null,
                    isCompleted = true,
                    isResultPending = true,
                    resultId = sessionId  // Use sessionId for pending results
                )
            }
            Log.d(TAG, "✅ Interview completed - background analysis scheduled")

        } catch (e: Exception) {
            ErrorLogger.log(e, "Exception completing voice interview")
            handleError(e, R.string.interview_error_complete_interview)
            _uiState.update { it.copy(isSubmittingResponse = false, loadingMessage = null) }
        }
    }

    /**
     * Enqueue the InterviewAnalysisWorker for background AI processing
     */
    private fun enqueueAnalysisWorker(sessionId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<InterviewAnalysisWorker>()
            .setInputData(
                workDataOf(InterviewAnalysisWorker.KEY_SESSION_ID to sessionId)
            )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "interview_analysis_$sessionId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        Log.d(TAG, "📥 InterviewAnalysisWorker enqueued for session: $sessionId")
    }

    private fun handleError(error: Throwable, stringResId: Int) {
        ErrorLogger.log(error, "Voice interview error")
        setError(stringResId)
    }

    private fun setError(stringResId: Int) {
        _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = context.getString(stringResId)) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    
    fun stopAll() {
        Log.d(TAG, "🛑 stopAll() called")
        isExiting = true
        ttsService.stop()
        _uiState.update { it.copy(isTTSSpeaking = false) }
    }

    override fun onCleared() {
        Log.d(TAG, "🧹 onCleared()")
        super.onCleared()
        isExiting = true
        sarvamTTSService.release()
        elevenLabsTTSService.release()
        androidTTSService.release()
    }
}

