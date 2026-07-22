package com.ssbmax.shared.presentation.interviewsession

import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.platform.tts.TTSService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of `app/.../ui/interview/session/TTSManager.kt`. This class was
 * already written against the Phase 4 [TTSService] `expect`/`actual` shim in
 * the Android original (not a fresh Android-only dependency), so the only
 * real change here is `android.util.Log` -> [DomainLogger].
 *
 * Manages TTS service initialization and speech control: initialize,
 * observe events (Ready/SpeechComplete/Error), speak, mute toggle, stop,
 * release.
 */
class TTSManager(
    private val ttsService: TTSService,
    private val scope: CoroutineScope,
    private val logger: DomainLogger
) {
    private val tag = "TTSManager"

    private val _isTTSReady = MutableStateFlow(false)
    val isTTSReady: StateFlow<Boolean> = _isTTSReady.asStateFlow()

    private val _isTTSSpeaking = MutableStateFlow(false)
    val isTTSSpeaking: StateFlow<Boolean> = _isTTSSpeaking.asStateFlow()

    private val _isTTSMuted = MutableStateFlow(false)
    val isTTSMuted: StateFlow<Boolean> = _isTTSMuted.asStateFlow()

    private var eventCollectionJob: Job? = null
    private var isReleased: Boolean = false

    suspend fun initialize() {
        observeTTSEvents()
    }

    private fun observeTTSEvents() {
        eventCollectionJob?.cancel()
        eventCollectionJob = scope.launch {
            ttsService.events.collect { event ->
                if (isReleased) return@collect
                when (event) {
                    is TTSService.TTSEvent.Ready -> _isTTSReady.update { true }
                    is TTSService.TTSEvent.SpeechComplete -> _isTTSSpeaking.update { false }
                    is TTSService.TTSEvent.Error -> {
                        _isTTSSpeaking.update { false }
                        logger.w(tag, "TTS error: ${event.message}")
                    }
                }
            }
        }
    }

    /** Speak the given text. Skips if muted or released. */
    fun speak(text: String) {
        if (isReleased || _isTTSMuted.value) return
        _isTTSSpeaking.update { true }
        scope.launch { ttsService.speak(text) }
    }

    /** Toggle mute state. When unmuting, optionally speaks the provided question. */
    fun toggleMute(currentQuestionText: String?) {
        val newMutedState = !_isTTSMuted.value
        _isTTSMuted.update { newMutedState }

        if (newMutedState) {
            ttsService.stop()
            _isTTSSpeaking.update { false }
        } else {
            currentQuestionText?.let { speak(it) }
        }
    }

    /** Stop current speech. */
    fun stop() {
        ttsService.stop()
        _isTTSSpeaking.update { false }
    }

    /** Release all TTS resources. */
    fun release() {
        isReleased = true
        eventCollectionJob?.cancel()
        eventCollectionJob = null
        ttsService.release()
    }
}
