package com.ssbmax.ui.interview.session

import android.util.Log
import com.ssbmax.core.data.analytics.AnalyticsManager
import com.ssbmax.core.domain.model.SubscriptionType
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.utils.tts.TTSService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Manages TTS service selection, initialization, and speech control.
 *
 * Responsibilities:
 * - Select TTS service based on subscription tier (Qwen TTS for Pro/Premium, Android TTS for Free)
 * - Handle TTS events (Ready, SpeechComplete, Error)
 * - Provide fallback logic when premium TTS fails
 * - Manage mute state and speech control
 *
 * TTS Priority: Qwen TTS (premium) → Android TTS (fallback/free)
 */
class TTSManager(
    private val qwenTTSService: TTSService,
    private val androidTTSService: TTSService,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val analyticsManager: AnalyticsManager,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "TTSManager"
        private const val AUTH_TIMEOUT_MS = 3000L
        private const val PROFILE_TIMEOUT_MS = 5000L
    }

    private val _isTTSReady = MutableStateFlow(false)
    val isTTSReady: StateFlow<Boolean> = _isTTSReady.asStateFlow()

    private val _isTTSSpeaking = MutableStateFlow(false)
    val isTTSSpeaking: StateFlow<Boolean> = _isTTSSpeaking.asStateFlow()

    private val _isTTSMuted = MutableStateFlow(false)
    val isTTSMuted: StateFlow<Boolean> = _isTTSMuted.asStateFlow()

    private var activeTTSService: TTSService = androidTTSService
    private var usingPremiumVoice: Boolean = false
    private var eventCollectionJob: Job? = null
    @Volatile private var isReleased: Boolean = false

    /**
     * Initialize TTS based on user subscription or force premium for testing.
     */
    suspend fun initialize(forcePremium: Boolean = false) {
        Log.d(TAG, "🔊 [TTS-INIT] Starting TTS initialization...")

        if (forcePremium) {
            Log.d(TAG, "🔊 [TTS-INIT] Force premium mode - selecting Qwen TTS")
            selectTTSService(SubscriptionType.PREMIUM)
            return
        }

        try {
            val userId = withTimeout(AUTH_TIMEOUT_MS) {
                authRepository.currentUser.first()?.id
            }
            if (userId == null) {
                Log.d(TAG, "🔊 [TTS-INIT] No user ID, using Android TTS")
                selectTTSService(SubscriptionType.FREE)
                return
            }

            Log.d(TAG, "🔊 [TTS-INIT] Fetching user profile for userId: $userId")
            val profileResult = withTimeout(PROFILE_TIMEOUT_MS) {
                userProfileRepository.getUserProfile(userId).first()
            }
            val subscriptionType = profileResult.getOrNull()?.subscriptionType ?: SubscriptionType.FREE

            Log.d(TAG, "🔊 [TTS-INIT] User subscription: $subscriptionType")
            selectTTSService(subscriptionType)
        } catch (e: Exception) {
            ErrorLogger.log(e, "[TTS-INIT] Failed to initialize TTS, falling back to Android TTS")
            selectTTSService(SubscriptionType.FREE)
        }
    }

    private fun selectTTSService(subscriptionType: SubscriptionType) {
        Log.d(TAG, "🔊 [TTS-SELECT] Selecting TTS for: $subscriptionType")

        activeTTSService = when (subscriptionType) {
            SubscriptionType.PRO, SubscriptionType.PREMIUM -> {
                Log.d(TAG, "🔊 [TTS-SELECT] Qwen ready: ${qwenTTSService.isReady()}, Android ready: ${androidTTSService.isReady()}")
                if (qwenTTSService.isReady()) {
                    Log.d(TAG, "🔊 [TTS-SELECT] ✅ Using Qwen TTS (Pro/Premium)")
                    usingPremiumVoice = true
                    qwenTTSService
                } else {
                    Log.w(TAG, "⚠️ [TTS-SELECT] Qwen TTS not ready, using Android TTS")
                    usingPremiumVoice = false
                    androidTTSService
                }
            }
            SubscriptionType.FREE -> {
                Log.d(TAG, "🔊 [TTS-SELECT] ✅ Using Android TTS (Free)")
                usingPremiumVoice = false
                androidTTSService
            }
        }
        Log.d(TAG, "🔊 [TTS-SELECT] Active: ${activeTTSService.javaClass.simpleName}")
        observeTTSEvents()
    }

    private fun observeTTSEvents() {
        // Cancel previous collection job if exists
        eventCollectionJob?.cancel()
        Log.d(TAG, "🔊 [TTS-EVENTS] Observing ${activeTTSService.javaClass.simpleName}")
        eventCollectionJob = scope.launch {
            activeTTSService.events.collect { event ->
                if (isReleased) return@collect
                Log.d(TAG, "🔊 [TTS-EVENTS] Event: $event")
                when (event) {
                    is TTSService.TTSEvent.Ready -> {
                        Log.d(TAG, "🔊 [TTS-EVENTS] ✅ TTS ready")
                        _isTTSReady.update { true }
                    }
                    is TTSService.TTSEvent.SpeechComplete -> {
                        Log.d(TAG, "✅ [TTS-EVENTS] Speech complete")
                        _isTTSSpeaking.update { false }
                    }
                    is TTSService.TTSEvent.Error -> {
                        ErrorLogger.log(Exception(event.message), "[TTS-EVENTS] TTS error")
                        _isTTSSpeaking.update { false }
                        handleTTSError(event)
                    }
                }
            }
        }
    }

    private fun handleTTSError(event: TTSService.TTSEvent.Error) {
        if (!event.fallbackToAndroid || !usingPremiumVoice) return

        if (activeTTSService == qwenTTSService && androidTTSService.isReady()) {
            Log.d(TAG, "🔄 Qwen TTS failed, falling back to Android TTS")
            analyticsManager.trackFeatureUsed("tts_fallback", mapOf("from_service" to "qwen_tts", "to_service" to "android_tts"))
            activeTTSService = androidTTSService
            usingPremiumVoice = false
            observeTTSEvents()
        } else {
            Log.w(TAG, "⚠️ TTS fallback failed")
            analyticsManager.trackFeatureUsed("tts_fallback", mapOf("from_service" to "qwen_tts", "to_service" to "none"))
        }
    }

    /**
     * Speak the given text. Skips if muted or released.
     */
    fun speak(text: String) {
        if (isReleased) {
            Log.d(TAG, "🔊 [TTS-SPEAK] Skipping - released")
            return
        }
        if (_isTTSMuted.value) {
            Log.d(TAG, "🔊 [TTS-SPEAK] Skipping - muted")
            return
        }
        Log.d(TAG, "🔊 [TTS-SPEAK] Speaking: ${text.take(50)}...")
        _isTTSSpeaking.update { true }
        scope.launch { activeTTSService.speak(text) }
    }

    /**
     * Toggle mute state. When unmuting, optionally speaks the provided question.
     */
    fun toggleMute(currentQuestionText: String?) {
        val newMutedState = !_isTTSMuted.value
        Log.d(TAG, "🔊 [TTS-MUTE] Toggle: ${_isTTSMuted.value} → $newMutedState")
        _isTTSMuted.update { newMutedState }

        if (newMutedState) {
            Log.d(TAG, "🔊 [TTS-MUTE] Muting - stopping speech")
            activeTTSService.stop()
            _isTTSSpeaking.update { false }
        } else {
            currentQuestionText?.let {
                Log.d(TAG, "🔊 [TTS-MUTE] Unmuting - speaking question")
                speak(it)
            }
        }
    }

    /** Stop current speech. */
    fun stop() {
        Log.d(TAG, "🛑 stop()")
        activeTTSService.stop()
        _isTTSSpeaking.update { false }
    }

    /** Release all TTS resources. */
    fun release() {
        Log.d(TAG, "🧹 release()")
        isReleased = true
        eventCollectionJob?.cancel()
        eventCollectionJob = null
        qwenTTSService.release()
        androidTTSService.release()
    }
}
