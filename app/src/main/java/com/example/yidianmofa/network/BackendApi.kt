package com.example.yidianmofa.network

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP client for the Lingbao Flask backend (`tree_mem`).
 *
 * Provides audio upload and status polling. The base URL is read from
 * [baseUrlProvider] so callers can point it at a development PC via
 * `adb reverse` or a LAN server.
 */
class BackendApi(baseUrlProvider: () -> String) {

    private val baseUrlProvider = baseUrlProvider
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    interface UploadCallback {
        fun onSuccess(hash: String)
        fun onError(message: String)
    }

    interface StatusCallback {
        fun onDone(result: JSONObject)
        fun onError(message: String)
    }

    /**
     * Uploads an audio file to `/api/audio_transcribe`.
     *
     * The server is expected to return JSON with a `hash` field.
     */
    fun uploadAudio(file: File, callback: UploadCallback) {
        val base = baseUrlProvider()
        val url = "$base/api/audio_transcribe".toHttpUrl()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                file.name,
                file.asRequestBody("audio/mpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Upload failed", e)
                callback.onError(e.message ?: "Upload failed")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bodyString = it.body?.string() ?: "{}"
                    if (!it.isSuccessful) {
                        callback.onError("HTTP ${it.code}: $bodyString")
                        return
                    }
                    try {
                        val json = JSONObject(bodyString)
                        val hash = json.optString("hash")
                        if (hash.isNotEmpty()) {
                            callback.onSuccess(hash)
                        } else {
                            callback.onError(json.optString("error", "Missing hash in response"))
                        }
                    } catch (e: Exception) {
                        callback.onError("Invalid JSON: $bodyString")
                    }
                }
            }
        })
    }

    /**
     * Polls `/api/upload_status` until the job is done or the max attempts
     * are exhausted.
     */
    fun pollStatus(hash: String, callback: StatusCallback) {
        val base = baseUrlProvider()
        val url = "$base/api/upload_status".toHttpUrl().newBuilder()
            .addQueryParameter("hash", hash)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Status poll failed", e)
                callback.onError(e.message ?: "Status poll failed")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bodyString = it.body?.string() ?: "{}"
                    if (!it.isSuccessful) {
                        callback.onError("HTTP ${it.code}: $bodyString")
                        return
                    }
                    try {
                        callback.onDone(JSONObject(bodyString))
                    } catch (e: Exception) {
                        callback.onError("Invalid JSON: $bodyString")
                    }
                }
            }
        })
    }

    companion object {
        private const val TAG = "BackendApi"
    }
}
