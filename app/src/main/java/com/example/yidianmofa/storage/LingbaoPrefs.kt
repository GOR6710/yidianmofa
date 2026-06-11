package com.example.yidianmofa.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight wrapper around SharedPreferences for Lingbao user data.
 *
 * In Phase 2 this replaces the browser's localStorage for the user title.
 */
class LingbaoPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var userTitle: String?
        get() = prefs.getString(KEY_USER_TITLE, null)
        set(value) = prefs.edit().putString(KEY_USER_TITLE, value).apply()

    var backendBaseUrl: String
        get() = prefs.getString(KEY_BACKEND_BASE_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
        set(value) = prefs.edit().putString(KEY_BACKEND_BASE_URL, value).apply()

    companion object {
        private const val PREFS_NAME = "lingbao_prefs"
        private const val KEY_USER_TITLE = "lingbao_user_title"
        private const val KEY_BACKEND_BASE_URL = "lingbao_backend_base_url"
        private const val DEFAULT_BACKEND_URL = "http://localhost:5001"
    }
}
