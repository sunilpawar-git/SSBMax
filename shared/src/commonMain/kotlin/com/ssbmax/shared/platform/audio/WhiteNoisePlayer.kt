package com.ssbmax.shared.platform.audio

/**
 * Plays looping background "white noise" (pink noise) audio during GTO
 * Group Discussion / Lecturette tests, simulating the real SSB GTO
 * environment's ambient distraction.
 *
 * The audio asset (`pink_noise.wav`) is owned once, in
 * `shared/commonMain/composeResources/files/`, and read via Compose
 * Multiplatform resources — not duplicated per platform. Construction
 * differs per platform (Android needs a `Context` for the underlying
 * `MediaPlayer`; iOS's `AVAudioPlayer` doesn't), mirroring the
 * `DatabaseDriverFactory`/`SettingsFactory` expect/actual pattern already
 * established in this module.
 */
expect class WhiteNoisePlayer {
    /** Start looping playback. No-op if already playing. */
    suspend fun startAudio()

    /** Stop playback and release the underlying player. */
    fun stopAudio()

    /** Whether white noise is currently playing. */
    fun isAudioPlaying(): Boolean

    /** Adjust volume (0.0 - 1.0). */
    fun setVolume(volume: Float)
}
