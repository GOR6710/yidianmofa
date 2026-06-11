package com.example.yidianmofa.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import java.io.IOException

/**
 * Plays pre-recorded MP3 assets and arbitrary audio files.
 *
 * In Phase 2 this is used for the voice_samples bundled with the app.
 */
class AudioPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    /**
     * Plays an MP3 file from the app's assets under the `lingbao/` folder.
     *
     * @param assetPath path relative to `assets/lingbao/`, e.g. `"voice_samples/lingbao_wake.mp3"`.
     * @param onComplete optional callback invoked when playback finishes.
     */
    fun playAsset(assetPath: String, onComplete: (() -> Unit)? = null) {
        stop()
        try {
            val fullPath = "lingbao/$assetPath"
            val afd: AssetFileDescriptor = context.assets.openFd(fullPath)
            player = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                setOnCompletionListener {
                    onComplete?.invoke()
                }
                setOnPreparedListener { start() }
                prepareAsync()
            }
            afd.close()
        } catch (e: IOException) {
            e.printStackTrace()
            onComplete?.invoke()
        }
    }

    /**
     * Plays an audio file from the local filesystem.
     */
    fun playFile(file: File, onComplete: (() -> Unit)? = null) {
        stop()
        player = MediaPlayer().apply {
            setDataSource(context, Uri.fromFile(file))
            setOnCompletionListener { onComplete?.invoke() }
            setOnPreparedListener { start() }
            prepareAsync()
        }
    }

    fun stop() {
        player?.apply {
            if (isPlaying) stop()
            release()
        }
        player = null
    }
}
