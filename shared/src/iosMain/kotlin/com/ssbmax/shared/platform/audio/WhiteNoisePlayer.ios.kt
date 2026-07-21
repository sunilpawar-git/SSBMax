package com.ssbmax.shared.platform.audio

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create
import ssbmax.shared.generated.resources.Res

@OptIn(ExperimentalForeignApi::class)
actual class WhiteNoisePlayer {
    private var player: AVAudioPlayer? = null

    actual suspend fun startAudio() {
        if (player?.isPlaying() == true) return
        try {
            val bytes = Res.readBytes(ASSET_PATH)
            val data = bytes.toNSData()
            val audioPlayer = AVAudioPlayer(data = data, error = null)
            audioPlayer.numberOfLoops = -1 // loop indefinitely
            audioPlayer.volume = NOISE_VOLUME
            audioPlayer.prepareToPlay()
            audioPlayer.play()
            player = audioPlayer
        } catch (e: Exception) {
            player = null
        }
    }

    actual fun stopAudio() {
        player?.stop()
        player = null
    }

    actual fun isAudioPlaying(): Boolean = player?.isPlaying() == true

    actual fun setVolume(volume: Float) {
        player?.volume = volume.coerceIn(0f, 1f)
    }

    @OptIn(BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }

    private companion object {
        const val NOISE_VOLUME = 0.65f
        const val ASSET_PATH = "files/pink_noise.wav"
    }
}
