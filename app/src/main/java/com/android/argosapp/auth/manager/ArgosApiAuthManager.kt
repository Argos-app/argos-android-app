package com.android.argosapp.auth.manager

import com.android.argosapp.data.api.ArgosApiRepository
import com.android.argosapp.auth.session.UserSessionStorage
import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.model.User

/**
 * Centraliza as operações de autenticação da API Argos.
 */
class ArgosApiAuthManager(
    private val argosApiRepository: ArgosApiRepository,
    private val userSessionStorage: UserSessionStorage,
) {
    suspend fun authenticate(
        firebaseIdToken: String,
        rememberMe: Boolean,
    ): ArgosApiAuthResponse {
        val authResponse = argosApiRepository.authenticate(firebaseIdToken)

        userSessionStorage.updateSession(
            bearerToken = authResponse.bearerToken,
            user = User(
                userName = authResponse.userName,
                userEmail = authResponse.email,
                farmName = authResponse.farmName,
            ),
            rememberMe = rememberMe,
        )

        return authResponse
    }

    suspend fun updateUserAccessStatus() {
        argosApiRepository.updateUserAccessStatus(requireBearerToken())
    }

    fun getBearerToken(): String? {
        return userSessionStorage.getBearerToken()
    }

    fun getUser(): User? {
        return userSessionStorage.getUser()
    }

    fun isRememberMeEnabled(): Boolean {
        return userSessionStorage.isRememberMeEnabled()
    }

    fun clearSession() {
        userSessionStorage.clearSession()
    }

    private fun requireBearerToken(): String {
        return requireNotNull(userSessionStorage.getBearerToken()?.takeIf { it.isNotBlank() }) {
            "A sessão do usuário não possui um bearer token válido."
        }
    }
}
