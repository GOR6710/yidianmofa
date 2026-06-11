package com.example.yidianmofa.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.io.IOException

/**
 * Wraps Android MediaRecorder to produce a local audio file.
 *
 * Output is stored in the app's cache directory. The file can then be uploaded
 * by [BackendApi].
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    /**
     * Starts recording. Returns the [File] that will contain the recording.
     *
     * @throws IllegalStateException if a recording is already in progress.
     * @throws IOException if MediaRecorder fails to prepare.
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun start(): File {
        if (recorder != null) {
            throw IllegalStateException("Recording already in progress")
        }

        outputFile = File(context.cacheDir, "lingbao_recording_${System.currentTimeMillis()}.m4a")
            .apply { parentFile?.mkdirs() }

        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16000)
            setAudioChannels(1)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }

        return outputFile!!
    }

    /**
     * Stops the current recording and releases the recorder.
     *
     * @return the recorded [File], or null if no recording was active.
     */
    fun stop(): File? {
        val file = outputFile
        recorder?.apply {
            try {
                stop()
            } catch (e: RuntimeException) {
                // stop() can throw if start() failed; surface via logs.
                e.printStackTrace()
            }
            release()
        }
        recorder = null
        outputFile = null
        return file
    }

    /**
     * Cancels the current recording and deletes the partial file.
     */
    fun cancel() {
        recorder?.apply {
            try { stop() } catch (_: RuntimeException) {}
            release()
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}
