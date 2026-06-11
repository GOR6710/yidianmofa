package com.example.yidianmofa.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Continuous wake-word detector for "灵宝" using the Android SpeechRecognizer.
 *
 * The detector restarts the recognizer after each session ends, giving the
 * effect of continuous listening while staying within the Android API model.
 */
class WakeWordDetector(context: Context) {

    private val speechRecognizer: SpeechRecognizer? =
        SpeechRecognizer.createSpeechRecognizer(context).takeIf {
            SpeechRecognizer.isRecognitionAvailable(context)
        }

    private var isListening = false
    private var onWakeWord: (() -> Unit)? = null

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            if (isListening) restart()
        }
        override fun onResults(results: Bundle?) {
            checkResults(results)
            if (isListening) restart()
        }
        override fun onPartialResults(partialResults: Bundle?) {
            checkResults(partialResults)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun setOnWakeWordListener(listener: () -> Unit) {
        onWakeWord = listener
    }

    /**
     * Starts listening for the wake word. Safe to call multiple times.
     */
    fun start() {
        if (speechRecognizer == null) return
        isListening = true
        restart()
    }

    /**
     * Stops listening.
     */
    fun stop() {
        isListening = false
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        isListening = false
        speechRecognizer?.destroy()
        onWakeWord = null
    }

    private fun restart() {
        if (!isListening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechRecognizer?.setRecognitionListener(listener)
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (match in matches) {
            if (match.contains(WAKE_WORD, ignoreCase = true)) {
                onWakeWord?.invoke()
                return
            }
        }
    }

    companion object {
        private const val WAKE_WORD = "灵宝"
    }
}
