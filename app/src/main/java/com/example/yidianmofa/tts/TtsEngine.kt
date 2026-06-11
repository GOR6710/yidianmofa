package com.example.yidianmofa.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import java.util.UUID

/**
 * Text-to-speech engine for Lingbao.
 *
 * Phase 2 uses the Android system TTS with Chinese locale. A Volcano Engine
 * HTTP fallback stub is kept for a future Phase 2.x enhancement.
 */
class TtsEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private val pending = mutableListOf<Pair<String, (() -> Unit)?>>()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.apply {
                    language = Locale.CHINESE
                    // Prefer a Chinese voice if more than one is installed.
                    val chineseVoice = voices?.firstOrNull { voice ->
                        voice.locale.language.equals("zh", ignoreCase = true) ||
                                voice.name.contains("chinese", ignoreCase = true) ||
                                voice.name.contains("中文", ignoreCase = true)
                    }
                    if (chineseVoice != null) {
                        try {
                            voice = chineseVoice
                        } catch (_: Exception) { }
                    }
                }
                isReady = true
                flushPending()
            }
        }
    }

    /**
     * Speaks [text] using the system TTS engine.
     *
     * @param onComplete optional callback invoked when the utterance finishes.
     */
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isReady) {
            pending.add(text to onComplete)
            return
        }
        val utteranceId = UUID.randomUUID().toString()
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { }
            override fun onDone(utteranceId: String?) {
                onComplete?.invoke()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onComplete?.invoke()
            }
        })
        @Suppress("DEPRECATION")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
    }

    private fun flushPending() {
        val copy = pending.toList()
        pending.clear()
        copy.forEach { (text, callback) -> speak(text, callback) }
    }
}
