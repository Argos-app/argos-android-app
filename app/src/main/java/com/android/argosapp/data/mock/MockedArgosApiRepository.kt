package com.android.argosapp.data.mock

import com.android.argosapp.data.api.ArgosApiRepository
import com.android.argosapp.dto.ArgosApiAuthResponse
import kotlinx.coroutines.delay

/**
 * Implementação local do repositório da API Argos para desenvolvimento e testes.
 */
class MockedArgosApiRepository(
    private val notFound: Boolean = false,
    firstAccess: Boolean = false,
) : ArgosApiRepository() {
    private var isFirstAccess = firstAccess

    override suspend fun authenticate(firebaseIdToken: String): ArgosApiAuthResponse {
        delay(1000)

        if (notFound) {
            throw NoSuchElementException("User not found.")
        }

        return ArgosApiAuthResponse(
            bearerToken = "mocked-bearer-token",
            userName = "Tobby",
            email = "tobby@argos.com",
            farmName = "Tobby & CIA",
            isFirstAccess = isFirstAccess,
        )
    }

    override suspend fun updateUserAccessStatus(bearerToken: String) {
        delay(1000)

        if (bearerToken.isBlank()) {
            throw SecurityException("Bearer token inválido.")
        }

        isFirstAccess = false
    }
}