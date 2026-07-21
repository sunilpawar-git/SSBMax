package com.ssbmax.ui.interview.session

import android.util.Log
import com.ssbmax.shared.platform.tts.TTSService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages TTS service initialization and speech control.
 *
 * Responsibilities:
 * - Initialize Android TTS service
 * - Handle TTS events (Ready, SpeechComplete, Error)
 * - Manage mute state and speech control
 */
class TTSManager(
    private val androidTTSService: TTSService,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "TTSManager"
    }

    private val _isTTSReady = MutableStateFlow(false)
    val isTTSReady: StateFlow<Boolean> = _isTTSReady.asStateFlow()

    private val _isTTSSpeaking = MutableStateFlow(false)
    val isTTSSpeaking: StateFlow<Boolean> = _isTTSSpeaking.asStateFlow()

    private val _isTTSMuted = MutableStateFlow(false)
    val isTTSMuted: StateFlow<Boolean> = _isTTSMuted.asStateFlow()

    private var eventCollectionJob: Job? = null
    @Volatile private var isReleased: Boolean = false

    /**
     * Initialize Android TTS service.
     */
    suspend fun initialize() {
        Log.d(TAG, "🔊 [TTS-INIT] Initializing Android TTS...")
        observeTTSEvents()
    }

    private fun observeTTSEvents() {
        // Cancel previous collection job if exists
        eventCollectionJob?.cancel()
        Log.d(TAG, "🔊 [TTS-EVENTS] Observing AndroidTTSService")
        eventCollectionJob = scope.launch {
            androidTTSService.events.collect { event ->
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
                        Log.w(TAG, "⚠️ [TTS-EVENTS] TTS error: ${event.message}")
                        _isTTSSpeaking.update { false }
                        handleTTSError(event)
                    }
                }
            }
        }
    }

    private fun handleTTSError(event: TTSService.TTSEvent.Error) {
        Log.w(TAG, "⚠️ TTS error: ${event.message}")
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
        scope.launch { androidTTSService.speak(text) }
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
            androidTTSService.stop()
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
        androidTTSService.stop()
        _isTTSSpeaking.update { false }
    }

    /** Release all TTS resources. */
    fun release() {
        Log.d(TAG, "🧹 release()")
        isReleased = true
        eventCollectionJob?.cancel()
        eventCollectionJob = null
        androidTTSService.release()
    }
}
