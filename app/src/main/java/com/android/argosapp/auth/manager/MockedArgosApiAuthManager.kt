package com.android.argosapp.auth.manager

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.model.User
import kotlinx.coroutines.delay

class MockedArgosApiAuthManager(
    private val notFound: Boolean = false,
    private val firstAccess: Boolean = false
) {

    suspend fun authenticate(
        firebaseIdToken: String,
    ): ArgosApiAuthResponse {
        delay(1000)

        if (this.notFound) {
            throw Exception("User not found.")
        }

        return ArgosApiAuthResponse(
            bearerToken = "mocked-bearer-token",
            user = User(
                userName = "Tobby",
                userEmail = "tobby@argos.com",
                farmName = "Tobby & CIA",
            ),
            isFirstAccess = this.firstAccess,
        )
    }
}
