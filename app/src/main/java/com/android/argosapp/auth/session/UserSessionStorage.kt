package com.android.argosapp.auth.session

import android.content.Context
import androidx.core.content.edit

/**
 * Classe que encapsula a manipulação de SharedReferences para persistência da sessão do usuário
 */
class UserSessionStorage(context: Context) {
    private companion object {
        const val SHARED_PREFERENCES_KEY = "user_session"
        const val BEARER_TOKEN_KEY = "bearerToken"
        const val REMEMBER_ME_KEY = "rememberMe"

        var inMemoryBearerToken: String? = null
    }

    private val preferences = context.getSharedPreferences(
        SHARED_PREFERENCES_KEY,
        Context.MODE_PRIVATE,
    )

    init {
        if (inMemoryBearerToken == null) {
            inMemoryBearerToken = preferences.getString(BEARER_TOKEN_KEY, null)
        }
    }

    fun getBearerToken(): String? {
        return inMemoryBearerToken
    }

    fun updateBearerToken(bearerToken: String, rememberMe: Boolean) {
        inMemoryBearerToken = bearerToken

        preferences.edit {
            putBoolean(REMEMBER_ME_KEY, rememberMe)

            if (rememberMe) {
                putString(BEARER_TOKEN_KEY, bearerToken)
            } else {
                remove(BEARER_TOKEN_KEY)
            }
        }
    }

    fun isRememberMeEnabled(): Boolean {
        return preferences.getBoolean(REMEMBER_ME_KEY, false)
    }

    fun clearBearerToken() {
        inMemoryBearerToken = null

        preferences.edit {
            remove(BEARER_TOKEN_KEY)
        }
    }
}
