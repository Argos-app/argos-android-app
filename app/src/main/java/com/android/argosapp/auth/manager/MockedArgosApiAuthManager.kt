package com.android.argosapp.auth.manager

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.model.User
import com.android.argosapp.util.AppLogger
import java.util.concurrent.CancellationException
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
        AppLogger.debug(AppLogger.AUTH_TAG, "argos_authenticate_started")

        return try {
            delay(1000)

            if (notFound) {
                throw NoSuchElementException("User not found.")
            }

            ArgosApiAuthResponse(
                bearerToken = "mocked-bearer-token",
                user = User(
                    userName = "Tobby",
                    userEmail = "tobby@argos.com",
                    farmName = "Tobby & CIA",
                ),
                isFirstAccess = isFirstAccess,
            ).also {
                AppLogger.info(AppLogger.AUTH_TAG, "argos_authenticate_success")
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppLogger.error(AppLogger.AUTH_TAG, "argos_authenticate_failure", error)
            throw error
        }
    }

    /**
     * Simula a confirmação, na API do Argos, de que o usuário concluiu o
     * primeiro acesso após definir a nova senha.
     */
    suspend fun updateUserAccessStatus(bearerToken: String) {
        AppLogger.debug(AppLogger.FIRST_ACCESS_TAG, "first_access_status_update_started")

        try {
            delay(1000)

            if (bearerToken.isBlank()) {
                throw SecurityException("Bearer token inválido.")
            }

            isFirstAccess = false
            AppLogger.info(AppLogger.FIRST_ACCESS_TAG, "first_access_status_update_success")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppLogger.error(AppLogger.FIRST_ACCESS_TAG, "first_access_status_update_failure", error)
            throw error
        }
    }
}
