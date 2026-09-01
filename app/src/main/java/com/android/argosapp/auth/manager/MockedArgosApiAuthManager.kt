package com.android.argosapp.auth.manager

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.model.User
import kotlinx.coroutines.delay

/**
 * Classe mocked que simula uma requisição de auth pela api do Argos.
 */
class MockedArgosApiAuthManager(
    private val notFound: Boolean = false,
    firstAccess: Boolean = false,
) {
    private var isFirstAccess = firstAccess

    suspend fun authenticate(
        firebaseIdToken: String,
    ): ArgosApiAuthResponse {
        delay(1000)

        if (notFound) {
            throw NoSuchElementException("User not found.")
        }

        return ArgosApiAuthResponse(
            bearerToken = "mocked-bearer-token",
            user = User(
                userName = "Tobby",
                userEmail = "tobby@argos.com",
                farmName = "Tobby & CIA",
            ),
            isFirstAccess = isFirstAccess,
        )
    }

    /**
     * Simula a confirmação, na API do Argos, de que o usuário concluiu o
     * primeiro acesso após definir a nova senha.
     */
    suspend fun updateUserAccessStatus(bearerToken: String) {
        delay(1000)

        if (bearerToken.isBlank()) {
            throw SecurityException("Bearer token inválido.")
        }

        isFirstAccess = false
    }
}
