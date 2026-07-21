package com.ssbmax.shared.platform.audio

import android.media.MediaDataSource
import android.media.MediaPlayer
import android.util.Log
import ssbmax.shared.generated.resources.Res

actual class WhiteNoisePlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    actual suspend fun startAudio() {
        if (isPlaying) {
            Log.d(TAG, "White noise already playing")
            return
        }
        try {
            val bytes = Res.readBytes(ASSET_PATH)
            val dataSource = ByteArrayMediaDataSource(bytes)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(dataSource)
                isLooping = true
                setVolume(NOISE_VOLUME, NOISE_VOLUME)
                prepare()
                start()
            }
            isPlaying = true
            Log.d(TAG, "White noise audio started (${NOISE_VOLUME * 100}% volume)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start white noise audio", e)
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        }
    }

    actual fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop white noise audio", e)
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }

    actual fun isAudioPlaying(): Boolean = isPlaying

    actual fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(clamped, clamped)
    }

    /** Wraps in-memory audio bytes as a [MediaDataSource] so MediaPlayer can play
     *  a Compose-Multiplatform-owned resource without writing a temp file. */
    private class ByteArrayMediaDataSource(private val data: ByteArray) : MediaDataSource() {
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position >= data.size) return -1
            val remaining = (data.size - position).toInt()
            val bytesToRead = minOf(size, remaining)
            System.arraycopy(data, position.toInt(), buffer, offset, bytesToRead)
            return bytesToRead
        }

        override fun getSize(): Long = data.size.toLong()

        override fun close() {
            // No resources to release -- backed by an in-memory ByteArray.
        }
    }

    private companion object {
        const val TAG = "WhiteNoisePlayer"
        const val NOISE_VOLUME = 0.65f
        const val ASSET_PATH = "files/pink_noise.wav"
    }
}
