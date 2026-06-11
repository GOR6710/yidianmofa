package com.example.yidianmofa.content

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.yidianmofa.audio.AudioPlayer
import com.example.yidianmofa.audio.AudioRecorder
import com.example.yidianmofa.bridge.LingbaoBridge
import com.example.yidianmofa.network.BackendApi
import com.example.yidianmofa.storage.LingbaoPrefs
import com.example.yidianmofa.tts.TtsEngine
import com.example.yidianmofa.voice.WakeWordDetector

/**
 * Embeds the Lingbao 2D UI inside a WebView and binds the native Android
 * bridge so that browser-only APIs are replaced with native implementations.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LingbaoWebView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val webView = WebView(context)
            val prefs = LingbaoPrefs(context)
            val backendApi = BackendApi { prefs.backendBaseUrl }
            val bridge = LingbaoBridge(
                context = context,
                webView = webView,
                wakeWordDetector = WakeWordDetector(context),
                audioRecorder = AudioRecorder(context),
                audioPlayer = AudioPlayer(context),
                ttsEngine = TtsEngine(context),
                backendApi = backendApi,
                prefs = prefs
            )

            webView.apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = false
                }

                webChromeClient = WebChromeClient()
                addJavascriptInterface(bridge, "LingbaoAndroid")

                loadUrl("file:///android_asset/lingbao/index.html")
            }
        }
    )
}
