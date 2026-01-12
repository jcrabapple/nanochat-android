package com.nanogpt.chat.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {

    private val appContext = context.applicationContext

    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        appContext,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Get the application context
     */
    fun getContext(): Context = appContext

    companion object {
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_LAST_MODEL_ID = "last_model_id"
        private const val KEY_LAST_CONVERSATION_ID = "last_conversation_id"

        // TTS Settings (local only)
        private const val KEY_TTS_MODEL = "tts_model"
        private const val KEY_TTS_VOICE = "tts_voice"
        private const val KEY_TTS_SPEED = "tts_speed"

        // STT Settings (local only)
        private const val KEY_STT_MODEL = "stt_model"

        // Theme Settings (local only)
        private const val KEY_LIGHT_THEME = "light_theme"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_USE_DARK_MODE = "use_dark_mode"
    }

    fun saveSessionToken(token: String) {
        sharedPreferences.edit().putString(KEY_SESSION_TOKEN, token).apply()
    }

    fun getSessionToken(): String? {
        return sharedPreferences.getString(KEY_SESSION_TOKEN, null)
    }

    fun saveBackendUrl(url: String) {
        sharedPreferences.edit().putString(KEY_BACKEND_URL, url).apply()
    }

    fun getBackendUrl(): String? {
        return sharedPreferences.getString(KEY_BACKEND_URL, null)
    }

    fun saveUserId(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return getSessionToken() != null
    }

    fun saveLastModelId(modelId: String) {
        sharedPreferences.edit().putString(KEY_LAST_MODEL_ID, modelId).apply()
    }

    fun getLastModelId(): String? {
        return sharedPreferences.getString(KEY_LAST_MODEL_ID, null)
    }

    fun saveLastConversationId(conversationId: String) {
        sharedPreferences.edit().putString(KEY_LAST_CONVERSATION_ID, conversationId).apply()
    }

    fun getLastConversationId(): String? {
        return sharedPreferences.getString(KEY_LAST_CONVERSATION_ID, null)
    }

    // TTS Settings (local storage only, not synced to backend)
    fun saveTtsModel(model: String?) {
        if (model != null) {
            sharedPreferences.edit().putString(KEY_TTS_MODEL, model).apply()
        } else {
            sharedPreferences.edit().remove(KEY_TTS_MODEL).apply()
        }
    }

    fun getTtsModel(): String? {
        return sharedPreferences.getString(KEY_TTS_MODEL, null)
    }

    fun saveTtsVoice(voice: String?) {
        if (voice != null) {
            sharedPreferences.edit().putString(KEY_TTS_VOICE, voice).apply()
        } else {
            sharedPreferences.edit().remove(KEY_TTS_VOICE).apply()
        }
    }

    fun getTtsVoice(): String? {
        return sharedPreferences.getString(KEY_TTS_VOICE, null)
    }

    fun saveTtsSpeed(speed: Float) {
        sharedPreferences.edit().putFloat(KEY_TTS_SPEED, speed).apply()
    }

    fun getTtsSpeed(): Float {
        return sharedPreferences.getFloat(KEY_TTS_SPEED, 1.0f)
    }

    // STT Settings (local storage only, not synced to backend)
    fun saveSttModel(model: String?) {
        if (model != null) {
            sharedPreferences.edit().putString(KEY_STT_MODEL, model).apply()
        } else {
            sharedPreferences.edit().remove(KEY_STT_MODEL).apply()
        }
    }

    fun getSttModel(): String? {
        return sharedPreferences.getString(KEY_STT_MODEL, null)
    }

    // Theme Settings (local storage only, not synced to backend)
    fun saveLightTheme(theme: String) {
        sharedPreferences.edit().putString(KEY_LIGHT_THEME, theme).apply()
    }

    fun getLightTheme(): String? {
        return sharedPreferences.getString(KEY_LIGHT_THEME, null)
    }

    fun saveDarkTheme(theme: String) {
        sharedPreferences.edit().putString(KEY_DARK_THEME, theme).apply()
    }

    fun getDarkTheme(): String? {
        return sharedPreferences.getString(KEY_DARK_THEME, null)
    }

    fun saveUseDarkMode(useDark: Boolean?) {
        if (useDark != null) {
            sharedPreferences.edit().putBoolean(KEY_USE_DARK_MODE, useDark).apply()
        } else {
            sharedPreferences.edit().remove(KEY_USE_DARK_MODE).apply()
        }
    }

    fun getUseDarkMode(): Boolean? {
        return if (sharedPreferences.contains(KEY_USE_DARK_MODE)) {
            sharedPreferences.getBoolean(KEY_USE_DARK_MODE, false)
        } else {
            null
        }
    }
}
