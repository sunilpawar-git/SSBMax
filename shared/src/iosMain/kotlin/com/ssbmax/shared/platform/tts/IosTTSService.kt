package com.ssbmax.shared.platform.tts

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject

/**
 * iOS actual of [TTSService], backed by `AVSpeechSynthesizer`.
 *
 * Mirrors [AndroidTTSService]'s behavior (English-India voice preference,
 * queue-flush-by-default speak, Ready/SpeechComplete/Error event stream) so
 * `TTSManager` in `app` (and its eventual Compose Multiplatform successor)
 * can treat both platforms identically through the shared [TTSService]
 * interface.
 */
@OptIn(ExperimentalForeignApi::class)
class IosTTSService : TTSService {

    private val synthesizer = AVSpeechSynthesizer()
    private var isReleased = false
    private var isCurrentlySpeaking = false

    private val _events = MutableSharedFlow<TTSService.TTSEvent>(replay = 1)
    override val events: SharedFlow<TTSService.TTSEvent> = _events.asSharedFlow()

    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        @ObjCSignatureOverride
        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            didStartSpeechUtterance: AVSpeechUtterance
        ) {
            isCurrentlySpeaking = true
        }

        @ObjCSignatureOverride
        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            didFinishSpeechUtterance: AVSpeechUtterance
        ) {
            if (isReleased) return
            isCurrentlySpeaking = false
            _events.tryEmit(TTSService.TTSEvent.SpeechComplete)
        }

        @ObjCSignatureOverride
        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            didCancelSpeechUtterance: AVSpeechUtterance
        ) {
            isCurrentlySpeaking = false
        }
    }

    init {
        synthesizer.delegate = delegate
        // AVSpeechSynthesizer has no async "engine ready" callback the way
        // android.speech.tts.TextToSpeech does -- it's ready to accept
        // utterances as soon as it's constructed. Emit Ready immediately so
        // callers written against TextToSpeech's init-then-Ready contract
        // behave the same way on iOS.
        _events.tryEmit(TTSService.TTSEvent.Ready)
    }

    override suspend fun speak(text: String, flush: Boolean) {
        if (isReleased) return
        if (flush && synthesizer.isSpeaking()) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text).apply {
            voice = selectBestVoice()
            rate = SPEECH_RATE
            pitchMultiplier = SPEECH_PITCH
        }
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        isCurrentlySpeaking = false
    }

    override fun release() {
        isReleased = true
        stop()
    }

    override fun isReady(): Boolean = !isReleased

    override fun isSpeaking(): Boolean = isCurrentlySpeaking

    /**
     * Prefer an Indian-English voice (matches the Android actual's locale
     * preference for SSB-context speech); fall back to generic English, then
     * the system default.
     */
    private fun selectBestVoice(): AVSpeechSynthesisVoice? {
        return AVSpeechSynthesisVoice.voiceWithLanguage("en-IN")
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
    }

    private companion object {
        // AVSpeechUtterance rate range is 0.0-1.0 (AVSpeechUtteranceMinimumSpeechRate/
        // Maximum), not TextToSpeech's 0.5-2.0 scale -- 0.5 is AVSpeechUtterance's
        // documented "default rate" middle ground, matching the Android actual's
        // slightly-slower-than-default SPEECH_RATE = 0.95f intent within iOS's own range.
        const val SPEECH_RATE = 0.48f
        const val SPEECH_PITCH = 1.0f
    }
}
