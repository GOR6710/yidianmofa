package com.example.yidianmofa.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.example.yidianmofa.audio.AudioPlayer
import com.example.yidianmofa.audio.AudioRecorder
import com.example.yidianmofa.network.BackendApi
import com.example.yidianmofa.storage.LingbaoPrefs
import com.example.yidianmofa.tts.TtsEngine
import com.example.yidianmofa.voice.WakeWordDetector
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference

/**
 * JavaScript bridge exposed to the Lingbao WebView as `LingbaoAndroid`.
 *
 * This class wires the browser-only APIs used by `lingbao_preview.html` to
 * native Android implementations:
 * - wake-word detection -> SpeechRecognizer
 * - audio recording -> MediaRecorder
 * - TTS -> Android TextToSpeech
 * - backend communication -> OkHttp
 * - persistent title -> SharedPreferences
 */
class LingbaoBridge(
    context: Context,
    webView: WebView,
    private val wakeWordDetector: WakeWordDetector,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val ttsEngine: TtsEngine,
    private val backendApi: BackendApi,
    private val prefs: LingbaoPrefs
) {

    private val context = context.applicationContext
    private val webViewRef = WeakReference(webView)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastRecordingFile: File? = null
    private var statusPollRunnable: Runnable? = null

    init {
        wakeWordDetector.setOnWakeWordListener {
            evaluateJs("window.onWakeWordDetected && window.onWakeWordDetected()")
        }
    }

    // region Utility

    @JavascriptInterface
    fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun evaluateJs(script: String) {
        mainHandler.post {
            webViewRef.get()?.evaluateJavascript(script, null)
        }
    }

    // region Wake word

    @JavascriptInterface
    fun startWakeWordListener(): String {
        mainHandler.post { wakeWordDetector.start() }
        return "started"
    }

    @JavascriptInterface
    fun stopWakeWordListener(): String {
        mainHandler.post { wakeWordDetector.stop() }
        return "stopped"
    }

    // region Recording

    @JavascriptInterface
    fun startRecording(): String {
        mainHandler.post {
            try {
                lastRecordingFile = audioRecorder.start()
                evaluateJs("window.onRecordingStarted && window.onRecordingStarted()")
            } catch (e: Exception) {
                e.printStackTrace()
                evaluateJs("window.onRecordingError && window.onRecordingError('${e.message?.escapeJs()}')")
            }
        }
        return "recording"
    }

    @JavascriptInterface
    fun stopRecording(): String {
        mainHandler.post {
            val file = audioRecorder.stop()
            lastRecordingFile = file ?: lastRecordingFile
            val path = lastRecordingFile?.absolutePath ?: ""
            evaluateJs("window.onRecordingStopped && window.onRecordingStopped('$path')")
        }
        return lastRecordingFile?.absolutePath ?: ""
    }

    // region Backend upload / status

    @JavascriptInterface
    fun uploadAudio(filePath: String): String {
        val file = File(filePath).takeIf { it.exists() } ?: lastRecordingFile
        if (file == null || !file.exists()) {
            evaluateJs("window.onUploadError && window.onUploadError('No recording file found')")
            return ""
        }

        backendApi.uploadAudio(file, object : BackendApi.UploadCallback {
            override fun onSuccess(hash: String) {
                evaluateJs("window.onUploadSuccess && window.onUploadSuccess('$hash')")
            }

            override fun onError(message: String) {
                evaluateJs("window.onUploadError && window.onUploadError('${message.escapeJs()}')")
            }
        })

        return "uploading"
    }

    @JavascriptInterface
    fun queryStatus(hash: String): String {
        backendApi.pollStatus(hash, object : BackendApi.StatusCallback {
            override fun onDone(result: JSONObject) {
                val json = result.toString().escapeJs()
                evaluateJs("window.onStatusResult && window.onStatusResult('$json')")
            }

            override fun onError(message: String) {
                evaluateJs("window.onUploadError && window.onUploadError('${message.escapeJs()}')")
            }
        })
        return "{}"
    }

    // region TTS / pre-recorded audio

    @JavascriptInterface
    fun playTTS(text: String): String {
        mainHandler.post {
            ttsEngine.speak(text) {
                evaluateJs("window.onTtsCompleted && window.onTtsCompleted()")
            }
        }
        return "playing"
    }

    @JavascriptInterface
    fun playPreRecorded(name: String): String {
        mainHandler.post {
            audioPlayer.playAsset(name) {
                evaluateJs("window.onAudioCompleted && window.onAudioCompleted('$name')")
            }
        }
        return "playing"
    }

    @JavascriptInterface
    fun stopAudio(): String {
        mainHandler.post {
            audioPlayer.stop()
            ttsEngine.stop()
        }
        return "stopped"
    }

    // region Storage

    @JavascriptInterface
    fun getUserName(): String {
        return prefs.userTitle ?: ""
    }

    @JavascriptInterface
    fun setUserName(name: String) {
        prefs.userTitle = name
    }

    // region Lifecycle

    fun destroy() {
        wakeWordDetector.destroy()
        audioRecorder.cancel()
        audioPlayer.stop()
        ttsEngine.shutdown()
        statusPollRunnable?.let { mainHandler.removeCallbacks(it) }
    }

    private fun String.escapeJs(): String {
        return this
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
