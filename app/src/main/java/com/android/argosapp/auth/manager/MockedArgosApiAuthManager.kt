package com.android.argosapp.auth.manager

import kotlinx.coroutines.delay

class MockedArgosApiAuthManager {

    suspend fun authenticate(firebaseIdToken: String): String {
        delay(1000)

        return "firebaseIdTokenAláArgos"
    }

}