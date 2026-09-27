package com.android.argosapp.util.session

import android.content.Context
import androidx.core.content.edit
import com.android.argosapp.model.User
import org.json.JSONObject

/**
 * Classe que encapsula a manipulação de SharedReferences para persistência da sessão do usuário
 */
class UserSessionStorage(context: Context) {
    private companion object {
        const val SHARED_PREFERENCES_KEY = "user_session"
        const val BEARER_TOKEN_KEY = "bearerToken"
        const val USER_KEY = "user"
        const val REMEMBER_ME_KEY = "rememberMe"
        const val SESSION_KEY_ALIAS = "argos_session_encryption_key"

        var inMemoryBearerToken: String? = null
        var inMemoryUser: User? = null
    }

    private val preferences = context.getSharedPreferences(
        SHARED_PREFERENCES_KEY,
        Context.MODE_PRIVATE,
    )
    private val cipher = SharedPreferencesCipher(SESSION_KEY_ALIAS)

    init {
        if (inMemoryBearerToken == null) {
            inMemoryBearerToken = cipher.readSecureValue(preferences, BEARER_TOKEN_KEY)
        }
        if (inMemoryUser == null) {
            inMemoryUser = deserializeUser(cipher.readSecureValue(preferences, USER_KEY))
        }
    }

    fun getBearerToken(): String? {
        return inMemoryBearerToken
    }

    fun getUser(): User? {
        return inMemoryUser
    }

    fun updateSession(
        bearerToken: String,
        user: User,
        rememberMe: Boolean,
    ) {
        inMemoryBearerToken = bearerToken
        inMemoryUser = user

        preferences.edit {
            putBoolean(REMEMBER_ME_KEY, rememberMe)

            if (rememberMe) {
                putString(BEARER_TOKEN_KEY, cipher.encrypt(bearerToken))
                putString(USER_KEY, cipher.encrypt(serializeUser(user)))
            } else {
                remove(BEARER_TOKEN_KEY)
                remove(USER_KEY)
            }
        }
    }

    fun isRememberMeEnabled(): Boolean {
        return preferences.getBoolean(REMEMBER_ME_KEY, false)
    }

    fun clearSession() {
        inMemoryBearerToken = null
        inMemoryUser = null

        preferences.edit {
            remove(BEARER_TOKEN_KEY)
            remove(USER_KEY)
            remove(REMEMBER_ME_KEY)
        }
    }

    private fun serializeUser(user: User): String {
        return JSONObject()
            .put("user_name", user.userName)
            .put("user_email", user.userEmail)
            .put("farm_name", user.farmName)
            .toString()
    }

    private fun deserializeUser(serializedUser: String?): User? {
        if (serializedUser.isNullOrBlank()) return null

        return runCatching {
            val json = JSONObject(serializedUser)
            User(
                userName = json.getString("user_name"),
                userEmail = json.getString("user_email"),
                farmName = json.getString("farm_name"),
            )
        }.getOrNull()
    }

}