package com.android.argosapp.auth.manager

import com.android.argosapp.dto.ArgosApiAuthResponse
import com.android.argosapp.model.User
import kotlinx.coroutines.delay

class MockedArgosApiAuthManager {

    suspend fun authenticate(
        firebaseIdToken: String,
        force_not_found: Boolean = false,
        force_first_access: Boolean = false,
    ): ArgosApiAuthResponse {
        delay(1000)

        if (force_not_found) {
            throw Exception("User not found.")
        }

        return ArgosApiAuthResponse(
            bearerToken = "mocked-bearer-token",
            user = User(
                userName = "Tobby",
                userEmail = "tobby@argos.com",
                farmName = "Tobby & CIA",
            ),
            isFirstAccess = force_first_access,
        )
    }
}
