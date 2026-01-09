package com.nanogpt.chat.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_LAST_MODEL_ID = "last_model_id"
        private const val KEY_LAST_CONVERSATION_ID = "last_conversation_id"
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
}
