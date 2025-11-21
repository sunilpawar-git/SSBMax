package com.ssbmax.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

/**
 * Audio recorder utility for interview voice responses
 *
 * Handles MediaRecorder lifecycle and file management
 */
class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recordingStartTime: Long = 0L

    /**
     * Start recording audio
     *
     * @return File path where audio is being recorded, or null on failure
     */
    fun startRecording(): String? {
        try {
            // Create output file
            val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
            val interviewDir = File(outputDir, "interview_audio")
            if (!interviewDir.exists()) {
                interviewDir.mkdirs()
            }

            outputFile = File.createTempFile(
                "interview_${System.currentTimeMillis()}",
                ".m4a",
                interviewDir
            )

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile!!.absolutePath)

                try {
                    prepare()
                    start()
                    recordingStartTime = System.currentTimeMillis()
                } catch (e: IOException) {
                    ErrorLogger.log(e, "Failed to prepare MediaRecorder")
                    release()
                    return null
                }
            }

            return outputFile?.absolutePath
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to start audio recording")
            release()
            return null
        }
    }

    /**
     * Stop recording and return file path + duration
     *
     * @return Pair of (file path, duration in milliseconds), or null on failure
     */
    fun stopRecording(): Pair<String, Long>? {
        return try {
            val duration = System.currentTimeMillis() - recordingStartTime

            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            val filePath = outputFile?.absolutePath
            if (filePath != null && outputFile?.exists() == true) {
                Pair(filePath, duration)
            } else {
                null
            }
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to stop audio recording")
            release()
            null
        }
    }

    /**
     * Cancel recording and delete file
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            // Delete the file
            outputFile?.delete()
            outputFile = null
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to cancel audio recording")
            release()
        }
    }

    /**
     * Release all resources
     */
    fun release() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to release MediaRecorder")
        }
    }

    /**
     * Delete a recorded audio file
     */
    fun deleteAudioFile(filePath: String) {
        try {
            File(filePath).delete()
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to delete audio file: $filePath")
        }
    }

    /**
     * Get recording duration in milliseconds
     */
    fun getRecordingDuration(): Long {
        return if (mediaRecorder != null && recordingStartTime > 0) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0L
        }
    }
}
