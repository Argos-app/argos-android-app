package com.android.argosapp.data.cache.storage

import android.content.Context
import androidx.core.content.edit
import com.android.argosapp.util.session.SharedPreferencesCipher

class TokenStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val cipher = SharedPreferencesCipher(KEY_ALIAS)

    init {
        if (inMemoryFirebaseIdToken == null) {
            inMemoryFirebaseIdToken = cipher.readSecureValue(preferences, FIREBASE_TOKEN_KEY)
        }
        if (inMemoryArgosApiToken == null) {
            inMemoryArgosApiToken = cipher.readSecureValue(preferences, ARGOS_TOKEN_KEY)
        }
    }

    fun getFirebaseIdToken(): String? = inMemoryFirebaseIdToken

    fun getArgosApiToken(): String? = inMemoryArgosApiToken

    fun isRememberMeEnabled(): Boolean = preferences.getBoolean(REMEMBER_ME_KEY, false)

    fun saveTokens(firebaseIdToken: String, argosApiToken: String, rememberMe: Boolean) {
        inMemoryFirebaseIdToken = firebaseIdToken
        inMemoryArgosApiToken = argosApiToken

        preferences.edit {
            putBoolean(REMEMBER_ME_KEY, rememberMe)
            if (rememberMe) {
                putString(FIREBASE_TOKEN_KEY, cipher.encrypt(firebaseIdToken))
                putString(ARGOS_TOKEN_KEY, cipher.encrypt(argosApiToken))
            } else {
                remove(FIREBASE_TOKEN_KEY)
                remove(ARGOS_TOKEN_KEY)
            }
        }
    }

    fun clearTokens() {
        inMemoryFirebaseIdToken = null
        inMemoryArgosApiToken = null
        preferences.edit {
            remove(FIREBASE_TOKEN_KEY)
            remove(ARGOS_TOKEN_KEY)
            remove(REMEMBER_ME_KEY)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "auth_tokens"
        const val FIREBASE_TOKEN_KEY = "firebaseIdToken"
        const val ARGOS_TOKEN_KEY = "argosApiToken"
        const val REMEMBER_ME_KEY = "rememberMe"
        const val KEY_ALIAS = "argos_session_encryption_key"

        @Volatile
        var inMemoryFirebaseIdToken: String? = null

        @Volatile
        var inMemoryArgosApiToken: String? = null
    }
}
